package com.campusshare.service.impl;

import com.campusshare.common.PageResult;
import com.campusshare.dto.CreateReservationDTO;
import com.campusshare.dto.ReservationQueryDTO;
import com.campusshare.entity.Reservation;
import com.campusshare.entity.Resource;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.ReservationMapper;
import com.campusshare.mapper.ResourceMapper;
import com.campusshare.service.ReservationService;
import com.campusshare.service.SystemConfigService;
import com.campusshare.utils.SecurityUtil;
import com.campusshare.vo.ReservationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_IN_USE = "IN_USE";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final int RESOURCE_ON_SHELF = 1;

    /** Redis Key 前缀 */
    private static final String DEDUP_KEY_PREFIX = "campusshare:dedup:reserve:";
    private static final String SLOT_KEY_PREFIX = "campusshare:reserve:slots:";
    private static final String RATE_LIMIT_KEY_PREFIX = "campusshare:ratelimit:";

    /** 时段占位 TTL（秒）：Redis 只是加速层，MySQL 才是最终事实 */
    private static final long SLOT_TTL_SECONDS = 86400L;

    /** 令牌桶参数：容量 10，每秒补充 1 个 */
    private static final long RATE_LIMIT_CAPACITY = 10L;
    private static final double RATE_LIMIT_REFILL_PER_SECOND = 1.0;

    /**
     * 并发预约原子预检（Lua）：
     * 用一个 ZSET 存该资源已占用的时段（member=startTime，score=endTime）。
     * 区间重叠算法同 Phase 4：s1 < e2 且 s2 < e1。
     * 先查所有 end > startTime 的区间（score > startTime），再逐个判断 start < endTime，
     * 有重叠返回 0；无重叠则 ZADD 占用并返回 1。整个过程单线程原子执行。
     */
    private static final String RESERVE_SLOT_LUA = """
            local key = KEYS[1]
            local startTime = tonumber(ARGV[1])
            local endTime = tonumber(ARGV[2])
            local ttl = tonumber(ARGV[3])
            local candidates = redis.call('ZRANGEBYSCORE', key, '(' .. startTime, '+inf')
            for _, member in ipairs(candidates) do
                if tonumber(member) < endTime then
                    return 0
                end
            end
            redis.call('ZADD', key, endTime, startTime)
            redis.call('EXPIRE', key, ttl)
            return 1
            """;

    /**
     * 令牌桶限流（Lua）：
     * HASH 存 tokens（当前令牌数）和 last（上次补充时间），
     * 每次先按流逝时间补充令牌，有令牌则扣 1 放行，无令牌拒绝。
     */
    private static final String TOKEN_BUCKET_LUA = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refillRate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local data = redis.call('HMGET', key, 'tokens', 'last')
            local tokens = tonumber(data[1])
            local last = tonumber(data[2])
            if tokens == nil then
                tokens = capacity
                last = now
            end
            local elapsed = now - last
            tokens = math.min(capacity, tokens + elapsed * refillRate)
            if tokens >= 1 then
                tokens = tokens - 1
                redis.call('HMSET', key, 'tokens', tokens, 'last', now)
                redis.call('EXPIRE', key, 60)
                return 1
            end
            redis.call('HMSET', key, 'tokens', tokens, 'last', now)
            redis.call('EXPIRE', key, 60)
            return 0
            """;

    private static final DefaultRedisScript<Long> RESERVE_SLOT_SCRIPT =
            new DefaultRedisScript<>(RESERVE_SLOT_LUA, Long.class);
    private static final DefaultRedisScript<Long> TOKEN_BUCKET_SCRIPT =
            new DefaultRedisScript<>(TOKEN_BUCKET_LUA, Long.class);

    private final ReservationMapper reservationMapper;
    private final ResourceMapper resourceMapper;
    private final SystemConfigService systemConfigService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public Long createReservation(CreateReservationDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long resourceId = dto.getResourceId();
        LocalDateTime startTime = dto.getStartTime();
        LocalDateTime endTime = dto.getEndTime();

        // 1. 时间合法性
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException("开始时间必须早于结束时间");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("预约时间已过去");
        }

        // 2. 接口限流（令牌桶）：Redis 挂了降级放行
        checkRateLimit(userId);

        // 3. 防重复提交（SETNX + TTL）：同一用户短时间连点，直接拒绝
        String dedupKey = DEDUP_KEY_PREFIX + userId + ":" + resourceId + ":" + startTime + ":" + endTime;
        if (!tryAcquireDedup(dedupKey)) {
            throw new BusinessException("请勿重复提交");
        }

        // 4. Redis Lua 原子预检：快速判断该时段是否已被占用（区间重叠）
        if (!tryAcquireSlot(resourceId, startTime, endTime)) {
            throw new BusinessException("该时间段已被预约");
        }

        // 5. MySQL 事务兜底（Phase 4 那套）：真正持久化，任何失败回滚 Redis 占位
        try {
            // 5.1 锁资源行（FOR UPDATE），串行化同一资源的并发预约
            Resource resource = resourceMapper.selectByIdForUpdate(resourceId);
            if (resource == null) {
                throw new BusinessException("资源不存在");
            }
            if (resource.getStatus() == null || resource.getStatus() != RESOURCE_ON_SHELF) {
                throw new BusinessException("资源未上架，无法预约");
            }

            // 5.2 时间冲突检测（区间重叠算法，MySQL 是最终权威判断）
            int conflictCount = reservationMapper.countConflict(resourceId, startTime, endTime);
            if (conflictCount > 0) {
                throw new BusinessException("该时间段已被预约");
            }

            // 5.3 用户有效预约数量限制
            int activeCount = reservationMapper.countActiveByUser(userId);
            int maxActive = systemConfigService.getIntValue("max_active_reservations", 3);
            if (activeCount >= maxActive) {
                throw new BusinessException("预约数量已达上限");
            }

            // 5.4 创建预约（默认自动确认）
            Reservation reservation = new Reservation();
            reservation.setUserId(userId);
            reservation.setResourceId(resourceId);
            reservation.setReserveDate(startTime.toLocalDate());
            reservation.setStartTime(startTime);
            reservation.setEndTime(endTime);
            reservation.setStatus(STATUS_CONFIRMED);
            reservation.setRemark(dto.getRemark());
            try {
                reservationMapper.insert(reservation);
            } catch (DuplicateKeyException e) {
                throw new BusinessException("重复预约");
            }
            return reservation.getId();
        } catch (RuntimeException e) {
            // MySQL 失败，回滚 Redis 占位，避免误占用时段
            releaseSlot(resourceId, startTime, endTime);
            throw e;
        }
    }

    @Override
    public PageResult<ReservationVO> listMine(String status, int page, int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }
        int offset = (page - 1) * size;
        long total = reservationMapper.countMine(userId, status);
        List<Reservation> list = reservationMapper.selectMine(userId, status, offset, size);
        return PageResult.of(total, list.stream().map(ReservationVO::from).toList());
    }

    @Override
    public ReservationVO getReservation(Long id) {
        Reservation r = reservationMapper.selectById(id);
        if (r == null) {
            throw new BusinessException("预约不存在");
        }
        Long userId = SecurityUtil.getCurrentUserId();
        if (!r.getUserId().equals(userId) && !isAdmin()) {
            throw new BusinessException(403, "无权查看该预约");
        }
        return ReservationVO.from(r);
    }

    @Override
    public void cancelReservation(Long id, String reason) {
        Reservation r = reservationMapper.selectById(id);
        if (r == null) {
            throw new BusinessException("预约不存在");
        }
        Long userId = SecurityUtil.getCurrentUserId();
        if (!r.getUserId().equals(userId) && !isAdmin()) {
            throw new BusinessException(403, "无权取消该预约");
        }
        if (!STATUS_CONFIRMED.equals(r.getStatus())) {
            throw new BusinessException("当前状态不能取消");
        }
        if (r.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("预约已开始，无法取消");
        }
        reservationMapper.updateStatus(id, STATUS_CANCELLED, null, null, reason);
        // 释放 Redis 占位，让该时段可被再次预约
        releaseSlot(r.getResourceId(), r.getStartTime(), r.getEndTime());
    }

    @Override
    public void checkin(Long id) {
        Reservation r = reservationMapper.selectById(id);
        if (r == null) {
            throw new BusinessException("预约不存在");
        }
        Long userId = SecurityUtil.getCurrentUserId();
        if (!r.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能签到自己的预约");
        }
        if (!STATUS_CONFIRMED.equals(r.getStatus())) {
            throw new BusinessException("当前状态不能签到");
        }
        LocalDateTime now = LocalDateTime.now();
        int earlyMinutes = systemConfigService.getIntValue("checkin_early_minutes", 15);
        int graceMinutes = systemConfigService.getIntValue("checkin_grace_minutes", 30);
        LocalDateTime earliest = r.getStartTime().minusMinutes(earlyMinutes);
        LocalDateTime latest = r.getStartTime().plusMinutes(graceMinutes);
        if (now.isBefore(earliest)) {
            throw new BusinessException("未到签到时间");
        }
        if (now.isAfter(latest)) {
            throw new BusinessException("已过签到时间");
        }
        reservationMapper.updateStatus(id, STATUS_IN_USE, now, null, null);
    }

    @Override
    public void complete(Long id) {
        Reservation r = reservationMapper.selectById(id);
        if (r == null) {
            throw new BusinessException("预约不存在");
        }
        Long userId = SecurityUtil.getCurrentUserId();
        if (!r.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能完成自己的预约");
        }
        if (!STATUS_IN_USE.equals(r.getStatus())) {
            throw new BusinessException("当前状态不能完成");
        }
        reservationMapper.updateStatus(id, STATUS_COMPLETED, null, LocalDateTime.now(), null);
        // 完成后释放占位（剩余时段不再被占用）
        releaseSlot(r.getResourceId(), r.getStartTime(), r.getEndTime());
    }

    @Override
    public PageResult<ReservationVO> pageReservations(ReservationQueryDTO query) {
        if (query.getPage() < 1) {
            query.setPage(1);
        }
        if (query.getSize() < 1) {
            query.setSize(10);
        }
        query.setOffset((query.getPage() - 1) * query.getSize());
        long total = reservationMapper.count(query);
        List<Reservation> list = reservationMapper.selectPage(query);
        return PageResult.of(total, list.stream().map(ReservationVO::from).toList());
    }

    /** 令牌桶限流：拒绝抛 429，Redis 异常时降级放行 */
    private void checkRateLimit(Long userId) {
        try {
            String key = RATE_LIMIT_KEY_PREFIX + userId + ":reserve";
            long now = System.currentTimeMillis() / 1000;
            Long result = stringRedisTemplate.execute(TOKEN_BUCKET_SCRIPT, List.of(key),
                    String.valueOf(RATE_LIMIT_CAPACITY),
                    String.valueOf(RATE_LIMIT_REFILL_PER_SECOND),
                    String.valueOf(now));
            if (result == null || result == 0L) {
                throw new BusinessException(429, "请求过于频繁，请稍后再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis 限流异常，降级放行: userId={}", userId, e);
        }
    }

    /** 防重复提交：SETNX 成功返回 true，已存在返回 false，Redis 异常降级放行 */
    private boolean tryAcquireDedup(String key) {
        try {
            Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(3));
            return !Boolean.FALSE.equals(first);
        } catch (Exception e) {
            log.warn("Redis 防重异常，降级放行: {}", key, e);
            return true;
        }
    }

    /** Lua 原子预检：占用成功返回 true，冲突返回 false，Redis 异常降级放行（由 MySQL 兜底） */
    private boolean tryAcquireSlot(Long resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            String key = SLOT_KEY_PREFIX + resourceId;
            long start = toEpochMilli(startTime);
            long end = toEpochMilli(endTime);
            Long result = stringRedisTemplate.execute(RESERVE_SLOT_SCRIPT, List.of(key),
                    String.valueOf(start), String.valueOf(end), String.valueOf(SLOT_TTL_SECONDS));
            return result != null && result == 1L;
        } catch (Exception e) {
            log.warn("Redis 并发预检异常，降级放行（由 MySQL 兜底）: resourceId={}", resourceId, e);
            return true;
        }
    }

    /** 释放占位：MySQL 失败回滚、取消、完成时调用 */
    private void releaseSlot(Long resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            stringRedisTemplate.opsForZSet().remove(SLOT_KEY_PREFIX + resourceId, String.valueOf(toEpochMilli(startTime)));
        } catch (Exception e) {
            log.warn("释放 Redis 占位失败: resourceId={}", resourceId, e);
        }
    }

    private long toEpochMilli(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private boolean isAdmin() {
        return "ADMIN".equals(SecurityUtil.getCurrentUser().getRole());
    }
}
