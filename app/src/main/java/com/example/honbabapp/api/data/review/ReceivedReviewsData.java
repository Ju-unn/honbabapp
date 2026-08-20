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

    // page/limit으로 호출했을 때만 서버가 채워줌 (기본값 0/false)
    private int page;
    private int limit;
    private int total_count;
    private boolean has_more;

    public ReceivedReviewsData() {}

    public List<ReceivedReview> getReviews() {
        return reviews;
    }

    public void setReviews(List<ReceivedReview> reviews) {
        this.reviews = reviews;
    }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public int getTotal_count() { return total_count; }
    public void setTotal_count(int total_count) { this.total_count = total_count; }

    public boolean isHas_more() { return has_more; }
    public void setHas_more(boolean has_more) { this.has_more = has_more; }
}