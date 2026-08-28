package com.campusshare.service.impl;

import com.campusshare.entity.SystemConfig;
import com.campusshare.mapper.SystemConfigMapper;
import com.campusshare.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;

    @Override
    public int getIntValue(String key, int defaultValue) {
        SystemConfig config = systemConfigMapper.selectByKey(key);
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(config.getConfigValue().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
