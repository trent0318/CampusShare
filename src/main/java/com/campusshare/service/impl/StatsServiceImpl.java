package com.campusshare.service.impl;

import com.campusshare.mapper.StatsMapper;
import com.campusshare.service.StatsService;
import com.campusshare.vo.HotResourceVO;
import com.campusshare.vo.StatsOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsMapper statsMapper;

    @Override
    public StatsOverviewVO overview() {
        StatsOverviewVO vo = new StatsOverviewVO();
        vo.setResourceCount(statsMapper.countResources());
        vo.setUserCount(statsMapper.countUsers());
        vo.setReservationCount(statsMapper.countReservations());
        vo.setCompletedCount(statsMapper.countReservationsByStatus("COMPLETED"));
        vo.setCancelledCount(statsMapper.countReservationsByStatus("CANCELLED"));
        return vo;
    }

    @Override
    public List<HotResourceVO> hotResources(int limit) {
        if (limit < 1) {
            limit = 10;
        }
        if (limit > 100) {
            limit = 100;
        }
        return statsMapper.selectHotResources(limit);
    }
}
