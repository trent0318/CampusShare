package com.campusshare.dto;

import lombok.Data;

@Data
public class ReservationQueryDTO {

    private Long userId;
    private Long resourceId;
    private String status;
    private int page = 1;
    private int size = 10;

    /** 分页偏移量，由 Service 层计算，前端无需传 */
    private int offset;
}
