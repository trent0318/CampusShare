package com.campusshare.controller.admin;

import com.campusshare.common.Result;
import com.campusshare.dto.AuditResourceDTO;
import com.campusshare.dto.CreateResourceDTO;
import com.campusshare.dto.UpdateResourceDTO;
import com.campusshare.dto.UpdateStatusDTO;
import com.campusshare.service.ResourceService;
import com.campusshare.vo.ResourceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/resources")
@RequiredArgsConstructor
public class AdminResourceController {

    private final ResourceService resourceService;

    @PostMapping
    public Result<Long> createResource(@Valid @RequestBody CreateResourceDTO dto) {
        return Result.success(resourceService.createResource(dto));
    }

    @PutMapping("/{id}")
    public Result<ResourceVO> updateResource(@PathVariable Long id, @Valid @RequestBody UpdateResourceDTO dto) {
        return Result.success(resourceService.updateResource(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusDTO dto) {
        resourceService.changeStatus(id, dto.getStatus());
        return Result.success();
    }

    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditResourceDTO dto) {
        resourceService.audit(id, dto.getApprove(), dto.getReason());
        return Result.success();
    }
}
