package com.example.honbabapp.activity.campaign;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.honbabapp.activity.MainActivity;
import com.example.honbabapp.activity.meeting.PlaceSearchActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.databinding.PageCampaignWritingBinding;
import com.google.android.material.slider.RangeSlider;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 체험단 등록 Activity
 *
 * 공유하는 것:
 * - PlaceSearchActivity (가게 검색) — 그대로 재사용
 * - S3 이미지 업로드 유틸 — 로직 단계에서 공통 유틸로 호출
 *
 */
public class CampaignWritingActivity extends AppCompatActivity {

    private static final String TAG = "CampaignWritingActivity";

    private PageCampaignWritingBinding binding;
    private ApiService apiService;

    private int userId;

    // 공통 입력값
    private String selectedCategory = "한식";
    private String selectedVisitDate = "";
    private String selectedVisitTime = "";
    private int participantCount = 2;
    private int minPrice = 0;
    private int maxPrice = 100000;

    // 가게 정보
    private String selectedPlaceName = "";
    private String selectedAddress = "";
    private String selectedRoadAddress = "";
    private String selectedSigungu = "";
    private String selectedDong = "";
    private String selectedMapX = "";
    private String selectedMapY = "";
    private String selectedPlaceCategory = "";
    private String selectedPhone = "";

    // 이미지 (모임 생성 버튼 누를 때까지 메모리에만 보관)
    private String pendingImageValue = null;
    private String pendingImageSource = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageCampaignWritingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        SharedPreferences prefs = getSharedPreferences("honbabapp_prefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        setupUI();
    }

