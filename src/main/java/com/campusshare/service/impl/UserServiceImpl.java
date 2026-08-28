package com.campusshare.service.impl;

import com.campusshare.common.PageResult;
import com.campusshare.dto.ChangePasswordDTO;
import com.campusshare.dto.UpdateProfileDTO;
import com.campusshare.entity.User;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.UserMapper;
import com.campusshare.service.OperationLogService;
import com.campusshare.service.UserService;
import com.campusshare.utils.SecurityUtil;
import com.campusshare.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

    @Override
    public UserVO updateProfile(UpdateProfileDTO dto) {
        Long id = SecurityUtil.getCurrentUserId();
        User user = new User();
        user.setId(id);
        user.setNickname(dto.getNickname());
        user.setAvatar(dto.getAvatar());
        user.setPhone(dto.getPhone());
        userMapper.updateProfile(user);
        return UserVO.from(userMapper.selectById(id));
    }

    @Override
    public void updatePassword(ChangePasswordDTO dto) {
        Long id = SecurityUtil.getCurrentUserId();
        String username = SecurityUtil.getCurrentUser().getUsername();
        User user = userMapper.selectByUsername(username);
        if (user == null || !passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        userMapper.updatePassword(id, passwordEncoder.encode(dto.getNewPassword()));
    }

    @Override
    public PageResult<UserVO> pageUsers(int page, int size) {
        long total = userMapper.countAll();
        int offset = (page - 1) * size;
        List<User> users = userMapper.selectPage(offset, size);
        List<UserVO> vos = users.stream().map(UserVO::from).toList();
        return PageResult.of(total, vos);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值非法，只能是 0 或 1");
        }
        int rows = userMapper.updateStatus(id, status);
        if (rows == 0) {
            throw new BusinessException("用户不存在");
        }
        operationLogService.record("UPDATE_USER_STATUS", "USER", id, status == 1 ? "启用" : "禁用");
    }
}
