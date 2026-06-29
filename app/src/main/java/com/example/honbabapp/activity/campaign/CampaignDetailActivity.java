package com.example.honbabapp.activity.campaign;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.activity.MainActivity;
import com.example.honbabapp.activity.profile.ProfileProfileSettingActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.request.meeting.DeleteMeetingRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.user.UserProfileResponse;
import com.example.honbabapp.databinding.PageCampaignDetailBinding;
import com.example.honbabapp.ui.adapter.ParticipantListAdapter;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 체험단 상세 Activity
 */
public class CampaignDetailActivity extends AppCompatActivity {

    private static final String TAG = "CampaignDetailActivity";

    private PageCampaignDetailBinding binding;
    private ApiService apiService;
    private ParticipantListAdapter participantAdapter;
    private NaverMap miniNaverMap;

    private int campaignId;
    private int currentUserId;
    private Meeting campaign;

    // ─────────────────────────────────────────────────────────────
    // ActivityResultLauncher
    // ─────────────────────────────────────────────────────────────

    /**
     * 체험단 신청자 목록 (호스트 전용) — 돌아오면 상세 새로고침
     */
    private final ActivityResultLauncher<Intent> applicantListLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            loadCampaignDetail();
                        }
                    }
            );

    /**
     * CampaignApplyActivity — 신청 완료 후 상세 새로고침
     */
    private final ActivityResultLauncher<Intent> applyLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            loadCampaignDetail();
                        }
                    }
            );

    // ─────────────────────────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageCampaignDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.miniMap.onCreate(savedInstanceState);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        campaignId = getIntent().getIntExtra("meeting_id", -1);
        if (campaignId == -1) {
            Toast.makeText(this, "잘못된 접근입니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("honbabapp_prefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);

        binding.btnBack.setOnClickListener(v -> finish());

        loadCampaignDetail();
    }

    // ─────────────────────────────────────────────────────────────
    // API 호출
    // ─────────────────────────────────────────────────────────────

    private void loadCampaignDetail() {
        Call<ApiResponse<Meeting>> call = apiService.getMeetingDetail(campaignId, currentUserId);

        call.enqueue(new Callback<ApiResponse<Meeting>>() {
            @Override
            public void onResponse(Call<ApiResponse<Meeting>> call,
                                   Response<ApiResponse<Meeting>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    campaign = response.body().getData();
                    Log.i(TAG, "체험단 로드 성공: " + campaign.getMeeting_title());
                    bindCampaignData();
                    setupApplyButton();
                } else {
                    Toast.makeText(CampaignDetailActivity.this,
                            "체험단 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Meeting>> call, Throwable throwable) {
                Log.e(TAG, "네트워크 오류: " + throwable.getMessage());
                Toast.makeText(CampaignDetailActivity.this,
                        "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 데이터 바인딩
    // ─────────────────────────────────────────────────────────────

    private void bindCampaignData() {
        binding.tvHeaderTitle.setText(campaign.getMeeting_title());

        String imageUrl = campaign.getMeeting_image();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_meeting_image)
                    .centerCrop()
                    .into(binding.ivCampaignImage);
        }

        binding.tvCampaignTitle.setText(campaign.getMeeting_title());

        if (campaign.getLocation() != null) {
            binding.tvStoreCategory.setText(campaign.getMeeting_category());
            binding.tvStoreAddress.setText(
                    campaign.getLocation().getRoad_address() != null
                            && !campaign.getLocation().getRoad_address().isEmpty()
                            ? campaign.getLocation().getRoad_address()
                            : campaign.getLocation().getAddress()
            );
        }
        binding.tvPriceRange.setText(campaign.getMeeting_price_range());

        if (campaign.getCampaign_channels() != null
                && !campaign.getCampaign_channels().isEmpty()) {
            binding.tvCampaignChannels.setText(
                    campaign.getCampaign_channels().replace(",", " · ")
            );
        }

        if (campaign.getCampaign_provided() != null) {
            binding.tvCampaignProvided.setText(campaign.getCampaign_provided());
        }

        if (campaign.getCampaign_requirements() != null
                && !campaign.getCampaign_requirements().isEmpty()) {
            binding.layoutRowRequirements.setVisibility(View.VISIBLE);
            binding.tvCampaignRequirements.setText(campaign.getCampaign_requirements());
        }

        if (campaign.getCampaign_deadline() != null
                && !campaign.getCampaign_deadline().isEmpty()) {
            binding.layoutRowDeadline.setVisibility(View.VISIBLE);
            String deadlineDisplay = campaign.getCampaign_deadline().length() >= 10
                    ? campaign.getCampaign_deadline().substring(0, 10) + " 마감"
                    : campaign.getCampaign_deadline();
            binding.tvCampaignDeadline.setText(deadlineDisplay);
        }

        binding.tvVisitDate.setText(formatDate(campaign.getMeeting_date()));
        binding.tvVisitTime.setText(formatTime(campaign.getMeeting_date()));

        String participantText = campaign.getMeeting_current_participants()
                + "/" + campaign.getMeeting_max_participants() + "명";
        binding.tvParticipantCount.setText(participantText);

        binding.tvParticipantsTitle.setText(
                "참여자 (" + campaign.getMeeting_current_participants()
                        + "/" + campaign.getMeeting_max_participants() + ")"
        );

        binding.tvHostMessage.setText(campaign.getMeeting_description());

        setupParticipantsRecyclerView();
        setupMiniMap();

        if (campaign.getHost() != null && campaign.getHost().getUser_id() == currentUserId) {
            binding.btnMoreOptions.setVisibility(View.VISIBLE);
            binding.btnMoreOptions.setOnClickListener(v -> showHostPopupMenu());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 신청 버튼 설정
    // ─────────────────────────────────────────────────────────────

    private void setupApplyButton() {
        if (campaign.isExpired()) {
            binding.applyButtonContainer.setVisibility(View.GONE);
            return;
        }

        // ── 호스트: CampaignApplicantListActivity 이동 ──
        if (campaign.getHost() != null && campaign.getHost().getUser_id() == currentUserId) {
            binding.applyButtonContainer.setVisibility(View.VISIBLE);
            binding.btnApplyCampaign.setText("신청자 목록");
            binding.btnApplyCampaign.setOnClickListener(v -> {
                Intent intent = new Intent(this, CampaignApplicantListActivity.class);
                intent.putExtra(CampaignApplicantListActivity.EXTRA_CAMPAIGN_ID, campaignId);
                intent.putExtra(CampaignApplicantListActivity.EXTRA_CAMPAIGN_TITLE,
                        campaign.getMeeting_title());
                applicantListLauncher.launch(intent);
            });
            return;
        }

        // ── 참여자: 참여 상태에 따라 버튼 표시 ──────────────────
        String participationStatus = campaign.getMy_participation_status();
        if ("취소".equals(participationStatus)) participationStatus = null;

        if (participationStatus != null) {
            switch (participationStatus) {
                case "승인":
                    binding.btnApplyCampaign.setText("신청 완료");
                    binding.btnApplyCampaign.setEnabled(false);
                    break;
                case "대기":
                    binding.btnApplyCampaign.setText("승인 대기 중");
                    binding.btnApplyCampaign.setEnabled(false);
                    break;
                case "거절":
                    binding.btnApplyCampaign.setText("신청 거절됨");
                    binding.btnApplyCampaign.setEnabled(false);
                    break;
            }
        } else if ("모집완료".equals(campaign.getMeeting_status())) {
            binding.btnApplyCampaign.setText("모집 완료");
            binding.btnApplyCampaign.setEnabled(false);
        } else {
            // ── 신청 버튼: SNS 체크 후 CampaignApplyActivity 이동 ──
            binding.btnApplyCampaign.setOnClickListener(v -> checkSnsAndNavigateToApply());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SNS 체크 CampaignApplyActivity 이동
    // "먼저 내 프로필 조회(getUserProfile)"
    // SNS 없으면 ProfileProfileSettingActivity로 유도
    // ─────────────────────────────────────────────────────────────

    private void checkSnsAndNavigateToApply() {
        binding.btnApplyCampaign.setEnabled(false);

        apiService.getUserProfile(currentUserId)
                .enqueue(new Callback<ApiResponse<UserProfileResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<UserProfileResponse>> call,
                                           Response<ApiResponse<UserProfileResponse>> response) {
                        binding.btnApplyCampaign.setEnabled(true);

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            UserProfileResponse profile = response.body().getData();

                            if (!profile.hasSnsId()) {
                                // SNS 없음 → 프로필 설정 유도
                                showSnsRequiredDialog();
                            } else {
                                // SNS 있음 → 신청폼 이동 (SNS ID 전달)
                                navigateToCampaignApply(profile);
                            }
                        } else {
                            Toast.makeText(CampaignDetailActivity.this,
                                    "프로필 조회에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<UserProfileResponse>> call,
                                          Throwable throwable) {
                        binding.btnApplyCampaign.setEnabled(true);
                        Log.e(TAG, "프로필 조회 오류: " + throwable.getMessage());
                        Toast.makeText(CampaignDetailActivity.this,
                                "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * SNS ID 없을 때 안내 다이얼로그
     */
    private void showSnsRequiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("SNS 계정 필요")
                .setMessage("체험단 신청을 위해서는 인스타그램 또는 블로그 ID가 필요합니다.\n"
                        + "프로필 설정에서 먼저 등록해 주세요.")
                .setPositiveButton("프로필 설정으로 이동", (dialog, which) -> {
                    Intent intent = new Intent(this, ProfileProfileSettingActivity.class);
                    intent.putExtra("focus_sns", true);
                    startActivity(intent);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * SNS 있음 → CampaignApplyActivity 이동 (SNS ID 미리 전달)
     */
    private void navigateToCampaignApply(UserProfileResponse profile) {
        Intent intent = new Intent(this, CampaignApplyActivity.class);
        intent.putExtra(CampaignApplyActivity.EXTRA_CAMPAIGN_ID, campaignId);
        intent.putExtra(CampaignApplyActivity.EXTRA_CAMPAIGN_TITLE, campaign.getMeeting_title());
        intent.putExtra(CampaignApplyActivity.EXTRA_INSTAGRAM_ID, profile.getUser_instagram_id());
        intent.putExtra(CampaignApplyActivity.EXTRA_BLOG_ID, profile.getUser_blog_id());
        applyLauncher.launch(intent);
    }

    // ─────────────────────────────────────────────────────────────
    // 참여자 RecyclerView
    // ─────────────────────────────────────────────────────────────

    private void setupParticipantsRecyclerView() {
        if (campaign.getParticipants() == null || campaign.getParticipants().isEmpty()) return;

        String hostNickname = (campaign.getHost() != null)
                ? campaign.getHost().getUser_nickname() : "";

        participantAdapter = new ParticipantListAdapter(
                campaign.getParticipants(), hostNickname
        );
        binding.rvParticipants.setAdapter(participantAdapter);
    }

    // ─────────────────────────────────────────────────────────────
    // 미니맵
    // ─────────────────────────────────────────────────────────────

    private void setupMiniMap() {
        if (campaign.getLocation() == null) return;

        // MeetingDetailActivity 동일 패턴:
        // 좌표 파싱을 getMapAsync 밖 try 블록에서 먼저 수행
        try {
            double lat = Double.parseDouble(campaign.getLocation().getMap_y());
            double lng = Double.parseDouble(campaign.getLocation().getMap_x());

            binding.miniMap.getMapAsync(naverMap -> {
                miniNaverMap = naverMap;

                // 인터랙션 비활성화 — 개별 메서드 각각 호출 (setAllGesturesEnabled 사용 안 함)
                UiSettings uiSettings = naverMap.getUiSettings();
                uiSettings.setZoomControlEnabled(false);
                uiSettings.setCompassEnabled(false);
                uiSettings.setScaleBarEnabled(false);
                uiSettings.setScrollGesturesEnabled(false);
                uiSettings.setZoomGesturesEnabled(false);
                uiSettings.setRotateGesturesEnabled(false);
                uiSettings.setTiltGesturesEnabled(false);
                uiSettings.setLogoClickEnabled(false);

                // 마커 + 카메라
                LatLng position = new LatLng(lat, lng);
                Marker marker = new Marker();
                marker.setPosition(position);
                marker.setMap(naverMap);

                naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(position, 15));
            });

            // 클릭 리스너는 getMapAsync 밖에 설정 — MeetingDetailActivity 동일 패턴
            binding.miniMap.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("move_to_map", true);
                intent.putExtra("map_lat", lat);
                intent.putExtra("map_lng", lng);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });

        } catch (NumberFormatException exception) {
            Log.e(TAG, "좌표 파싱 오류: " + exception.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 호스트 팝업 메뉴 / 삭제
    // ─────────────────────────────────────────────────────────────

    private void showHostPopupMenu() {
        PopupMenu popup = new PopupMenu(this, binding.btnMoreOptions);
        popup.getMenuInflater().inflate(R.menu.menu_meeting_detail, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete_meeting) {
                showDeleteConfirmDialog();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("체험단 삭제")
                .setMessage("체험단을 삭제하시겠습니까?\n모든 신청이 취소됩니다.")
                .setPositiveButton("삭제", (dialog, which) -> deleteCampaign())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteCampaign() {
        DeleteMeetingRequest request = new DeleteMeetingRequest(campaignId, currentUserId);
        apiService.deleteMeeting(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    Toast.makeText(CampaignDetailActivity.this,
                            "체험단이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CampaignDetailActivity.this, MainActivity.class);
                    intent.putExtra("refresh_campaign", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    String message = response.body() != null
                            ? response.body().getMessage() : "삭제에 실패했습니다.";
                    Toast.makeText(CampaignDetailActivity.this,
                            message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable throwable) {
                Toast.makeText(CampaignDetailActivity.this,
                        "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 날짜 / 시간 포맷 (기존 그대로)
    // ─────────────────────────────────────────────────────────────

    private String formatDate(String datetime) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
            SimpleDateFormat outputFormat = new SimpleDateFormat("M월 d일", Locale.KOREA);
            Date date = inputFormat.parse(datetime);
            return date != null ? outputFormat.format(date) : datetime;
        } catch (Exception exception) {
            return datetime != null ? datetime : "";
        }
    }

    private String formatTime(String datetime) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
            Date date = inputFormat.parse(datetime);
            if (date == null) return datetime;
            SimpleDateFormat hourFormat = new SimpleDateFormat("HH", Locale.KOREA);
            SimpleDateFormat minuteFormat = new SimpleDateFormat("mm", Locale.KOREA);
            int hour = Integer.parseInt(hourFormat.format(date));
            int minute = Integer.parseInt(minuteFormat.format(date));
            String ampm = hour >= 12 ? "오후" : "오전";
            int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
            return String.format("%s %d:%02d", ampm, displayHour, minute);
        } catch (Exception exception) {
            return datetime != null ? datetime : "";
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void onStart() {
        super.onStart();
        binding.miniMap.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.miniMap.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.miniMap.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        binding.miniMap.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) binding.miniMap.onDestroy();
        binding = null;
    }

    @Override
    protected void onSaveInstanceState(android.os.Bundle outState) {
        super.onSaveInstanceState(outState);
        binding.miniMap.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        binding.miniMap.onLowMemory();
    }
}