package com.example.honbabapp.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.honbabapp.R;
import com.example.honbabapp.databinding.BottomsheetCampaignFilterBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 체험단 필터 바텀시트
 * MapFilterBottomSheetDialog 완전 동일 패턴
 *
 * 선택 상태 관리:
 * - button.setSelected(true/false) 로 관리
 * - drawable의 state_selected가 자동으로 스타일 적용
 * - 별도 selected drawable 불필요
 */
public class CampaignFilterBottomSheetDialog extends BottomSheetDialogFragment {

    private BottomsheetCampaignFilterBinding binding;

    // ─── 콜백 ───────────────────────────────────────────────────
    public interface OnFilterAppliedListener {
        void onFilterApplied(
                Set<String> channels,
                Set<String> categories,
                int minPrice,
                int maxPrice
        );
    }
    public OnFilterAppliedListener listener;

    // ─── 이전 상태 복원용 ───────────────────────────────────────
    public Set<String> initialChannels = new HashSet<>();
    public Set<String> initialCategories = new HashSet<>();
    public int initialMinPrice = 0;
    public int initialMaxPrice = 100000;

    // ─── 내부 상태 ──────────────────────────────────────────────
    private final Set<String> selectedChannels = new HashSet<>();
    private final Set<String> selectedCategories = new HashSet<>();
    private int minPrice = 0;
    private int maxPrice = 100000;

    private List<String> channelNames;
    private List<Button> channelButtons;

    private List<String> categoryNames;
    private List<Button> categoryButtons;

    // ─── 바텀시트 전체 높이 확장 ────────────────────────────────
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            FrameLayout bottomSheet =
                    ((BottomSheetDialog) d).findViewById(
                            com.google.android.material.R.id.design_bottom_sheet
                    );
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
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = BottomsheetCampaignFilterBinding.inflate(inflater, container, false);

        setupChannelButtons();
        setupCategoryButtons();
        setupPriceSlider();

        // 이전 상태 복원
        for (String channel : initialChannels) toggleChannel(channel);
        for (String category : initialCategories) toggleCategory(category);
        setPrice(initialMinPrice, initialMaxPrice);

        // 휴지통: 전체 초기화
        binding.btnTrash.setOnClickListener(v -> clearAll());

        // 닫기 버튼
        binding.btnClose.setOnClickListener(v -> dismiss());

        // 초기화 버튼
        binding.btnReset.setOnClickListener(v -> clearAll());

