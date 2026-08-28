package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源实体，对应 resource 表（物品 ITEM / 场地 VENUE）
 */
@Data
public class Resource {

    private Long id;
    private String name;
    private Long categoryId;
    private String type;
    private String description;
    private String image;
    private String location;
    private Integer totalCount;
    private Integer status;
    private String auditReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 关联查询字段（非表字段），由 JOIN category 查询填充 */
    private String categoryName;
}
