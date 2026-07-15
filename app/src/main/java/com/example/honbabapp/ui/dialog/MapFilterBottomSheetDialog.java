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
import android.widget.Toast;

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
import java.util.Locale;
import java.util.Set;

/**
 * 지도 필터 바텀시트
 */
public class MapFilterBottomSheetDialog extends BottomSheetDialogFragment {

    BottomsheetMapFilterBinding binding;

    // ──────────────────────────────────────────────
    // 콜백 인터페이스
    // MapFragment가 구현 → 필터 적용 시 결과 전달
    // ──────────────────────────────────────────────
    public interface OnFilterAppliedListener {
        void onFilterApplied(
                Set<String> categories,
                Set<String> channels,
                int minPrice,
                int maxPrice,
                String date,   // YYYY-MM-DD (쿼리용)
                String time    // HH:MM
        );
    }
    public OnFilterAppliedListener listener;

    // ──────────────────────────────────────────────
    // 외부 주입값
    // MapFragment → 여기로 전달되는 사전 선택 카테고리
    // ──────────────────────────────────────────────
    public String preSelectedCategory = null;

    // 필터 재오픈 시 이전 상태 복원용
    public Set<String> initialSelectedCategories = null;
    public Set<String> initialSelectedChannels = null;
    public int initialMinPrice = 0;
    public int initialMaxPrice = 100000;
    public String initialDateQuery = null;   // YYYY-MM-DD
    public String initialTimeQuery = null;   // HH:MM

    private final Set<String> selectedCategories = new HashSet<>();
    private final Set<String> selectedChannels = new HashSet<>();
    private int minPrice = 0;
    private int maxPrice = 100000;

    // 표시용(한글) / 쿼리용(ISO) 날짜·시간을 분리 보관
    private String selectedDateDisplay = null;
    private String selectedDateQuery = null;
    private String selectedTime = null; // HH:MM 형식 그대로 표시 + 쿼리 겸용

    private List<String> categoryNames;
    private List<Button> categoryButtons;

    private List<String> channelNames;
    private List<Button> channelButtons;

    private List<Button> priceButtons;
    private Button selectedPriceButton = null;

    // 칩 tag 접두사 — 카테고리/채널 칩은 라벨 그대로를 tag로 쓰지만,
    // 날짜·시간 칩은 같은 종류의 칩이 항상 1개만 존재해야 하므로
    // 고정 tag("DATE_CHIP", "TIME_CHIP")로 구분해서 관리한다.
    private static final String DATE_CHIP_TAG = "__date_chip__";
    private static final String TIME_CHIP_TAG = "__time_chip__";

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

        setupChannelButtons();
        setupCategoryButtons();
        setupPriceSlider();
        setupDateTimePickers();

        // 휴지통 : 모든 필터 초기화
        binding.btnTrash.setOnClickListener(v ->  clearAllFilters());

        // 닫기 버튼
        binding.btnClose.setOnClickListener(v -> dismiss());

        // 적용하기 버튼
        binding.btnApplyFilter.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFilterApplied(
                        new HashSet<>(selectedCategories),
                        new HashSet<>(selectedChannels),
                        minPrice,
                        maxPrice,
                        selectedDateQuery,
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

        // 채널 이전 상태 복원
        if (initialSelectedChannels != null) {
            for (String channel : initialSelectedChannels) {
                toggleChannel(channel);
            }
        }

        // 가격 슬라이더는 항상 이전 값으로 복원 (Home에서 왔을 때도 유지)
        setPrice(initialMinPrice, initialMaxPrice);

        // 날짜/시간 이전 상태 복원 (쿼리값만 보관되어 있으므로 표시 텍스트는 그대로 보여줌)
        if (initialDateQuery != null) {
            selectedDateQuery = initialDateQuery;
            selectedDateDisplay = initialDateQuery; // 재오픈 시엔 ISO 형식으로라도 표시
            setDateChip(selectedDateDisplay);
        }
        if (initialTimeQuery != null) {
            selectedTime = initialTimeQuery;
            setTimeChip(formatTimeDisplay(initialTimeQuery));
        }

        // 카테고리/채널 칩이 없어도 날짜·시간만 복원된 경우 휴지통이 보이도록 갱신
        updateChipAreaVisibility();

        return binding.getRoot();
    }

    // ──────────────────────────────────────────────
    // 홍보 플랫폼(채널) 버튼
    // ──────────────────────────────────────────────

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

