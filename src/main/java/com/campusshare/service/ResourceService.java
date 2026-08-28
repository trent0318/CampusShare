package com.campusshare.service;

import com.campusshare.common.PageResult;
import com.campusshare.dto.CreateResourceDTO;
import com.campusshare.dto.ResourceQueryDTO;
import com.campusshare.dto.UpdateResourceDTO;
import com.campusshare.vo.ResourceVO;

public interface ResourceService {

    PageResult<ResourceVO> pageResources(ResourceQueryDTO query);

    ResourceVO getResource(Long id);

    Long createResource(CreateResourceDTO dto);

    ResourceVO updateResource(Long id, UpdateResourceDTO dto);

    void deleteResource(Long id);

    void changeStatus(Long id, Integer status);

    void audit(Long id, boolean approve, String reason);
}
