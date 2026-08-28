package com.campusshare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCategoryDTO {

    @NotBlank(message = "分类名不能为空")
    @Size(max = 50, message = "分类名最长 50 字符")
    private String name;

    @NotBlank(message = "分类类型不能为空")
    private String type;

    private Integer sort;
}
