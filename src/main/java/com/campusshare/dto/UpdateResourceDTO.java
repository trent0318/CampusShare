package com.campusshare.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateResourceDTO {

    @Size(max = 100, message = "资源名最长 100 字符")
    private String name;

    private Long categoryId;

    private String type;

    @Size(max = 2000, message = "描述过长")
    private String description;

    @Size(max = 255, message = "封面图地址过长")
    private String image;

    @Size(max = 255, message = "位置过长")
    private String location;

    private Integer totalCount;
}
