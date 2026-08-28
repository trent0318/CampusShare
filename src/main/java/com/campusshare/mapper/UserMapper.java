package com.campusshare.mapper;

import com.campusshare.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {

    User selectById(@Param("id") Long id);

    User selectByUsername(@Param("username") String username);

    int insert(User user);

    long countAll();

    List<User> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    int updateProfile(User user);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
