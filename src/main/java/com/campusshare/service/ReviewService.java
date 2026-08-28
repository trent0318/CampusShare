package com.campusshare.service;

import com.campusshare.common.PageResult;
import com.campusshare.dto.CreateReviewDTO;
import com.campusshare.vo.ReviewVO;

public interface ReviewService {

    Long createReview(CreateReviewDTO dto);

    PageResult<ReviewVO> listByResource(Long resourceId, int page, int size);
}
