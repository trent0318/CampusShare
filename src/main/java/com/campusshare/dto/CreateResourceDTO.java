package com.campusshare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateResourceDTO {

    @NotBlank(message = "资源名不能为空")
    @Size(max = 100, message = "资源名最长 100 字符")
    private String name;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "资源类型不能为空")
    private String type;

    @Size(max = 2000, message = "描述过长")
    private String description;

    @Size(max = 255, message = "封面图地址过长")
    private String image;

    @Size(max = 255, message = "位置过长")
    private String location;

    private Integer totalCount;
}
