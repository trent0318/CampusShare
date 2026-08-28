package com.campusshare.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，对应数据库 user 表
 */
@Data
public class User {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String phone;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
