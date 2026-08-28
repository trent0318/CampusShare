package com.campusshare.service.impl;

import com.campusshare.common.PageResult;
import com.campusshare.dto.CreateReviewDTO;
import com.campusshare.entity.Reservation;
import com.campusshare.entity.Review;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.ReservationMapper;
import com.campusshare.mapper.ReviewMapper;
import com.campusshare.service.ReviewService;
import com.campusshare.utils.SecurityUtil;
import com.campusshare.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final String STATUS_COMPLETED = "COMPLETED";

    private final ReviewMapper reviewMapper;
    private final ReservationMapper reservationMapper;

    @Override
    public Long createReview(CreateReviewDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 1. 预约必须存在、属于当前用户、且已完成
        Reservation reservation = reservationMapper.selectById(dto.getReservationId());
        if (reservation == null) {
            throw new BusinessException("预约不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能评价自己的预约");
        }
        if (!STATUS_COMPLETED.equals(reservation.getStatus())) {
            throw new BusinessException("只有已完成的预约才能评价");
        }

        // 2. 一个预约只能评一次（先查 + 唯一约束双重保证）
        if (reviewMapper.selectByReservationId(dto.getReservationId()) != null) {
            throw new BusinessException("该预约已评价过");
        }

        Review review = new Review();
        review.setUserId(userId);
        review.setResourceId(reservation.getResourceId());
        review.setReservationId(dto.getReservationId());
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("该预约已评价过");
        }
        return review.getId();
    }

    @Override
    public PageResult<ReviewVO> listByResource(Long resourceId, int page, int size) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }
        int offset = (page - 1) * size;
        long total = reviewMapper.countByResource(resourceId);
        List<Review> list = reviewMapper.selectPageByResource(resourceId, offset, size);
        return PageResult.of(total, list.stream().map(ReviewVO::from).toList());
    }
}
