package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.api.data.chat.MessageData;
import com.example.honbabapp.databinding.RvMessageReceivedBinding;
import com.example.honbabapp.databinding.RvMessageSentBinding;
import com.example.honbabapp.databinding.RvMessageSystemBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // ViewType 상수
    static final int VIEW_TYPE_MINE   = 1; // 오른쪽 말풍선 (rv_message_sent.xml)
    static final int VIEW_TYPE_OTHER  = 2; // 왼쪽 말풍선 + 프로필 (rv_message_received.xml)
    static final int VIEW_TYPE_SYSTEM = 3; // 가운데 시스템 텍스트 (rv_message_system.xml)

    private List<MessageData> messages = new ArrayList<>();

    // ─────────────────────────────────────────
    // 프로필 클릭 콜백 (외부 주입)
    // ─────────────────────────────────────────

    public interface OnProfileClickListener {
        void onProfileClick(int userId);
    }

    private final OnProfileClickListener profileClickListener;

    public MessageAdapter() {
        profileClickListener = null;
    }

    /**
     * 프로필 클릭 콜백을 받는 생성자
     */
    public MessageAdapter(OnProfileClickListener listener) {
        this.profileClickListener = listener;
    }



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
            return new OtherViewHolder(binding, profileClickListener);

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
            // 내 메세지 : 시간 표시 여부만 필요
            boolean showTime = isLastInTimeRun(position);

            ((MineViewHolder) holder).bind(msg , showTime);
        } else if (holder instanceof OtherViewHolder) {
            // 상대 메세지: 시간 + 닉네임 표시 여부 둘 다 필요
            boolean showTime       = isLastInTimeRun(position);
            boolean showSenderName = isFirstInSenderRun(position);

            ((OtherViewHolder) holder).bind(msg, showTime, showSenderName);
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
     * 직전 메세지가 내가 보낸거라면 닉네임 한번 호출 + 메세지 시간 한번호출
     */
    public void addMessage(MessageData msg) {
        messages.add(msg);
        int newIndex = messages.size() -1;
        notifyItemInserted(newIndex);

        // 직전 메세지 재계산 (2개 이상일 경우)
        if (messages.size() >= 2) {
            notifyItemChanged(newIndex - 1);
        }
    }

    /**
     * 이전 페이지 메시지를 맨 앞에 삽입
     * 무한 스크롤로 이전 메시지 로드 시 호출
     */
    public void prependMessages(List<MessageData> older) {
        messages.addAll(0, older);
        notifyItemRangeInserted(0, older.size());

        // 기존 첫 메세지 (삽입후 인덱스 = older.size()) 재계산
        if (!older.isEmpty() && messages.size() > older.size()) {
            notifyItemChanged(older.size());
        }
    }

    // 특정 message_id들의 unread_count만 -1
    // 서버에서 실제로 새로 읽힌 message_id 목록만 정확하게 -1
    public void decrementUnreadForMessages(Set<Integer> messageIds) {
        for (int i = 0; i < messages.size(); i++) {
            MessageData msg = messages.get(i);
            if (messageIds.contains(msg.getMessage_id()) && msg.getUnread_count() > 0) {
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

        void bind(MessageData msg , boolean showTime) {
            // ── 이미지 / 텍스트 분기 ──
            // RecyclerView 는 ViewHolder 를 재사용하므로
            // 두 경우 모두에서 textContent / imageContent visibility 를 명시적으로 설정해야
            // 재활용 시 잔상(이전 이미지가 텍스트 메시지에 남는 버그) 이 안 생긴다.
            if (msg.isImage()) {
                binding.textContent.setVisibility(View.GONE);
                binding.imageContent.setVisibility(View.VISIBLE);
                Glide.with(binding.getRoot().getContext())
                        .load(msg.getImage_url())
                        .dontAnimate()
                        .placeholder(R.drawable.message_image_background)
                        .error(R.drawable.message_image_background)
                        .into(binding.imageContent);
            } else {
                binding.imageContent.setVisibility(View.GONE);
                binding.textContent.setVisibility(View.VISIBLE);
                binding.textContent.setText(msg.getContent());
            }

            // 시간 : 런의 마지막일 때만 표시
            if (showTime) {
                binding.textTime.setText(formatTime(msg.getCreated_at()));
                binding.textTime.setVisibility(View.VISIBLE);
            } else {
                binding.textTime.setVisibility(View.GONE);
            }

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
        private final OnProfileClickListener profileClickListener;

        OtherViewHolder(RvMessageReceivedBinding binding, OnProfileClickListener profileClickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.profileClickListener = profileClickListener;
        }

        void bind(MessageData msg, boolean showTime, boolean showSenderName) {
            // 닉네임: 런의 첫 번째일 때만 표시
            if (showSenderName) {
                binding.textSenderName.setText(msg.getSender_name());
                binding.textSenderName.setVisibility(View.VISIBLE);
            } else {
                binding.textSenderName.setVisibility(View.GONE);
            }

            // ── 이미지 / 텍스트 분기 ──
            // (MineViewHolder 와 동일 — 재활용 잔상 방지 위해 양쪽 모두 visibility 명시)
            if (msg.isImage()) {
                binding.textContent.setVisibility(View.GONE);
                binding.imageContent.setVisibility(View.VISIBLE);
                Glide.with(binding.getRoot().getContext())
                        .load(msg.getImage_url())
                        .dontAnimate()
                        .placeholder(R.drawable.message_image_background)
                        .error(R.drawable.message_image_background)
                        .into(binding.imageContent);
            } else {
                binding.imageContent.setVisibility(View.GONE);
                binding.textContent.setVisibility(View.VISIBLE);
                binding.textContent.setText(msg.getContent());
            }

            // 시간: 런의 마지막일 때만 표시
            if (showTime) {
                binding.textTime.setText(formatTime(msg.getCreated_at()));
                binding.textTime.setVisibility(View.VISIBLE);
            } else {
                binding.textTime.setVisibility(View.GONE);
            }

            // 읽지 않은 인원 수 (0이면 숨김)
            if (msg.getUnread_count() > 0) {
                binding.textUnread.setVisibility(View.VISIBLE);
                binding.textUnread.setText(String.valueOf(msg.getUnread_count()));
            } else {
                binding.textUnread.setVisibility(View.GONE);
            }

            // 프로필 이미지 — 첫 번째 메세지만 표시, 나머지는 INVISIBLE (자리는 유지)
            if (showSenderName) {
                binding.imgProfile.setVisibility(View.VISIBLE);
                Glide.with(binding.getRoot().getContext())
                        .load(msg.getSender_profile_image())
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(binding.imgProfile);
            } else {
                binding.imgProfile.setVisibility(View.INVISIBLE); // GONE 아님! 자리 유지
            }

            // 프로필/ 닉네임 클릭시 -> ChatRoomParticipantDetailActivity 이동
            View.OnClickListener profileClick = v -> {
                if (profileClickListener != null) {
                    profileClickListener.onProfileClick(msg.getSender_id());
                }
            };
            binding.imgProfile.setOnClickListener(profileClick);
            binding.textSenderName.setOnClickListener(profileClick);
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
     * "2026-04-17 10:00:00" → "2026-04-17 10:00" (앞 16자리만 자름)
     * 같은 분(minute)인지 비교할 때 사용
     * null이거나 너무 짧으면 빈 문자열 반환 (NullPointerException 방지)
     */
    private static String minuteKey(String createdAt) {
        if (createdAt == null || createdAt.length() < 16) return "";
        return createdAt.substring(0, 16); // "yyyy-MM-dd HH:mm"
    }

    /**
     * position 메세지가 "시간 런"의 마지막인지 판단
     * 마지막이면 true → 시간 표시 / 아니면 false → 시간 숨김
     *
     * 마지막인 조건 (하나라도 해당하면 true):
     *   1. 리스트의 맨 끝 메세지 (다음 메세지 없음)
     *   2. 다음 메세지가 시스템 메세지 (입장/퇴장 알림)
     *   3. 다음 메세지의 sender_id가 다름 (다른 사람이 보냄)
     *   4. 다음 메세지의 분(minute)이 다름 (1분 이상 차이)
     */
    private boolean isLastInTimeRun(int position) {
        // 1. 마지막 아이템이면 무조건 true
        if (position >= messages.size() - 1) return true;

        MessageData current = messages.get(position);
        MessageData next    = messages.get(position + 1);

        // 2. 다음이 시스템 메세지면 런 끊김
        if ("system".equals(next.getMsg_type())) return true;

        // 3. 다음 메세지가 다른 사람이면 런 끊김
        if (current.getSender_id() != next.getSender_id()) return true;

        // 4. 다음 메세지가 다른 분(minute)이면 런 끊김
        if (!minuteKey(current.getCreated_at()).equals(minuteKey(next.getCreated_at()))) return true;

        // 위 조건 모두 해당 없음 → 아직 런 중간 → 시간 숨김
        return false;
    }

    /**
     * position 메세지가 "발신자 런"의 첫 번째인지 판단
     * 첫 번째이면 true → 닉네임 표시 / 아니면 false → 닉네임 숨김
     * (OtherViewHolder — 상대방 메세지에만 적용)
     *
     * 첫 번째인 조건 (하나라도 해당하면 true):
     *   1. 리스트의 맨 첫 메세지 (이전 메세지 없음)
     *   2. 이전 메세지가 시스템 메세지
     *   3. 이전 메세지의 sender_id가 다름
     */
    private boolean isFirstInSenderRun(int position) {
        // 1. 첫 번째 아이템이면 무조건 true
        if (position == 0) return true;

        MessageData current  = messages.get(position);
        MessageData previous = messages.get(position - 1);

        // 2. 이전이 시스템 메세지면 런 끊김
        if ("system".equals(previous.getMsg_type())) return true;

        // 3. 이전 메세지가 다른 사람이면 런 끊김
        if (current.getSender_id() != previous.getSender_id()) return true;

        // 위 조건 모두 해당 없음 → 런 중간 → 닉네임 숨김
        return false;
    }

    /**
     * "2026-04-17 10:00:00" → "10:00"
     */
    private static String formatTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return "";
        try {
            SimpleDateFormat input  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
            input.setTimeZone(TimeZone.getTimeZone("UTC"));
            SimpleDateFormat output = new SimpleDateFormat("a h:mm", Locale.KOREAN);
            output.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            return output.format(input.parse(timeStr));
        } catch (Exception e) {
            return "";
        }
    }
}