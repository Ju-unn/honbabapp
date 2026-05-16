package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.api.data.meeting.MeetingParticipant;
import com.example.honbabapp.databinding.RvChatParticipantBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 채팅방 참가자 리스트 어댑터
 *
 * - 행 1개당 : 원형 프로필 + 닉네임 + (방장이면) 방장 뱃지
 * - 행 클릭 시 : OnParticipantClickListener.onClick(participant) 호출
 *   → ChatRoomParticipantDetailActivity 로 이동
 *
 * 방장 판별:
 *   - participant.user_id == hostId 이면 방장
 *   - hostId 는 Activity 에서 setHostId() 로 주입
 *
 * 사용:
 *   ChatParticipantAdapter adapter = new ChatParticipantAdapter(this::onClick);
 *   adapter.setHostId(meeting.getHost().getUser_id());
 *   adapter.setItems(meeting.getParticipants());
 *   binding.rvParticipants.setAdapter(adapter);
 */
public class ChatParticipantAdapter
        extends RecyclerView.Adapter<ChatParticipantAdapter.ViewHolder> {

    // ─────────────────────────────────────────
    // 클릭 리스너 인터페이스
    // ─────────────────────────────────────────

    public interface OnParticipantClickListener {
        void onClick(MeetingParticipant participant);
    }

    // ─────────────────────────────────────────
    // 내부 상태
    // ─────────────────────────────────────────

    private List<MeetingParticipant> participantsItem = new ArrayList<>();
    private int hostId = -1;                      // 방장의 user_id (뱃지 표시 판단용)
    private final OnParticipantClickListener listener;

    // ─────────────────────────────────────────
    // 생성자
    // ─────────────────────────────────────────

    public ChatParticipantAdapter(OnParticipantClickListener listener) {
        this.listener = listener;
    }

    // ─────────────────────────────────────────
    // 데이터 갱신 메서드 (외부에서 호출)
    // ─────────────────────────────────────────

    /** 참가자 리스트 전체 교체 후 다시 그리기 */
    public void setItems(List<MeetingParticipant> newItems) {
        this.participantsItem = (newItems != null) ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    /** 방장 ID 주입 (방장 뱃지 표시용) */
    public void setHostId(int hostId) {
        this.hostId = hostId;
        notifyDataSetChanged();   // 호스트가 바뀌면 뱃지 위치도 바뀌므로 갱신
    }

    // ─────────────────────────────────────────
    // RecyclerView.Adapter 필수 메서드 4종
    // ─────────────────────────────────────────

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvChatParticipantBinding binding = RvChatParticipantBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(participantsItem.get(position));
    }

    @Override
    public int getItemCount() {
        return participantsItem.size();
    }

    // ─────────────────────────────────────────
    // ViewHolder
    // ─────────────────────────────────────────

    class ViewHolder extends RecyclerView.ViewHolder {

        private final RvChatParticipantBinding binding;

        ViewHolder(RvChatParticipantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MeetingParticipant participant) {
            // ── 닉네임 ──
            binding.tvParticipantNickname.setText(participant.getUser_nickname());

            // ── 프로필 이미지 (원형) ──
            // ShapeableImageView 는 자동으로 원형 마스킹됨 (XML 의 shapeAppearanceOverlay)
            String imageUrl = participant.getUser_profile_image();
            Glide.with(binding.getRoot().getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .centerCrop()
                    .into(binding.ivParticipantAvatar);

            // ── 방장 뱃지 toggle ──
            boolean isHost = (participant.getUser_id() == hostId);
            binding.tvHostBadge.setVisibility(isHost ? View.VISIBLE : View.GONE);

            // ── 행 클릭 → 외부로 위임 ──
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(participant);
                }
            });
        }
    }
}