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
import com.example.honbabapp.api.data.request.MeetingCreateRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.MeetingCreateResponse;
import com.example.honbabapp.databinding.PageMeetingCardWritingBinding;
import com.google.android.gms.common.util.IOUtils;
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
 * 모임 생성 Activity
 *
 * 이미지 처리 흐름:
 * 1. 가게 선택 → 이미지 URL/Uri 를 메모리(pendingImageValue)에만 저장
 * 2. 미리보기만 Glide 로 표시
 * 3. "모임 생성" 버튼 → 이미지 있으면 S3 업로드 → 완료 후 create.php 호출
 *    (취소 시 S3에 불필요한 파일이 쌓이지 않음)
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
    private String pendingImageValue  = null; // naver URL or content:// Uri 문자열
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

                        // 1. 장소 정보 저장
                        selectedPlaceName     = data.getStringExtra("place_name");
                        selectedAddress       = data.getStringExtra("address");
                        selectedRoadAddress   = data.getStringExtra("road_address");
                        selectedSigungu       = data.getStringExtra("sigungu");
                        selectedDong          = data.getStringExtra("dong");
                        selectedMapX          = data.getStringExtra("map_x");
                        selectedMapY          = data.getStringExtra("map_y");
                        selectedPlaceCategory = data.getStringExtra("category");
                        selectedPhone         = data.getStringExtra("phone");

                        // 2. 검색창 + 가게 카드 업데이트
                        binding.etStoreSearch.setText(selectedPlaceName);
                        binding.selectedStoreCard.setVisibility(View.VISIBLE);
                        binding.tvSelectedStore.setText(selectedPlaceName);
                        binding.tvSelectedStoreAddress.setText(selectedRoadAddress);

                        // 3. 모임명 반자동 입력 (비어있을 때만)
                        if (binding.etMeetingTitle.getText().toString().trim().isEmpty()) {
                            binding.etMeetingTitle.setText(selectedPlaceName + "에서 같이 먹어요!!");
                        }

                        // 4. 카테고리 자동 선택
                        autoSelectCategory(selectedPlaceCategory);

                        // 5. 이미지 — S3 업로드 하지 않고 메모리에만 저장
                        String imageSource = data.getStringExtra("image_source");
                        String imageValue  = data.getStringExtra("selected_image_url");

                        if (imageValue != null && !imageValue.isEmpty()) {
                            pendingImageSource = imageSource;
                            pendingImageValue  = imageValue;

                            // 미리보기만 표시 (Glide 는 URL / content:// Uri 둘 다 처리 가능)
                            Glide.with(MeetingCardWritingActivity.this)
                                    .load(imageValue)
                                    .into(binding.ivMeetingPhotoPreview);
                            binding.ivMeetingPhotoPreview.setVisibility(View.VISIBLE);
                        }

                        Log.i(TAG, "선택한 장소: " + selectedPlaceName);
                        Log.i(TAG, "주소: " + selectedRoadAddress);
                        Log.i(TAG, "좌표: (" + selectedMapX + ", " + selectedMapY + ")");
                    }
            );

    /**
     * 장소 선택 버튼 설정
     */
    private void setupPlaceSelection() {
        binding.btnSearchStore.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlaceSearchActivity.class);
            String query = binding.etStoreSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                intent.putExtra("search_query", query);
            }
            placeSearchLauncher.launch(intent);
        });
    }

    /**
     * 네이버 API category 로 RadioButton 자동 선택
     * 예) "음식점 > 한식 > 삼겹살" → 한식 선택
     */
    private void autoSelectCategory(String naverCategory) {
        if (naverCategory == null || naverCategory.isEmpty()) return;

        String lower = naverCategory.toLowerCase();

        RadioButton target = null;
        String text = null;

        if      (lower.contains("오마카세"))
        { target = binding.radioCategoryOmakase;  text = "오마카세"; }
        else if (lower.contains("카페") || lower.contains("커피") || lower.contains("디저트"))
        { target = binding.radioCategoryCafe;     text = "카페"; }
        else if (lower.contains("술집") || lower.contains("호프") || lower.contains("이자카야"))
        { target = binding.radioCategoryPub;      text = "술집"; }
        else if (lower.contains("중식") || lower.contains("중국"))
        { target = binding.radioCategoryChinese;  text = "중식"; }
        else if (lower.contains("일식") || lower.contains("스시") || lower.contains("초밥") || lower.contains("돈카") || lower.contains("회"))
        { target = binding.radioCategoryJapanese; text = "일식"; }
        else if (lower.contains("양식") || lower.contains("피자") || lower.contains("햄버거") || lower.contains("파스타") || lower.contains("스테이크"))
        { target = binding.radioCategoryWestern;  text = "양식"; }
        else if (lower.contains("한식") || lower.contains("삼겹살") || lower.contains("국밥") || lower.contains("김치"))
        { target = binding.radioCategoryKorean;   text = "한식"; }

        if (target != null) {
            RadioButton[] all = {
                    binding.radioCategoryKorean,
                    binding.radioCategoryChinese,
                    binding.radioCategoryJapanese,
                    binding.radioCategoryWestern,
                    binding.radioCategoryOmakase,
                    binding.radioCategoryCafe,
                    binding.radioCategoryPub
            };
            for (RadioButton b : all) b.setChecked(false);
            target.setChecked(true);
            selectedCategory = text;
            Log.i(TAG, "카테고리 자동 선택: " + selectedCategory);
        }
    }

    /**
     * 입력 검증
     * 이미지 있으면 S3 업로드 먼저 → 완료 후 createMeeting 호출
     * 이미지 없으면 바로 createMeeting 호출
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

        // 버튼 비활성화
        binding.btnCreateMeeting.setEnabled(false);
        binding.btnCreateMeeting.setText("생성 중...");

        if (pendingImageValue != null) {
            // 이미지 있음 → S3 업로드 후 모임 생성
            uploadImageThenCreate(title, description, selectedDate, selectedTime, priceRange);
        } else {
            // 이미지 없음 → 바로 모임 생성
            createMeeting(title, description, selectedDate, selectedTime, priceRange, null);
        }
    }

    /**
     * S3 업로드 → 완료 후 createMeeting 호출
     * "모임 생성" 버튼을 누른 시점에만 실행되므로 불필요한 S3 저장 없음
     */
    private void uploadImageThenCreate(String title, String description,
                                       String date, String time, String priceRange) {
        new Thread(() -> {
            File tmpFile = null;
            try {
                // 이미지 소스에 따라 임시 파일 생성
                if ("gallery".equals(pendingImageSource)) {
                    // content:// Uri → InputStream → 임시 파일
                    java.io.InputStream is = getContentResolver()
                            .openInputStream(android.net.Uri.parse(pendingImageValue));
                    if (is == null) {
                        onUploadFailed();
                        return;
                    }
                    tmpFile = File.createTempFile("meeting_gallery_", ".jpg", getCacheDir());
                    try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                        fos.write(IOUtils.toByteArray(is));
                    }

                } else {
                    // 네이버 URL → OkHttp 다운로드 → 임시 파일
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(pendingImageValue).build();
                    ResponseBody body = client.newCall(request).execute().body();
                    if (body == null) {
                        onUploadFailed();
                        return;
                    }
                    tmpFile = File.createTempFile("meeting_img_", ".jpg", getCacheDir());
                    try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                        fos.write(body.bytes());
                    }
                }

                // Multipart S3 업로드
                RequestBody reqBody = RequestBody.create(MediaType.parse("image/jpeg"), tmpFile);
                MultipartBody.Part part = MultipartBody.Part.createFormData(
                        "meeting_image", tmpFile.getName(), reqBody);

                final File finalTmpFile = tmpFile;

                apiService.uploadMeetingImage(part)
                        .enqueue(new Callback<ApiResponse<ImageUploadData>>() {
                            @Override
                            public void onResponse(Call<ApiResponse<ImageUploadData>> call,
                                                   Response<ApiResponse<ImageUploadData>> response) {
                                finalTmpFile.delete();

                                if (response.isSuccessful() && response.body() != null
                                        && response.body().isSuccess()) {
                                    String imageUrl = response.body().getData().getImage_url();
                                    Log.i(TAG, "모임 이미지 S3 업로드 완료: " + imageUrl);
                                    // 업로드 완료 → 모임 생성
                                    createMeeting(title, description, date, time, priceRange, imageUrl);
                                } else {
                                    Log.e(TAG, "모임 이미지 업로드 실패");
                                    onUploadFailed();
                                }
                            }

                            @Override
                            public void onFailure(Call<ApiResponse<ImageUploadData>> call, Throwable t) {
                                finalTmpFile.delete();
                                Log.e(TAG, "모임 이미지 업로드 네트워크 오류: " + t.getMessage());
                                onUploadFailed();
                            }
                        });

            } catch (IOException e) {
                Log.e(TAG, "이미지 처리 오류: " + e.getMessage());
                if (tmpFile != null) tmpFile.delete();
                onUploadFailed();
            }
        }).start();
    }

    /** 업로드 실패 시 버튼 복구 */
    private void onUploadFailed() {
        runOnUiThread(() -> {
            Toast.makeText(this, "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
            binding.btnCreateMeeting.setEnabled(true);
            binding.btnCreateMeeting.setText("모임 생성");
        });
    }

    /**
     * 모임 생성 API 호출
     * @param meetingImageUrl S3 URL (없으면 null)
     */
    private void createMeeting(String title, String description, String date, String time, String priceRange, String meetingImageUrl) {
        String datetime = date + " " + time;

        MeetingCreateRequest.LocationRequest locationRequest =
                new MeetingCreateRequest.LocationRequest(
                        selectedPlaceName,
                        selectedAddress,
                        selectedRoadAddress,
                        selectedSigungu,
                        selectedDong,
                        selectedMapX,
                        selectedMapY,
                        selectedPlaceCategory,
                        selectedPhone
                );

        MeetingCreateRequest request = new MeetingCreateRequest(
                userId,
                title,
                description,
                selectedCategory,
                datetime,
                participantCount,
                priceRange,
                meetingImageUrl,    // S3 URL or null
                locationRequest
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
                        Log.e(TAG, "API 실패: " + apiResponse.getMessage());
                        Toast.makeText(MeetingCardWritingActivity.this,
                                apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        binding.btnCreateMeeting.setEnabled(true);
                        binding.btnCreateMeeting.setText("모임 생성");
                    }
                } else {
                    Log.e(TAG, "HTTP 오류: " + response.code());
                    Toast.makeText(MeetingCardWritingActivity.this,
                            "모임 생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    binding.btnCreateMeeting.setEnabled(true);
                    binding.btnCreateMeeting.setText("모임 생성");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingCreateResponse>> call, Throwable t) {
                Log.e(TAG, "네트워크 오류: " + t.getMessage());
                Toast.makeText(MeetingCardWritingActivity.this,
                        "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                binding.btnCreateMeeting.setEnabled(true);
                binding.btnCreateMeeting.setText("모임 생성");
            }
        });
    }

    /**
     * 카테고리 설정
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

        String[] categoryTexts = {
                "한식", "중식", "일식", "양식", "오마카세", "카페", "술집"
        };

        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            buttons[i].setOnClickListener(v -> {
                for (RadioButton button : buttons) button.setChecked(false);
                buttons[index].setChecked(true);
                selectedCategory = categoryTexts[index];
                Log.i(TAG, "선택된 카테고리: " + selectedCategory);
            });
        }

        binding.radioCategoryKorean.setChecked(true);
        selectedCategory = "한식";
    }

    /**
     * 날짜 선택 DatePicker
     */
    private void setupDatePicker() {
        binding.etMeetingDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year  = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day   = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        selectedDate = String.format("%04d-%02d-%02d",
                                selectedYear, selectedMonth + 1, selectedDay);
                        String displayDate = String.format("%d월  %d일",
                                selectedMonth + 1, selectedDay);
                        binding.etMeetingDate.setText(displayDate);
                        Log.i(TAG, "선택된 날짜: " + selectedDate);
                    }, year, month, day);

            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    /**
     * 시간 선택 TimePicker
     */
    private void setupTimePicker() {
        binding.etMeetingTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour   = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, selectedHour, selectedMinute) -> {
                        selectedTime = String.format("%02d:%02d:00", selectedHour, selectedMinute);
                        String ampm = selectedHour >= 12 ? "오후" : "오전";
                        int displayHour = selectedHour > 12 ? selectedHour - 12 :
                                (selectedHour == 0 ? 12 : selectedHour);
                        String displayTime = String.format("%s %d:%02d",
                                ampm, displayHour, selectedMinute);
                        binding.etMeetingTime.setText(displayTime);
                        Log.i(TAG, "선택된 시간: " + selectedTime);
                    }, hour, minute, false);

            timePickerDialog.show();
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

    /**
     * 참여 인원 텍스트 업데이트
     */
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
                    Log.i(TAG, "가격대 변경: " + minPrice + "원 ~ " + maxPrice + "원");
                }
            }
        });
    }

    /**
     * 가격대 표시 업데이트
     */
    private void updatePriceRangeDisplay(int min, int max) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);
        String displayText = formatter.format(min) + "원 ~ " + formatter.format(max) + "원";
        binding.tvPriceRangeDisplay.setText(displayText);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}