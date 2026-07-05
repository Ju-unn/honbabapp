package com.example.honbabapp.activity.campaign;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.honbabapp.activity.meeting.PlaceSearchActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.ImageUploadData;
import com.example.honbabapp.api.data.Location;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.request.campaign.CampaignUpdateRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.databinding.PageCampaignWritingBinding;
import com.google.android.material.slider.RangeSlider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
 * 체험단 수정 Activity
 */
public class CampaignEditActivity extends AppCompatActivity {

    private static final String TAG = "CampaignEditActivity";

    public static final String EXTRA_CAMPAIGN_ID = "campaign_id";

    private PageCampaignWritingBinding binding;
    private ApiService apiService;

    private int campaignId;
    private int userId;
    private Meeting existingCampaign;

    // 상태 변수
    private String selectedCategory  = "한식";
    private int participantCount = 2;
    private int minPrice = 0;
    private int maxPrice = 100000;

    // 가게 정보
    private String selectedPlaceName     = "";
    private String selectedAddress       = "";
    private String selectedRoadAddress   = "";
    private String selectedSido          = "";
    private String selectedSigungu       = "";
    private String selectedDong          = "";
    private String selectedMapX          = "";
    private String selectedMapY          = "";
    private String selectedPlaceCategory = "";
    private String selectedPhone         = "";

    // 이미지
    private String pendingImageValue  = null;  // 새로 선택한 naver URL or content:// Uri
    private String pendingImageSource = null;  // "naver" | "gallery"
    private String existingImageUrl   = null;  // 기존 S3 URL — 새 이미지 없으면 그대로 전달

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
                        selectedSido          = data.getStringExtra("sido");
                        selectedSigungu       = data.getStringExtra("sigungu");
                        selectedDong          = data.getStringExtra("dong");
                        selectedMapX          = data.getStringExtra("map_x");
                        selectedMapY          = data.getStringExtra("map_y");
                        selectedPlaceCategory = data.getStringExtra("category");
                        selectedPhone         = data.getStringExtra("phone");
                        pendingImageSource    = data.getStringExtra("image_source");
                        pendingImageValue     = data.getStringExtra("selected_image_url");

                        String displayAddress = selectedRoadAddress != null && !selectedRoadAddress.isEmpty()
                                ? selectedRoadAddress : selectedAddress;

                        binding.etStoreSearch.setText(displayAddress);
                        binding.selectedStoreCard.setVisibility(View.VISIBLE);
                        binding.tvSelectedStore.setText(selectedPlaceName);
                        binding.tvSelectedStoreAddress.setText(displayAddress);

                        if (pendingImageValue != null) {
                            binding.layoutPhotoPlaceholder.setVisibility(View.GONE);
                            binding.ivCampaignPhotoPreview.setVisibility(View.VISIBLE);
                            Glide.with(this).load(pendingImageValue).centerCrop()
                                    .into(binding.ivCampaignPhotoPreview);
                        }

                        autoSelectCategory(selectedPlaceCategory);
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

