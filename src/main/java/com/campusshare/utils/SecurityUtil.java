package com.campusshare.utils;

import com.campusshare.exception.BusinessException;
import com.campusshare.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 SecurityContext 获取当前登录用户，供 Service 层做防越权判断
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static LoginUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            throw new BusinessException(401, "未登录");
        }
        return (LoginUser) authentication.getPrincipal();
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
