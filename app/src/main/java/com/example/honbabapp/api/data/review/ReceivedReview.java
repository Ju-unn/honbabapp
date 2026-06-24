package com.example.honbabapp.api.data.review;

/**
 *
 * 익명 방침에 따라 reviewer 정보는 포함하지 않는다.
 *
 * 응답 예시:
 * {
 *   "review_id": 12,
 *   "rating": 4,
 *   "content": "좋았어요",
 *   "created_at": "2026-06-24 19:00:00"
 * }
 */
public class ReceivedReview {

    private int review_id;
    private int rating;
    private String content;       // nullable — 내용 없이 별점만 남긴 경우 null
    private String created_at;

    public ReceivedReview() {}

    public int getReview_id() {
        return review_id;
    }

    public void setReview_id(int review_id) {
        this.review_id = review_id;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }
}