        campaignId = getIntent().getIntExtra(EXTRA_CAMPAIGN_ID, -1);
        if (campaignId == -1) {
            Toast.makeText(this, "잘못된 접근입니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRegisterCampaign.setText("체험단 수정");

        loadCampaignDetail();
    }

    // ─────────────────────────────────────────────────────────────
    // 기존 체험단 데이터 로드
    // ─────────────────────────────────────────────────────────────

    private void loadCampaignDetail() {
        apiService.getMeetingDetail(campaignId, userId)
                .enqueue(new Callback<ApiResponse<Meeting>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Meeting>> call,
                                           Response<ApiResponse<Meeting>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            existingCampaign = response.body().getData();
                            prefillForm();
                        } else {
                            Toast.makeText(CampaignEditActivity.this,
                                    "체험단 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Meeting>> call, Throwable throwable) {
                        Log.e(TAG, "로드 오류: " + throwable.getMessage());
                        Toast.makeText(CampaignEditActivity.this,
                                "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    /**
     * 서버 데이터로 폼 채우기
     */
    private void prefillForm() {
        binding.etCampaignTitle.setText(existingCampaign.getMeeting_title());
        binding.etCampaignDescription.setText(existingCampaign.getMeeting_description());

        selectedCategory = existingCampaign.getMeeting_category();
        preselectCategory(selectedCategory);

        participantCount = existingCampaign.getMeeting_max_participants();
        binding.tvParticipantCount.setText(participantCount + "명");

        parsePriceRange(existingCampaign.getMeeting_price_range());

        // 가게 정보
        if (existingCampaign.getLocation() != null) {
            Location location = existingCampaign.getLocation();
            selectedPlaceName     = nvl(location.getPlace_name());
            selectedAddress       = nvl(location.getAddress());
            selectedRoadAddress   = nvl(location.getRoad_address());
            selectedSido          = nvl(location.getSido());
            selectedSigungu       = nvl(location.getSigungu());
            selectedDong          = nvl(location.getDong());
            selectedMapX          = nvl(location.getMap_x());
            selectedMapY          = nvl(location.getMap_y());
            selectedPlaceCategory = nvl(location.getCategory());
            selectedPhone         = nvl(location.getPhone_number());

            String displayAddress = selectedRoadAddress.isEmpty() ? selectedAddress : selectedRoadAddress;
            binding.etStoreSearch.setText(displayAddress);
            binding.selectedStoreCard.setVisibility(View.VISIBLE);
            binding.tvSelectedStore.setText(selectedPlaceName);
            binding.tvSelectedStoreAddress.setText(displayAddress);
        }

        // 기존 이미지
        existingImageUrl = existingCampaign.getMeeting_image();
        if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
            binding.layoutPhotoPlaceholder.setVisibility(View.GONE);
            binding.ivCampaignPhotoPreview.setVisibility(View.VISIBLE);
            Glide.with(this).load(existingImageUrl).centerCrop()
                    .into(binding.ivCampaignPhotoPreview);
        }

        // 체험단 전용 필드
        String channels = existingCampaign.getCampaign_channels();
        if (channels != null) {
            binding.checkboxChannelBlog.setChecked(channels.contains("블로그"));
            binding.checkboxChannelInstagram.setChecked(channels.contains("인스타그램"));
            binding.checkboxChannelYoutube.setChecked(channels.contains("유튜브"));
        }
        if (existingCampaign.getCampaign_provided() != null) {
            binding.etCampaignProvided.setText(existingCampaign.getCampaign_provided());
        }
        if (existingCampaign.getCampaign_requirements() != null) {
            binding.etCampaignRequirements.setText(existingCampaign.getCampaign_requirements());
        }
        if (existingCampaign.getCampaign_deadline() != null) {
            String deadline = existingCampaign.getCampaign_deadline();
            binding.etCampaignDeadline.setText(
                    deadline.length() >= 10 ? deadline.substring(0, 10) : deadline
            );
        }

        // UI 세팅 (프리필 후 리스너 등록)
        setupCategorySelection();
        setupParticipantCounter();
        setupPriceRangeSlider();
        setupPlaceSelection();
        setupDeadlinePicker();

        binding.btnRegisterCampaign.setOnClickListener(v -> validateAndUpdate());
    }

    // ─────────────────────────────────────────────────────────────
    // 유효성 검증
    // ─────────────────────────────────────────────────────────────

    private void validateAndUpdate() {
        String campaignTitle = binding.etCampaignTitle.getText().toString().trim();
        if (campaignTitle.isEmpty()) {
            Toast.makeText(this, "체험단 제목을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedPlaceName.isEmpty()) {
            Toast.makeText(this, "가게를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        String campaignDescription = binding.etCampaignDescription.getText().toString().trim();
        if (campaignDescription.isEmpty()) {
            Toast.makeText(this, "체험단 설명을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isBlogChecked      = binding.checkboxChannelBlog.isChecked();
        boolean isInstagramChecked = binding.checkboxChannelInstagram.isChecked();
        boolean isYoutubeChecked   = binding.checkboxChannelYoutube.isChecked();

        if (!isBlogChecked && !isInstagramChecked && !isYoutubeChecked) {
            Toast.makeText(this, "홍보 채널을 하나 이상 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String campaignProvided = binding.etCampaignProvided.getText().toString().trim();
        if (campaignProvided.isEmpty()) {
            Toast.makeText(this, "제공 내역을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 모집 마감일 필수 선택 검증 (등록 화면과 동일한 정책 적용)
        String campaignDeadline = binding.etCampaignDeadline.getText().toString().trim();
        if (campaignDeadline.isEmpty()) {
            Toast.makeText(this, "모집 마감일을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 채널 문자열 조합
        StringBuilder channelsBuilder = new StringBuilder();
        if (isBlogChecked)      channelsBuilder.append("블로그,");
        if (isInstagramChecked) channelsBuilder.append("인스타그램,");
        if (isYoutubeChecked)   channelsBuilder.append("유튜브,");
        String campaignChannels = channelsBuilder.toString().replaceAll(",$", "");

        String campaignRequirements = binding.etCampaignRequirements.getText().toString().trim();

        NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);
        String priceRange = formatter.format(minPrice) + "원 ~ " + formatter.format(maxPrice) + "원";

        binding.btnRegisterCampaign.setEnabled(false);
        binding.btnRegisterCampaign.setText("수정 중...");

        if (pendingImageValue != null) {
            uploadImageThenUpdate(campaignTitle, campaignDescription, priceRange,
                    campaignChannels, campaignProvided, campaignRequirements, campaignDeadline);
        } else {
            updateCampaign(campaignTitle, campaignDescription, priceRange,
                    campaignChannels, campaignProvided, campaignRequirements, campaignDeadline,
                    existingImageUrl);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // S3 이미지 업로드 → updateCampaign
    // ─────────────────────────────────────────────────────────────

    private void uploadImageThenUpdate(
            String title, String description, String priceRange,
            String channels, String provided, String requirements, String deadline) {

        new Thread(() -> {
            File temporaryFile = null;
            try {
                if ("gallery".equals(pendingImageSource)) {
                    java.io.InputStream inputStream = getContentResolver()
                            .openInputStream(android.net.Uri.parse(pendingImageValue));
                    if (inputStream == null) { onUploadFailed(); return; }
                    temporaryFile = File.createTempFile("campaign_edit_gallery_", ".jpg", getCacheDir());
                    try (FileOutputStream fileOutputStream = new FileOutputStream(temporaryFile)) {
                        byte[] buffer = new byte[4096];
                        int length;
                        while ((length = inputStream.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0, length);
                        }
                    }
                    inputStream.close();
                } else {
                    OkHttpClient okHttpClient = new OkHttpClient();
                    Request httpRequest = new Request.Builder().url(pendingImageValue).build();
                    ResponseBody responseBody = okHttpClient.newCall(httpRequest).execute().body();
                    if (responseBody == null) { onUploadFailed(); return; }
                    temporaryFile = File.createTempFile("campaign_edit_img_", ".jpg", getCacheDir());
                    try (FileOutputStream fileOutputStream = new FileOutputStream(temporaryFile)) {
                        fileOutputStream.write(responseBody.bytes());
                    }
                }

                RequestBody requestBody = RequestBody.create(
                        MediaType.parse("image/jpeg"), temporaryFile);
                MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                        "meeting_image", temporaryFile.getName(), requestBody);

                final File finalTemporaryFile = temporaryFile;

                apiService.uploadMeetingImage(imagePart)
                        .enqueue(new Callback<ApiResponse<ImageUploadData>>() {
                            @Override
                            public void onResponse(Call<ApiResponse<ImageUploadData>> call,
                                                   Response<ApiResponse<ImageUploadData>> response) {
                                finalTemporaryFile.delete();
                                if (response.isSuccessful() && response.body() != null
                                        && response.body().isSuccess()) {
                                    String imageUrl = response.body().getData().getImage_url();
                                    Log.i(TAG, "이미지 S3 업로드 완료: " + imageUrl);
                                    updateCampaign(title, description, priceRange,
                                            channels, provided, requirements, deadline, imageUrl);
                                } else {
                                    onUploadFailed();
                                }
                            }

                            @Override
                            public void onFailure(Call<ApiResponse<ImageUploadData>> call,
                                                  Throwable throwable) {
                                finalTemporaryFile.delete();
                                Log.e(TAG, "이미지 업로드 오류: " + throwable.getMessage());
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
            Toast.makeText(this, "이미지 업로드에 실패했습니다. 다시 시도해주세요.",
                    Toast.LENGTH_SHORT).show();
            resetUpdateButton();
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 체험단 수정 API 호출 — api/campaign/update.php
    // ─────────────────────────────────────────────────────────────

    private void updateCampaign(
            String title, String description, String priceRange,
            String channels, String provided, String requirements,
            String deadline, @Nullable String imageUrl) {

        // 방문 날짜/시간 제거 — 채팅으로 조율
        String datetime = null;

        CampaignUpdateRequest.LocationRequest locationRequest =
                new CampaignUpdateRequest.LocationRequest(
                        selectedPlaceName, selectedAddress, selectedRoadAddress,
                        selectedSido, selectedSigungu, selectedDong,
                        selectedMapX, selectedMapY,
                        selectedPlaceCategory, selectedPhone
                );

        CampaignUpdateRequest request = new CampaignUpdateRequest(
                campaignId, userId,
                title, description,
                selectedCategory, datetime,
                participantCount, priceRange, imageUrl,
                locationRequest,
                channels,
                provided,
                requirements.isEmpty() ? null : requirements,
                deadline + " 00:00:00"
        );

        apiService.updateCampaign(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    Toast.makeText(CampaignEditActivity.this,
                            "체험단이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    String message = response.body() != null
                            ? response.body().getMessage() : "수정에 실패했습니다.";
                    Toast.makeText(CampaignEditActivity.this,
                            message, Toast.LENGTH_SHORT).show();
                    resetUpdateButton();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable throwable) {
                Log.e(TAG, "수정 네트워크 오류: " + throwable.getMessage());
                Toast.makeText(CampaignEditActivity.this,
                        "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                resetUpdateButton();
            }
        });
    }

    private void resetUpdateButton() {
        binding.btnRegisterCampaign.setEnabled(true);
        binding.btnRegisterCampaign.setText("체험단 수정");
    }

    // ─────────────────────────────────────────────────────────────
    // UI 세팅
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
                binding.radioCategoryKorean, binding.radioCategoryChinese,
                binding.radioCategoryJapanese, binding.radioCategoryWestern,
                binding.radioCategoryOmakase, binding.radioCategoryCafe,
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
    }

    private void preselectCategory(String category) {
        RadioButton[] buttons = {
                binding.radioCategoryKorean, binding.radioCategoryChinese,
                binding.radioCategoryJapanese, binding.radioCategoryWestern,
                binding.radioCategoryOmakase, binding.radioCategoryCafe,
                binding.radioCategoryPub
        };
        String[] categoryValues = {"한식", "중식", "일식", "양식", "오마카세", "카페", "술집"};

        for (int index = 0; index < buttons.length; index++) {
            buttons[index].setChecked(categoryValues[index].equals(category));
        }
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

    private void setupParticipantCounter() {
        binding.tvParticipantCount.setText(participantCount + "명");
        binding.btnDecreaseCount.setOnClickListener(v -> {
            if (participantCount > 2) {
                participantCount--;
                binding.tvParticipantCount.setText(participantCount + "명");
            }
        });
        binding.btnIncreaseCount.setOnClickListener(v -> {
            if (participantCount < 20) {
                participantCount++;
                binding.tvParticipantCount.setText(participantCount + "명");
            }
        });
    }

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
            Long maximumDeadlineTimestamp = calculateMaximumDeadlineTimestampOrNull();

            if (maximumDeadlineTimestamp == null) {
                // 등록일 정보를 신뢰할 수 없으면 새로운 14일 창을 열어주지 않는다
                // (fail-closed). 기존 마감일 값은 그대로 유지된다.
                Toast.makeText(this,
                        "등록일 정보를 확인할 수 없어 마감일을 수정할 수 없습니다. 잠시 후 다시 시도해주세요.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Calendar calendar = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        binding.etCampaignDeadline.setText(String.format(Locale.KOREA,
                                "%04d-%02d-%02d", year, month + 1, dayOfMonth));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            // 오늘 이전 날짜 선택 불가
            dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            // 원본 등록일(meeting_created_at) 기준 최대 14일까지만 선택 가능
            dialog.getDatePicker().setMaxDate(maximumDeadlineTimestamp);
            dialog.show();
        });
    }

    /**
     * 원본 등록일(existingCampaign.getMeeting_created_at()) + 14일의 타임스탬프를 반환한다.
     *
     * @return 최대 선택 가능 타임스탬프, 신뢰할 수 없으면 null
     */
    @Nullable
    private Long calculateMaximumDeadlineTimestampOrNull() {
        String originalCreatedAt = existingCampaign != null
                ? existingCampaign.getMeeting_created_at() : null;

        if (originalCreatedAt == null || originalCreatedAt.isEmpty()) {
            return null;
        }

        try {
            SimpleDateFormat createdAtFormat =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
            Date originalCreatedAtDate = createdAtFormat.parse(originalCreatedAt);

            if (originalCreatedAtDate == null) {
                return null;
            }

            Calendar maximumDeadlineCalendar = Calendar.getInstance();
            maximumDeadlineCalendar.setTime(originalCreatedAtDate);
            maximumDeadlineCalendar.add(Calendar.DAY_OF_YEAR, 14);
            return maximumDeadlineCalendar.getTimeInMillis();

        } catch (ParseException parseException) {
            Log.w(TAG, "meeting_created_at 파싱 실패: " + originalCreatedAt);
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 유틸
    // ─────────────────────────────────────────────────────────────

    private String nvl(String value) {
        return value != null ? value : "";
    }

    private void parsePriceRange(String priceRangeString) {
        if (priceRangeString == null || priceRangeString.isEmpty()) return;
        try {
            String cleaned = priceRangeString.replaceAll("[^0-9~]", "");
            String[] parts = cleaned.split("~");
            if (parts.length == 2) {
                minPrice = Integer.parseInt(parts[0].trim());
                maxPrice = Integer.parseInt(parts[1].trim());
            }
        } catch (NumberFormatException numberFormatException) {
            Log.w(TAG, "가격대 파싱 실패, 기본값 유지: " + priceRangeString);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}