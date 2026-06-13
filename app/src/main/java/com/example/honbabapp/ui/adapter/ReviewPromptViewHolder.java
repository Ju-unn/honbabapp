package com.example.honbabapp.ui.adapter;

import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.api.data.chat.MessageData;
import com.example.honbabapp.databinding.RvMessageReviewPromptBinding;

/**
 * 후기 안내 메시지 (review_prompt).
 *
 * 안내 텍스트 + "후기작성" 버튼을 표시한다.
 * 버튼 클릭은 MessageAdapter.OnReviewPromptClickListener 로 Activity에 전달된다.
 */
class ReviewPromptViewHolder extends RecyclerView.ViewHolder {

    private final RvMessageReviewPromptBinding binding;

    ReviewPromptViewHolder(RvMessageReviewPromptBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    void bind(MessageData message, MessageAdapter.OnReviewPromptClickListener listener) {
        binding.textContent.setText(message.getContent());

        binding.btnWriteReview.setOnClickListener(view -> {
            if (listener != null) {
                listener.onReviewPromptClick();
            }
        });
    }
}