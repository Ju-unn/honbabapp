package com.example.honbabapp.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.honbabapp.R;
import com.example.honbabapp.databinding.BottomsheetCampaignFilterBinding;
import com.example.honbabapp.util.KoreaRegion;
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
 */
public class CampaignFilterBottomSheetDialog extends BottomSheetDialogFragment {

    private BottomsheetCampaignFilterBinding binding;

    // ─── 콜백 ───────────────────────────────────────────────────
    public interface OnFilterAppliedListener {
        void onFilterApplied(
                Set<String> channels,
                Set<String> categories,
                int minPrice,
                int maxPrice,
                String sido,
                String sigungu
        );
    }
    public OnFilterAppliedListener listener;

    // ─── 이전 상태 복원용 ───────────────────────────────────────
    public Set<String> initialChannels = new HashSet<>();
    public Set<String> initialCategories = new HashSet<>();
    public int initialMinPrice = 0;
    public int initialMaxPrice = 100000;
    public String initialSido = null;
    public String initialSigungu = null;

    // ─── 내부 상태 ──────────────────────────────────────────────
    private final Set<String> selectedChannels = new HashSet<>();
    private final Set<String> selectedCategories = new HashSet<>();
    private int minPrice = 0;
    private int maxPrice = 100000;
    private String selectedSido = null;
    private String selectedSigungu = null;

    private List<String> channelNames;
    private List<Button> channelButtons;

    private List<String> categoryNames;
    private List<Button> categoryButtons;

    // 구 스피너 자체 변경(adapter 재설정) 중에는 onItemSelected 콜백을
    // 무시하기 위한 플래그 (재귀적인 초기화 방지)
    private boolean isUpdatingSigunguSpinner = false;

    // restoreInitialRegionSelection()의 setSelection()이
    // spinnerSido/spinnerSigungu의 onItemSelected를 비동기로 트리거해
    // selectedSigungu를 null로 덮어쓰는 것을 막기 위한 플래그
    private boolean isRestoringRegion = false;

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

        setupRegionSpinners();
        setupChannelButtons();
        setupCategoryButtons();
        setupPriceSlider();

        // 이전 상태 복원
        for (String channel : initialChannels) toggleChannel(channel);
        for (String category : initialCategories) toggleCategory(category);
        setPrice(initialMinPrice, initialMaxPrice);
        restoreInitialRegionSelection();

        // 복원 완료 후 (post로 비동기 콜백까지 처리된 뒤) 칩/가시성 갱신
        binding.spinnerSido.post(() -> {
            updateChipAreaVisibility();
            updateRegionChips();
        });

        // 휴지통: 전체 초기화
        binding.btnTrash.setOnClickListener(v -> clearAll());

        // 닫기 버튼
        binding.btnClose.setOnClickListener(v -> dismiss());

