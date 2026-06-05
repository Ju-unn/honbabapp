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

    static final int VIEW_TYPE_MINE = 1;
    static final int VIEW_TYPE_OTHER = 2;
    static final int VIEW_TYPE_SYSTEM = 3;

    private List<MessageData> messages = new ArrayList<>();

    public List<MessageData> getMessages() {
        return messages;
    }

    // ── 콜백 인터페이스 ──────────────────────────

    public interface OnProfileClickListener {
        void onProfileClick(int userId);
    }

    public interface OnImageClickListener {
        void onImageClick(List<String> urls, int startIndex);
    }

    /**
     * 실패 메시지 재전송/삭제 콜백
     */
    public interface OnFailedMessageListener {
        void onRetry(MessageData msg);

        void onDelete(MessageData msg);
    }

    private final OnProfileClickListener profileClickListener;
    private OnImageClickListener imageClickListener;
    private OnFailedMessageListener failedMessageListener;

    public MessageAdapter() {
        profileClickListener = null;
    }

    public MessageAdapter(OnProfileClickListener listener) {
        this.profileClickListener = listener;
    }

    public void setImageClickListener(OnImageClickListener l) {
        this.imageClickListener = l;
    }

    public void setFailedMessageListener(OnFailedMessageListener l) {
        this.failedMessageListener = l;
    }

    // ── ViewType ─────────────────────────────────

    @Override
    public int getItemViewType(int position) {
        MessageData msg = messages.get(position);
        if ("system".equals(msg.getMsg_type())) return VIEW_TYPE_SYSTEM;
        if (msg.isIs_mine()) return VIEW_TYPE_MINE;
        return VIEW_TYPE_OTHER;
    }

    // ── ViewHolder 생성 ───────────────────────────

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_MINE) {
            return new MineViewHolder(RvMessageSentBinding.inflate(inflater, parent, false));
        } else if (viewType == VIEW_TYPE_OTHER) {
            return new OtherViewHolder(RvMessageReceivedBinding.inflate(inflater, parent, false), profileClickListener);
        } else {
            return new SystemViewHolder(RvMessageSystemBinding.inflate(inflater, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageData msg = messages.get(position);
        if (holder instanceof MineViewHolder) {
            ((MineViewHolder) holder).bind(msg, isLastInTimeRun(position),
                    imageClickListener, failedMessageListener);
        } else if (holder instanceof OtherViewHolder) {
            ((OtherViewHolder) holder).bind(msg, isLastInTimeRun(position),
                    isFirstInSenderRun(position), imageClickListener);
        } else if (holder instanceof SystemViewHolder) {
            ((SystemViewHolder) holder).bind(msg);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ── 데이터 갱신 메서드 ────────────────────────

    public void setMessages(List<MessageData> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    public void addMessage(MessageData msg) {
        messages.add(msg);
        int newIndex = messages.size() - 1;
        notifyItemInserted(newIndex);
        if (messages.size() >= 2) notifyItemChanged(newIndex - 1);
    }

    public void prependMessages(List<MessageData> older) {
        messages.addAll(0, older);
        notifyItemRangeInserted(0, older.size());
        if (!older.isEmpty() && messages.size() > older.size()) {
            notifyItemChanged(older.size());
        }
    }

    public void decrementUnreadForMessages(Set<Integer> messageIds) {
        for (int i = 0; i < messages.size(); i++) {
            MessageData msg = messages.get(i);
            if (messageIds.contains(msg.getMessage_id()) && msg.getUnread_count() > 0) {
                msg.setUnread_count(msg.getUnread_count() - 1);
                notifyItemChanged(i);
            }
        }
    }

    // ── Optimistic UI 메서드 ──────────────────────

    /**
     * 서버 echo 수신 시 로컬 optimistic(SENDING) 메시지를 서버 확정 데이터로 교체한다.
     * → 재전송 성공 시 그 메시지가 pending 블록을 벗어나 시간순(맨 아래 성공 자리)으로 올라간다.
     * optimistic 을 못 찾으면(구 서버 echo) addIncomingSent 로 처리.
     */
    public void confirmMessage(String clientMsgId, MessageData serverMessage) {
        int oldIndex = findByClientMsgId(clientMsgId);
        if (oldIndex < 0) {
            addIncomingSent(serverMessage);
            return;
        }

        // optimistic 제거 후, 제거 직후 기준 firstPendingIndex(= SENT 블록 끝)에 재삽입.
        // 방금 확정된 메시지는 message_id 가 가장 크므로 SENT 블록의 마지막에 놓인다.
        messages.remove(oldIndex);
        int insertIndex = firstPendingIndex();
        messages.add(insertIndex, serverMessage);

        if (oldIndex == insertIndex) {
            // 위치 동일(대기 항목이 하나뿐인 일반 케이스) → 단순 교체로 처리해 깜빡임 제거
            notifyItemChanged(insertIndex);
        } else {
            notifyItemRemoved(oldIndex);
            notifyItemInserted(insertIndex);
        }
        refreshNeighbors(insertIndex);
    }

    /**
     * client_msg_id 로 찾아서 STATUS_FAILED 로 마킹
     */
    public void markFailed(String clientMsgId) {
        int idx = findByClientMsgId(clientMsgId);
        if (idx >= 0) {
            messages.get(idx).setStatus(MessageData.STATUS_FAILED);
            notifyItemChanged(idx);
        }
    }

    /**
     * 로컬 메시지 제거 (실패 → 삭제 버튼, 서버 호출 없음)
     */
    public void removeByClientMsgId(String clientMsgId) {
        int idx = findByClientMsgId(clientMsgId);
        if (idx >= 0) {
            messages.remove(idx);
            notifyItemRemoved(idx);
            if (idx > 0) notifyItemChanged(idx - 1); // 직전 메시지 시간 재계산
        }
    }

    private int findByClientMsgId(String clientMsgId) {
        if (clientMsgId == null) return -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (clientMsgId.equals(messages.get(i).getClient_msg_id())) return i;
        }
        return -1;
    }

    /**
     * 불변식 유지를 위한 첫 pending 인덱스 탐색.
     * 리스트는 [SENT 블록] ++ [SENDING/FAILED 블록] 구조이므로,
     * 앞에서부터 STATUS_SENT 가 아닌 첫 항목의 인덱스가 곧 pending 블록의 시작점이다.
     * pending 항목이 없으면 리스트 크기를 반환한다(= SENT 블록의 끝 위치).
     */
    private int firstPendingIndex() {
        for (int index = 0; index < messages.size(); index++) {
            if (messages.get(index).getStatus() != MessageData.STATUS_SENT) {
                return index;
            }
        }
        return messages.size();
    }

    /**
     * 서버 확정(SENT) 메시지를 SENT 블록의 끝(= pending 블록 바로 위)에 삽입한다.
     * 타인 메시지 수신, 또는 confirmMessage 에서 optimistic 을 못 찾은 경우에 사용한다.
     * 이로써 전송중/실패 메시지는 항상 리스트 맨 아래에 유지된다.
     */
    public void addIncomingSent(MessageData incomingMessage) {
        int insertIndex = firstPendingIndex();
        messages.add(insertIndex, incomingMessage);
        notifyItemInserted(insertIndex);
        refreshNeighbors(insertIndex);
    }

    /**
     * 삽입/이동 후 인접 항목의 시간·발신자 그룹 표시를 다시 계산한다.
     * (이전 항목의 isLastInTimeRun, 다음 항목의 isFirstInSenderRun 이 바뀔 수 있음)
     */
    private void refreshNeighbors(int index) {
        if (index - 1 >= 0) {
            notifyItemChanged(index - 1);
        }
        if (index + 1 < messages.size()) {
            notifyItemChanged(index + 1);
        }
    }


    // ── ViewHolder ────────────────────────────────

    static class MineViewHolder extends RecyclerView.ViewHolder {
        private final RvMessageSentBinding binding;

        MineViewHolder(RvMessageSentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MessageData msg, boolean showTime,
                  OnImageClickListener imageClick,
                  OnFailedMessageListener failedListener) {

            List<String> images = msg.getImages();

            // ── 3분기: 텍스트 / 단일 이미지 / 다중 이미지 ──
            // 재활용 잔상 방지: 3가지 뷰 모두 visibility 명시
            if (!msg.isImage()) {
                // 텍스트
                binding.textContent.setVisibility(View.VISIBLE);
                binding.imageContent.setVisibility(View.GONE);
                binding.rvImageGrid.setVisibility(View.GONE);
                binding.textContent.setText(msg.getContent());

            } else if (images.size() == 1) {
                // 단일 이미지 — 기존 룩 유지
                binding.textContent.setVisibility(View.GONE);
                binding.imageContent.setVisibility(View.VISIBLE);
                binding.rvImageGrid.setVisibility(View.GONE);
                Glide.with(binding.getRoot().getContext())
                        .load(images.get(0))
                        .dontAnimate()
                        .placeholder(R.drawable.message_image_background)
                        .error(R.drawable.message_image_background)
                        .into(binding.imageContent);
                binding.imageContent.setOnClickListener(v -> {
                    if (imageClick != null) imageClick.onImageClick(images, 0);
                });

            } else {
                // 다중 이미지 그리드
                binding.textContent.setVisibility(View.GONE);
                binding.imageContent.setVisibility(View.GONE);
                binding.rvImageGrid.setVisibility(View.VISIBLE);
                new ImageGridAdapter(images, (urls, idx) -> {
                    if (imageClick != null) imageClick.onImageClick(urls, idx);
                }).attachTo(binding.rvImageGrid);
            }

            // ── 실패 오버레이 ──
            if (msg.isFailed()) {
                binding.failOverlay.setVisibility(View.VISIBLE);
                binding.btnRetry.setOnClickListener(v -> {
                    if (failedListener != null) failedListener.onRetry(msg);
                });
                binding.btnDelete.setOnClickListener(v -> {
                    if (failedListener != null) failedListener.onDelete(msg);
                });
            } else {
                binding.failOverlay.setVisibility(View.GONE);
            }

            // ── 시간 ──
            if (showTime) {
                binding.textTime.setText(formatTime(msg.getCreated_at()));
                binding.textTime.setVisibility(View.VISIBLE);
            } else {
                binding.textTime.setVisibility(View.GONE);
            }

            // ── 읽음 수 ──
            if (msg.getUnread_count() > 0) {
                binding.textUnread.setVisibility(View.VISIBLE);
                binding.textUnread.setText(String.valueOf(msg.getUnread_count()));
            } else {
                binding.textUnread.setVisibility(View.GONE);
            }
        }
    }

    static class OtherViewHolder extends RecyclerView.ViewHolder {
        private final RvMessageReceivedBinding binding;
        private final OnProfileClickListener profileClickListener;

        OtherViewHolder(RvMessageReceivedBinding binding, OnProfileClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.profileClickListener = listener;
        }

        void bind(MessageData msg, boolean showTime, boolean showSenderName,
                  OnImageClickListener imageClick) {

            // ── 닉네임 ──
            if (showSenderName) {
                binding.textSenderName.setText(msg.getSender_name());
                binding.textSenderName.setVisibility(View.VISIBLE);
            } else {
                binding.textSenderName.setVisibility(View.GONE);
            }

            List<String> images = msg.getImages();

            // ── 3분기 ──
            if (!msg.isImage()) {
                binding.textContent.setVisibility(View.VISIBLE);
                binding.imageContent.setVisibility(View.GONE);
                binding.rvImageGrid.setVisibility(View.GONE);
                binding.textContent.setText(msg.getContent());

            } else if (images.size() == 1) {
                binding.textContent.setVisibility(View.GONE);
                binding.imageContent.setVisibility(View.VISIBLE);
                binding.rvImageGrid.setVisibility(View.GONE);
                Glide.with(binding.getRoot().getContext())
                        .load(images.get(0))
                        .dontAnimate()
                        .placeholder(R.drawable.message_image_background)
                        .error(R.drawable.message_image_background)
                        .into(binding.imageContent);
                binding.imageContent.setOnClickListener(v -> {
                    if (imageClick != null) imageClick.onImageClick(images, 0);
                });

            } else {
                binding.textContent.setVisibility(View.GONE);
                binding.imageContent.setVisibility(View.GONE);
                binding.rvImageGrid.setVisibility(View.VISIBLE);
                new ImageGridAdapter(images, (urls, idx) -> {
                    if (imageClick != null) imageClick.onImageClick(urls, idx);
                }).attachTo(binding.rvImageGrid);
            }

            // ── 시간 ──
            if (showTime) {
                binding.textTime.setText(formatTime(msg.getCreated_at()));
                binding.textTime.setVisibility(View.VISIBLE);
            } else {
                binding.textTime.setVisibility(View.GONE);
            }

            // ── 읽음 수 ──
            if (msg.getUnread_count() > 0) {
                binding.textUnread.setVisibility(View.VISIBLE);
                binding.textUnread.setText(String.valueOf(msg.getUnread_count()));
            } else {
                binding.textUnread.setVisibility(View.GONE);
            }

            // ── 프로필 ──
            if (showSenderName) {
                binding.imgProfile.setVisibility(View.VISIBLE);
                Glide.with(binding.getRoot().getContext())
                        .load(msg.getSender_profile_image())
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(binding.imgProfile);
            } else {
                binding.imgProfile.setVisibility(View.INVISIBLE);
            }

            View.OnClickListener profileClick = v -> {
                if (profileClickListener != null)
                    profileClickListener.onProfileClick(msg.getSender_id());
            };
            binding.imgProfile.setOnClickListener(profileClick);
            binding.textSenderName.setOnClickListener(profileClick);
        }
    }

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

    // ── 시간 포맷 유틸 ────────────────────────────

    private static String minuteKey(String createdAt) {
        if (createdAt == null || createdAt.length() < 16) return "";
        return createdAt.substring(0, 16);
    }

    private boolean isLastInTimeRun(int position) {
        if (position == messages.size() - 1) return true;
        MessageData cur = messages.get(position);
        MessageData next = messages.get(position + 1);
        if (!cur.isIs_mine() || !next.isIs_mine()) {
            if (cur.getSender_id() != next.getSender_id()) return true;
        }
        return !minuteKey(cur.getCreated_at()).equals(minuteKey(next.getCreated_at()));
    }

    private boolean isFirstInSenderRun(int position) {
        if (position == 0) return true;
        MessageData cur = messages.get(position);
        MessageData prev = messages.get(position - 1);
        return cur.getSender_id() != prev.getSender_id()
                || !minuteKey(cur.getCreated_at()).equals(minuteKey(prev.getCreated_at()));
    }

    private static String formatTime(String createdAt) {
        if (createdAt == null || createdAt.length() < 16) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            java.util.Date date = sdf.parse(createdAt);
            SimpleDateFormat out = new SimpleDateFormat("a h:mm", Locale.KOREAN);
            out.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            return out.format(date);
        } catch (Exception e) {
            return createdAt.substring(11, 16);
        }
    }
}