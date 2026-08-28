package com.campusshare.vo;

import com.campusshare.entity.Resource;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class ResourceVO {

    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String type;
    private String description;
    private String image;
    private String location;
    private Integer totalCount;
    private Integer status;
    private String auditReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ResourceVO from(Resource resource) {
        ResourceVO vo = new ResourceVO();
        BeanUtils.copyProperties(resource, vo);
        return vo;
    }
}
