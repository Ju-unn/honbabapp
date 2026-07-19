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
import androidx.core.view.WindowCompat;

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
import com.example.honbabapp.util.InsetsUtils;
import com.example.honbabapp.ui.adapter.ParticipantListAdapter;
import com.example.honbabapp.util.ToastUtil;
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

    private final ActivityResultLauncher<Intent> applicantListLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) loadCampaignDetail();
                    }
            );

    private final ActivityResultLauncher<Intent> applyLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) loadCampaignDetail();
                    }
            );

    private final ActivityResultLauncher<Intent> editCampaignLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) loadCampaignDetail();
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
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsets(binding.getRoot());

        binding.miniMap.onCreate(savedInstanceState);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        campaignId = getIntent().getIntExtra("meeting_id", -1);
        if (campaignId == -1) {
            ToastUtil.makeText(this, "잘못된 접근입니다", Toast.LENGTH_SHORT).show();
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
        apiService.getMeetingDetail(campaignId, currentUserId)
                .enqueue(new Callback<ApiResponse<Meeting>>() {
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
                            ToastUtil.makeText(CampaignDetailActivity.this,
                                    "체험단 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Meeting>> call, Throwable throwable) {
                        Log.e(TAG, "네트워크 오류: " + throwable.getMessage());
                        ToastUtil.makeText(CampaignDetailActivity.this,
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

        if (campaign.getCampaign_channels() != null && !campaign.getCampaign_channels().isEmpty()) {
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

        if (campaign.getCampaign_deadline() != null && !campaign.getCampaign_deadline().isEmpty()) {
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
            binding.btnApplyCampaign.setOnClickListener(v -> checkSnsAndNavigateToApply());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 호스트 팝업 메뉴
    // ─────────────────────────────────────────────────────────────

    private void showHostPopupMenu() {
        PopupMenu popup = new PopupMenu(this, binding.btnMoreOptions);
        popup.getMenuInflater().inflate(R.menu.menu_meeting_detail, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_edit_meeting) {
                Intent intent = new Intent(this, CampaignEditActivity.class);
                intent.putExtra(CampaignEditActivity.EXTRA_CAMPAIGN_ID, campaignId);
                editCampaignLauncher.launch(intent);
                return true;
            }

            if (itemId == R.id.action_delete_meeting) {
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
                .setMessage("체험단을 삭제하시겠습니까?\n대기 중인 신청은 모두 취소되며, 채팅방도 함께 삭제됩니다.\n(승인된 참여자가 있으면 삭제할 수 없습니다)")
                .setPositiveButton("삭제", (dialog, which) -> deleteCampaign())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteCampaign() {
        DeleteMeetingRequest request = new DeleteMeetingRequest(campaignId);
        apiService.deleteMeeting(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    ToastUtil.makeText(CampaignDetailActivity.this,
                            "체험단이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CampaignDetailActivity.this, MainActivity.class);
                    intent.putExtra("refresh_campaign", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    String message = response.body() != null
                            ? response.body().getMessage() : "삭제에 실패했습니다.";
                    ToastUtil.makeText(CampaignDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable throwable) {
                Log.e(TAG, "삭제 네트워크 오류: " + throwable.getMessage());
                ToastUtil.makeText(CampaignDetailActivity.this,
                        "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // SNS 체크 → CampaignApplyActivity 이동
    // ─────────────────────────────────────────────────────────────

    private void checkSnsAndNavigateToApply() {
        binding.btnApplyCampaign.setEnabled(false);

        apiService.getUserProfile(currentUserId)
                .enqueue(new Callback<ApiResponse<UserProfileResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<UserProfileResponse>> call,
                                           Response<ApiResponse<UserProfileResponse>> response) {
                        binding.btnApplyCampaign.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            UserProfileResponse profile = response.body().getData();
                            if (!profile.hasSnsId()) {
                                showSnsRequiredDialog();
                            } else {
                                navigateToCampaignApply(profile);
                            }
                        } else {
                            ToastUtil.makeText(CampaignDetailActivity.this,
                                    "프로필 조회에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<UserProfileResponse>> call,
                                          Throwable throwable) {
                        binding.btnApplyCampaign.setEnabled(true);
                        Log.e(TAG, "프로필 조회 오류: " + throwable.getMessage());
                        ToastUtil.makeText(CampaignDetailActivity.this,
                                "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

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

        participantAdapter = new ParticipantListAdapter(campaign.getParticipants(), hostNickname);
        binding.rvParticipants.setAdapter(participantAdapter);
    }

    // ─────────────────────────────────────────────────────────────
    // 미니맵
    // ─────────────────────────────────────────────────────────────

    private void setupMiniMap() {
        if (campaign.getLocation() == null) return;
        try {
            double lat = Double.parseDouble(campaign.getLocation().getMap_y());
            double lng = Double.parseDouble(campaign.getLocation().getMap_x());

            binding.miniMap.getMapAsync(naverMap -> {
                miniNaverMap = naverMap;
                UiSettings uiSettings = naverMap.getUiSettings();
                uiSettings.setZoomControlEnabled(false);
                uiSettings.setCompassEnabled(false);
                uiSettings.setScaleBarEnabled(false);
                uiSettings.setScrollGesturesEnabled(false);
                uiSettings.setZoomGesturesEnabled(false);
                uiSettings.setRotateGesturesEnabled(false);
                uiSettings.setTiltGesturesEnabled(false);
                uiSettings.setLogoClickEnabled(false);

                LatLng position = new LatLng(lat, lng);
                Marker marker = new Marker();
                marker.setPosition(position);
                marker.setMap(naverMap);
                naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(position, 15));
            });

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
    // 날짜/시간 포맷 유틸
    // ─────────────────────────────────────────────────────────────

    private String formatDate(String datetime) {
        if (datetime == null || datetime.length() < 10) return "";
        try {
            SimpleDateFormat inputFormat  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
            SimpleDateFormat outputFormat = new SimpleDateFormat("M월 d일 (E)", Locale.KOREA);
            Date date = inputFormat.parse(datetime);
            return date != null ? outputFormat.format(date) : datetime.substring(0, 10);
        } catch (Exception e) {
            return datetime.substring(0, 10);
        }
    }

    private String formatTime(String datetime) {
        if (datetime == null || datetime.length() < 16) return "";
        try {
            SimpleDateFormat inputFormat  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
            SimpleDateFormat outputFormat = new SimpleDateFormat("a h:mm", Locale.KOREA);
            Date date = inputFormat.parse(datetime);
            return date != null ? outputFormat.format(date) : datetime.substring(11, 16);
        } catch (Exception e) {
            return datetime.substring(11, 16);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 생명주기 위임
    // ─────────────────────────────────────────────────────────────

    @Override protected void onStart()   { super.onStart();   binding.miniMap.onStart(); }
    @Override protected void onResume()  { super.onResume();  binding.miniMap.onResume(); }
    @Override protected void onPause()   { super.onPause();   binding.miniMap.onPause(); }
    @Override protected void onStop()    { super.onStop();    binding.miniMap.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); binding.miniMap.onDestroy(); binding = null; }
    @Override public void onLowMemory() { super.onLowMemory(); binding.miniMap.onLowMemory(); }
    @Override public void onSaveInstanceState(@Nullable android.os.Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) binding.miniMap.onSaveInstanceState(outState);
    }
}