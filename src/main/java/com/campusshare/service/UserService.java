package com.campusshare.service;

import com.campusshare.common.PageResult;
import com.campusshare.dto.ChangePasswordDTO;
import com.campusshare.dto.UpdateProfileDTO;
import com.campusshare.vo.UserVO;

public interface UserService {

    UserVO updateProfile(UpdateProfileDTO dto);

    void updatePassword(ChangePasswordDTO dto);

    PageResult<UserVO> pageUsers(int page, int size);

    void updateStatus(Long id, Integer status);
}
