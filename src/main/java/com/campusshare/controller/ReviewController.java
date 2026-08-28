package com.campusshare.controller;

import com.campusshare.common.PageResult;
import com.campusshare.common.Result;
import com.campusshare.dto.CreateReviewDTO;
import com.campusshare.service.ReviewService;
import com.campusshare.vo.ReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/reviews")
    public Result<Long> createReview(@Valid @RequestBody CreateReviewDTO dto) {
        return Result.success(reviewService.createReview(dto));
    }

    @GetMapping("/resources/{resourceId}/reviews")
    public Result<PageResult<ReviewVO>> listByResource(@PathVariable Long resourceId,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return Result.success(reviewService.listByResource(resourceId, page, size));
    }
}
