package com.campusshare.mapper;

import com.campusshare.dto.ReservationQueryDTO;
import com.campusshare.entity.Reservation;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationMapper {

    int insert(Reservation reservation);

    Reservation selectById(@Param("id") Long id);

    int countConflict(@Param("resourceId") Long resourceId,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    int countActiveByUser(@Param("userId") Long userId);

    List<Reservation> selectMine(@Param("userId") Long userId,
                                 @Param("status") String status,
                                 @Param("offset") int offset,
                                 @Param("size") int size);

    long countMine(@Param("userId") Long userId, @Param("status") String status);

    List<Reservation> selectPage(ReservationQueryDTO query);

    long count(ReservationQueryDTO query);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("checkinTime") LocalDateTime checkinTime,
                     @Param("finishTime") LocalDateTime finishTime,
                     @Param("cancelReason") String cancelReason);

    /** 定时任务：查超时未签到的预约（CONFIRMED 且开始时间早于 deadline） */
    List<Reservation> selectExpiredCandidates(@Param("deadline") LocalDateTime deadline);

    /** 定时任务：查到期的使用中预约（IN_USE 且结束时间早于 now） */
    List<Reservation> selectFinishedCandidates(@Param("now") LocalDateTime now);

    /** 定时任务：CONFIRMED → EXPIRED，带状态条件保证幂等 */
    int expireById(@Param("id") Long id);

    /** 定时任务：IN_USE → COMPLETED，带状态条件保证幂等 */
    int completeById(@Param("id") Long id);
}
