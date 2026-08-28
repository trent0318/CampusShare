package com.campusshare.vo;

import com.campusshare.entity.Reservation;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReservationVO {

    private Long id;
    private Long userId;
    private String username;
    private Long resourceId;
    private String resourceName;
    private LocalDate reserveDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private LocalDateTime checkinTime;
    private LocalDateTime finishTime;
    private String cancelReason;
    private String remark;
    private LocalDateTime createTime;

    public static ReservationVO from(Reservation reservation) {
        ReservationVO vo = new ReservationVO();
        BeanUtils.copyProperties(reservation, vo);
        return vo;
    }
}
