package com.campusshare.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileDTO {

    @Size(max = 50, message = "昵称最长 50 字符")
    private String nickname;

    @Size(max = 255, message = "头像地址过长")
    private String avatar;

    @Size(max = 20, message = "手机号最长 20 字符")
    private String phone;
}
