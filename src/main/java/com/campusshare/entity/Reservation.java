package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约实体，对应 reservation 表（核心）
 */
@Data
public class Reservation {

    private Long id;
    private Long userId;
    private Long resourceId;
    private LocalDate reserveDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private LocalDateTime checkinTime;
    private LocalDateTime finishTime;
    private String cancelReason;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 关联查询字段（非表字段），由 JOIN resource 查询填充 */
    private String resourceName;

    /** 关联查询字段（非表字段），由 JOIN user 查询填充，管理端使用 */
    private String username;
}
