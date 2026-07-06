package com.example.honbabapp.api.data.request.review;

/**
 * 후기 제출 요청 바디 DTO
 * POST /api/review/submit.php
 *
 * reviewer_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 * content 는 선택 입력 — 빈 문자열이면 서버가 NULL로 저장
 */
public class ReviewSubmitRequest {

    private int meeting_id;
    private int reviewee_id;
    private int rating;
    private String content;

    public ReviewSubmitRequest(int meeting_id, int reviewee_id, int rating, String content) {
        this.meeting_id = meeting_id;
        this.reviewee_id = reviewee_id;
        this.rating = rating;
        this.content = content;
    }

    public int getMeeting_id() {
        return meeting_id;
    }

    public void setMeeting_id(int meeting_id) {
        this.meeting_id = meeting_id;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}