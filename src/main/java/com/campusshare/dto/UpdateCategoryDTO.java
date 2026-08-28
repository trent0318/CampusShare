package com.campusshare.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCategoryDTO {

    @Size(max = 50, message = "分类名最长 50 字符")
    private String name;

    private String type;

    private Integer sort;

    private Integer status;
}
