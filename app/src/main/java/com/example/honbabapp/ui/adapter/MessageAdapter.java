package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.api.data.chat.MessageData;
import com.example.honbabapp.databinding.RvMessageSentBinding;
import com.example.honbabapp.databinding.RvMessageReceivedBinding;
import com.example.honbabapp.databinding.RvMessageSystemBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // ViewType 상수
    static final int VIEW_TYPE_MINE   = 1; // 오른쪽 말풍선 (rv_message_sent.xml)
    static final int VIEW_TYPE_OTHER  = 2; // 왼쪽 말풍선 + 프로필 (rv_message_received.xml)
    static final int VIEW_TYPE_SYSTEM = 3; // 가운데 시스템 텍스트 (rv_message_system.xml)

    private List<MessageData> messages = new ArrayList<>();

    // ─────────────────────────────────────────
    // ViewType 결정
    // ─────────────────────────────────────────

    @Override
    public int getItemViewType(int position) {
        MessageData msg = messages.get(position);
        if ("system".equals(msg.getMsg_type())) return VIEW_TYPE_SYSTEM;
        if (msg.isIs_mine())                    return VIEW_TYPE_MINE;
        return VIEW_TYPE_OTHER;
    }

    // ─────────────────────────────────────────
    // ViewHolder 생성 — ViewType 에 맞는 xml inflate
    // ─────────────────────────────────────────

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_MINE) {
            RvMessageSentBinding binding =
                    RvMessageSentBinding.inflate(inflater, parent, false);
            return new MineViewHolder(binding);

        } else if (viewType == VIEW_TYPE_OTHER) {
            RvMessageReceivedBinding binding =
                    RvMessageReceivedBinding.inflate(inflater, parent, false);
            return new OtherViewHolder(binding);

        } else {
            RvMessageSystemBinding binding =
                    RvMessageSystemBinding.inflate(inflater, parent, false);
            return new SystemViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageData msg = messages.get(position);

        if (holder instanceof MineViewHolder) {
            ((MineViewHolder) holder).bind(msg);
        } else if (holder instanceof OtherViewHolder) {
            ((OtherViewHolder) holder).bind(msg);
        } else if (holder instanceof SystemViewHolder) {
            ((SystemViewHolder) holder).bind(msg);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ─────────────────────────────────────────
    // 데이터 갱신 메서드
    // ─────────────────────────────────────────

    /**
     * 히스토리 메시지 전체 세팅
     * ChatRoomActivity 최초 진입 시 REST API 응답 받으면 호출
     */
    public void setMessages(List<MessageData> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    /**
     * 새 메시지 1개 추가
     * WebSocket 실시간 수신 시 호출 — 전체를 다시 그리지 않고 맨 아래만 추가
     */
    public void addMessage(MessageData msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    /**
     * 특정 메시지의 읽음 카운트 직접 지정
     * messageId 기준으로 찾아서 newUnreadCount 로 세팅
     */
    public void updateReadCount(int messageId, int newUnreadCount) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getMessage_id() == messageId) {
                messages.get(i).setUnread_count(newUnreadCount);
                notifyItemChanged(i);
                break;
            }
        }
    }

    /**
     * 특정 발신자의 메시지 unread_count 를 1씩 감소
     * read_update 수신 시 호출 — 상대방이 읽었을 때 내 메시지 읽음 숫자 실시간 갱신
     *
     * senderId : 내 userId (내가 보낸 메시지만 감소 대상)
     * 최솟값 0 보장 — 이미 0이면 변경 없음
     */
    public void decreaseUnreadCount(int senderId) {
        for (int i = 0; i < messages.size(); i++) {
            MessageData msg = messages.get(i);
            if (msg.getSender_id() == senderId && msg.getUnread_count() > 0) {
                msg.setUnread_count(msg.getUnread_count() - 1);
                notifyItemChanged(i);
            }
        }
    }

    // ─────────────────────────────────────────
    // ViewHolder 3개
    // ─────────────────────────────────────────

    /**
     * 내가 보낸 메시지 (오른쪽 말풍선)
     * rv_message_sent.xml 사용
     */
    static class MineViewHolder extends RecyclerView.ViewHolder {
        private final RvMessageSentBinding binding;

        MineViewHolder(RvMessageSentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MessageData msg) {
            binding.textContent.setText(msg.getContent());
            binding.textTime.setText(formatTime(msg.getCreated_at()));

            // 읽지 않은 인원 수 (0이면 숨김)
            if (msg.getUnread_count() > 0) {
                binding.textUnread.setVisibility(View.VISIBLE);
                binding.textUnread.setText(String.valueOf(msg.getUnread_count()));
            } else {
                binding.textUnread.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 상대방 메시지 (왼쪽 말풍선 + 프로필 이미지 + 닉네임)
     * rv_message_received.xml 사용
     */
    static class OtherViewHolder extends RecyclerView.ViewHolder {
        private final RvMessageReceivedBinding binding;

        OtherViewHolder(RvMessageReceivedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MessageData msg) {
            binding.textSenderName.setText(msg.getSender_name());
            binding.textContent.setText(msg.getContent());
            binding.textTime.setText(formatTime(msg.getCreated_at()));

            if (msg.getUnread_count() > 0) {
                binding.textUnread.setVisibility(View.VISIBLE);
                binding.textUnread.setText(String.valueOf(msg.getUnread_count()));
            } else {
                binding.textUnread.setVisibility(View.GONE);
            }

            // 프로필 이미지 Glide 로드
            Glide.with(binding.getRoot().getContext())
                    .load(msg.getSender_profile_image())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(binding.imgProfile);
        }
    }

    /**
     * 시스템 메시지 (가운데 텍스트)
     * rv_message_system.xml 사용
     */
    static class SystemViewHolder extends RecyclerView.ViewHolder {
        private final RvMessageSystemBinding binding;

        SystemViewHolder(RvMessageSystemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MessageData msg) {
            binding.textContent.setText(msg.getContent());
        }
    }

    // ─────────────────────────────────────────
    // 공통 시간 포맷 유틸
    // ─────────────────────────────────────────

    /**
     * "2026-04-17 10:00:00" → "10:00"
     */
    private static String formatTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return "";
        try {
            SimpleDateFormat input  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
            SimpleDateFormat output = new SimpleDateFormat("HH:mm", Locale.KOREA);
            return output.format(input.parse(timeStr));
        } catch (Exception e) {
            return "";
        }
    }
}