package com.example.honbabapp.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.honbabapp.databinding.BottomsheetChatAttachBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ChatAttachBottomSheetDialog extends BottomSheetDialogFragment {

    // ─────────────────────────────────────────
    // 외부 주입 필드 (Activity 에서 직접 세팅)
    // 프로젝트 컨벤션 : ChatParticipantActionBottomSheetDialog 와 동일하게
    // public 필드 + listener 형태 사용
    // ─────────────────────────────────────────

    public OnAttachActionListener listener;

    public interface OnAttachActionListener {
        /** 사용자가 "갤러리" 선택 */
        void onPickGallery();
        /** 사용자가 "카메라" 선택 */
        void onPickCamera();
    }

    // ─────────────────────────────────────────
    // 내부 상태
    // ─────────────────────────────────────────

    private BottomsheetChatAttachBinding binding;

    // ─────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomsheetChatAttachBinding.inflate(inflater , container , false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 갤러리 -> 콜백 호출후 dismiss
        // 실제 PickMultipleVisualMedia 실행은 Activity 책임
        binding.btnPickGallery.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPickGallery();
            }
            dismiss();
        });

        // 카메라 → 콜백 호출 후 dismiss
        // 실제 권한 체크 + TakePicture 실행은 Activity 책임
        binding.btnPickCamera.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPickCamera();
            }
            dismiss();
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
