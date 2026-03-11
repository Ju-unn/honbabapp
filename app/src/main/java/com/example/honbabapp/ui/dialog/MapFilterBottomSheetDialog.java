package com.example.honbabapp.ui.dialog;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.honbabapp.R;
import com.example.honbabapp.databinding.BottomsheetMapFilterBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MapFilterBottomSheetDialog extends BottomSheetDialogFragment {

    BottomsheetMapFilterBinding binding;

    // ──────────────────────────────────────────────
    // 콜백 인터페이스
    // MapFragment가 구현 → 필터 적용 시 결과 전달
    // ──────────────────────────────────────────────
    public interface OnFilterAppliedListener {
        void onFilterApplied(Set<String> categories, int minPrice, int maxPrice, String date , String time);
    }
    public OnFilterAppliedListener listener;

    // ──────────────────────────────────────────────
    // 외부 주입값
    // HomeFragment → MapFragment → 여기로 전달되는 사전 선택 카테고리
    // ──────────────────────────────────────────────
    public String preSelectedCategory = null;

    // 필터 재오픈 시 이전 상태 복원용
    public Set<String> initialSelectedCategories = null;
    public int initialMinPrice = 0;
    public int initialMaxPrice = 100000;
    // 필터 재오픈 시 이전 상태 복원용

    private final Set<String> selectedCategories = new HashSet<>();
    private int minPrice = 0;
    private int maxPrice = 100000;
    private String selectedDate = null;
    private String selectedTime = null;

    private List<String> categoryNames;
    private List<Button> categoryButtons;

    private List<Button> priceButtons;
    private Button selectedPriceButton = null;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            FrameLayout bottomSheet = ((BottomSheetDialog) d )
                    .findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //bottomsheet_map_filter.xml을 binding으로 가져와서 사용
        binding = BottomsheetMapFilterBinding.inflate(inflater , container , false);

        setupCategoryButtons();
        setupPriceSlider();
        setupDateTimePickers();

        // 휴지통 : 모든 카테고리 초기화
        binding.btnTrash.setOnClickListener(v ->  clearAllCategories());

        // 닫기 버튼
        binding.btnClose.setOnClickListener(v -> dismiss());

        // 적용하기 버튼
        binding.btnApplyFilter.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFilterApplied(
                        new HashSet<>(selectedCategories),
                        minPrice,
                        maxPrice,
                        selectedDate,
                        selectedTime
                );
            }
            dismiss();
        });

        // 필터 버튼 재클릭시 기존 카테고리 등록
        if (preSelectedCategory != null && !preSelectedCategory.equals("전체")) {
            toggleCategory(preSelectedCategory);
        } else if (initialSelectedCategories != null) {
            for (String cat : initialSelectedCategories) {
                toggleCategory(cat);
            }
        }

        // 가격 슬라이더는 항상 이전 값으로 복원 (Home에서 왔을 때도 유지)
        setPrice(initialMinPrice, initialMaxPrice);

        return binding.getRoot();
    }

    // ──────────────────────────────────────────────
    // 카테고리 버튼 초기화
    // ──────────────────────────────────────────────
    private void setupCategoryButtons() {
        categoryNames = Arrays.asList("한식", "중식", "일식", "양식", "오마카세", "술집", "카페");
        categoryButtons = Arrays.asList(
                binding.btnKorean,
                binding.btnChinese,
                binding.btnJapanese,
                binding.btnWestern,
                binding.btnOmakase,
                binding.btnPub,
                binding.btnCafe
        );

        for (int i = 0; i < categoryButtons.size(); i++) {
            final String category = categoryNames.get(i);
            categoryButtons.get(i).setOnClickListener(v -> toggleCategory(category));
        }
    }

    // ──────────────────────────────────────────────
    // 카테고리 토글 (다중 선택)
    // 이미 선택된 카테고리 클릭 → 선택 해제 + 칩 제거
    // 미선택 카테고리 클릭 → 선택 + 칩 추가
    // 칩이 1개 이상 → 휴지통 표시 / 0개 → 휴지통 숨김
    // ──────────────────────────────────────────────
    private void toggleCategory(String category) {
        if (selectedCategories.contains(category)) {
            //선택 해제

            selectedCategories.remove(category);
            updateButtonState(category , false);
            removeChip(category);
        } else {
            // 선택
            selectedCategories.add(category);
            updateButtonState(category, true);
            addChip(category);
        }
        // 휴지통 가시성 업데이트
        binding.layoutChipArea.setVisibility(
                selectedCategories.isEmpty() ? View.GONE : View.VISIBLE
        );
    }

    // ──────────────────────────────────────────────
    // 버튼 선택 상태 변경
    // chip_button_background.xml이 state_selected에 따라 스타일 자동 적용
    // ──────────────────────────────────────────────
    private void updateButtonState(String category , boolean selected) {
        int idx = categoryNames.indexOf(category);
        if (idx != -1) {
            categoryButtons.get(idx).setSelected(selected);
        }
    }

    // ──────────────────────────────────────────────
    // 칩 추가
    // tag에 카테고리명 저장 → removeChip에서 찾을 때 사용
    // ──────────────────────────────────────────────
    private void addChip(String category) {
        Chip chip = new Chip(requireContext() , null , com.google.android.material.R.attr.chipStyle);
        chip.setChipBackgroundColorResource(R.color.primary_100);
        chip.setChipStrokeWidth(0f);
        chip.setTextColor(getResources().getColor(R.color.white, null));
        chip.setCloseIconTint(ColorStateList.valueOf(
                getResources().getColor(R.color.white, null)));
        chip.setChipCornerRadius(50f); // 완전 둥글게
        chip.setText(category);
        chip.setTag(category);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> toggleCategory(category));
        binding.chipGroupSelected.addView(chip);
    }

    // ──────────────────────────────────────────────
    // 칩 제거
    // ChipGroup에서 tag가 일치하는 Chip을 찾아서 제거
    // ──────────────────────────────────────────────
    private void removeChip(String category) {
        for (int i = 0; i < binding.chipGroupSelected.getChildCount(); i++ ) {
            View child = binding.chipGroupSelected.getChildAt(i);
            if (child instanceof Chip && category.equals(child.getTag())) {
                binding.chipGroupSelected.removeViewAt(i);
                break;
            }
        }
    }

    // ──────────────────────────────────────────────
    // 전체 카테고리 초기화 (휴지통 클릭)
    // ──────────────────────────────────────────────
    private void clearAllCategories() {
        selectedCategories.clear();
        for (Button btn : categoryButtons) {
            btn.setSelected(false);
        }
        binding.chipGroupSelected.removeAllViews();
        binding.layoutChipArea.setVisibility(View.GONE);
    }

    // ──────────────────────────────────────────────
    // 가격 슬라이더 설정
    // RangeSlider: values[0]=min, values[1]=max
    // 빠른 선택 버튼 클릭 시 슬라이더 포인터도 함께 이동
    // ──────────────────────────────────────────────
    private void setupPriceSlider() {
        // 가격 빠른선택 버튼 목록
        priceButtons = Arrays.asList(
                binding.btnPrice10k20k,
                binding.btnPrice20k30k,
                binding.btnPriceUpper50k
        );

        // 슬라이더 실시간 변경 리스너
        binding.priceRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                clearPriceButtonSelection(); // 슬라이더 직접 건드리면 버튼 선택 해제
            }

            List<Float> values = slider.getValues();
            minPrice = values.get(0).intValue();
            maxPrice = values.get(1).intValue();
            updatePriceText();
        });
        // 빠른 선택 버튼들
        binding.btnPrice10k20k.setOnClickListener(v -> {
            selectPriceButton(binding.btnPrice10k20k);
            setPrice(10000, 20000);
        });
        binding.btnPrice20k30k.setOnClickListener(v -> {
            selectPriceButton(binding.btnPrice20k30k);
            setPrice(20000, 30000);
        });
        binding.btnPriceUpper50k.setOnClickListener(v -> {
            selectPriceButton(binding.btnPriceUpper50k);
            setPrice(50000, 100000);
        });
    }

    // 가격설정 + 슬라이더 + 텍스트 동시 업데이트
    private void setPrice(int min , int max) {
        minPrice = min;
        maxPrice = max;
        binding.priceRangeSlider.setValues((float) min, (float) max);
        updatePriceText();
    }

    private void updatePriceText() {
        binding.tvMinPrice.setText(String.format("%,d원", minPrice));
        binding.tvMaxPrice.setText(String.format("%,d원", maxPrice));
    }

    // 가격 버튼 단일 선택 토글
    // 이미 선택된 버튼 재클릭 → 선택 해제
    private void selectPriceButton(Button button) {
        if (selectedPriceButton == button) {
            // 재클릭 → 선택 해제
            button.setSelected(false);
            selectedPriceButton = null;
        } else {
            // 기존 선택 해제 후 새 버튼 선택
            clearPriceButtonSelection();
            button.setSelected(true);
            selectedPriceButton = button;
        }
    }

    // 모든 가격 버튼 선택 해제
    private void clearPriceButtonSelection() {
        for (Button btn : priceButtons) {
            btn.setSelected(false);
        }
        selectedPriceButton = null;
    }

    // ──────────────────────────────────────────────
    // 날짜/시간 피커 설정
    // Android 내장 DatePickerDialog / TimePickerDialog 사용
    // → 별도 라이브러리 불필요, OS 스타일 일관성
    // ──────────────────────────────────────────────

    private void setupDateTimePickers() {
        binding.btnDatePick.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(
                    requireContext(),
                    (view, year, month, day) -> {
                        // month는 0부터 시작 → +1 필요
                        selectedDate = year + "년 " + (month + 1) + "월 " + day + "일";
                        binding.tvDateDisplay.setText(selectedDate);
                        binding.tvDateDisplay.setVisibility(View.VISIBLE);
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        binding.btnTimePick.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(
                    requireContext(),
                    (view, hour, minute) -> {
                        // %02d: 한 자리 숫자를 "09:05" 형식으로 표시
                        selectedTime = String.format("%02d:%02d", hour, minute);
                        binding.tvTimeDisplay.setText(selectedTime);
                        binding.tvTimeDisplay.setVisibility(View.VISIBLE);
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true // true = 24시간 형식
            ).show();
        });
    }

    // ──────────────────────────────────────────────
    // 메모리 누수 방지
    // ──────────────────────────────────────────────
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}




































