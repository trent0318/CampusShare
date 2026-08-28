package com.campusshare.controller.admin;

import com.campusshare.common.Result;
import com.campusshare.service.StatsService;
import com.campusshare.vo.HotResourceVO;
import com.campusshare.vo.StatsOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public Result<StatsOverviewVO> overview() {
        return Result.success(statsService.overview());
    }

    @GetMapping("/hot-resources")
    public Result<List<HotResourceVO>> hotResources(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(statsService.hotResources(limit));
    }
}
