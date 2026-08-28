package com.campusshare.service;

import com.campusshare.common.PageResult;
import com.campusshare.dto.CreateReservationDTO;
import com.campusshare.dto.ReservationQueryDTO;
import com.campusshare.vo.ReservationVO;

public interface ReservationService {

    Long createReservation(CreateReservationDTO dto);

    PageResult<ReservationVO> listMine(String status, int page, int size);

    ReservationVO getReservation(Long id);

    void cancelReservation(Long id, String reason);

    void checkin(Long id);

    void complete(Long id);

    PageResult<ReservationVO> pageReservations(ReservationQueryDTO query);
}
