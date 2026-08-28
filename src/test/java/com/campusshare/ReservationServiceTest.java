package com.campusshare;

import com.campusshare.dto.CreateReservationDTO;
import com.campusshare.dto.ResourceQueryDTO;
import com.campusshare.entity.Resource;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.ResourceMapper;
import com.campusshare.security.LoginUser;
import com.campusshare.service.ReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 7 单元/集成测试：预约核心业务逻辑。
 * 用虚构 userId（不存在于 user 表，但预约表无外键约束）隔离测试数据；
 * 用随机未来天 + 随机分钟隔离时段，保证测试重复运行互不冲突。
 */
@SpringBootTest
class ReservationServiceTest {

    private static final AtomicInteger DAY_OFFSET = new AtomicInteger(50);

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ResourceMapper resourceMapper;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Long userId) {
        LoginUser loginUser = new LoginUser(userId, "tuser" + userId, "USER");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Long onShelfResourceId() {
        ResourceQueryDTO query = new ResourceQueryDTO();
        query.setStatus(1);
        query.setPage(1);
        query.setSize(1);
        query.setOffset(0);
        List<Resource> list = resourceMapper.selectPage(query);
        return list.get(0).getId();
    }

    private LocalDateTime base() {
        return LocalDateTime.now()
                .plusDays(DAY_OFFSET.getAndIncrement() + ThreadLocalRandom.current().nextInt(365))
                .plusMinutes(ThreadLocalRandom.current().nextInt(24 * 60))
                .withSecond(0).withNano(0);
    }

    private CreateReservationDTO dto(Long resourceId, LocalDateTime start, LocalDateTime end) {
        CreateReservationDTO dto = new CreateReservationDTO();
        dto.setResourceId(resourceId);
        dto.setStartTime(start);
        dto.setEndTime(end);
        return dto;
    }

    @Test
    void createReservation_success() {
        loginAs(9001L);
        LocalDateTime b = base();
        Long id = reservationService.createReservation(dto(onShelfResourceId(), b, b.plusHours(2)));
        assertNotNull(id);
    }

    @Test
    void createReservation_startAfterEnd_rejected() {
        loginAs(9002L);
        LocalDateTime b = base();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.createReservation(dto(onShelfResourceId(), b.plusHours(2), b)));
        assertEquals("开始时间必须早于结束时间", ex.getMessage());
    }

    @Test
    void createReservation_pastTime_rejected() {
        loginAs(9003L);
        LocalDateTime b = LocalDateTime.now().minusHours(1);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.createReservation(dto(onShelfResourceId(), b, b.plusHours(2))));
        assertEquals("预约时间已过去", ex.getMessage());
    }

    @Test
    void createReservation_resourceNotFound_rejected() {
        loginAs(9004L);
        LocalDateTime b = base();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.createReservation(dto(999999L, b, b.plusHours(2))));
        assertEquals("资源不存在", ex.getMessage());
    }

    @Test
    void createReservation_overlapConflict_rejected() {
        Long resourceId = onShelfResourceId();
        LocalDateTime b = base();

        loginAs(9005L);
        reservationService.createReservation(dto(resourceId, b, b.plusHours(2)));

        loginAs(9006L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.createReservation(dto(resourceId, b.plusHours(1), b.plusHours(3))));
        assertEquals("该时间段已被预约", ex.getMessage());
    }

    @Test
    void createReservation_adjacentEndpoints_success() {
        Long resourceId = onShelfResourceId();
        LocalDateTime b = base();

        loginAs(9007L);
        reservationService.createReservation(dto(resourceId, b, b.plusHours(2)));

        loginAs(9008L);
        Long id = reservationService.createReservation(dto(resourceId, b.plusHours(2), b.plusHours(4)));
        assertNotNull(id);
    }

    @Test
    void createReservation_duplicateSubmit_rejected() {
        Long resourceId = onShelfResourceId();
        LocalDateTime b = base();
        loginAs(9009L);

        reservationService.createReservation(dto(resourceId, b, b.plusHours(2)));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.createReservation(dto(resourceId, b, b.plusHours(2))));
        assertTrue("请勿重复提交".equals(ex.getMessage()) || "该时间段已被预约".equals(ex.getMessage()));
    }

    @Test
    void getReservation_otherUser_forbidden() {
        loginAs(9010L);
        LocalDateTime b = base();
        Long id = reservationService.createReservation(dto(onShelfResourceId(), b, b.plusHours(2)));

        loginAs(9011L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.getReservation(id));
        assertEquals(403, ex.getCode());
    }

    @Test
    void cancelReservation_otherUser_forbidden() {
        loginAs(9012L);
        LocalDateTime b = base();
        Long id = reservationService.createReservation(dto(onShelfResourceId(), b, b.plusHours(2)));

        loginAs(9013L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.cancelReservation(id, null));
        assertEquals(403, ex.getCode());
    }

    @Test
    void checkin_cancelledReservation_rejected() {
        loginAs(9014L);
        LocalDateTime b = base();
        Long id = reservationService.createReservation(dto(onShelfResourceId(), b, b.plusHours(2)));
        reservationService.cancelReservation(id, "test");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.checkin(id));
        assertEquals("当前状态不能签到", ex.getMessage());
    }

    @Test
    void complete_confirmedReservation_rejected() {
        loginAs(9015L);
        LocalDateTime b = base();
        Long id = reservationService.createReservation(dto(onShelfResourceId(), b, b.plusHours(2)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.complete(id));
        assertEquals("当前状态不能完成", ex.getMessage());
    }
}
