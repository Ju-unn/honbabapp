package com.example.honbabapp.util;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;

/**
 * 채팅 화면 전용 Glide 이미지 로딩 헬퍼.
 *
 * 채팅에서 쓰는 이미지 로딩은 크게 3종이라 각각 메서드로 노출한다.
 * (placeholder/error 리소스와 crop 방식이 달라 하나로 합치면 동작이 달라짐)
 */
public final class ChatGlide {

    private ChatGlide() {
    }

    /**
     * 정사각 썸네일 로딩 (사진 그리드 / 사진 미리보기).
     * placeholder·error = 회색 배경(bg_300), centerCrop.
     */
    public static void loadSquareThumbnail(ImageView targetImageView, String imageUrl) {
        Glide.with(targetImageView.getContext())
                .load(imageUrl)
                .placeholder(R.color.bg_300)
                .error(R.color.bg_300)
                .centerCrop()
                .into(targetImageView);
    }

    /**
     * 원형 프로필 로딩 (메시지 발신자 / 채팅방 목록 프로필).
     * placeholder·error = 기본 프로필(ic_profile), circleCrop.
     * imageUrl 이 null 이어도 error 리소스가 기본 이미지를 표시한다.
     */
    public static void loadCircleProfile(ImageView targetImageView, String imageUrl) {
        Glide.with(targetImageView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(targetImageView);
    }

    /**
     * 메시지 말풍선 내 단일 이미지 로딩.
     * dontAnimate + 말풍선 배경(message_image_background), crop 미적용(원본 비율 유지).
     */
    public static void loadMessageImage(ImageView targetImageView, String imageUrl) {
        Glide.with(targetImageView.getContext())
                .load(imageUrl)
                .dontAnimate()
                .placeholder(R.drawable.message_image_background)
                .error(R.drawable.message_image_background)
                .into(targetImageView);
    }
}