    // ─────────────────────────────────────────────────────────────
    // UI 초기화
    // ─────────────────────────────────────────────────────────────

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());
        setupCategorySelection();
        setupDatePicker();
        setupTimePicker();
        setupParticipantCounter();
        setupPriceRangeSlider();
        setupPlaceSelection();
        setupDeadlinePicker();
        binding.btnRegisterCampaign.setOnClickListener(v -> validateAndRegister());
    }

    // ─────────────────────────────────────────────────────────────
    // 가게 검색 Launcher — PlaceSearchActivity 재사용
    // ─────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<Intent> placeSearchLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                        Intent data = result.getData();

                        selectedPlaceName     = data.getStringExtra("place_name");
                        selectedAddress       = data.getStringExtra("address");
                        selectedRoadAddress   = data.getStringExtra("road_address");
                        selectedSigungu       = data.getStringExtra("sigungu");
                        selectedDong          = data.getStringExtra("dong");
                        selectedMapX          = data.getStringExtra("map_x");
                        selectedMapY          = data.getStringExtra("map_y");
                        selectedPlaceCategory = data.getStringExtra("category");
                        selectedPhone         = data.getStringExtra("phone");

                        String imageValue  = data.getStringExtra("image_value");
                        String imageSource = data.getStringExtra("image_source");
                        if (imageValue != null && imageSource != null) {
                            pendingImageValue  = imageValue;
                            pendingImageSource = imageSource;

                            Glide.with(this)
                                    .load(imageValue)
                                    .into(binding.ivCampaignPhotoPreview);
                            binding.ivCampaignPhotoPreview.setVisibility(View.VISIBLE);
                            binding.layoutPhotoPlaceholder.setVisibility(View.GONE);
                        }

                        // 가게 카드 업데이트
                        binding.etStoreSearch.setText(selectedPlaceName);
                        binding.selectedStoreCard.setVisibility(View.VISIBLE);
                        binding.tvSelectedStore.setText(selectedPlaceName);
                        binding.tvSelectedStoreAddress.setText(
                                selectedRoadAddress != null && !selectedRoadAddress.isEmpty()
                                        ? selectedRoadAddress : selectedAddress
                        );

                        // 체험단명 반자동 입력 (비어있을 때만)
                        if (binding.etCampaignTitle.getText().toString().trim().isEmpty()) {
                            binding.etCampaignTitle.setText(selectedPlaceName + " 체험단 모집");
                        }

                        // 카테고리 자동 선택
                        autoSelectCategory(selectedPlaceCategory);

                        Log.i(TAG, "가게 선택 완료: " + selectedPlaceName);
                    }
            );

    /**
     * 가게 검색 버튼
     */
    private void setupPlaceSelection() {
        binding.btnSearchStore.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlaceSearchActivity.class);
            String query = binding.etStoreSearch.getText().toString().trim();
            if (!query.isEmpty()) intent.putExtra("search_query", query);
            placeSearchLauncher.launch(intent);
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 카테고리 선택 — MeetingCardWritingActivity 동일 패턴
    // ─────────────────────────────────────────────────────────────

    private void setupCategorySelection() {
        RadioButton[] buttons = {
                binding.radioCategoryKorean,
                binding.radioCategoryChinese,
                binding.radioCategoryJapanese,
                binding.radioCategoryWestern,
                binding.radioCategoryOmakase,
                binding.radioCategoryCafe,
                binding.radioCategoryPub
        };

        String[] categoryValues = {"한식", "중식", "일식", "양식", "오마카세", "카페", "술집"};

        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            buttons[i].setOnClickListener(v -> {
                for (RadioButton button : buttons) button.setChecked(false);
                buttons[index].setChecked(true);
                selectedCategory = categoryValues[index];
                Log.i(TAG, "카테고리 선택: " + selectedCategory);
            });
        }

        binding.radioCategoryKorean.setChecked(true);
        selectedCategory = "한식";
    }

    /**
     * 가게 카테고리 문자열로 RadioButton 자동 선택
     */
    private void autoSelectCategory(String placeCategory) {
        if (placeCategory == null || placeCategory.isEmpty()) return;

        String lower = placeCategory.toLowerCase();
        if (lower.contains("한식"))      binding.radioCategoryKorean.performClick();
        else if (lower.contains("중식")) binding.radioCategoryChinese.performClick();
        else if (lower.contains("일식")) binding.radioCategoryJapanese.performClick();
        else if (lower.contains("양식")) binding.radioCategoryWestern.performClick();
        else if (lower.contains("카페")) binding.radioCategoryCafe.performClick();
        else if (lower.contains("술"))   binding.radioCategoryPub.performClick();
    }

    // ─────────────────────────────────────────────────────────────
    // 날짜 / 시간 — MeetingCardWritingActivity 동일 패턴
    // ─────────────────────────────────────────────────────────────

    private void setupDatePicker() {
        binding.etCampaignVisitDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(this,
                    (view, year, month, day) -> {
                        selectedVisitDate = String.format("%04d-%02d-%02d", year, month + 1, day);
                        String displayDate = String.format("%d월 %d일", month + 1, day);
                        binding.etCampaignVisitDate.setText(displayDate);
                        Log.i(TAG, "방문 날짜: " + selectedVisitDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMinDate(System.currentTimeMillis());
            dialog.show();
        });
    }

    private void setupTimePicker() {
        binding.etCampaignVisitTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new TimePickerDialog(this,
                    (view, hour, minute) -> {
                        selectedVisitTime = String.format("%02d:%02d:00", hour, minute);
                        String ampm = hour >= 12 ? "오후" : "오전";
                        int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
                        binding.etCampaignVisitTime.setText(
                                String.format("%s %d:%02d", ampm, displayHour, minute)
                        );
                        Log.i(TAG, "방문 시간: " + selectedVisitTime);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
            ).show();
        });
    }

    private void setupDeadlinePicker() {
        binding.etCampaignDeadline.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(this,
                    (view, year, month, day) -> {
                        String deadlineDate = String.format("%04d-%02d-%02d", year, month + 1, day);
                        binding.etCampaignDeadline.setText(deadlineDate);
                        Log.i(TAG, "모집 마감일: " + deadlineDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMinDate(System.currentTimeMillis());
            dialog.show();
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 인원 / 가격
    // ─────────────────────────────────────────────────────────────

    private void setupParticipantCounter() {
        updateParticipantCountDisplay();

        binding.btnDecreaseCount.setOnClickListener(v -> {
            if (participantCount > 2) {
                participantCount--;
                updateParticipantCountDisplay();
            }
        });

        binding.btnIncreaseCount.setOnClickListener(v -> {
            if (participantCount < 20) {
                participantCount++;
                updateParticipantCountDisplay();
            }
        });
    }

    private void updateParticipantCountDisplay() {
        binding.tvParticipantCount.setText(participantCount + "명");
    }

    private void setupPriceRangeSlider() {
        updatePriceRangeDisplay(minPrice, maxPrice);

        binding.rangeSliderPrice.addOnChangeListener(new RangeSlider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
                List<Float> values = slider.getValues();
                if (values.size() == 2) {
                    minPrice = values.get(0).intValue();
                    maxPrice = values.get(1).intValue();
                    updatePriceRangeDisplay(minPrice, maxPrice);
                }
            }
        });
    }

    private void updatePriceRangeDisplay(int min, int max) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);
        binding.tvPriceRangeDisplay.setText(
                formatter.format(min) + "원 ~ " + formatter.format(max) + "원"
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 유효성 검증 — 체험단 전용 필드 포함
    // ─────────────────────────────────────────────────────────────

    private void validateAndRegister() {
        String campaignTitle = binding.etCampaignTitle.getText().toString().trim();
        if (campaignTitle.isEmpty()) {
            Toast.makeText(this, "체험단명을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String campaignDescription = binding.etCampaignDescription.getText().toString().trim();
        if (campaignDescription.isEmpty()) {
            Toast.makeText(this, "체험단 설명을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedVisitDate.isEmpty()) {
            Toast.makeText(this, "방문 날짜를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedVisitTime.isEmpty()) {
            Toast.makeText(this, "원하시는 방문 시간을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedPlaceName.isEmpty()) {
            Toast.makeText(this, "가게를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 체험단 전용 필드 검증
        boolean isBlogChecked      = binding.checkboxChannelBlog.isChecked();
        boolean isInstagramChecked = binding.checkboxChannelInstagram.isChecked();
        boolean isYoutubeChecked   = binding.checkboxChannelYoutube.isChecked();

        if (!isBlogChecked && !isInstagramChecked && !isYoutubeChecked) {
            Toast.makeText(this, "홍보 플랫폼을 하나 이상 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String campaignProvided = binding.etCampaignProvided.getText().toString().trim();
        if (campaignProvided.isEmpty()) {
            Toast.makeText(this, "제공 내역을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 선택 채널 문자열 조합
        StringBuilder channelsBuilder = new StringBuilder();
        if (isBlogChecked)      channelsBuilder.append("블로그,");
        if (isInstagramChecked) channelsBuilder.append("인스타그램,");
        if (isYoutubeChecked)   channelsBuilder.append("유튜브,");
        String campaignChannels = channelsBuilder.toString().replaceAll(",$", "");

        String campaignRequirements = binding.etCampaignRequirements.getText().toString().trim();
        String campaignDeadline     = binding.etCampaignDeadline.getText().toString().trim();

        NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);
        String priceRange = formatter.format(minPrice) + "원 ~ " + formatter.format(maxPrice) + "원";

        // 버튼 비활성화
        binding.btnRegisterCampaign.setEnabled(false);
        binding.btnRegisterCampaign.setText("등록 중...");

        // TODO: 로직 단계 - 이미지 업로드 후 API 호출
        // registerCampaign(campaignTitle, campaignDescription, priceRange,
        //         campaignChannels, campaignProvided, campaignRequirements,
        //         campaignDeadline, null);

        Log.i(TAG, "체험단 등록 준비 완료");
        Log.i(TAG, "채널: " + campaignChannels);
        Log.i(TAG, "제공 내역: " + campaignProvided);
    }

    private void resetRegisterButton() {
        binding.btnRegisterCampaign.setEnabled(true);
        binding.btnRegisterCampaign.setText("체험단 등록");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
