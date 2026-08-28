package com.campusshare.mapper;

import com.campusshare.dto.ResourceQueryDTO;
import com.campusshare.entity.Resource;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ResourceMapper {

    Resource selectById(@Param("id") Long id);

    Resource selectByIdForUpdate(@Param("id") Long id);

    List<Resource> selectPage(ResourceQueryDTO query);

    long count(ResourceQueryDTO query);

    int insert(Resource resource);

    int update(Resource resource);

    int deleteById(@Param("id") Long id);

    long countByCategoryId(@Param("categoryId") Long categoryId);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("auditReason") String auditReason);
}
