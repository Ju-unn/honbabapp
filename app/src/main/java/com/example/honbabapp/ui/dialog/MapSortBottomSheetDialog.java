package com.example.honbabapp.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.honbabapp.databinding.PageMapBottomSheetSortBinding;
import com.example.honbabapp.ui.fragment.MapFragment;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MapSortBottomSheetDialog extends BottomSheetDialogFragment {

    public PageMapBottomSheetSortBinding binding;
    public interface OnSortSelectedListener {
        void OnSortSelected(String sortOption);
    }
    public OnSortSelectedListener listener;

    public String currentSortOption;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = PageMapBottomSheetSortBinding.inflate(inflater , container , false);

        updateCheckMarksWithBinding();

        /// 닫기 버튼 클릭
        binding.btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        /// 전체 버튼 클릭
        binding.sortAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.OnSortSelected("전체");
                dismiss();
            }
        });

        /// 인기순 버튼 클릭
        binding.sortPopular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.OnSortSelected("인기순");
                dismiss();
            }
        });

        /// 거리순 클릭
        binding.sortDistance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.OnSortSelected("거리순");
                dismiss();
            }
        });

        return binding.getRoot();
    }

    /// 체크표시 여부
    public void updateCheckMarksWithBinding() {
        /// 모든 체크 숨기기
        binding.checkAll.setVisibility(View.GONE);
        binding.checkPopular.setVisibility(View.GONE);
        binding.checkDistance.setVisibility(View.GONE);

        /// 현재 선택된 정렬 옵션에만 체크 표시
        if (currentSortOption.equals("전체")) {
            binding.checkAll.setVisibility(View.VISIBLE);
        } else if (currentSortOption.equals("인기순")) {
            binding.sortPopular.setVisibility(View.VISIBLE);
            binding.checkPopular.setVisibility(View.VISIBLE);
        } else if (currentSortOption.equals("거리순")) {
            binding.sortDistance.setVisibility(View.VISIBLE);
            binding.checkDistance.setVisibility(View.VISIBLE);
        }
    }

}
