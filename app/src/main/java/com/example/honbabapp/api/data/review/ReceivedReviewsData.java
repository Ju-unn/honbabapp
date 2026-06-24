package com.example.honbabapp.api.data.review;

import java.util.List;

/**
 * 받은 후기 목록 조회 응답 DTO
 * GET /api/review/received.php 의 data 부분
 *
 * 응답 예시:
 * {
 *   "reviews": [
 *     {
 *       "review_id": 12,
 *       "rating": 4,
 *       "content": "좋았어요",
 *       "created_at": "2026-06-24 19:00:00"
 *     }
 *   ]
 * }
 *
 * reviews 가 빈 배열이면 받은 후기가 없는 상태.
 */
public class ReceivedReviewsData {

    private List<ReceivedReview> reviews;

    public ReceivedReviewsData() {}

    public List<ReceivedReview> getReviews() {
        return reviews;
    }

    public void setReviews(List<ReceivedReview> reviews) {
        this.reviews = reviews;
    }
}