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
import com.example.honbabapp.activity.meeting.MeetingApplicantListActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.meeting.MeetingParticipant;
import com.example.honbabapp.api.data.request.meeting.DeleteMeetingRequest;
import com.example.honbabapp.api.data.request.meeting.JoinMeetingRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.ParticipantResponse;
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
 *
 * MeetingDetailActivity와 완전히 분리된 독립 Activity.
 * 체험단 전용 섹션(홍보채널 / 제공내역 / 신청조건 / 마감일)을 상단에 표시.
 * 신청 흐름(join → 승인 → 채팅)은 기존 API 그대로 재사용.
 */
public class CampaignDetailActivity extends AppCompatActivity {

    private static final String TAG = "CampaignDetailActivity";

    private PageCampaignDetailBinding binding;
    private ApiService apiService;
    private ParticipantListAdapter participantAdapter;
    private NaverMap miniNaverMap;

    private int campaignId;   // meeting_id 와 동일 — 백엔드 재사용
    private int currentUserId;
    private Meeting campaign; // Meeting 모델 재사용 (campaign 필드 포함)

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
    // API 호출 — MeetingDetailActivity.loadMeetingDetail() 동일 패턴
    // 백엔드 getMeetingDetail 엔드포인트 그대로 재사용
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
        // 헤더 타이틀
        binding.tvHeaderTitle.setText(campaign.getMeeting_title());

