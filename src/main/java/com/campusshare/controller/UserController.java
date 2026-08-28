package com.campusshare.controller;

import com.campusshare.common.Result;
import com.campusshare.dto.ChangePasswordDTO;
import com.campusshare.dto.UpdateProfileDTO;
import com.campusshare.service.UserService;
import com.campusshare.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        return Result.success(userService.updateProfile(dto));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.updatePassword(dto);
        return Result.success();
    }
}
