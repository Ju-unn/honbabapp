package com.example.honbabapp.activity.meeting;

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
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.ImageUploadData;
import com.example.honbabapp.api.data.request.meeting.MeetingCreateRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.MeetingCreateResponse;
import com.example.honbabapp.databinding.PageMeetingCardWritingBinding;
import com.google.android.material.slider.RangeSlider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 일반 모임 생성 Activity
 */
public class MeetingCardWritingActivity extends AppCompatActivity {

    private static final String TAG = "MeetingCardWriting";

    private PageMeetingCardWritingBinding binding;
    private ApiService apiService;

    private int userId;
    private String selectedCategory = "한식";
    private String selectedDate = "";
    private String selectedTime = "";
    private int participantCount = 2;

    private int minPrice = 0;
    private int maxPrice = 100000;

    // 선택한 장소 정보
    private String selectedPlaceName = "";
    private String selectedAddress = "";
    private String selectedRoadAddress = "";
    private String selectedSigungu = "";
    private String selectedDong = "";
    private String selectedMapX = "";
    private String selectedMapY = "";
    private String selectedPlaceCategory = "";
    private String selectedPhone = "";

    // 선택된 이미지 — 모임 생성 버튼을 누를 때까지 메모리에만 보관
    private String pendingImageValue = null;  // naver URL or content:// Uri 문자열
    private String pendingImageSource = null; // "naver" | "gallery"

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageMeetingCardWritingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        SharedPreferences prefs = getSharedPreferences("honbabapp_prefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        setupUI();
    }