    private void updateChannelButtonState(String channel, boolean selected) {
        int idx = channelNames.indexOf(channel);
        if (idx != -1) {
            channelButtons.get(idx).setSelected(selected);
        }
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
        // 칩 영역 가시성 업데이트
        updateChipAreaVisibility();
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
    // tag: 칩을 구분/제거할 때 쓰는 키.
    //   - 카테고리/채널 칩은 라벨 자체를 tag로 사용 (기존 동작 그대로)
    //   - 날짜/시간 칩은 라벨이 매번 바뀌므로 고정 tag(DATE_CHIP_TAG,
    //     TIME_CHIP_TAG)를 사용해 "이 종류는 항상 1개만 존재"를 보장한다.
    // onCloseClick: 닫기 아이콘 클릭 시 실행할 동작 (호출부에서 주입)
    // ──────────────────────────────────────────────
    private void addChip(String label, Object tag, Runnable onCloseClick) {
        Chip chip = new Chip(requireContext() , null , com.google.android.material.R.attr.chipStyle);
        chip.setChipBackgroundColorResource(R.color.primary_100);
        chip.setChipStrokeWidth(0f);
        chip.setTextColor(getResources().getColor(R.color.white, null));
        chip.setCloseIconTint(ColorStateList.valueOf(
                getResources().getColor(R.color.white, null)));
        chip.setChipCornerRadius(50f); // 완전 둥글게
        chip.setText(label);
        chip.setTag(tag);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> onCloseClick.run());
        binding.chipGroupSelected.addView(chip);
    }

    // 카테고리/채널 칩 — 기존 시그니처 그대로 유지 (라벨 = tag)
    private void addChip(String label) {
        addChip(label, label, () -> {
            if (channelNames.contains(label)) toggleChannel(label);
            else toggleCategory(label);
        });
    }

    // ──────────────────────────────────────────────
    // 칩 제거
    // ChipGroup에서 tag가 일치하는 Chip을 찾아서 제거
    // ──────────────────────────────────────────────
    private void removeChip(Object tag) {
        for (int i = 0; i < binding.chipGroupSelected.getChildCount(); i++ ) {
            View child = binding.chipGroupSelected.getChildAt(i);
            if (child instanceof Chip && tag.equals(child.getTag())) {
                binding.chipGroupSelected.removeViewAt(i);
                break;
            }
        }
    }

    // ──────────────────────────────────────────────
    // 날짜 칩 — 같은 종류(DATE_CHIP_TAG)가 항상 1개만 존재하도록
    // 기존 칩을 먼저 제거한 뒤 새로 추가한다.
    // ──────────────────────────────────────────────
    private void setDateChip(String label) {
        removeChip(DATE_CHIP_TAG);
        addChip(label, DATE_CHIP_TAG, this::clearDateSelection);
    }

    private void clearDateSelection() {
        selectedDateDisplay = null;
        selectedDateQuery = null;
        removeChip(DATE_CHIP_TAG);

        // 날짜가 사라지면 시간만 단독으로 남는 건 의미가 모호하므로
        // (서버 쿼리가 date 없이 time만 보내는 상황을 피하기 위해)
        // 함께 골라둔 시간도 같이 초기화한다 — 날짜 변경 시 시간 초기화하던
        // 기존 정책과 동일한 방향.
        clearTimeSelection();
        updateChipAreaVisibility();
    }

    // ──────────────────────────────────────────────
    // 시간 칩 — 같은 종류(TIME_CHIP_TAG)가 항상 1개만 존재하도록
    // 기존 칩을 먼저 제거한 뒤 새로 추가한다.
    // ──────────────────────────────────────────────
    private void setTimeChip(String label) {
        removeChip(TIME_CHIP_TAG);
        addChip(label, TIME_CHIP_TAG, this::clearTimeSelection);
    }

    private void clearTimeSelection() {
        selectedTime = null;
        removeChip(TIME_CHIP_TAG);
        updateChipAreaVisibility();
    }

    private void updateChipAreaVisibility() {
        // 카테고리/채널 칩이 없어도 날짜나 시간이 선택되어 있으면
        // 휴지통 버튼(이 영역에 함께 있음)이 보여야 초기화할 수 있다.
        boolean hasFilter = !selectedCategories.isEmpty()
                || !selectedChannels.isEmpty()
                || selectedDateQuery != null
                || selectedTime != null;
        binding.layoutChipArea.setVisibility(hasFilter ? View.VISIBLE : View.GONE);
    }

