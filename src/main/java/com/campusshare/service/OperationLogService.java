package com.campusshare.service;

public interface OperationLogService {

    /** 记录一条管理员操作日志。失败仅告警，不影响主流程 */
    void record(String operationType, String targetType, Long targetId, String detail);
}