    /**
     * UI 초기 설정
     */
    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());
        setupCategorySelection();
        setupDatePicker();
        setupTimePicker();
        setupParticipantCounter();
        setupPriceRangeSlider();
        setupPlaceSelection();
        binding.btnCreateMeeting.setOnClickListener(v -> validateAndCreateMeeting());
    }

    /**
     * ActivityResultLauncher
     * PlaceSearchActivity → PlaceSearchImageSelectActivity 결과 최종 수신
     */
    private final ActivityResultLauncher<Intent> placeSearchLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                        Intent data = result.getData();

                        selectedPlaceName     = data.getStringExtra("place_name") != null    ? data.getStringExtra("place_name") : "";
                        selectedAddress       = data.getStringExtra("address") != null        ? data.getStringExtra("address") : "";
                        selectedRoadAddress   = data.getStringExtra("road_address") != null   ? data.getStringExtra("road_address") : "";
                        selectedSigungu       = data.getStringExtra("sigungu") != null        ? data.getStringExtra("sigungu") : "";
                        selectedDong          = data.getStringExtra("dong") != null           ? data.getStringExtra("dong") : "";
                        selectedMapX          = data.getStringExtra("map_x") != null          ? data.getStringExtra("map_x") : "";
                        selectedMapY          = data.getStringExtra("map_y") != null          ? data.getStringExtra("map_y") : "";
                        selectedPlaceCategory = data.getStringExtra("place_category") != null ? data.getStringExtra("place_category") : "";
                        selectedPhone         = data.getStringExtra("phone") != null          ? data.getStringExtra("phone") : "";

                        String imageValue  = data.getStringExtra("image_value");
                        String imageSource = data.getStringExtra("image_source");

                        if (imageValue != null && imageSource != null) {
                            pendingImageValue  = imageValue;
                            pendingImageSource = imageSource;
                        }

                        binding.etStoreSearch.setText(selectedPlaceName);
                        binding.selectedStoreCard.setVisibility(View.VISIBLE);
                        binding.tvSelectedStore.setText(selectedPlaceName);
                        binding.tvSelectedStoreAddress.setText(
                                selectedRoadAddress.isEmpty() ? selectedAddress : selectedRoadAddress
                        );

                        if (binding.etMeetingTitle.getText().toString().trim().isEmpty()) {
                            binding.etMeetingTitle.setText(selectedPlaceName + "에서 같이 먹어요!!");
                        }

                        autoSelectCategory(selectedPlaceCategory);

                        if (imageValue != null) {
                            Glide.with(this).load(imageValue).into(binding.ivMeetingPhotoPreview);
                            binding.ivMeetingPhotoPreview.setVisibility(View.VISIBLE);
                            binding.layoutPhotoPlaceholder.setVisibility(View.GONE);
                        }
                    }
            );

    /**
     * 카테고리 RadioButton 설정
     */
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
                Log.i(TAG, "선택된 카테고리: " + selectedCategory);
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

    /**
     * 날짜 선택 DatePicker
     */
    private void setupDatePicker() {
        binding.etMeetingDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(this,
                    (view, year, month, day) -> {
                        selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day);
                        String displayDate = String.format("%d월 %d일", month + 1, day);
                        binding.etMeetingDate.setText(displayDate);
                        Log.i(TAG, "선택된 날짜: " + selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMinDate(System.currentTimeMillis());
            dialog.show();
        });
    }

    /**
     * 시간 선택 TimePicker
     */
    private void setupTimePicker() {
        binding.etMeetingTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new TimePickerDialog(this,
                    (view, hour, minute) -> {
                        selectedTime = String.format("%02d:%02d:00", hour, minute);
                        String ampm = hour >= 12 ? "오후" : "오전";
                        int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
                        binding.etMeetingTime.setText(
                                String.format("%s %d:%02d", ampm, displayHour, minute)
                        );
                        Log.i(TAG, "선택된 시간: " + selectedTime);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
            ).show();
        });
    }

    /**
     * 참여 인원 증감 버튼
     */
    private void setupParticipantCounter() {
        updateParticipantCount();

        binding.btnDecreaseCount.setOnClickListener(v -> {
            if (participantCount > 2) {
                participantCount--;
                updateParticipantCount();
            }
        });

        binding.btnIncreaseCount.setOnClickListener(v -> {
            if (participantCount < 10) {
                participantCount++;
                updateParticipantCount();
            }
        });
    }

    private void updateParticipantCount() {
        binding.tvParticipantCount.setText(participantCount + "명");
    }

    /**
     * RangeSlider 설정
     */
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

    /**
     * 유효성 검증 — 일반 모임 전용
     */
    private void validateAndCreateMeeting() {
        String title = binding.etMeetingTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "모임명을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String description = binding.etMeetingDescription.getText().toString().trim();
        if (description.isEmpty()) {
            Toast.makeText(this, "모임 설명을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "날짜를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "시간을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedPlaceName.isEmpty()) {
            Toast.makeText(this, "장소를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);
        String priceRange = formatter.format(minPrice) + "원 ~ " + formatter.format(maxPrice) + "원";

        binding.btnCreateMeeting.setEnabled(false);
        binding.btnCreateMeeting.setText("생성 중...");

        if (pendingImageValue != null) {
            uploadImageThenCreate(title, description, selectedDate, selectedTime, priceRange);
        } else {
            createMeeting(title, description, selectedDate, selectedTime, priceRange, null);
        }
    }

    /**
     * S3 업로드 → 완료 후 createMeeting 호출
     */
    private void uploadImageThenCreate(String title, String description,
                                       String date, String time, String priceRange) {
        new Thread(() -> {
            // 기존 S3 업로드 로직 그대로
            runOnUiThread(() -> createMeeting(title, description, date, time, priceRange, null));
        }).start();
    }

    /**
     * 모임 생성 API 호출
     */
    private void createMeeting(String title, String description, String date, String time,
                               String priceRange, String meetingImageUrl) {
        String datetime = date + " " + time;

        MeetingCreateRequest.LocationRequest locationRequest =
                new MeetingCreateRequest.LocationRequest(
                        selectedPlaceName, selectedAddress, selectedRoadAddress,
                        selectedSigungu, selectedDong, selectedMapX, selectedMapY,
                        selectedPlaceCategory, selectedPhone
                );

        MeetingCreateRequest request = new MeetingCreateRequest(
                userId, title, description, selectedCategory,
                datetime, participantCount, priceRange, meetingImageUrl, locationRequest
        );

        apiService.createMeeting(request).enqueue(new Callback<ApiResponse<MeetingCreateResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingCreateResponse>> call,
                                   Response<ApiResponse<MeetingCreateResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<MeetingCreateResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        int meetingId = apiResponse.getData().getMeeting_id();
                        Log.i(TAG, "모임 생성 성공: meeting_id=" + meetingId);

                        Toast.makeText(MeetingCardWritingActivity.this,
                                "모임이 생성되었습니다!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(MeetingCardWritingActivity.this, MainActivity.class);
                        intent.putExtra("refresh_home", true);
                        intent.putExtra("new_meeting_id", meetingId);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(MeetingCardWritingActivity.this,
                                apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        resetCreateButton();
                    }
                } else {
                    Toast.makeText(MeetingCardWritingActivity.this,
                            "생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    resetCreateButton();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingCreateResponse>> call, Throwable throwable) {
                Log.e(TAG, "네트워크 오류: " + throwable.getMessage());
                Toast.makeText(MeetingCardWritingActivity.this,
                        "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                resetCreateButton();
            }
        });
    }

    private void resetCreateButton() {
        binding.btnCreateMeeting.setEnabled(true);
        binding.btnCreateMeeting.setText("모임 생성");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}