        // 결과 보기 버튼
        binding.btnApplyFilter.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFilterApplied(
                        new HashSet<>(selectedChannels),
                        new HashSet<>(selectedCategories),
                        minPrice,
                        maxPrice
                );
            }
            dismiss();
        });

        return binding.getRoot();
    }

    // ─────────────────────────────────────────────────────────────
    // 홍보 채널 버튼 — MapFilterBottomSheetDialog.setupCategoryButtons() 동일 패턴
    // ─────────────────────────────────────────────────────────────

    private void setupChannelButtons() {
        channelNames = Arrays.asList("블로그", "인스타그램", "유튜브");
        channelButtons = Arrays.asList(
                binding.btnChannelBlog,
                binding.btnChannelInstagram,
                binding.btnChannelYoutube
        );

        for (int i = 0; i < channelButtons.size(); i++) {
            final String channel = channelNames.get(i);
            channelButtons.get(i).setOnClickListener(v -> toggleChannel(channel));
        }
    }

    private void toggleChannel(String channel) {
        if (selectedChannels.contains(channel)) {
            selectedChannels.remove(channel);
            updateChannelButtonState(channel, false);
            removeChip(channel);
        } else {
            selectedChannels.add(channel);
            updateChannelButtonState(channel, true);
            addChip(channel);
        }
        updateChipAreaVisibility();
    }

    // setSelected() 로 관리 — MapFilterBottomSheetDialog.updateButtonState() 동일 패턴
    private void updateChannelButtonState(String channel, boolean isSelected) {
        int index = channelNames.indexOf(channel);
        if (index < 0) return;
        channelButtons.get(index).setSelected(isSelected);
    }

    // ─────────────────────────────────────────────────────────────
    // 카테고리 버튼 — MapFilterBottomSheetDialog.setupCategoryButtons() 동일 패턴
    // ─────────────────────────────────────────────────────────────

    private void setupCategoryButtons() {
        categoryNames = Arrays.asList("한식", "중식", "일식", "양식", "오마카세", "카페", "술집");
        categoryButtons = Arrays.asList(
                binding.btnCategoryKorean,
                binding.btnCategoryChinese,
                binding.btnCategoryJapanese,
                binding.btnCategoryWestern,
                binding.btnCategoryOmakase,
                binding.btnCategoryCafe,
                binding.btnCategoryPub
        );

        for (int i = 0; i < categoryButtons.size(); i++) {
            final String category = categoryNames.get(i);
            categoryButtons.get(i).setOnClickListener(v -> toggleCategory(category));
        }
    }

    private void toggleCategory(String category) {
        if (selectedCategories.contains(category)) {
            selectedCategories.remove(category);
            updateCategoryButtonState(category, false);
            removeChip(category);
        } else {
            selectedCategories.add(category);
            updateCategoryButtonState(category, true);
            addChip(category);
        }
        updateChipAreaVisibility();
    }

    // setSelected() 로 관리 — MapFilterBottomSheetDialog.updateButtonState() 동일 패턴
    private void updateCategoryButtonState(String category, boolean isSelected) {
        int index = categoryNames.indexOf(category);
        if (index < 0) return;
        categoryButtons.get(index).setSelected(isSelected);
    }

    // ─────────────────────────────────────────────────────────────
    // 가격 슬라이더 — MapFilterBottomSheetDialog.setupPriceSlider() 동일 패턴
    // ─────────────────────────────────────────────────────────────

    private void setupPriceSlider() {
        binding.rangeSliderPrice.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            if (values.size() == 2) {
                minPrice = values.get(0).intValue();
                maxPrice = values.get(1).intValue();
                updatePriceDisplay();
            }
        });
    }

    private void setPrice(int min, int max) {
        minPrice = min;
        maxPrice = max;
        binding.rangeSliderPrice.setValues((float) min, (float) max);
        updatePriceDisplay();
    }

    private void updatePriceDisplay() {
        NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);
        binding.tvPriceRangeDisplay.setText(
                formatter.format(minPrice) + "원 ~ " + formatter.format(maxPrice) + "원"
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 칩 추가/제거 — MapFilterBottomSheetDialog.addChip()/removeChip() 동일 패턴
    // ─────────────────────────────────────────────────────────────

    private void addChip(String label) {
        Chip chip = new Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle);
        chip.setChipBackgroundColorResource(R.color.primary_100);
        chip.setChipStrokeWidth(0f);
        chip.setTextColor(getResources().getColor(R.color.white, null));
        chip.setChipCornerRadius(50f);
        chip.setText(label);
        chip.setTag(label);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            if (channelNames.contains(label)) toggleChannel(label);
            else toggleCategory(label);
        });
        binding.chipGroupSelected.addView(chip);
    }

    private void removeChip(String label) {
        for (int i = 0; i < binding.chipGroupSelected.getChildCount(); i++) {
            View child = binding.chipGroupSelected.getChildAt(i);
            if (child instanceof Chip && label.equals(child.getTag())) {
                binding.chipGroupSelected.removeViewAt(i);
                break;
            }
        }
    }

    private void updateChipAreaVisibility() {
        boolean hasFilter = !selectedChannels.isEmpty() || !selectedCategories.isEmpty();
        binding.layoutChipArea.setVisibility(hasFilter ? View.VISIBLE : View.GONE);
    }

    // ─────────────────────────────────────────────────────────────
    // 전체 초기화 — MapFilterBottomSheetDialog.clearAllCategories() 동일 패턴
    // ─────────────────────────────────────────────────────────────

    private void clearAll() {
        // 채널 초기화
        selectedChannels.clear();
        for (Button button : channelButtons) button.setSelected(false);

        // 카테고리 초기화
        selectedCategories.clear();
        for (Button button : categoryButtons) button.setSelected(false);

        // 칩 초기화
        binding.chipGroupSelected.removeAllViews();
        binding.layoutChipArea.setVisibility(View.GONE);

        // 가격 초기화
        setPrice(0, 100000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}