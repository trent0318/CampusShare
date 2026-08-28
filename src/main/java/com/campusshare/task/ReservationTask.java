package com.campusshare.task;

import com.campusshare.entity.Reservation;
import com.campusshare.mapper.ReservationMapper;
import com.campusshare.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 预约定时任务（Phase 6）：
 * 1. 超时未签到 → EXPIRED（释放时段名额）
 * 2. 使用中到期 → COMPLETED（释放时段名额）
 * 两条 SQL 的 WHERE 都带状态条件，重复执行也幂等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationTask {

    private static final String SLOT_KEY_PREFIX = "campusshare:reserve:slots:";

    private final ReservationMapper reservationMapper;
    private final SystemConfigService systemConfigService;
    private final StringRedisTemplate stringRedisTemplate;

    /** 每分钟扫描：CONFIRMED 且超过「开始时间 + 宽限期」仍未签到 → EXPIRED */
    @Scheduled(fixedDelay = 60000)
    public void expireNoShow() {
        int graceMinutes = systemConfigService.getIntValue("checkin_grace_minutes", 30);
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(graceMinutes);
        List<Reservation> candidates = reservationMapper.selectExpiredCandidates(deadline);
        for (Reservation r : candidates) {
            int rows = reservationMapper.expireById(r.getId());
            if (rows > 0) {
                releaseSlot(r.getResourceId(), r.getStartTime());
                log.info("预约 {} 超时未签到，已转 EXPIRED", r.getId());
            }
        }
    }

    /** 每分钟扫描：IN_USE 且已过结束时间 → COMPLETED */
    @Scheduled(fixedDelay = 60000)
    public void autoComplete() {
        List<Reservation> candidates = reservationMapper.selectFinishedCandidates(LocalDateTime.now());
        for (Reservation r : candidates) {
            int rows = reservationMapper.completeById(r.getId());
            if (rows > 0) {
                releaseSlot(r.getResourceId(), r.getStartTime());
                log.info("预约 {} 已到结束时间，自动转 COMPLETED", r.getId());
            }
        }
    }

    /** 释放 Redis 时段占位（和 ReservationServiceImpl 的 member 格式保持一致） */
    private void releaseSlot(Long resourceId, LocalDateTime startTime) {
        try {
            stringRedisTemplate.opsForZSet().remove(SLOT_KEY_PREFIX + resourceId, String.valueOf(toEpochMilli(startTime)));
        } catch (Exception e) {
            log.warn("释放 Redis 占位失败: resourceId={}", resourceId, e);
        }
    }

    private long toEpochMilli(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
