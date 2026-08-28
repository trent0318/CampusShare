package com.campusshare.vo;

import com.campusshare.entity.Review;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class ReviewVO {

    private Long id;
    private Long userId;
    private String username;
    private Long resourceId;
    private String resourceName;
    private Long reservationId;
    private Integer rating;
    private String content;
    private LocalDateTime createTime;

    public static ReviewVO from(Review review) {
        ReviewVO vo = new ReviewVO();
        BeanUtils.copyProperties(review, vo);
        return vo;
    }
}
