package com.campusshare.mapper;

import com.campusshare.entity.Category;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CategoryMapper {

    List<Category> selectList(@Param("type") String type);

    Category selectById(@Param("id") Long id);

    int insert(Category category);

    int update(Category category);

    int deleteById(@Param("id") Long id);
}