    // ──────────────────────────────────────────────
    // 전체 필터 초기화 (휴지통 클릭)
    // 카테고리/채널/날짜/시간을 모두 초기화한다.
    // 가격은 setupPriceSlider() 쪽 빠른선택 버튼 해제만 처리하고
    // 슬라이더 값 자체는 별도 초기화 버튼(btn_reset 등)이 없으므로
    // 기존 동작을 그대로 유지한다.
    // ──────────────────────────────────────────────
    private void clearAllFilters() {
        selectedCategories.clear();
        for (Button btn : categoryButtons) {
            btn.setSelected(false);
        }

        selectedChannels.clear();
        for (Button btn : channelButtons) {
            btn.setSelected(false);
        }

        binding.chipGroupSelected.removeAllViews();
        binding.layoutChipArea.setVisibility(View.GONE);

        // 날짜/시간 초기화 (칩도 함께 제거)
        selectedDateDisplay = null;
        selectedDateQuery = null;
        selectedTime = null;
        removeChip(DATE_CHIP_TAG);
        removeChip(TIME_CHIP_TAG);
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
    // ──────────────────────────────────────────────

    private void setupDateTimePickers() {
        binding.btnDatePick.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, day) -> {
                        // month는 0부터 시작 → +1 필요
                        selectedDateDisplay = year + "년 " + (month + 1) + "월 " + day + "일";
                        selectedDateQuery = String.format(Locale.KOREA, "%04d-%02d-%02d", year, month + 1, day);
                        setDateChip(selectedDateDisplay);

                        // 날짜가 바뀌면 이미 선택해 둔 시간이 새 날짜 기준으로
                        // 과거가 될 수 있으므로 초기화한다 (MeetingCardWritingActivity와 동일 정책)
                        selectedTime = null;
                        removeChip(TIME_CHIP_TAG);

                        updateChipAreaVisibility();
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            // 오늘 이전 날짜는 선택 자체를 막는다
            dialog.getDatePicker().setMinDate(System.currentTimeMillis());
            dialog.show();
        });

//        binding.btnTimePick.setOnClickListener(v -> {
//            Calendar now = Calendar.getInstance();
//            new TimePickerDialog(
//                    requireContext(),
//                    (view, hour, minute) -> {
//                        // 선택된 날짜가 "오늘"이거나, 날짜를 아직 선택하지 않은
//                        // 경우에만 현재 시각 이전인지 검증한다.
//                        String todayQuery = String.format(Locale.KOREA, "%04d-%02d-%02d",
//                                now.get(Calendar.YEAR),
//                                now.get(Calendar.MONTH) + 1,
//                                now.get(Calendar.DAY_OF_MONTH));
//                        boolean isTodaySelected = selectedDateQuery == null || todayQuery.equals(selectedDateQuery);
//
//                        if (isTodaySelected) {
//                            int currentHour = now.get(Calendar.HOUR_OF_DAY);
//                            int currentMinute = now.get(Calendar.MINUTE);
//                            boolean isPastTime = hour < currentHour
//                                    || (hour == currentHour && minute <= currentMinute);
//
//                            if (isPastTime) {
//                                Toast.makeText(requireContext(),
//                                        "현재 시간 이후를 선택해주세요",
//                                        Toast.LENGTH_SHORT).show();
//                                return; // selectedTime 설정 안 함
//                            }
//                        }
//
//                        // 쿼리용 값은 항상 24시간제 "HH:MM"으로 저장
//                        selectedTime = String.format("%02d:%02d", hour, minute);
//                        // 칩에는 MeetingCardWritingActivity와 동일한 "오후 7:30" 형식으로 표시
//                        setTimeChip(formatTimeDisplay(hour, minute));
//
//                        updateChipAreaVisibility();
//                    },
//                    now.get(Calendar.HOUR_OF_DAY),
//                    now.get(Calendar.MINUTE),
//                    false // false = 12시간 형식(오전/오후) — MeetingCardWritingActivity와 동일 UI
//            ).show();
//        });
    }

    /** "HH:MM" 쿼리값을 "오후 7:30" 형식으로 변환 (재오픈 시 칩 라벨 복원용) */
    private String formatTimeDisplay(String hhmm) {
        try {
            String[] parts = hhmm.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return formatTimeDisplay(hour, minute);
        } catch (Exception e) {
            return hhmm; // 파싱 실패 시 원본 그대로 표시
        }
    }

    /** 24시간제 hour/minute */
    private String formatTimeDisplay(int hour, int minute) {
        String ampm = hour >= 12 ? "오후" : "오전";
        int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
        return String.format(Locale.KOREA, "%s %d:%02d", ampm, displayHour, minute);
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