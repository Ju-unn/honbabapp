package com.example.honbabapp.activity.meeting;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.bumptech.glide.Glide;
import com.example.honbabapp.activity.MainActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.ImageUploadData;
import com.example.honbabapp.api.data.request.meeting.MeetingCreateRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.MeetingCreateResponse;
import com.example.honbabapp.databinding.PageMeetingCardWritingBinding;
import com.example.honbabapp.util.InsetsUtils;
import com.example.honbabapp.util.ToastUtil;
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
 *
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
    private String selectedSido = "";
    private String selectedSigungu = "";
    private String selectedDong = "";
    private String selectedMapX = "";
    private String selectedMapY = "";
    private String selectedPlaceCategory = "";
    private String selectedPhone = "";

    // 선택된 이미지 — 모임 생성 버튼을 누를 때까지 메모리에만 보관
    private String pendingImageValue = null;  // naver URL or content:// Uri 문자열
    private String pendingImageSource = null; // "naver" | "gallery"

    // true = 자동 입력된 상태 → 가게 변경 시 덮어쓰기 허용
    // false = 사용자가 직접 수정한 상태 → 가게 변경 시 유지
    private boolean isTitleAutoFilled = false;
    // 자동 입력 중 TextWatcher 트리거 방지 플래그
    private boolean isAutoFillingTitle = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageMeetingCardWritingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsetsWithIme(binding.getRoot());

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
        setupTitleAutoFillWatcher(); // Issue #8
        binding.btnCreateMeeting.setOnClickListener(v -> validateAndCreateMeeting());
    }

    private void setupTitleAutoFillWatcher() {
        binding.etMeetingTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence sequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence sequence, int start, int before, int count) {
                if (!isAutoFillingTitle) {
                    isTitleAutoFilled = false;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
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
                        selectedSido          = data.getStringExtra("sido") != null           ? data.getStringExtra("sido") : "";
                        selectedSigungu       = data.getStringExtra("sigungu") != null        ? data.getStringExtra("sigungu") : "";
                        selectedDong          = data.getStringExtra("dong") != null           ? data.getStringExtra("dong") : "";
                        selectedMapX          = data.getStringExtra("map_x") != null          ? data.getStringExtra("map_x") : "";
                        selectedMapY          = data.getStringExtra("map_y") != null          ? data.getStringExtra("map_y") : "";
                        selectedPlaceCategory = data.getStringExtra("category") != null ? data.getStringExtra("category") : "";
                        selectedPhone         = data.getStringExtra("phone") != null          ? data.getStringExtra("phone") : "";

                        String imageValue  = data.getStringExtra("selected_image_url");
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

                        String currentTitle = binding.etMeetingTitle.getText().toString().trim();
                        if (currentTitle.isEmpty() || isTitleAutoFilled) {
                            isAutoFillingTitle = true;
                            binding.etMeetingTitle.setText(selectedPlaceName + "에서 같이 먹어요!!");
                            isAutoFillingTitle = false;
                            isTitleAutoFilled = true;
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
                        selectedDate = String.format(Locale.KOREA, "%04d-%02d-%02d", year, month + 1, day);
                        String displayDate = String.format(Locale.KOREA, "%d월 %d일", month + 1, day);
                        binding.etMeetingDate.setText(displayDate);
                        // 날짜 변경 시 시간 초기화 — 잘못된 과거 시간 방지
                        selectedTime = "";
                        binding.etMeetingTime.setText("");
                        Log.i(TAG, "선택된 날짜: " + selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMinDate(System.currentTimeMillis());
            dialog.show();
        });
    }

    private void setupTimePicker() {
        binding.etMeetingTime.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            new TimePickerDialog(this,
                    (view, hour, minute) -> {
                        // 오늘을 선택한 경우에만 시간 유효성 검증
                        String todayString = String.format(Locale.KOREA,
                                "%04d-%02d-%02d",
                                now.get(Calendar.YEAR),
                                now.get(Calendar.MONTH) + 1,
                                now.get(Calendar.DAY_OF_MONTH));

                        if (selectedDate.equals(todayString)) {
                            int currentHour   = now.get(Calendar.HOUR_OF_DAY);
                            int currentMinute = now.get(Calendar.MINUTE);
                            boolean isPastTime = hour < currentHour
                                    || (hour == currentHour && minute <= currentMinute);

                            if (isPastTime) {
                                ToastUtil.makeText(this,
                                        "현재 시간 이후를 선택해주세요",
                                        Toast.LENGTH_SHORT).show();
                                return; // selectedTime 설정 안 함
                            }
                        }

                        selectedTime = String.format(Locale.KOREA, "%02d:%02d:00", hour, minute);
                        String ampm = hour >= 12 ? "오후" : "오전";
                        int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
                        String displayTime = String.format(Locale.KOREA,
                                "%s %d:%02d", ampm, displayHour, minute);
                        binding.etMeetingTime.setText(displayTime);
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    false
            ).show();
        });
    }

    /**
     * 참여자 카운터
     */
    private void setupParticipantCounter() {
        binding.tvParticipantCount.setText(participantCount + "명");

        binding.btnDecreaseCount.setOnClickListener(v -> {
            if (participantCount > 2) {
                participantCount--;
                binding.tvParticipantCount.setText(participantCount + "명");
            }
        });

        binding.btnIncreaseCount.setOnClickListener(v -> {
            if (participantCount < 10) {
                participantCount++;
                binding.tvParticipantCount.setText(participantCount + "명");
            }
        });
    }

    /**
     * 가격 슬라이더
     */
    private void setupPriceRangeSlider() {
        NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);
        int min = minPrice;
        int max = maxPrice;
        binding.tvPriceRangeDisplay.setText(
                formatter.format(min) + "원 ~ " + formatter.format(max) + "원"
        );
        binding.rangeSliderPrice.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            minPrice = values.get(0).intValue();
            maxPrice = values.get(1).intValue();
            binding.tvPriceRangeDisplay.setText(
                    formatter.format(minPrice) + "원 ~ " + formatter.format(maxPrice) + "원"
            );
        });
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
            ToastUtil.makeText(this, "모임명을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (title.length() < 5 || title.length() > 30) {
            ToastUtil.makeText(this, "모임 제목은 5~30자 이내로 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String description = binding.etMeetingDescription.getText().toString().trim();
        if (description.isEmpty()) {
            ToastUtil.makeText(this, "모임 설명을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (description.length() < 10 || description.length() > 100) {
            ToastUtil.makeText(this, "모임 설명은 10~100자 이내로 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDate.isEmpty()) {
            ToastUtil.makeText(this, "날짜를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTime.isEmpty()) {
            ToastUtil.makeText(this, "시간을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedPlaceName.isEmpty()) {
            ToastUtil.makeText(this, "장소를 선택해주세요", Toast.LENGTH_SHORT).show();
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

    private void uploadImageThenCreate(String title, String description,
                                       String date, String time, String priceRange) {
        new Thread(() -> {
            File temporaryFile = null;
            try {
                byte[] originalBytes;
                if ("gallery".equals(pendingImageSource)) {
                    java.io.InputStream inputStream = getContentResolver()
                            .openInputStream(android.net.Uri.parse(pendingImageValue));
                    if (inputStream == null) { onUploadFailed(); return; }
                    java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        buf.write(buffer, 0, length);
                    }
                    inputStream.close();
                    originalBytes = buf.toByteArray();
                    temporaryFile = File.createTempFile("meeting_gallery_", ".jpg", getCacheDir());
                } else {
                    // 네이버 이미지 URL → OkHttp 다운로드
                    OkHttpClient okHttpClient = new OkHttpClient();
                    Request httpRequest = new Request.Builder().url(pendingImageValue).build();
                    ResponseBody responseBody = okHttpClient.newCall(httpRequest).execute().body();
                    if (responseBody == null) { onUploadFailed(); return; }
                    originalBytes = responseBody.bytes();
                    temporaryFile = File.createTempFile("meeting_img_", ".jpg", getCacheDir());
                }

                byte[] uploadBytes = com.example.honbabapp.util.ImageCompressUtil.compress(originalBytes);
                Log.i(TAG, "이미지 압축: "
                        + String.format("%.2f", originalBytes.length / 1024.0 / 1024.0) + "MB → "
                        + String.format("%.2f", uploadBytes.length / 1024.0 / 1024.0) + "MB");
                try (FileOutputStream fileOutputStream = new FileOutputStream(temporaryFile)) {
                    fileOutputStream.write(uploadBytes);
                }

                Log.i(TAG, "업로드 이미지 크기: "
                        + String.format("%.2f", temporaryFile.length() / 1024.0 / 1024.0) + "MB ("
                        + temporaryFile.length() + " bytes)");

                RequestBody requestBody = RequestBody.create(
                        MediaType.parse("image/jpeg"), temporaryFile
                );
                MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                        "meeting_image", temporaryFile.getName(), requestBody
                );

                final File finalTemporaryFile = temporaryFile;

                apiService.uploadMeetingImage(imagePart)
                        .enqueue(new Callback<ApiResponse<ImageUploadData>>() {
                            @Override
                            public void onResponse(Call<ApiResponse<ImageUploadData>> call,
                                                   Response<ApiResponse<ImageUploadData>> response) {
                                finalTemporaryFile.delete();
                                if (response.isSuccessful()
                                        && response.body() != null
                                        && response.body().isSuccess()) {
                                    String imageUrl = response.body().getData().getImage_url();
                                    Log.i(TAG, "이미지 S3 업로드 완료: " + imageUrl);
                                    createMeeting(title, description, date, time, priceRange, imageUrl);
                                } else {
                                    String failMessage = response.body() != null
                                            ? response.body().getMessage() : "body=null";
                                    Log.e(TAG, "이미지 S3 업로드 실패: httpCode=" + response.code()
                                            + ", success=" + (response.body() != null && response.body().isSuccess())
                                            + ", message=" + failMessage);
                                    onUploadFailed();
                                }
                            }

                            @Override
                            public void onFailure(Call<ApiResponse<ImageUploadData>> call,
                                                  Throwable throwable) {
                                finalTemporaryFile.delete();
                                Log.e(TAG, "이미지 업로드 네트워크 오류: " + throwable.getMessage());
                                onUploadFailed();
                            }
                        });

            } catch (IOException ioException) {
                Log.e(TAG, "이미지 처리 오류: " + ioException.getMessage());
                if (temporaryFile != null) temporaryFile.delete();
                onUploadFailed();
            }
        }).start();
    }

    private void onUploadFailed() {
        runOnUiThread(() -> {
            ToastUtil.makeText(this, "이미지 업로드에 실패했습니다. 다시 시도해주세요.",
                    Toast.LENGTH_SHORT).show();
            resetCreateButton();
        });
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
                        selectedSido, selectedSigungu, selectedDong, selectedMapX, selectedMapY,
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

                        ToastUtil.makeText(MeetingCardWritingActivity.this,
                                "모임이 생성되었습니다!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(MeetingCardWritingActivity.this, MainActivity.class);
                        intent.putExtra("refresh_home", true);
                        intent.putExtra("new_meeting_id", meetingId);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    } else {
                        ToastUtil.makeText(MeetingCardWritingActivity.this,
                                apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        resetCreateButton();
                    }
                } else {
                    ToastUtil.makeText(MeetingCardWritingActivity.this,
                            "생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    resetCreateButton();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingCreateResponse>> call, Throwable throwable) {
                Log.e(TAG, "네트워크 오류: " + throwable.getMessage());
                ToastUtil.makeText(MeetingCardWritingActivity.this,
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