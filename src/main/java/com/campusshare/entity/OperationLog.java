package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体，对应 operation_log 表（管理员操作可追溯）
 */
@Data
public class OperationLog {

    private Long id;
    private Long userId;
    private String username;
    private String operationType;
    private String targetType;
    private Long targetId;
    private String detail;
    private String result;
    private LocalDateTime createTime;
}
