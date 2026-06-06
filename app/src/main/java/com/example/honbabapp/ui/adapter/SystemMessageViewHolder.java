package com.example.honbabapp.ui.adapter;

import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.api.data.chat.MessageData;
import com.example.honbabapp.databinding.RvMessageSystemBinding;

/**
 * 시스템 메시지 (입장/퇴장/방장 위임 등 가운데 정렬 안내 문구).
 * 본문 텍스트만 표시한다.
 */
class SystemMessageViewHolder extends RecyclerView.ViewHolder {

    private final RvMessageSystemBinding binding;

    SystemMessageViewHolder(RvMessageSystemBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    void bind(MessageData message) {
        binding.textContent.setText(message.getContent());
    }
}
