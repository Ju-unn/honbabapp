package com.example.honbabapp.api.data.review;

import java.util.List;

/**
 * 내가 쓴 후기 목록 조회 응답 DTO
 * GET /api/review/written.php 의 data 부분
 *
 * reviews 가 빈 배열이면 작성한 후기가 없는 상태.
 */
public class WrittenReviewsData {

    private List<WrittenReview> reviews;

    public WrittenReviewsData() {}

    public List<WrittenReview> getReviews() {
        return reviews;
    }

    public void setReviews(List<WrittenReview> reviews) {
        this.reviews = reviews;
    }
}