        // 대표 이미지
        String imageUrl = campaign.getMeeting_image();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_meeting_image)
                    .centerCrop()
                    .into(binding.ivCampaignImage);
        }

        // 기본 정보
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

        // ── 체험단 전용 섹션 ──────────────────────────────────────

        // 홍보 채널 ("블로그,인스타그램" → "블로그 · 인스타그램")
        if (campaign.getCampaign_channels() != null
                && !campaign.getCampaign_channels().isEmpty()) {
            binding.tvCampaignChannels.setText(
                    campaign.getCampaign_channels().replace(",", " · ")
            );
        }

        // 제공 내역
        if (campaign.getCampaign_provided() != null) {
            binding.tvCampaignProvided.setText(campaign.getCampaign_provided());
        }

        // 신청 조건 (있을 때만)
        if (campaign.getCampaign_requirements() != null
                && !campaign.getCampaign_requirements().isEmpty()) {
            binding.layoutRowRequirements.setVisibility(View.VISIBLE);
            binding.tvCampaignRequirements.setText(campaign.getCampaign_requirements());
        }

        // 모집 마감일 (있을 때만)
        if (campaign.getCampaign_deadline() != null
                && !campaign.getCampaign_deadline().isEmpty()) {
            binding.layoutRowDeadline.setVisibility(View.VISIBLE);
            String deadlineDisplay = campaign.getCampaign_deadline().length() >= 10
                    ? campaign.getCampaign_deadline().substring(0, 10) + " 마감"
                    : campaign.getCampaign_deadline();
            binding.tvCampaignDeadline.setText(deadlineDisplay);
        }

        // ── 방문 정보 ─────────────────────────────────────────────
        binding.tvVisitDate.setText(formatDate(campaign.getMeeting_date()));
        binding.tvVisitTime.setText(formatTime(campaign.getMeeting_date()));

        String participantText = campaign.getMeeting_current_participants()
                + "/" + campaign.getMeeting_max_participants() + "명";
        binding.tvParticipantCount.setText(participantText);

        // 참여자 타이틀
        binding.tvParticipantsTitle.setText(
                "참여자 (" + campaign.getMeeting_current_participants()
                        + "/" + campaign.getMeeting_max_participants() + ")"
        );

        // ── 호스트 메시지 ─────────────────────────────────────────
        binding.tvHostMessage.setText(campaign.getMeeting_description());

        // ── 참여자 RecyclerView ───────────────────────────────────
        setupParticipantsRecyclerView();

        // ── 미니맵 ───────────────────────────────────────────────
        setupMiniMap();

        // ── 호스트일 때 더보기 버튼 ──────────────────────────────
        if (campaign.getHost() != null
                && campaign.getHost().getUser_id() == currentUserId) {
            binding.btnMoreOptions.setVisibility(View.VISIBLE);
            binding.btnMoreOptions.setOnClickListener(v -> showHostPopupMenu());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 참여자 RecyclerView — MeetingDetailActivity 동일 패턴
    // ─────────────────────────────────────────────────────────────

    private void setupParticipantsRecyclerView() {
        if (campaign.getParticipants() == null || campaign.getParticipants().isEmpty()) return;

        // ParticipantListAdapter 생성자: (List<MeetingParticipant>, String hostNickName)
        String hostNickname = (campaign.getHost() != null)
                ? campaign.getHost().getUser_nickname() : "";

        participantAdapter = new ParticipantListAdapter(
                campaign.getParticipants(),
                hostNickname
        );
        binding.rvParticipants.setAdapter(participantAdapter);
    }

    // ─────────────────────────────────────────────────────────────
    // 미니맵 — MeetingDetailActivity 동일 패턴
    // ─────────────────────────────────────────────────────────────

    private void setupMiniMap() {
        if (campaign.getLocation() == null) return;

        try {
            double latitude  = Double.parseDouble(campaign.getLocation().getMap_y());
            double longitude = Double.parseDouble(campaign.getLocation().getMap_x());

            binding.miniMap.getMapAsync(naverMap -> {
                miniNaverMap = naverMap;

                // 인터랙션 비활성화 — MeetingDetailActivity 동일 패턴
                UiSettings uiSettings = naverMap.getUiSettings();
                uiSettings.setZoomControlEnabled(false);
                uiSettings.setCompassEnabled(false);
                uiSettings.setScaleBarEnabled(false);
                uiSettings.setScrollGesturesEnabled(false);
                uiSettings.setZoomGesturesEnabled(false);
                uiSettings.setRotateGesturesEnabled(false);
                uiSettings.setTiltGesturesEnabled(false);
                uiSettings.setLogoClickEnabled(false);

                LatLng position = new LatLng(latitude, longitude);

                Marker marker = new Marker();
                marker.setPosition(position);
                marker.setMap(naverMap);

                naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(position, 15));
            });

            // 클릭 시 MapFragment로 이동 — MeetingDetailActivity 동일 패턴
            binding.miniMap.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("move_to_map", true);
                intent.putExtra("map_lat", latitude);
                intent.putExtra("map_lng", longitude);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });

        } catch (NumberFormatException exception) {
            Log.e(TAG, "좌표 파싱 오류: " + exception.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 신청 버튼 — MeetingDetailActivity.setupJoinButton() 동일 패턴
    // 백엔드 join.php 그대로 재사용
    // ─────────────────────────────────────────────────────────────

    private void setupApplyButton() {
        if (campaign.isExpired()) {
            binding.applyButtonContainer.setVisibility(View.GONE);
            return;
        }

        // 호스트인 경우 → 신청자 목록 버튼
        if (campaign.getHost() != null
                && campaign.getHost().getUser_id() == currentUserId) {
            binding.applyButtonContainer.setVisibility(View.VISIBLE);
            binding.btnApplyCampaign.setText("신청자 목록");
            binding.btnApplyCampaign.setOnClickListener(v -> {
                Intent intent = new Intent(this, MeetingApplicantListActivity.class);
                intent.putExtra("meeting_id", campaignId);
                intent.putExtra("meeting_title", campaign.getMeeting_title());
                applicantListLauncher.launch(intent);
            });
            return;
        }

        // 참여 상태 확인
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
            binding.btnApplyCampaign.setOnClickListener(v -> showApplyConfirmDialog());
        }
    }

    private void showApplyConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("체험단 신청")
                .setMessage("이 체험단에 신청하시겠습니까?\n호스트의 승인 후 참여가 확정됩니다.")
                .setPositiveButton("신청하기", (dialog, which) -> applyCampaign())
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 체험단 신청 — 기존 join.php 그대로 재사용
     */
    private void applyCampaign() {
        binding.btnApplyCampaign.setEnabled(false);
        binding.btnApplyCampaign.setText("신청 중...");

        JoinMeetingRequest request = new JoinMeetingRequest(campaignId, currentUserId);
        Call<ApiResponse<ParticipantResponse>> call = apiService.joinMeeting(request);

        call.enqueue(new Callback<ApiResponse<ParticipantResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ParticipantResponse>> call,
                                   Response<ApiResponse<ParticipantResponse>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    binding.btnApplyCampaign.setText("신청 완료");
                    Toast.makeText(CampaignDetailActivity.this,
                            "체험단 신청이 완료되었습니다.\n호스트의 승인을 기다려주세요.",
                            Toast.LENGTH_LONG).show();
                    loadCampaignDetail();
                } else {
                    String message = response.body() != null
                            ? response.body().getMessage() : "신청에 실패했습니다.";
                    Toast.makeText(CampaignDetailActivity.this,
                            message, Toast.LENGTH_SHORT).show();
                    resetApplyButton();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ParticipantResponse>> call,
                                  Throwable throwable) {
                Log.e(TAG, "네트워크 오류: " + throwable.getMessage());
                Toast.makeText(CampaignDetailActivity.this,
                        "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                resetApplyButton();
            }
        });
    }

    private void resetApplyButton() {
        binding.btnApplyCampaign.setEnabled(true);
        binding.btnApplyCampaign.setText("체험단 신청하기");
    }

    // ─────────────────────────────────────────────────────────────
    // 호스트 팝업 메뉴 — MeetingDetailActivity 동일 패턴
    // ─────────────────────────────────────────────────────────────

    private void showHostPopupMenu() {
        PopupMenu popup = new PopupMenu(this, binding.btnMoreOptions);
        popup.getMenuInflater().inflate(R.menu.menu_meeting_detail, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_delete_meeting) {
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
                    intent.putExtra("refresh_home", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
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
    // ActivityResultLauncher
    // ─────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<Intent> applicantListLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            loadCampaignDetail();
                        }
                    }
            );

    // ─────────────────────────────────────────────────────────────
    // 날짜 / 시간 포맷 — MeetingDetailActivity 동일 패턴
    // ─────────────────────────────────────────────────────────────

    private String formatDate(String datetime) {
        try {
            SimpleDateFormat inputFormat  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
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
            int hour   = Integer.parseInt(hourFormat.format(date));
            int minute = Integer.parseInt(minuteFormat.format(date));
            String ampm       = hour >= 12 ? "오후" : "오전";
            int displayHour   = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
            return String.format("%s %d:%02d", ampm, displayHour, minute);
        } catch (Exception exception) {
            return datetime != null ? datetime : "";
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 생명주기 — 미니맵 위임
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void onStart()   { super.onStart();   binding.miniMap.onStart();   }
    @Override
    protected void onResume()  { super.onResume();  binding.miniMap.onResume();  }
    @Override
    protected void onPause()   { super.onPause();   binding.miniMap.onPause();   }
    @Override
    protected void onStop()    { super.onStop();    binding.miniMap.onStop();    }
    @Override
    protected void onDestroy() { super.onDestroy(); binding.miniMap.onDestroy(); binding = null; }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        binding.miniMap.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        binding.miniMap.onLowMemory();
    }
}