package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置实体，对应 system_config 表
 */
@Data
public class SystemConfig {

    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime updateTime;
}
