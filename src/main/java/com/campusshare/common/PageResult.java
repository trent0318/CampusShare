package com.campusshare.common;

import lombok.Data;

import java.util.List;

/**
 * 分页响应：{ total, list }
 */
@Data
public class PageResult<T> {

    private Long total;
    private List<T> list;

    public static <T> PageResult<T> of(Long total, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.total = total;
        result.list = list;
        return result;
    }
}
