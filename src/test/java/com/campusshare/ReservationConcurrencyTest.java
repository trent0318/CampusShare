package com.campusshare;

import com.campusshare.dto.CreateReservationDTO;
import com.campusshare.dto.ResourceQueryDTO;
import com.campusshare.entity.Resource;
import com.campusshare.mapper.ResourceMapper;
import com.campusshare.security.LoginUser;
import com.campusshare.service.ReservationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Phase 5/7 并发验收测试：
 * 1 个名额的资源，N 个并发请求预约同一时段，必须恰好 1 人成功，
 * 其余 N-1 人被 Redis Lua 原子预检 / MySQL 冲突检测拦下。
 *
 * <p>隔离策略：
 * <ul>
 *   <li>随机未来天 + 随机分钟生成时段，避免多次运行的时段冲突；</li>
 *   <li>按当前毫秒生成独立 userId 段，避免跨运行累积“有效预约”触发数量上限；</li>
 *   <li>N 取 100 / 500 / 1000，对应设计文档压测要求。</li>
 * </ul>
 */
@SpringBootTest
class ReservationConcurrencyTest {

    private static final AtomicInteger DAY_OFFSET = new AtomicInteger(0);

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ResourceMapper resourceMapper;

    @ParameterizedTest
    @ValueSource(ints = {100, 500, 1000})
    void oneQuotaResource_NConcurrentReservations_onlyOneSucceeds(int total) throws Exception {
        // 1. 找一个已上架的资源作为预约目标
        ResourceQueryDTO query = new ResourceQueryDTO();
        query.setStatus(1);
        query.setPage(1);
        query.setSize(1);
        query.setOffset(0);
        List<Resource> resources = resourceMapper.selectPage(query);
        assertFalse(resources.isEmpty(), "测试需要一个已上架的资源，请先上架资源");

        Long resourceId = resources.get(0).getId();
        // 随机未来时段，N 个线程共用完全相同的起止时间，才会触发时段冲突；
        // 随机天 + 随机分钟保证重复运行不撞历史时段，DAY_OFFSET 保证本次运行内三个档位互不重叠
        LocalDateTime startTime = LocalDateTime.now()
                .plusDays(30 + DAY_OFFSET.getAndIncrement() + ThreadLocalRandom.current().nextInt(365))
                .plusMinutes(ThreadLocalRandom.current().nextInt(24 * 60))
                .withSecond(0).withNano(0);
        LocalDateTime endTime = startTime.plusHours(2);

        // 按当前毫秒生成独立 userId 段：本次运行内各线程 userId 唯一，跨运行也不会重复
        long userIdBase = System.currentTimeMillis() * 1000L;

        ExecutorService pool = Executors.newFixedThreadPool(total);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(total);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        for (int i = 0; i < total; i++) {
            final long userId = userIdBase + i;
            pool.submit(() -> {
                try {
                    startGate.await();
                    CreateReservationDTO dto = new CreateReservationDTO();
                    dto.setResourceId(resourceId);
                    dto.setStartTime(startTime);
                    dto.setEndTime(endTime);
                    dto.setRemark("并发测试");

                    LoginUser loginUser = new LoginUser(userId, "tuser" + userId, "USER");
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    try {
                        Long id = reservationService.createReservation(dto);
                        if (id != null) {
                            success.incrementAndGet();
                        } else {
                            fail.incrementAndGet();
                        }
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                } catch (Exception e) {
                    fail.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        doneGate.await();
        pool.shutdown();

        System.out.println("=== 并发预约验收 N=" + total + "：成功=" + success.get() + "，失败=" + fail.get() + " ===");
        assertEquals(1, success.get(), total + " 个并发请求预约同一时段，应当恰好 1 人成功");
    }
}
