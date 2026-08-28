package com.campusshare.dto;

import lombok.Data;

@Data
public class ResourceQueryDTO {

    private Long categoryId;
    private String type;
    private String keyword;
    private Integer status;
    private int page = 1;
    private int size = 10;

    /** 分页偏移量，由 Service 层根据 page/size 计算，前端无需传 */
    private int offset;
}
