package com.campusshare.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuditResourceDTO {

    @NotNull(message = "审核结果不能为空")
    private Boolean approve;

    private String reason;
}
