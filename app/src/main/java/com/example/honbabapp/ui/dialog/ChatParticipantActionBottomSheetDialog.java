package com.example.honbabapp.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.honbabapp.api.data.meeting.MeetingParticipant;
import com.example.honbabapp.databinding.BottomsheetChatParticipantActionBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * 채팅방 참가자 액션 바텀시트
 * 강퇴하기
 */
public class ChatParticipantActionBottomSheetDialog extends BottomSheetDialogFragment {

    // ─────────────────────────────────────────
    // 외부 주입 필드 (Activity 에서 직접 세팅)
    // 프로젝트 컨벤션 : 기존 LocationSelectionBottomSheetDialog 와 동일하게
    // public 필드 + listener 형태 사용
    // ─────────────────────────────────────────

    /** 액션 대상이 되는 참가자 */
    public MeetingParticipant participant;

    /** 강퇴 클릭 콜백 */
    public OnKickClickListener listener;

    public interface OnKickClickListener {
        void onKick(MeetingParticipant participant);
    }

    // ─────────────────────────────────────────
    // 내부 상태
    // ─────────────────────────────────────────

    private BottomsheetChatParticipantActionBinding binding;

    // ─────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomsheetChatParticipantActionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 대상 정보가 없으면 즉시 닫기 (방어 코드)
        if (participant == null) {
            dismiss();
            return;
        }

        // 닉네임 표시
        binding.tvTargetNickname.setText(participant.getUser_nickname());

        // 강퇴하기 → 콜백 호출 후 dismiss
        // 실제 확인 다이얼로그 → API 호출은 Activity 책임
        // (다이얼로그는 "사용자가 강퇴를 의도했다" 만 알림)
        binding.btnKick.setOnClickListener(v -> {
            if (listener != null) {
                listener.onKick(participant);
            }
            dismiss();
        });

        // 취소 → 그냥 닫기
        binding.btnCancel.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}