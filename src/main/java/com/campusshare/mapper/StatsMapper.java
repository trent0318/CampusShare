package com.campusshare.mapper;

import com.campusshare.vo.HotResourceVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StatsMapper {

    long countResources();

    long countUsers();

    long countReservations();

    long countReservationsByStatus(@Param("status") String status);

    List<HotResourceVO> selectHotResources(@Param("limit") int limit);
}
