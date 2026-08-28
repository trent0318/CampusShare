package com.campusshare.vo;

import com.campusshare.entity.Category;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class CategoryVO {

    private Long id;
    private String name;
    private String type;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;

    public static CategoryVO from(Category category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }
}
