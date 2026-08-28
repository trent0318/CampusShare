package com.campusshare.controller;

import com.campusshare.common.PageResult;
import com.campusshare.common.Result;
import com.campusshare.dto.ResourceQueryDTO;
import com.campusshare.service.ResourceService;
import com.campusshare.vo.ResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public Result<PageResult<ResourceVO>> pageResources(ResourceQueryDTO query) {
        return Result.success(resourceService.pageResources(query));
    }

    @GetMapping("/{id}")
    public Result<ResourceVO> getResource(@PathVariable Long id) {
        return Result.success(resourceService.getResource(id));
    }
}
