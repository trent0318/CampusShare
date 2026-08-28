package com.campusshare.service;

import com.campusshare.dto.LoginDTO;
import com.campusshare.dto.RegisterDTO;
import com.campusshare.vo.LoginVO;
import com.campusshare.vo.UserVO;

public interface AuthService {

    void register(RegisterDTO dto);

    LoginVO login(LoginDTO dto);

    UserVO me();
}
