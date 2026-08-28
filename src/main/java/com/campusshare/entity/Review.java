package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价实体，对应 review 表
 */
@Data
public class Review {

    private Long id;
    private Long userId;
    private Long resourceId;
    private Long reservationId;
    private Integer rating;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 关联查询字段（非表字段），由 JOIN user 填充 */
    private String username;

    /** 关联查询字段（非表字段），由 JOIN resource 填充 */
    private String resourceName;
}
