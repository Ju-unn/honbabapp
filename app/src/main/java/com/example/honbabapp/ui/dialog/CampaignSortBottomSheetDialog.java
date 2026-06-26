package com.example.honbabapp.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.honbabapp.databinding.BottomsheetCampaignSortBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * 체험단 정렬 바텀시트
 * MapSortBottomSheetDialog 동일 패턴
 *
 * 정렬 옵션: 최신순 / 마감임박순 / 인기순
 */
public class CampaignSortBottomSheetDialog extends BottomSheetDialogFragment {

    private BottomsheetCampaignSortBinding binding;

    public interface OnSortSelectedListener {
        void onSortSelected(String sortOption);
    }

    public OnSortSelectedListener listener;
    public String currentSortOption = "최신순";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = BottomsheetCampaignSortBinding.inflate(inflater, container, false);

        updateCheckMarks();

        binding.btnClose.setOnClickListener(v -> dismiss());

        binding.sortLatest.setOnClickListener(v -> {
            notifySort("최신순");
        });

        binding.sortDeadline.setOnClickListener(v -> {
            notifySort("마감임박순");
        });

        binding.sortPopular.setOnClickListener(v -> {
            notifySort("인기순");
        });

        return binding.getRoot();
    }

    private void notifySort(String sortOption) {
        if (listener != null) listener.onSortSelected(sortOption);
        dismiss();
    }

    /**
     * 현재 선택된 정렬에만 체크 표시
     * MapSortBottomSheetDialog.updateCheckMarksWithBinding() 동일 패턴
     */
    private void updateCheckMarks() {
        binding.checkLatest.setVisibility(View.GONE);
        binding.checkDeadline.setVisibility(View.GONE);
        binding.checkPopular.setVisibility(View.GONE);

        switch (currentSortOption) {
            case "최신순":
                binding.checkLatest.setVisibility(View.VISIBLE);
                break;
            case "마감임박순":
                binding.checkDeadline.setVisibility(View.VISIBLE);
                break;
            case "인기순":
                binding.checkPopular.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}