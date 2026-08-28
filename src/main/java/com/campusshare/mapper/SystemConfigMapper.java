package com.campusshare.mapper;

import com.campusshare.entity.SystemConfig;
import org.apache.ibatis.annotations.Param;

public interface SystemConfigMapper {

    SystemConfig selectByKey(@Param("configKey") String configKey);
}
