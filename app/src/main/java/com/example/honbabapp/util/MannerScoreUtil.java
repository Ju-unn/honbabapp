package com.example.honbabapp.util;

import com.example.honbabapp.R;

/**
 * 매너점수 등급별 헤더 색상.
 * ProfileFragment(내 프로필)와 ChatRoomParticipantDetailActivity(다른 유저 프로필)가 공유.
 */
public class MannerScoreUtil {

    private MannerScoreUtil() {}

    public static int colorRes(float score) {
        if (score < 1.0f) return R.color.error_red;        // 불량 (0점)
        if (score < 10.0f) return R.color.warning_orange;   // 1~10
        return R.color.success_green;                       // 10 이상
    }
}
