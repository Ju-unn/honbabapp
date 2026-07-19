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
import androidx.core.view.WindowCompat;

import com.bumptech.glide.Glide;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.ImageUploadData;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.request.meeting.UpdateMeetingRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.databinding.PageMeetingCardWritingBinding;
import com.example.honbabapp.util.InsetsUtils;
import com.example.honbabapp.util.ToastUtil;
import com.google.android.gms.common.util.IOUtils;
import com.google.android.material.slider.RangeSlider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
 * 모임 수정 Activity
 *
 */
public class MeetingEditActivity extends AppCompatActivity {

    private static final String TAG = "MeetingEditActivity";
    private PageMeetingCardWritingBinding binding;  // 기존 XML 재사용
    private ApiService apiService;

    private int meetingId;
    private int userId;
    private Meeting meeting; // 서버에서 받아온 기존 데이터

    // 상태 변수 (MeetingCardWritingActivity와 동일)
    private String selectedCategory = "한식";
    private String selectedDate = "";
    private String selectedTime = "";
    private int participantCount = 2;
    private int minPrice = 0;
    private int maxPrice = 100000;

    // 장소 정보
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

    // 이미지 — 새로 선택한 경우만 S3 업로드
    private String pendingImageValue  = null; // naver URL or content:// Uri 문자열
    private String pendingImageSource = null; // "naver" | "gallery"

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

