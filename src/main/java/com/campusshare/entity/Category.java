package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类实体，对应 category 表
 */
@Data
public class Category {

    private Long id;
    private String name;
    private String type;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
