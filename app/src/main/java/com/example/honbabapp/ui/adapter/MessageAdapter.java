package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.api.data.chat.MessageData;
import com.example.honbabapp.databinding.RvMessageReceivedBinding;
import com.example.honbabapp.databinding.RvMessageReviewPromptBinding;
import com.example.honbabapp.databinding.RvMessageSentBinding;
import com.example.honbabapp.databinding.RvMessageSystemBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 채팅 메시지 어댑터.
 *
 * 책임:
 *  - 메시지 리스트 보관 / ViewType 결정 / ViewHolder 연결
 *  - 낙관적(optimistic) UI: SENT 블록 ++ SENDING/FAILED 블록 불변식 유지
 *    (confirmMessage / addIncomingSent / markFailed / removeByClientMsgId)
 *  - 시간·발신자 묶음 표시 판정(isLastInTimeRun / isFirstInSenderRun)
 *
 * 실제 셀 렌더는 MineMessageViewHolder / OtherMessageViewHolder / SystemMessageViewHolder 가 담당.
 */
public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final int VIEW_TYPE_MINE = 1;
    static final int VIEW_TYPE_OTHER = 2;
    static final int VIEW_TYPE_SYSTEM = 3;
    static final int VIEW_TYPE_REVIEW_PROMPT = 4;

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

    /** 후기 안내 메시지의 "후기작성" 버튼 클릭 콜백 */
    public interface OnReviewPromptClickListener {
        void onReviewPromptClick();
    }

    /** 실패 메시지 재전송/삭제 콜백 */
    public interface OnFailedMessageListener {
        void onRetry(MessageData message);
        void onDelete(MessageData message);
    }

    private final OnProfileClickListener profileClickListener;
    private OnImageClickListener imageClickListener;
    private OnFailedMessageListener failedMessageListener;
    private OnReviewPromptClickListener reviewPromptClickListener;

    public MessageAdapter() {
        this.profileClickListener = null;
    }

    public MessageAdapter(OnProfileClickListener profileClickListener) {
        this.profileClickListener = profileClickListener;
    }

    public void setImageClickListener(OnImageClickListener listener) {
        this.imageClickListener = listener;
    }

    public void setFailedMessageListener(OnFailedMessageListener listener) {
        this.failedMessageListener = listener;
    }

    public void setReviewPromptClickListener(OnReviewPromptClickListener listener) {
        this.reviewPromptClickListener = listener;
    }



    // ── ViewType ─────────────────────────────────

    @Override
    public int getItemViewType(int position) {
        MessageData message = messages.get(position);
        if (message.isReviewPrompt()) return VIEW_TYPE_REVIEW_PROMPT;
        if ("system".equals(message.getMsg_type())) return VIEW_TYPE_SYSTEM;
        if (message.isIs_mine()) return VIEW_TYPE_MINE;
        return VIEW_TYPE_OTHER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_MINE) {
            return new MineMessageViewHolder(RvMessageSentBinding.inflate(inflater, parent, false));
        } else if (viewType == VIEW_TYPE_OTHER) {
            return new OtherMessageViewHolder(
                    RvMessageReceivedBinding.inflate(inflater, parent, false), profileClickListener);
        } else if (viewType == VIEW_TYPE_REVIEW_PROMPT) {
            return new ReviewPromptViewHolder(RvMessageReviewPromptBinding.inflate(inflater, parent, false));
        } else {
            return new SystemMessageViewHolder(RvMessageSystemBinding.inflate(inflater, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageData message = messages.get(position);
        if (holder instanceof MineMessageViewHolder) {
            ((MineMessageViewHolder) holder).bind(
                    message, isLastInTimeRun(position), imageClickListener, failedMessageListener);
        } else if (holder instanceof OtherMessageViewHolder) {
            ((OtherMessageViewHolder) holder).bind(
                    message, isLastInTimeRun(position), isFirstInSenderRun(position), imageClickListener);
        } else if (holder instanceof ReviewPromptViewHolder) {
            ((ReviewPromptViewHolder) holder).bind(message, reviewPromptClickListener);
        } else if (holder instanceof SystemMessageViewHolder) {
            ((SystemMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ── 데이터 갱신 ───────────────────────────────

    public void setMessages(List<MessageData> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    public void addMessage(MessageData message) {
        messages.add(message);
        int newIndex = messages.size() - 1;
        notifyItemInserted(newIndex);
        if (messages.size() >= 2) notifyItemChanged(newIndex - 1);
    }

    public void prependMessages(List<MessageData> olderMessages) {
        messages.addAll(0, olderMessages);
        notifyItemRangeInserted(0, olderMessages.size());
        if (!olderMessages.isEmpty() && messages.size() > olderMessages.size()) {
            notifyItemChanged(olderMessages.size());
        }
    }

    public void decrementUnreadForMessages(Set<Integer> messageIds) {
        for (int index = 0; index < messages.size(); index++) {
            MessageData message = messages.get(index);
            if (messageIds.contains(message.getMessage_id()) && message.getUnread_count() > 0) {
                message.setUnread_count(message.getUnread_count() - 1);
                notifyItemChanged(index);
            }
        }
    }

    // ── 낙관적(Optimistic) UI ─────────────────────

    /**
     * 서버 echo 수신 시 로컬 optimistic(SENDING) 메시지를 서버 확정 데이터로 교체.
     * optimistic 을 못 찾으면(구 서버 echo) addIncomingSent 로 처리.
     */
    public void confirmMessage(String clientMsgId, MessageData serverMessage) {
        int oldIndex = findByClientMsgId(clientMsgId);
        if (oldIndex < 0) {
            addIncomingSent(serverMessage);
            return;
        }
        // optimistic 제거 후, 제거 직후 기준 firstPendingIndex(= SENT 블록 끝)에 재삽입
        messages.remove(oldIndex);
        int insertIndex = firstPendingIndex();
        messages.add(insertIndex, serverMessage);

        if (oldIndex == insertIndex) {
            notifyItemChanged(insertIndex);          // 위치 동일 → 깜빡임 제거
        } else {
            notifyItemRemoved(oldIndex);
            notifyItemInserted(insertIndex);
        }
        refreshNeighbors(insertIndex);
    }

    /** client_msg_id 로 찾아 STATUS_FAILED 마킹 */
    public void markFailed(String clientMsgId) {
        int index = findByClientMsgId(clientMsgId);
        if (index >= 0) {
            messages.get(index).setStatus(MessageData.STATUS_FAILED);
            notifyItemChanged(index);
        }
    }

    /** 로컬 메시지 제거 (실패 → 삭제 버튼, 서버 호출 없음) */
    public void removeByClientMsgId(String clientMsgId) {
        int index = findByClientMsgId(clientMsgId);
        if (index >= 0) {
            messages.remove(index);
            notifyItemRemoved(index);
            if (index > 0) notifyItemChanged(index - 1);  // 직전 메시지 시간 재계산
        }
    }

    /**
     * 서버 확정(SENT) 메시지를 SENT 블록의 끝(= pending 블록 바로 위)에 삽입한다.
     * → 전송중/실패 메시지는 항상 리스트 맨 아래에 유지된다.
     */
    public void addIncomingSent(MessageData incomingMessage) {
        int insertIndex = firstPendingIndex();
        messages.add(insertIndex, incomingMessage);
        notifyItemInserted(insertIndex);
        refreshNeighbors(insertIndex);
    }

    private int findByClientMsgId(String clientMsgId) {
        if (clientMsgId == null) return -1;
        for (int index = messages.size() - 1; index >= 0; index--) {
            if (clientMsgId.equals(messages.get(index).getClient_msg_id())) return index;
        }
        return -1;
    }

    /**
     * 불변식 유지를 위한 첫 pending 인덱스.
     * 리스트는 [SENT 블록] ++ [SENDING/FAILED 블록] 구조이므로,
     * 앞에서부터 STATUS_SENT 가 아닌 첫 항목 인덱스가 pending 블록의 시작점.
     * pending 항목이 없으면 리스트 크기를 반환(= SENT 블록 끝).
     */
    private int firstPendingIndex() {
        for (int index = 0; index < messages.size(); index++) {
            if (messages.get(index).getStatus() != MessageData.STATUS_SENT) {
                return index;
            }
        }
        return messages.size();
    }

    /** 삽입/이동 후 인접 항목의 시간·발신자 묶음 표시 재계산 */
    private void refreshNeighbors(int index) {
        if (index - 1 >= 0) notifyItemChanged(index - 1);
        if (index + 1 < messages.size()) notifyItemChanged(index + 1);
    }

    // ── 그룹(묶음) 표시 판정 ───────────────────────

    private static String minuteKey(String createdAt) {
        if (createdAt == null || createdAt.length() < 16) return "";
        return createdAt.substring(0, 16);
    }

    /** 같은 분/같은 발신자 묶음에서 마지막 메시지인지 → 시간 표시 여부 */
    private boolean isLastInTimeRun(int position) {
        if (position == messages.size() - 1) return true;
        MessageData current = messages.get(position);
        MessageData next = messages.get(position + 1);
        if (!current.isIs_mine() || !next.isIs_mine()) {
            if (current.getSender_id() != next.getSender_id()) return true;
        }
        return !minuteKey(current.getCreated_at()).equals(minuteKey(next.getCreated_at()));
    }

    /** 같은 분/같은 발신자 묶음에서 첫 메시지인지 → 프로필/닉네임 표시 여부 */
    private boolean isFirstInSenderRun(int position) {
        if (position == 0) return true;
        MessageData current = messages.get(position);
        MessageData previous = messages.get(position - 1);
        return current.getSender_id() != previous.getSender_id()
                || !minuteKey(current.getCreated_at()).equals(minuteKey(previous.getCreated_at()));
    }
}
