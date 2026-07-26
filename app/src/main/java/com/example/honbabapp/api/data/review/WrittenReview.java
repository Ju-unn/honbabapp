package com.example.honbabapp.api.data.review;

/**
 * 내가 쓴 후기 단건 DTO
 * GET /api/review/written.php 의 data.reviews[] 항목
 *
 * 받은 후기(ReceivedReview)와 달리 작성자 본인이 보는 기록이므로
 * 후기 대상(reviewee)과 모임 정보가 함께 내려온다.
 *
 * 응답 예시:
 * {
 *   "review_id": 12,
 *   "rating": 4,
 *   "content": "좋았어요",
 *   "created_at": "2026-06-24 19:00:00",
 *   "meeting_id": 7,
 *   "meeting_title": "강남 파스타 모임",
 *   "reviewee_id": 3,
 *   "reviewee_nickname": "혼밥러",
 *   "reviewee_profile_image": "https://..."
 * }
 */
public class WrittenReview {

    private int review_id;
    private int rating;
    private String content;                   // nullable
    private String created_at;
    private int meeting_id;
    private String meeting_title;             // nullable (모임 삭제 시)
    private Integer reviewee_id;              // nullable (탈퇴 시)
    private String reviewee_nickname;         // nullable
    private String reviewee_profile_image;    // nullable

    public WrittenReview() {}

    public int getReview_id() {
        return review_id;
    }

    public int getRating() {
        return rating;
    }

    public String getContent() {
        return content;
    }

    public String getCreated_at() {
        return created_at;
    }

    public int getMeeting_id() {
        return meeting_id;
    }

    public String getMeeting_title() {
        return meeting_title;
    }

    public Integer getReviewee_id() {
        return reviewee_id;
    }

    public String getReviewee_nickname() {
        return reviewee_nickname;
    }

    public String getReviewee_profile_image() {
        return reviewee_profile_image;
    }
}
