package com.example.honbabapp.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.honbabapp.databinding.BottomsheetMapCategoryBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Arrays;
import java.util.List;

public class MapCategoryBottomSheetDialog extends BottomSheetDialogFragment {
    private BottomsheetMapCategoryBinding binding;

    public interface OnCategorySelectedListener {
        void onCategorySelected(String category);
    }
    public OnCategorySelectedListener listener;
    public String currentCategory = "전체";

    private List<String> categories;
    private List<Button> buttons;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomsheetMapCategoryBinding.inflate(inflater, container, false);

        categories = Arrays.asList("전체", "한식", "중식", "일식", "양식", "오마카세", "술집", "카페");
        buttons = Arrays.asList(
                binding.btnCategoryAll,
                binding.btnCategoryKorean,
                binding.btnCategoryChinese,
                binding.btnCategoryJapanese,
                binding.btnCategoryWestern,
                binding.btnCategoryOmakase,
                binding.btnCategoryPub,
                binding.btnCategoryCafe
        );

        // 사전 선택 상태 반영 (dialog 열릴 때 현재 카테고리)
        updateCategorySelection();

        for (int i = 0; i < buttons.size(); i++) {
            final String category = categories.get(i);
            buttons.get(i).setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategorySelected(category); //mapfragment로 전달
                }
                dismiss();
            });
        }

        //닫기 버튼
        binding.btnClose.setOnClickListener(v -> dismiss());

        return binding.getRoot();
    }

    private void updateCategorySelection() {
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setSelected(categories.get(i).equals(currentCategory));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
