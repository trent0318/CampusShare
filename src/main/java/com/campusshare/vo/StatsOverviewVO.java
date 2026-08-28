package com.campusshare.vo;

import lombok.Data;

/**
 * 管理端总览统计：资源数 / 用户数 / 预约数 / 完成数 / 取消数
 */
@Data
public class StatsOverviewVO {

    private Long resourceCount;
    private Long userCount;
    private Long reservationCount;
    private Long completedCount;
    private Long cancelledCount;
}
