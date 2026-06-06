package com.example.honbabapp.ui.adapter;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.api.data.chat.MessageData;
import com.example.honbabapp.databinding.RvMessageReceivedBinding;
import com.example.honbabapp.util.ChatGlide;
import com.example.honbabapp.util.ChatTimeFormatter;

/**
 * 상대가 보낸 메시지 말풍선 (왼쪽 정렬, 프로필/닉네임 포함).
 * 본문 3분기는 MessageContentBinder, 시간은 ChatTimeFormatter, 프로필은 ChatGlide 로 위임.
 * showSenderName(발신자 묶음의 첫 메시지)일 때만 닉네임/프로필을 보인다.
 */
class OtherMessageViewHolder extends RecyclerView.ViewHolder {

    private final RvMessageReceivedBinding binding;
    private final MessageAdapter.OnProfileClickListener profileClickListener;

    OtherMessageViewHolder(RvMessageReceivedBinding binding,
                           MessageAdapter.OnProfileClickListener profileClickListener) {
        super(binding.getRoot());
        this.binding = binding;
        this.profileClickListener = profileClickListener;
    }

    void bind(MessageData message,
              boolean showTime,
              boolean showSenderName,
              MessageAdapter.OnImageClickListener imageClickListener) {

        // ── 닉네임 ──
        if (showSenderName) {
            binding.textSenderName.setText(message.getSender_name());
            binding.textSenderName.setVisibility(View.VISIBLE);
        } else {
            binding.textSenderName.setVisibility(View.GONE);
        }

        // ── 본문 (텍스트/단일이미지/다중이미지) ──
        MessageContentBinder.bind(
                binding.textContent, binding.imageContent, binding.rvImageGrid,
                message, imageClickListener);

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

        // ── 프로필 ──
        if (showSenderName) {
            binding.imgProfile.setVisibility(View.VISIBLE);
            ChatGlide.loadCircleProfile(binding.imgProfile, message.getSender_profile_image());
        } else {
            binding.imgProfile.setVisibility(View.INVISIBLE);
        }

        View.OnClickListener profileClick = view -> {
            if (profileClickListener != null) {
                profileClickListener.onProfileClick(message.getSender_id());
            }
        };
        binding.imgProfile.setOnClickListener(profileClick);
        binding.textSenderName.setOnClickListener(profileClick);
    }
}
