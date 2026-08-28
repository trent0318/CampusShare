package com.campusshare.controller;

import com.campusshare.common.PageResult;
import com.campusshare.common.Result;
import com.campusshare.dto.CreateReservationDTO;
import com.campusshare.service.ReservationService;
import com.campusshare.vo.ReservationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public Result<Long> createReservation(@Valid @RequestBody CreateReservationDTO dto) {
        return Result.success(reservationService.createReservation(dto));
    }

    @GetMapping("/mine")
    public Result<PageResult<ReservationVO>> listMine(@RequestParam(required = false) String status,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return Result.success(reservationService.listMine(status, page, size));
    }

    @GetMapping("/{id}")
    public Result<ReservationVO> getReservation(@PathVariable Long id) {
        return Result.success(reservationService.getReservation(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> cancelReservation(@PathVariable Long id, @RequestParam(required = false) String reason) {
        reservationService.cancelReservation(id, reason);
        return Result.success();
    }

    @PutMapping("/{id}/checkin")
    public Result<Void> checkin(@PathVariable Long id) {
        reservationService.checkin(id);
        return Result.success();
    }

    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        reservationService.complete(id);
        return Result.success();
    }
}
