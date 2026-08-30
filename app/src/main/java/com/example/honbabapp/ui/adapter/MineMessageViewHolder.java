package com.example.honbabapp.ui.adapter;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.api.data.chat.MessageData;
import com.example.honbabapp.databinding.RvMessageSentBinding;
import com.example.honbabapp.util.ChatTimeFormatter;

/**
 * 내가 보낸 메시지 말풍선 (오른쪽 정렬).
 * 본문 3분기는 MessageContentBinder, 시간은 ChatTimeFormatter 로 위임.
 * 전송 실패 시 재전송/삭제 오버레이를 노출한다.
 */
class MineMessageViewHolder extends RecyclerView.ViewHolder {

    private final RvMessageSentBinding binding;

    MineMessageViewHolder(RvMessageSentBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    void bind(MessageData message,
              boolean showTime,
              MessageAdapter.OnImageClickListener imageClickListener,
              MessageAdapter.OnFailedMessageListener failedMessageListener) {

        // ── 본문 (텍스트/단일이미지/다중이미지) ──
        MessageContentBinder.bind(
                binding.textContent, binding.imageContent, binding.imageGrid,
                message, imageClickListener);

        // ── 실패 오버레이 ──
        if (message.isFailed()) {
            binding.failOverlay.setVisibility(View.VISIBLE);
            binding.btnRetry.setOnClickListener(view -> {
                if (failedMessageListener != null) failedMessageListener.onRetry(message);
            });
            binding.btnDelete.setOnClickListener(view -> {
                if (failedMessageListener != null) failedMessageListener.onDelete(message);
            });
        } else {
            binding.failOverlay.setVisibility(View.GONE);
        }

        // ── 시간 ──
        if (showTime) {
            binding.textTime.setText(ChatTimeFormatter.formatMessageTime(message.getCreated_at()));
            binding.textTime.setVisibility(View.VISIBLE);
        } else {
            binding.textTime.setVisibility(View.GONE);
        }

        // ── 읽음 수 ──
        if (message.getUnread_count() > 0) {
            binding.textUnread.setVisibility(View.VISIBLE);
            binding.textUnread.setText(String.valueOf(message.getUnread_count()));
        } else {
            binding.textUnread.setVisibility(View.GONE);
        }
    }
}