        meetingId = getIntent().getIntExtra("meeting_id", -1);
        if (meetingId == -1) {
            ToastUtil.makeText(this, "잘못된 접근입니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());

        // 버튼 텍스트 변경
        binding.btnCreateMeeting.setText("모임 수정");

        // 기존 데이터 불러오기
        loadMeetingDetail();
    }

    // ─────────────────────────────────────────────
    // 장소 검색 Launcher
    // ─────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> placeSearchLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                        Intent data = result.getData();

                        // 장소 정보 저장
                        selectedPlaceName     = data.getStringExtra("place_name");
                        selectedAddress       = data.getStringExtra("address");
                        selectedRoadAddress   = data.getStringExtra("road_address");
                        selectedSido          = data.getStringExtra("sido");
                        selectedSigungu       = data.getStringExtra("sigungu");
                        selectedDong          = data.getStringExtra("dong");
                        selectedMapX          = data.getStringExtra("map_x");
                        selectedMapY          = data.getStringExtra("map_y");
                        selectedPlaceCategory = data.getStringExtra("category");
                        selectedPhone         = data.getStringExtra("phone");

                        // UI 업데이트
                        binding.etStoreSearch.setText(selectedPlaceName);
                        binding.selectedStoreCard.setVisibility(View.VISIBLE);
                        binding.tvSelectedStore.setText(selectedPlaceName);
                        binding.tvSelectedStoreAddress.setText(selectedRoadAddress);

                        // 모임명 반자동 입력
                        binding.etMeetingTitle.setText(selectedPlaceName + "에서 같이 먹어요!!");


                        // 카테고리 자동 선택
                        autoSelectCategory(selectedPlaceCategory);

                        // 이미지 — 새로 선택한 경우만 교체
                        String imageSource = data.getStringExtra("image_source");
                        String imageValue  = data.getStringExtra("selected_image_url");
                        if (imageValue != null && !imageValue.isEmpty()) {
                            pendingImageSource = imageSource;
                            pendingImageValue  = imageValue;
                            Glide.with(MeetingEditActivity.this)
                                    .load(imageValue)
                                    .into(binding.ivMeetingPhotoPreview);
                            binding.ivMeetingPhotoPreview.setVisibility(View.VISIBLE);
                        }

                        Log.i(TAG, "선택한 장소: " + selectedPlaceName);
                    }
            );

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

    // ─────────────────────────────────────────────
    // 기존 모임 데이터 로드
    // ─────────────────────────────────────────────

    /**
     * 모임 상세 API 호출 → 성공 시 prefillForm()
     */
    private void loadMeetingDetail() {
        apiService.getMeetingDetail(meetingId, userId)
                .enqueue(new Callback<ApiResponse<Meeting>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Meeting>> call,
                                           Response<ApiResponse<Meeting>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            meeting = response.body().getData();
                            prefillForm();
                        } else {
                            ToastUtil.makeText(MeetingEditActivity.this,
                                    "모임 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Meeting>> call, Throwable t) {
                        ToastUtil.makeText(MeetingEditActivity.this,
                                "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    /**
     * 서버 데이터로 폼 채우기
     */
    private void prefillForm() {

        // 제목
        binding.etMeetingTitle.setText(meeting.getMeeting_title());

        // 설명
        binding.etMeetingDescription.setText(meeting.getMeeting_description());

        // 카테고리
        selectedCategory = meeting.getMeeting_category();
        preselectCategory(selectedCategory);

        // 날짜/시간 파싱 "2025-07-15 19:00:00"
        String datetime = meeting.getMeeting_date();
        if (datetime != null && datetime.contains(" ")) {
            String[] parts = datetime.split(" ");
            selectedDate = parts[0]; // "2025-07-15"
            selectedTime = parts[1]; // "19:00:00"

            // 표시용 포맷
            String[] dateParts = selectedDate.split("-");
            binding.etMeetingDate.setText(
                    dateParts[1] + "월 " + dateParts[2] + "일");

            String[] timeParts = selectedTime.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            String ampm = hour >= 12 ? "오후" : "오전";
            int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
            binding.etMeetingTime.setText(
                    String.format("%s %d:%02d", ampm, displayHour, minute));
        }

        // 참여 인원
        participantCount = meeting.getMeeting_max_participants();
        binding.tvParticipantCount.setText(participantCount + "명");

        // 가격대 파싱 "10,000원 ~ 30,000원" → 숫자 추출
        try {
            String priceStr = meeting.getMeeting_price_range()
                    .replaceAll("[^0-9~]", ""); // "10000~30000"
            String[] prices = priceStr.split("~");
            minPrice = Integer.parseInt(prices[0].trim());
            maxPrice = Integer.parseInt(prices[1].trim());
            binding.rangeSliderPrice.setValues((float) minPrice, (float) maxPrice);
            updatePriceRangeDisplay(minPrice, maxPrice);
        } catch (Exception e) {
            Log.e(TAG, "가격대 파싱 오류: " + e.getMessage());
        }

        // 장소
        if (meeting.getLocation() != null) {
            selectedPlaceName     = meeting.getLocation().getPlace_name();
            selectedAddress       = meeting.getLocation().getAddress();
            selectedRoadAddress   = meeting.getLocation().getRoad_address();
            selectedSido          = meeting.getLocation().getSido();
            selectedSigungu       = meeting.getLocation().getSigungu();
            selectedDong          = meeting.getLocation().getDong();
            selectedMapX          = meeting.getLocation().getMap_x();
            selectedMapY          = meeting.getLocation().getMap_y();
            selectedPlaceCategory = meeting.getLocation().getCategory();
            selectedPhone         = meeting.getLocation().getPhone_number();

            binding.etStoreSearch.setText(selectedPlaceName);
            binding.selectedStoreCard.setVisibility(View.VISIBLE);
            binding.tvSelectedStore.setText(selectedPlaceName);
            binding.tvSelectedStoreAddress.setText(selectedAddress);
        }

        // 기존 이미지 미리보기
        String existingImage = meeting.getMeeting_image();
        if (existingImage != null && !existingImage.isEmpty()) {
            Glide.with(this)
                    .load(existingImage)
                    .into(binding.ivMeetingPhotoPreview);
            binding.ivMeetingPhotoPreview.setVisibility(View.VISIBLE);
        }

        // UI 세팅
        setupPlaceSelection();
        setupDatePicker();
        setupTimePicker();
        setupParticipantCounter();
        setupPriceRangeSlider();

        // 저장 버튼
        binding.btnCreateMeeting.setOnClickListener(v -> validateAndUpdate());
    }

    // ─────────────────────────────────────────────
    // UI 세팅 메서드 (MeetingCardWritingActivity와 동일)
    // ─────────────────────────────────────────────

    /**
     * 장소 선택 버튼
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
     * 카테고리 RadioButton 선택
     */
    private void preselectCategory(String category) {
        RadioButton[] buttons = {
                binding.radioCategoryKorean,
                binding.radioCategoryChinese,
                binding.radioCategoryJapanese,
                binding.radioCategoryWestern,
                binding.radioCategoryOmakase,
                binding.radioCategoryCafe,
                binding.radioCategoryPub
        };
        String[] texts = {"한식", "중식", "일식", "양식", "오마카세", "카페", "술집"};

        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setChecked(texts[i].equals(category));
            final int idx = i;
            buttons[i].setOnClickListener(v -> {
                for (RadioButton b : buttons) b.setChecked(false);
                buttons[idx].setChecked(true);
                selectedCategory = texts[idx];
            });
        }
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

            DatePickerDialog dialog = new DatePickerDialog(this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        selectedDate = String.format("%04d-%02d-%02d",
                                selectedYear, selectedMonth + 1, selectedDay);
                        binding.etMeetingDate.setText(String.format("%d월 %d일",
                                selectedMonth + 1, selectedDay));
                        Log.i(TAG, "선택된 날짜: " + selectedDate);
                    }, year, month, day);

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
            int hour   = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            new TimePickerDialog(this,
                    (view, selectedHour, selectedMinute) -> {
                        // 오늘을 선택한 경우에만 시간 유효성 검증 (생성 화면과 동일)
                        Calendar now = Calendar.getInstance();
                        String todayString = String.format(Locale.KOREA, "%04d-%02d-%02d",
                                now.get(Calendar.YEAR),
                                now.get(Calendar.MONTH) + 1,
                                now.get(Calendar.DAY_OF_MONTH));

                        if (selectedDate.equals(todayString)) {
                            int currentHour   = now.get(Calendar.HOUR_OF_DAY);
                            int currentMinute = now.get(Calendar.MINUTE);
                            boolean isPastTime = selectedHour < currentHour
                                    || (selectedHour == currentHour && selectedMinute <= currentMinute);

                            if (isPastTime) {
                                ToastUtil.makeText(this,
                                        "현재 시간 이후를 선택해주세요",
                                        Toast.LENGTH_SHORT).show();
                                return; // selectedTime 설정 안 함
                            }
                        }

                        selectedTime = String.format("%02d:%02d:00", selectedHour, selectedMinute);
                        String ampm = selectedHour >= 12 ? "오후" : "오전";
                        int displayHour = selectedHour > 12 ? selectedHour - 12 :
                                (selectedHour == 0 ? 12 : selectedHour);
                        binding.etMeetingTime.setText(String.format("%s %d:%02d",
                                ampm, displayHour, selectedMinute));
                        Log.i(TAG, "선택된 시간: " + selectedTime);
                    }, hour, minute, false).show();
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
                formatter.format(min) + "원 ~ " + formatter.format(max) + "원");
    }

    // ─────────────────────────────────────────────
    // 수정 로직
    // ─────────────────────────────────────────────

    /**
     * 입력 검증 후 수정 진행
     */
    private void validateAndUpdate() {
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

        if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
            ToastUtil.makeText(this, "날짜와 시간을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 과거 시각 최종 검증 — "시간 먼저 선택 후 날짜를 오늘로 변경" 같은 조합도 커버
        // (서버 update.php도 거부하지만 클라이언트에서 먼저 안내)
        try {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
            java.util.Date meetingDateTime = sdf.parse(selectedDate + " " + selectedTime);
            if (meetingDateTime == null || !meetingDateTime.after(new java.util.Date())) {
                ToastUtil.makeText(this, "현재 시간 이후를 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (ParseException e) {
            ToastUtil.makeText(this, "날짜와 시간을 다시 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);
        String priceRange = formatter.format(minPrice) + "원 ~ " + formatter.format(maxPrice) + "원";
        String datetime = selectedDate + " " + selectedTime;

        binding.btnCreateMeeting.setEnabled(false);
        binding.btnCreateMeeting.setText("수정 중...");

        if (pendingImageValue != null) {
            // 새 이미지 선택 → S3 업로드 후 수정
            uploadImageThenUpdate(title, description, datetime, priceRange);
        } else {
            // 이미지 변경 없음 → 기존 이미지 URL 그대로 사용
            updateMeeting(title, description, datetime, priceRange, meeting.getMeeting_image());
        }
    }

    /**
     * S3 업로드 → 완료 후 updateMeeting() 호출
     */
    private void uploadImageThenUpdate(String title, String description,
                                       String datetime, String priceRange) {
        new Thread(() -> {
            File tmpFile = null;
            try {
                if ("gallery".equals(pendingImageSource)) {
                    java.io.InputStream is = getContentResolver()
                            .openInputStream(android.net.Uri.parse(pendingImageValue));
                    if (is == null) { onUploadFailed(); return; }
                    tmpFile = File.createTempFile("meeting_gallery_", ".jpg", getCacheDir());
                    try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                        fos.write(IOUtils.toByteArray(is));
                    }
                } else {
                    // 네이버 URL → OkHttp 다운로드
                    OkHttpClient client = new OkHttpClient();
                    Request req = new Request.Builder().url(pendingImageValue).build();
                    ResponseBody body = client.newCall(req).execute().body();
                    if (body == null) { onUploadFailed(); return; }
                    tmpFile = File.createTempFile("meeting_img_", ".jpg", getCacheDir());
                    try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                        fos.write(body.bytes());
                    }
                }

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
                                    Log.i(TAG, "이미지 S3 업로드 완료: " + imageUrl);
                                    updateMeeting(title, description, datetime, priceRange, imageUrl);
                                } else {
                                    onUploadFailed();
                                }
                            }

                            @Override
                            public void onFailure(Call<ApiResponse<ImageUploadData>> call, Throwable t) {
                                finalTmpFile.delete();
                                Log.e(TAG, "이미지 업로드 네트워크 오류: " + t.getMessage());
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
            ToastUtil.makeText(this, "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
            binding.btnCreateMeeting.setEnabled(true);
            binding.btnCreateMeeting.setText("모임 수정");
        });
    }

    /**
     * 모임 수정 API 호출
     */
    private void updateMeeting(String title, String description,
                               String datetime, String priceRange, String imageUrl) {
        // location 중첩 객체 생성
        UpdateMeetingRequest.LocationRequest locationRequest =
                new UpdateMeetingRequest.LocationRequest(
                        selectedPlaceName,
                        selectedAddress,
                        selectedRoadAddress,
                        selectedSido,
                        selectedSigungu,
                        selectedDong,
                        selectedMapX,
                        selectedMapY,
                        selectedPlaceCategory,
                        selectedPhone
                );

        // 변경
        UpdateMeetingRequest request = new UpdateMeetingRequest(
                meetingId,
                title,
                description,
                selectedCategory,
                datetime,
                participantCount,
                priceRange,
                imageUrl,
                locationRequest   // ← 중첩 객체로
        );

        apiService.updateMeeting(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    ToastUtil.makeText(MeetingEditActivity.this,
                            "모임이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // DetailActivity에 새로고침 신호
                    finish();
                } else {
                    String msg = response.body() != null
                            ? response.body().getMessage() : "수정에 실패했습니다.";
                    ToastUtil.makeText(MeetingEditActivity.this, msg, Toast.LENGTH_SHORT).show();
                    binding.btnCreateMeeting.setEnabled(true);
                    binding.btnCreateMeeting.setText("모임 수정");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "네트워크 오류: " + t.getMessage());
                ToastUtil.makeText(MeetingEditActivity.this,
                        "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                binding.btnCreateMeeting.setEnabled(true);
                binding.btnCreateMeeting.setText("모임 수정");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}