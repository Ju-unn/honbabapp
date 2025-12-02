package com.example.honbabapp.ui.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.honbabapp.MainActivity;
import com.example.honbabapp.R;
import com.example.honbabapp.databinding.FragmentMapBinding;
import com.example.honbabapp.databinding.PageMapBottomSheetFilterBinding;
import com.example.honbabapp.databinding.PageMapBottomSheetSortBinding;
import com.example.honbabapp.databinding.PageMapFilterFoodBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;

import java.util.HashSet;

public class MapFragment extends Fragment {

    private FragmentMapBinding fragmentMapbinding;
    private PageMapBottomSheetSortBinding bottomSheetSortBinding;
    private PageMapBottomSheetFilterBinding bottomSheetFilterBinding;
    private HashSet<String> selectedFoodTypes = new HashSet<>();
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
        // 뒤로가기 버튼
        onBackButtonClick();

        // 정렬 버튼 초기화
        initSortButton();

        // 필터 버튼 초기화
        initFilterButton();
    }

    public void onBackButtonClick() {
        fragmentMapbinding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity mainActivity = (MainActivity) requireActivity();
                mainActivity.navigationToHome();
            }
        });
    }

    /// 인기순 , 전체 , 거리순 필터 버튼
    public void initSortButton() {
        fragmentMapbinding.btnSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSortButtonSheet();
            }
        });
    }

    /// 버튼 클릭 이벤트
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
                updateSortButtonText();
                bottomSheetDialog.dismiss();
            }
        });

        /// 인기순 버튼 클릭
        bottomSheetSortBinding.sortPopular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentSortOption = "인기순";
                updateCheckMarksWithBinding(bottomSheetSortBinding);
                updateSortButtonText();
                bottomSheetDialog.dismiss();
            }
        });

        /// 거리순 클릭
        bottomSheetSortBinding.sortDistance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentSortOption = "거리순";
                updateCheckMarksWithBinding(bottomSheetSortBinding);
                updateSortButtonText();
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog.show();
    }

    /// 체크표시 여부
    public void updateCheckMarksWithBinding(PageMapBottomSheetSortBinding bottomSheetSortBinding) {
        /// 모든 체크 숨기기
        bottomSheetSortBinding.checkAll.setVisibility(View.GONE);
        bottomSheetSortBinding.checkPopular.setVisibility(View.GONE);
        bottomSheetSortBinding.checkDistance.setVisibility(View.GONE);

        /// 현재 선택된 정렬 옵션에만 체크 표시
        if (currentSortOption.equals("전체")) {
            bottomSheetSortBinding.checkAll.setVisibility(View.VISIBLE);
        } else if (currentSortOption.equals("인기순")) {
            bottomSheetSortBinding.checkPopular.setVisibility(View.VISIBLE);
        } else if (currentSortOption.equals("거리순")) {
            bottomSheetSortBinding.checkDistance.setVisibility(View.VISIBLE);
        }

    }

    /// fragment_map.xml textview수정
    public void updateSortButtonText() {
        if (fragmentMapbinding != null & fragmentMapbinding.tvSortOption != null) {
            fragmentMapbinding.tvSortOption.setText(currentSortOption);
        }
    }


    /// 필터 버튼 클릭 이벤트
    public void initFilterButton() {
        fragmentMapbinding.btnFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFilterBottomSheet();
            }
        });
    }

    public void showFilterBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetFilterBinding = PageMapBottomSheetFilterBinding.inflate(LayoutInflater.from(getContext()));
        bottomSheetDialog.setContentView(bottomSheetFilterBinding.getRoot());

        /// 음식 버튼 클릭시
        setupFoodButtons();

        /// 쓰레기통 아이콘 클릭시
        bottomSheetFilterBinding.icDeleteAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedFoodTypes.clear();
                resetAllFoodButtons();
                updateSelectedChips();
            }
        });

        /// close 아이콘 클릭시
        bottomSheetFilterBinding.btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
            }
        });

        /// 초기화 버튼 클릭시
        bottomSheetFilterBinding.btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedFoodTypes.clear();
                resetAllFoodButtons();
                updateSelectedChips();
            }
        });

        /// 결과 보기 버튼 클릭시
        bottomSheetFilterBinding.btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedFoodTypes.clear();
                updateSelectedChips();
                resetAllFoodButtons();
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog.show();
    }

    public void setupFoodButtons() {
        // 한식
        setupFoodButton(bottomSheetFilterBinding.pageFood.btnKorean, "한식");
        // 중식
        setupFoodButton(bottomSheetFilterBinding.pageFood.btnChinese, "중식");
        // 일식
        setupFoodButton(bottomSheetFilterBinding.pageFood.btnJapanese, "일식");
        // 오마카세
        setupFoodButton(bottomSheetFilterBinding.pageFood.btnOmakase, "오마카세");
        // 양식
        setupFoodButton(bottomSheetFilterBinding.pageFood.btnWestern, "양식");
        // 술집
        setupFoodButton(bottomSheetFilterBinding.pageFood.btnBar, "술집");
        // 카페
        setupFoodButton(bottomSheetFilterBinding.pageFood.btnCafe, "카페");
    }

    /// 선택된 버튼 selected 활성화
    public void setupFoodButton(Button button , String foodType) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (button.isSelected()) {
                    button.setSelected(false);
                    selectedFoodTypes.remove(foodType);
                }
                else {
                    button.setSelected(true);
                    selectedFoodTypes.add(foodType);
                }

                updateSelectedChips();
            }
        });
    }

    /// 필터 chip 생성
    public Chip createFilterChip(String foodType) {
        Chip chip = new Chip(requireContext());
        chip.setText(foodType);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.primary_100);
        chip.setTextColor(getResources().getColor(android.R.color.white , null));
        chip.setCloseIconTintResource(android.R.color.white);
        chip.setChipStrokeWidth(0);
        chip.setEnsureMinTouchTargetSize(false);

        chip.setOnCloseIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedFoodTypes.remove(foodType);
                uncheckFoodButton(foodType);
                updateSelectedChips();
            }
        });
        return chip;
    }

    /// 선택된 필터 chip
    public void updateSelectedChips() {
        //chip 초기화
        bottomSheetFilterBinding.chipGroupSelected.removeAllViews();

        if (selectedFoodTypes.isEmpty()){
            bottomSheetFilterBinding.selectedFiltersScroll.setVisibility(View.GONE);
            return;
        }

        bottomSheetFilterBinding.selectedFiltersScroll.setVisibility(View.VISIBLE);

        for (String foodType : selectedFoodTypes) {
            Chip chip = createFilterChip(foodType);
            bottomSheetFilterBinding.chipGroupSelected.addView(chip);
        }
    }

    public void uncheckFoodButton(String foodType) {
        Button button = null;

        switch (foodType) {
            case "한식":
                button = bottomSheetFilterBinding.pageFood.btnKorean;
                break;
            case "중식":
                button = bottomSheetFilterBinding.pageFood.btnChinese;
                break;
            case "일식":
                button = bottomSheetFilterBinding.pageFood.btnJapanese;
                break;
            case "오마카세":
                button = bottomSheetFilterBinding.pageFood.btnOmakase;
                break;
            case "양식":
                button = bottomSheetFilterBinding.pageFood.btnWestern;
                break;
            case "술집":
                button = bottomSheetFilterBinding.pageFood.btnBar;
                break;
            case "카페":
                button = bottomSheetFilterBinding.pageFood.btnCafe;
                break;
        }

        if (button != null) {
            button.setSelected(false);
        }
    }

    /// 모든 음식 버튼 초기화
    private void resetAllFoodButtons() {
        bottomSheetFilterBinding.pageFood.btnKorean.setSelected(false);
        bottomSheetFilterBinding.pageFood.btnChinese.setSelected(false);
        bottomSheetFilterBinding.pageFood.btnJapanese.setSelected(false);
        bottomSheetFilterBinding.pageFood.btnOmakase.setSelected(false);
        bottomSheetFilterBinding.pageFood.btnWestern.setSelected(false);
        bottomSheetFilterBinding.pageFood.btnBar.setSelected(false);
        bottomSheetFilterBinding.pageFood.btnCafe.setSelected(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fragmentMapbinding = null;
        bottomSheetSortBinding = null;
        bottomSheetFilterBinding = null;
    }

}