package com.campusshare.service.impl;

import com.campusshare.entity.OperationLog;
import com.campusshare.mapper.OperationLogMapper;
import com.campusshare.security.LoginUser;
import com.campusshare.service.OperationLogService;
import com.campusshare.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public void record(String operationType, String targetType, Long targetId, String detail) {
        try {
            LoginUser user = SecurityUtil.getCurrentUser();
            OperationLog operationLog = new OperationLog();
            operationLog.setUserId(user.getId());
            operationLog.setUsername(user.getUsername());
            operationLog.setOperationType(operationType);
            operationLog.setTargetType(targetType);
            operationLog.setTargetId(targetId);
            operationLog.setDetail(detail);
            operationLog.setResult("SUCCESS");
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.warn("记录操作日志失败: type={}, targetId={}", operationType, targetId, e);
        }
    }
}
