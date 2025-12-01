package com.example.honbabapp.ui.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.honbabapp.R;
import com.example.honbabapp.databinding.FragmentChatBinding;
import com.example.honbabapp.databinding.FragmentMapBinding;
import com.example.honbabapp.databinding.PageMapBottomSheetSortBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class MapFragment extends Fragment {

    private FragmentMapBinding fragmentMapbinding;
    private PageMapBottomSheetSortBinding bottomSheetSortBinding;
    private String selectedCategory;
    private String currentSortOption = "전체";

    public MapFragment() {
        Log.i("MapFragment" , "생성");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("MapFragment" , "onCreateView");

        fragmentMapbinding = FragmentMapBinding.inflate(inflater , container , false);
        return fragmentMapbinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i("MapFragment", "onViewCreated");

        // 정렬 버튼 클릭 이벤트
        initSortButton();

    }
    public void initSortButton() {
        fragmentMapbinding.btnSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSortButtonSheet();
            }
        });
    }

    public void showSortButtonSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetSortBinding = PageMapBottomSheetSortBinding.inflate(LayoutInflater.from(getContext()));
        bottomSheetDialog.setContentView(bottomSheetSortBinding.getRoot());

        updateCheckMarksWithBinding(bottomSheetSortBinding);

        /// 닫기 버튼 클릭
        bottomSheetSortBinding.btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
            }
        });

        /// 전체 버튼 클릭
        bottomSheetSortBinding.sortAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentSortOption = "전체";
                updateCheckMarksWithBinding(bottomSheetSortBinding);
                bottomSheetDialog.dismiss();
            }
        });

        /// 인기순 버튼 클릭
        bottomSheetSortBinding.sortPopular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentSortOption = "인기순";
                updateCheckMarksWithBinding(bottomSheetSortBinding);
                bottomSheetDialog.dismiss();
            }
        });

        /// 거리순 클릭
        bottomSheetSortBinding.sortDistance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentSortOption = "거리순";
                updateCheckMarksWithBinding(bottomSheetSortBinding);
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog.show();
    }

    public void updateCheckMarksWithBinding(PageMapBottomSheetSortBinding bottomSheetSortBinding) {
        /// 모든 체크 숨기기
        bottomSheetSortBinding.checkAll.setVisibility(View.GONE);
        bottomSheetSortBinding.checkPopular.setVisibility(View.GONE);
        bottomSheetSortBinding.checkDistance.setVisibility(View.GONE);

        /// 현재 선택된 정렬 옵션에만 체크 표시
        if (currentSortOption.equals("전체")) {
            bottomSheetSortBinding.checkAll.setVisibility(View.VISIBLE);
        } else if (currentSortOption.equals("인기순")) {
            bottomSheetSortBinding.sortPopular.setVisibility(View.VISIBLE);
        } else if (currentSortOption.equals("거리순")) {
            bottomSheetSortBinding.sortDistance.setVisibility(View.VISIBLE);
        }

    }
}