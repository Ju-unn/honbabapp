package com.example.honbabapp.api.data.review;

import java.util.List;

/**
 * 후기 대상 목록 조회 응답 DTO
 * GET /api/review/targets.php 의 data 부분
 *
 * 응답 예시:
 * {
 *   "is_eligible": true,
 *   "targets": [
 *     {
 *       "user_id": 5,
 *       "user_nickname": "김철수",
 *       "user_profile_image": "https://...",
 *       "already_reviewed": false
 *     }
 *   ]
 * }
 *
 * is_eligible = false 이면 QR 인증을 하지 않은 사용자
 * → targets 는 빈 배열로 내려옴
 */
public class ReviewTargetsData {

    private boolean is_eligible;
    private List<ReviewTarget> targets;

    public ReviewTargetsData() {}

    public boolean isIs_eligible() {
        return is_eligible;
    }

    public void setIs_eligible(boolean is_eligible) {
        this.is_eligible = is_eligible;
    }

    public List<ReviewTarget> getTargets() {
        return targets;
    }

    public void setTargets(List<ReviewTarget> targets) {
        this.targets = targets;
    }
}