        // 적용하기 버튼
        binding.btnApplyFilter.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFilterApplied(
                        new HashSet<>(selectedChannels),
                        new HashSet<>(selectedCategories),
                        minPrice,
                        maxPrice,
                        selectedSido,
                        selectedSigungu
                );
            }
            dismiss();
        });

        return binding.getRoot();
    }

    // ─────────────────────────────────────────────────────────────
    // 지역 필터 (시/도 → 구 2단계 스피너)
    // ─────────────────────────────────────────────────────────────

    private void setupRegionSpinners() {
        // 시/도 스피너
        ArrayAdapter<String> sidoAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                KoreaRegion.getSidoList()
        );
        sidoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSido.setAdapter(sidoAdapter);

        binding.spinnerSigungu.setOnTouchListener((v, event) -> {
            if (selectedSido == null && event.getAction() == MotionEvent.ACTION_UP) {
                Toast.makeText(requireContext(), "시/도를 먼저 선택해주세요", Toast.LENGTH_SHORT).show();
                return true; // 이벤트 소비 → 드롭다운 열리지 않음
            }
            return false; // 정상 동작(드롭다운 열림) 허용
        });

        // 구 스피너 어댑터 먼저 세팅 (이 시점엔 아직 리스너 미등록 →
        // setAdapter()가 자동 트리거하는 onItemSelected를 무시할 필요 없음)
        updateSigunguSpinner(null, null);

        // 시/도 스피너 리스너는 어댑터/구 스피너 세팅이 끝난 뒤 등록
        binding.spinnerSido.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isRestoringRegion) return;
                String sido = (String) parent.getItemAtPosition(position);
                selectedSido = KoreaRegion.ALL.equals(sido) ? null : sido;
                // 시/도가 바뀌면 구 목록을 새로 채우고 "전체"로 리셋
                updateSigunguSpinner(selectedSido, null);
                updateChipAreaVisibility();
                updateRegionChips();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });

        binding.spinnerSigungu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // isUpdatingSigunguSpinner: setAdapter() 직후 자동 트리거 방어
                // isRestoringRegion: restoreInitialRegionSelection()의 setSelection()이
                //   spinnerSigungu.onItemSelected를 비동기 트리거해
                //   selectedSigungu를 null로 덮어쓰는 것을 방어
                if (isUpdatingSigunguSpinner || isRestoringRegion) return;
                String sigungu = (String) parent.getItemAtPosition(position);
                selectedSigungu = KoreaRegion.ALL.equals(sigungu) ? null : sigungu;
                updateChipAreaVisibility();
                updateRegionChips();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });
    }

    /**
     * 구 스피너를 선택된 시/도에 맞는 목록으로 교체.
     * preselectGu가 주어지면 해당 값으로 선택 위치를 맞춘다(초기 상태 복원용).
     */
    private void updateSigunguSpinner(String sido, String preselectGu) {
        isUpdatingSigunguSpinner = true;

        List<String> guList = KoreaRegion.getSigunguList(sido);
        ArrayAdapter<String> guAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                guList
        );
        guAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSigungu.setAdapter(guAdapter);

        if (preselectGu != null) {
            int index = guList.indexOf(preselectGu);
            binding.spinnerSigungu.setSelection(index >= 0 ? index : 0);
            selectedSigungu = index >= 0 ? preselectGu : null;
        } else {
            binding.spinnerSigungu.setSelection(0);
            selectedSigungu = null;
        }

        isUpdatingSigunguSpinner = false;
    }

    /** 다이얼로그 재오픈 시 이전에 선택했던 시/도·구로 스피너 위치 복원 */
    private void restoreInitialRegionSelection() {
        if (initialSido == null) return;

        List<String> sidoList = KoreaRegion.getSidoList();
        int sidoIndex = sidoList.indexOf(initialSido);
        if (sidoIndex < 0) return;

        // setSelection()이 유발하는 비동기 onItemSelected 콜백이
        // selectedSigungu를 null로 덮어쓰지 못하도록 복원 중 플래그를 켠다.
        isRestoringRegion = true;

        binding.spinnerSido.setSelection(sidoIndex);
        selectedSido = initialSido;
        updateSigunguSpinner(initialSido, initialSigungu);

        // spinnerSido, spinnerSigungu 모두 setSelection() 비동기 콜백이
        // 처리된 이후 시점에 플래그를 꺼야 한다.
        binding.spinnerSido.post(() -> binding.spinnerSigungu.post(() -> isRestoringRegion = false));
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
        boolean hasFilter = !selectedChannels.isEmpty()
                || !selectedCategories.isEmpty()
                || selectedSido != null
                || selectedSigungu != null;
        binding.layoutChipArea.setVisibility(hasFilter ? View.VISIBLE : View.GONE);
    }

    // 지역 칩 — 카테고리/채널 칩과 동일한 ChipGroup에 표시.
    // 시/도 칩(TAG: "REGION_SIDO"), 구/군 칩(TAG: "REGION_SIGUNGU")을
    // 각각 1개씩 유지하며, 선택 변경 시 기존 칩을 제거하고 새로 추가한다.
    // 닫기 아이콘을 누르면 해당 지역 선택이 초기화된다.
    private static final String TAG_SIDO    = "__region_sido__";
    private static final String TAG_SIGUNGU = "__region_sigungu__";

    private void updateRegionChips() {
        // 시/도 칩
        removeChip(TAG_SIDO);
        if (selectedSido != null) {
            addRegionChip(selectedSido, TAG_SIDO, () -> {
                // 시/도 칩 닫기 → 시/도 + 구 모두 초기화
                selectedSido = null;
                selectedSigungu = null;
                binding.spinnerSido.setSelection(0);
                updateSigunguSpinner(null, null);
                updateRegionChips();
                updateChipAreaVisibility();
            });
        }

        // 구/군 칩 (시/도가 선택된 경우에만)
        removeChip(TAG_SIGUNGU);
        if (selectedSigungu != null) {
            addRegionChip(selectedSigungu, TAG_SIGUNGU, () -> {
                // 구/군 칩 닫기 → 구만 초기화, 시/도는 유지
                selectedSigungu = null;
                List<String> guList = KoreaRegion.getSigunguList(selectedSido);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        guList
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                isUpdatingSigunguSpinner = true;
                binding.spinnerSigungu.setAdapter(adapter);
                binding.spinnerSigungu.setSelection(0);
                isUpdatingSigunguSpinner = false;
                updateRegionChips();
                updateChipAreaVisibility();
            });
        }
    }

    private void addRegionChip(String label, String tag, Runnable onClose) {
        Chip chip = new Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle);
        chip.setChipBackgroundColorResource(R.color.primary_100);
        chip.setChipStrokeWidth(0f);
        chip.setTextColor(getResources().getColor(R.color.white, null));
        chip.setChipCornerRadius(50f);
        chip.setText(label);
        chip.setTag(tag);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> onClose.run());
        binding.chipGroupSelected.addView(chip);
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

        // 지역 초기화
        binding.spinnerSido.setSelection(0);
        selectedSido = null;
        updateSigunguSpinner(null, null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}