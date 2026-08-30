package com.example.honbabapp.ui.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.honbabapp.api.data.chat.MessageData;
import com.example.honbabapp.ui.view.ChatImageGridView;
import com.example.honbabapp.util.ChatGlide;

import java.util.List;

/**
 * 메시지 말풍선 본문(텍스트 / 단일 이미지 / 다중 이미지)을 그리는 공통 바인더.
 *
 * Mine·Other 말풍선의 본문 분기 로직이 동일해 여기로 모았다.
 * 재활용 잔상 방지를 위해 세 영역(텍스트/단일이미지/그리드)의 visibility 를 매번 명시한다.
 */
final class MessageContentBinder {

    private MessageContentBinder() {
        // 유틸 클래스 — 인스턴스화 금지
    }

    static void bind(TextView textContent,
                     ImageView imageContent,
                     ChatImageGridView imageGrid,
                     MessageData message,
                     MessageAdapter.OnImageClickListener imageClickListener) {

        List<String> images = message.getImages();

        if (!message.isImage()) {
            // 텍스트
            textContent.setVisibility(View.VISIBLE);
            imageContent.setVisibility(View.GONE);
            imageGrid.setVisibility(View.GONE);
            textContent.setText(message.getContent());

        } else if (images.size() == 1) {
            // 단일 이미지
            textContent.setVisibility(View.GONE);
            imageContent.setVisibility(View.VISIBLE);
            imageGrid.setVisibility(View.GONE);
            ChatGlide.loadMessageImage(imageContent, images.get(0));
            imageContent.setOnClickListener(view -> {
                if (imageClickListener != null) {
                    imageClickListener.onImageClick(images, 0);
                }
            });

        } else {
            // 다중 이미지 그리드 (카톡식 행 분배)
            textContent.setVisibility(View.GONE);
            imageContent.setVisibility(View.GONE);
            imageGrid.setVisibility(View.VISIBLE);
            imageGrid.setImages(images, imageClickListener);
        }
    }
}
