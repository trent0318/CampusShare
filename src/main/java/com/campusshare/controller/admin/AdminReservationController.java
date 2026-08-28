package com.campusshare.controller.admin;

import com.campusshare.common.PageResult;
import com.campusshare.common.Result;
import com.campusshare.dto.ReservationQueryDTO;
import com.campusshare.service.ReservationService;
import com.campusshare.vo.ReservationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
public class AdminReservationController {

    private final ReservationService reservationService;

    @GetMapping
    public Result<PageResult<ReservationVO>> pageReservations(ReservationQueryDTO query) {
        return Result.success(reservationService.pageReservations(query));
    }
}
