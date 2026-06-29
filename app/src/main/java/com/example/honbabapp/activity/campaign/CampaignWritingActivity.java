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
import com.example.honbabapp.api.data.ImageUploadData;
import com.example.honbabapp.api.data.request.meeting.MeetingCreateRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.MeetingCreateResponse;
import com.example.honbabapp.databinding.PageCampaignWritingBinding;
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
 * 체험단 등록 Activity
 *
 * MeetingCardWritingActivity / MeetingEditActivity 동일 패턴:
 * 1. 유효성 검증
 * 2. pendingImage 있으면 S3 업로드 (OkHttp 다운로드 → Multipart)
 * 3. registerCampaign() → createMeeting(meeting_type="체험단")
 * 4. 성공 → MainActivity refresh_campaign
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

    // 이미지 — MeetingCardWritingActivity 동일 패턴
    private String pendingImageValue = null;  // naver URL or content:// Uri
    private String pendingImageSource = null; // "naver" | "gallery"

    // ─────────────────────────────────────────────────────────────
    // 가게 검색 Launcher
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
                        pendingImageSource    = data.getStringExtra("image_source");
                        pendingImageValue     = data.getStringExtra("selected_image_url");

                        // 검색창에 주소 표시
                        binding.etStoreSearch.setText(
                                selectedRoadAddress != null && !selectedRoadAddress.isEmpty()
                                        ? selectedRoadAddress : selectedAddress
                        );

                        // 선택된 가게 카드 표시
                        binding.selectedStoreCard.setVisibility(View.VISIBLE);
                        binding.tvSelectedStore.setText(selectedPlaceName);
                        binding.tvSelectedStoreAddress.setText(
                                selectedRoadAddress != null && !selectedRoadAddress.isEmpty()
                                        ? selectedRoadAddress : selectedAddress
                        );

                        // 체험단명 자동 입력 (비어있을 때만)
                        if (binding.etCampaignTitle.getText().toString().trim().isEmpty()) {
                            binding.etCampaignTitle.setText(selectedPlaceName + " 체험단 모집");
                        }

                        // 이미지 미리보기
                        if (pendingImageValue != null) {
                            binding.layoutPhotoPlaceholder.setVisibility(View.GONE);
                            binding.ivCampaignPhotoPreview.setVisibility(View.VISIBLE);
                            Glide.with(this)
                                    .load(pendingImageValue)
                                    .centerCrop()
                                    .into(binding.ivCampaignPhotoPreview);
                        }

                        autoSelectCategory(selectedPlaceCategory);
                        Log.i(TAG, "가게 선택 완료: " + selectedPlaceName);
                    }
            );

    // ─────────────────────────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────────────────────────

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
    // 유효성 검증 + 등록 진입
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
            Toast.makeText(this, "방문 시간을 선택해주세요", Toast.LENGTH_SHORT).show();
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

        // 채널 문자열 조합
        StringBuilder channelsBuilder = new StringBuilder();
        if (isBlogChecked)      channelsBuilder.append("블로그,");
        if (isInstagramChecked) channelsBuilder.append("인스타그램,");
        if (isYoutubeChecked)   channelsBuilder.append("유튜브,");
        String campaignChannels = channelsBuilder.toString().replaceAll(",$", "");

        String campaignRequirements = binding.etCampaignRequirements.getText().toString().trim();
        String campaignDeadline     = binding.etCampaignDeadline.getText().toString().trim();

        NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);
        String priceRange = formatter.format(minPrice) + "원 ~ " + formatter.format(maxPrice) + "원";

        binding.btnRegisterCampaign.setEnabled(false);
        binding.btnRegisterCampaign.setText("등록 중...");

        if (pendingImageValue != null) {
            uploadImageThenRegister(
                    campaignTitle, campaignDescription, priceRange,
                    campaignChannels, campaignProvided, campaignRequirements, campaignDeadline
            );
        } else {
            registerCampaign(
                    campaignTitle, campaignDescription, priceRange,
                    campaignChannels, campaignProvided, campaignRequirements, campaignDeadline, null
            );
        }
    }

    // ─────────────────────────────────────────────────────────────
    // S3 이미지 업로드 → registerCampaign
    // MeetingEditActivity.uploadImageThenUpdate() 동일 패턴
    // ─────────────────────────────────────────────────────────────

    private void uploadImageThenRegister(
            String title, String description, String priceRange,
            String channels, String provided, String requirements, String deadline) {

        new Thread(() -> {
            File temporaryFile = null;
            try {
                if ("gallery".equals(pendingImageSource)) {
                    java.io.InputStream inputStream = getContentResolver()
                            .openInputStream(android.net.Uri.parse(pendingImageValue));
                    if (inputStream == null) { onUploadFailed(); return; }
                    temporaryFile = File.createTempFile("campaign_gallery_", ".jpg", getCacheDir());
                    try (FileOutputStream fileOutputStream = new FileOutputStream(temporaryFile)) {
                        byte[] buffer = new byte[4096];
                        int length;
                        while ((length = inputStream.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0, length);
                        }
                    }
                    inputStream.close();
                } else {
                    // 네이버 이미지 URL → OkHttp 다운로드
                    OkHttpClient okHttpClient = new OkHttpClient();
                    Request httpRequest = new Request.Builder().url(pendingImageValue).build();
                    ResponseBody responseBody = okHttpClient.newCall(httpRequest).execute().body();
                    if (responseBody == null) { onUploadFailed(); return; }
                    temporaryFile = File.createTempFile("campaign_img_", ".jpg", getCacheDir());
                    try (FileOutputStream fileOutputStream = new FileOutputStream(temporaryFile)) {
                        fileOutputStream.write(responseBody.bytes());
                    }
                }

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
                                    registerCampaign(title, description, priceRange,
                                            channels, provided, requirements, deadline, imageUrl);
                                } else {
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
            Toast.makeText(this, "이미지 업로드에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            resetRegisterButton();
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 체험단 등록 API 호출
    // MeetingCardWritingActivity.createMeeting() + campaign 필드 추가
    // ─────────────────────────────────────────────────────────────

    private void registerCampaign(
            String title, String description, String priceRange,
            String channels, String provided, String requirements,
            String deadline, @Nullable String imageUrl) {

        String datetime = selectedVisitDate + " " + selectedVisitTime;

        MeetingCreateRequest.LocationRequest locationRequest =
                new MeetingCreateRequest.LocationRequest(
                        selectedPlaceName, selectedAddress, selectedRoadAddress,
                        selectedSigungu, selectedDong, selectedMapX, selectedMapY,
                        selectedPlaceCategory, selectedPhone
                );

        MeetingCreateRequest request = new MeetingCreateRequest(
                userId, title, description, selectedCategory,
                datetime, participantCount, priceRange, imageUrl, locationRequest
        );

        // 체험단 전용 필드 세팅
        request.setMeeting_type("체험단");
        request.setCampaign_channels(channels);
        request.setCampaign_provided(provided);
        request.setCampaign_requirements(requirements.isEmpty() ? null : requirements);
        request.setCampaign_deadline(deadline.isEmpty() ? null : deadline + ":00");

        apiService.createMeeting(request)
                .enqueue(new Callback<ApiResponse<MeetingCreateResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<MeetingCreateResponse>> call,
                                           Response<ApiResponse<MeetingCreateResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            int meetingId = response.body().getData().getMeeting_id();
                            Log.i(TAG, "체험단 등록 성공: meeting_id=" + meetingId);

                            Toast.makeText(CampaignWritingActivity.this,
                                    "체험단이 등록되었습니다!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(CampaignWritingActivity.this, MainActivity.class);
                            intent.putExtra("refresh_campaign", true);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            finish();

                        } else {
                            String message = response.body() != null
                                    ? response.body().getMessage() : "등록에 실패했습니다.";
                            Toast.makeText(CampaignWritingActivity.this,
                                    message, Toast.LENGTH_SHORT).show();
                            resetRegisterButton();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<MeetingCreateResponse>> call,
                                          Throwable throwable) {
                        Log.e(TAG, "네트워크 오류: " + throwable.getMessage());
                        Toast.makeText(CampaignWritingActivity.this,
                                "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        resetRegisterButton();
                    }
                });
    }

    private void resetRegisterButton() {
        binding.btnRegisterCampaign.setEnabled(true);
        binding.btnRegisterCampaign.setText("체험단 등록");
    }

    // ─────────────────────────────────────────────────────────────
    // UI 세팅 메서드들 (XML ID 정확히 맞춤)
    // ─────────────────────────────────────────────────────────────

    private void setupPlaceSelection() {
        binding.btnSearchStore.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlaceSearchActivity.class);
            String query = binding.etStoreSearch.getText().toString().trim();
            if (!query.isEmpty()) intent.putExtra("search_query", query);
            placeSearchLauncher.launch(intent);
        });
    }

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

        for (int index = 0; index < buttons.length; index++) {
            final int categoryIndex = index;
            buttons[index].setOnClickListener(v -> {
                for (RadioButton button : buttons) button.setChecked(false);
                buttons[categoryIndex].setChecked(true);
                selectedCategory = categoryValues[categoryIndex];
            });
        }
        binding.radioCategoryKorean.setChecked(true);
        selectedCategory = "한식";
    }

    private void autoSelectCategory(String placeCategory) {
        if (placeCategory == null || placeCategory.isEmpty()) return;
        String lower = placeCategory.toLowerCase();
        if (lower.contains("한식"))       binding.radioCategoryKorean.performClick();
        else if (lower.contains("중식")) binding.radioCategoryChinese.performClick();
        else if (lower.contains("일식")) binding.radioCategoryJapanese.performClick();
        else if (lower.contains("양식")) binding.radioCategoryWestern.performClick();
        else if (lower.contains("카페")) binding.radioCategoryCafe.performClick();
        else if (lower.contains("술"))   binding.radioCategoryPub.performClick();
    }

    private void setupDatePicker() {
        binding.etCampaignVisitDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        selectedVisitDate = String.format(Locale.KOREA,
                                "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        binding.etCampaignVisitDate.setText(selectedVisitDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void setupTimePicker() {
        binding.etCampaignVisitTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        selectedVisitTime = String.format(Locale.KOREA,
                                "%02d:%02d:00", hourOfDay, minute);
                        String displayTime = String.format(Locale.KOREA,
                                "%s %02d:%02d",
                                hourOfDay >= 12 ? "오후" : "오전",
                                hourOfDay > 12 ? hourOfDay - 12 : hourOfDay,
                                minute);
                        binding.etCampaignVisitTime.setText(displayTime);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
            ).show();
        });
    }

    /**
     * 참여자 카운터
     * XML ID: btn_decrease_count / btn_increase_count / tv_participant_count
     * 표시 형식: "2명" (XML 힌트 텍스트 "2명" 과 일치)
     */
    private void setupParticipantCounter() {
        binding.tvParticipantCount.setText(participantCount + "명");

        // btn_decrease_count
        binding.btnDecreaseCount.setOnClickListener(v -> {
            if (participantCount > 2) {
                participantCount--;
                binding.tvParticipantCount.setText(participantCount + "명");
            }
        });

        // btn_increase_count (체험단 최대 20명)
        binding.btnIncreaseCount.setOnClickListener(v -> {
            if (participantCount < 20) {
                participantCount++;
                binding.tvParticipantCount.setText(participantCount + "명");
            }
        });
    }

    /**
     * 가격 슬라이더
     * XML ID: range_slider_price / tv_price_range_display
     */
    private void setupPriceRangeSlider() {
        NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);
        binding.tvPriceRangeDisplay.setText(
                formatter.format(minPrice) + "원 ~ " + formatter.format(maxPrice) + "원"
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

    private void setupDeadlinePicker() {
        binding.etCampaignDeadline.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        String deadlineDate = String.format(Locale.KOREA,
                                "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        binding.etCampaignDeadline.setText(deadlineDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}