package com.example.honbabapp.data.review;

import java.util.Date;

public class Review {
    /// Review : 모임 후 참여자 평가 (매너 점수 평가)
    public int review_meeting_id; /// 어떤 모임에 대한 리뷰인지
    public int review_reviewer_id; /// 평가하는 사람 User 참조
    public int review_reviewee_id; /// 평가받는 사람 User 참조
    public Double review_rating; /// 평점 User의 manner_score에 반영
    public Date review_created_at; /// 리뷰 작성 시간

}
