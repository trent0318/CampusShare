package com.campusshare.vo;

import lombok.Data;

/**
 * 热门资源排行项
 */
@Data
public class HotResourceVO {

    private Long resourceId;
    private String resourceName;
    private Long reservationCount;
}
