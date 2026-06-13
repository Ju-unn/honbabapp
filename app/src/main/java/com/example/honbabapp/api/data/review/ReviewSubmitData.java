package com.example.honbabapp.api.data.review;

/**
 * 후기 제출 응답 DTO
 * POST /api/review/submit.php 의 data 부분
 *
 * 응답 예시:
 * {
 *   "review_id": 12,
 *   "reviewee_id": 5,
 *   "rating": 4,
 *   "manner_score_added": 0.8,
 *   "reviewee_new_manner_score": 36.8
 * }
 */
public class ReviewSubmitData {

    private int review_id;
    private int reviewee_id;
    private int rating;
    private double manner_score_added;
    private double reviewee_new_manner_score;

    public ReviewSubmitData() {}

    public int getReview_id() {
        return review_id;
    }

    public void setReview_id(int review_id) {
        this.review_id = review_id;
    }

    public int getReviewee_id() {
        return reviewee_id;
    }

    public void setReviewee_id(int reviewee_id) {
        this.reviewee_id = reviewee_id;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public double getManner_score_added() {
        return manner_score_added;
    }

    public void setManner_score_added(double manner_score_added) {
        this.manner_score_added = manner_score_added;
    }

    public double getReviewee_new_manner_score() {
        return reviewee_new_manner_score;
    }

    public void setReviewee_new_manner_score(double reviewee_new_manner_score) {
        this.reviewee_new_manner_score = reviewee_new_manner_score;
    }
}