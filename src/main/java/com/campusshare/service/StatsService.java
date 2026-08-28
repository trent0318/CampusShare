package com.campusshare.service;

import com.campusshare.vo.HotResourceVO;
import com.campusshare.vo.StatsOverviewVO;

import java.util.List;

public interface StatsService {

    StatsOverviewVO overview();

    List<HotResourceVO> hotResources(int limit);
}
