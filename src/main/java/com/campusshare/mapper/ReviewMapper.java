package com.campusshare.mapper;

import com.campusshare.entity.Review;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReviewMapper {

    int insert(Review review);

    Review selectByReservationId(@Param("reservationId") Long reservationId);

    List<Review> selectPageByResource(@Param("resourceId") Long resourceId,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    long countByResource(@Param("resourceId") Long resourceId);
}
