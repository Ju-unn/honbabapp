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
 * - 행 1개당 : 원형 프로필 + 닉네임 + (방장이면) 방장 뱃지 + (조건부) 3-dot 메뉴
 * - 행 클릭 시 : OnParticipantClickListener.onClick(participant) 호출
 *   → ChatRoomParticipantDetailActivity 로 이동
 * - 3-dot 클릭 시 : OnParticipantMenuClickListener.onMenuClick(participant) 호출
 *   → ChatParticipantActionBottomSheetDialog 띄움
 *
 * 방장 뱃지 표시 조건:
 *   - participant.user_id == hostId
 *
 * 3-dot 메뉴 표시 조건:
 *   - (currentUserId == hostId)  → 내가 방장
 *   AND
 *   - (participant.user_id != hostId) → 대상이 방장이 아님 (자기 자신 강퇴 방지)
 *
 *   즉 "내가 방장일 때, 내가 아닌 일반 멤버 행에만" 3-dot 노출.
 *
 */
public class ChatParticipantAdapter
        extends RecyclerView.Adapter<ChatParticipantAdapter.ViewHolder> {

    // ─────────────────────────────────────────
    // 클릭 리스너 인터페이스
    // ─────────────────────────────────────────

    /** 행 전체 클릭 — 프로필 상세 화면으로 이동 */
    public interface OnParticipantClickListener {
        void onClick(MeetingParticipant participant);
    }

    /** 3-dot 메뉴 클릭 — 강퇴 등 액션 바텀시트 띄움 */
    public interface OnParticipantMenuClickListener {
        void onMenuClick(MeetingParticipant participant);
    }

    // ─────────────────────────────────────────
    // 내부 상태
    // ─────────────────────────────────────────

    private List<MeetingParticipant> participantsItem = new ArrayList<>();
    private int hostId        = -1;                  // 방장의 user_id (뱃지/메뉴 판단용)
    private int currentUserId = -1;                  // 현재 로그인한 user_id (메뉴 노출 판단용)

    private final OnParticipantClickListener listener;
    private OnParticipantMenuClickListener menuClickListener;   // optional

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

    /** 방장 ID 주입 (방장 뱃지 + 3-dot 표시 판단용) */
    public void setHostId(int hostId) {
        this.hostId = hostId;
        notifyDataSetChanged();   // 호스트가 바뀌면 뱃지/메뉴 위치도 바뀌므로 갱신
    }

    /** 현재 로그인한 user_id 주입 (내가 방장인지 판단용) */
    public void setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }

    /** 3-dot 메뉴 클릭 리스너 등록 (Activity 에서 등록) */
    public void setOnMenuClickListener(OnParticipantMenuClickListener listener) {
        this.menuClickListener = listener;
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

            // ── 3-dot 메뉴 표시/숨김 ──
            // 조건: 내가 방장이고 + 대상이 방장이 아닐 때만
            //  방장 본인 행에는 메뉴 자체를 안 보여줌)
            boolean iAmHost      = (currentUserId == hostId) && (currentUserId != -1);
            boolean targetIsHost = isHost;
            boolean showMenu     = iAmHost && !targetIsHost;
            binding.btnParticipantMenu.setVisibility(showMenu ? View.VISIBLE : View.GONE);

            // ── 3-dot 클릭 → 외부로 위임 ──
            binding.btnParticipantMenu.setOnClickListener(v -> {
                if (menuClickListener != null) {
                    menuClickListener.onMenuClick(participant);
                }
            });

            // ── 행 클릭 → 외부로 위임 (프로필 상세) ──
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(participant);
                }
            });
        }
    }
}