package com.example.honbabapp.activity.campaign;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.request.meeting.ApproveMeetingRequest;
import com.example.honbabapp.api.data.request.chat.CreateRoomRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.chat.CreateRoomResponse;
import com.example.honbabapp.api.data.response.meeting.ApplicantsResponse;
import com.example.honbabapp.api.data.response.meeting.ApproveResponse;
import com.example.honbabapp.activity.chat.ChatRoomActivity;
import com.example.honbabapp.databinding.PageCampaignApplicantListBinding;
import com.example.honbabapp.ui.adapter.CampaignApplicantAdapter;
import com.example.honbabapp.ui.dialog.CampaignApplicantDetailBottomSheet;


import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 체험단 신청자 목록 Activity (호스트 전용)
 *
 * MeetingApplicantListActivity 패턴 기반 + 체험단 전용 확장:
 * - 필터 탭: 대기 / 승인
 * - 신청자 카드에 SNS 뱃지 표시 (CampaignApplicantAdapter)
 * - 채팅하기: 1:1 채팅방 생성 후 이동 (그룹채팅 생성 안 함 — approve.php 스킵)
 * - 상세보기: CampaignApplicantDetailBottomSheet
 */
public class CampaignApplicantListActivity extends AppCompatActivity {

    private static final String TAG = "CampaignApplicantList";

    public static final String EXTRA_CAMPAIGN_ID    = "campaign_id";
    public static final String EXTRA_CAMPAIGN_TITLE = "campaign_title";

    private PageCampaignApplicantListBinding binding;
    private ApiService apiService;
    private CampaignApplicantAdapter campaignApplicantAdapter;

    private int campaignId;
    private int hostId;

    // 현재 선택된 탭 — "대기" | "승인"
    private String currentStatusFilter = "대기";

    // ─────────────────────────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageCampaignApplicantListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        campaignId = getIntent().getIntExtra(EXTRA_CAMPAIGN_ID, -1);
        if (campaignId == -1) {
            Toast.makeText(this, "잘못된 접근입니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String campaignTitle = getIntent().getStringExtra(EXTRA_CAMPAIGN_TITLE);

        SharedPreferences prefs = getSharedPreferences("honbabapp_prefs", MODE_PRIVATE);
        hostId = prefs.getInt("user_id", -1);

        binding.btnBack.setOnClickListener(v -> finish());

        if (campaignTitle != null) {
            binding.tvTitle.setText(campaignTitle);
        }

        setupFilterTabs();
        setupRecyclerView();
        loadApplicants("대기");
    }

    // ─────────────────────────────────────────────────────────────
    // 필터 탭 (대기 / 승인)
    // ─────────────────────────────────────────────────────────────

    private void setupFilterTabs() {
        // 초기 상태: 대기 탭 활성
        updateTabStyle("대기");

        binding.btnFilterWaiting.setOnClickListener(v -> {
            if (!"대기".equals(currentStatusFilter)) {
                currentStatusFilter = "대기";
                updateTabStyle("대기");
                loadApplicants("대기");
            }
        });

        binding.btnFilterApproved.setOnClickListener(v -> {
            if (!"승인".equals(currentStatusFilter)) {
                currentStatusFilter = "승인";
                updateTabStyle("승인");
                loadApplicants("승인");
            }
        });
    }

    /**
     * 필터 버튼 활성/비활성 스타일
     * XML에 인디케이터 뷰가 없으므로 텍스트 색상으로만 구분
     */
    private void updateTabStyle(String activeStatus) {
        boolean isWaiting = "대기".equals(activeStatus);

        binding.btnFilterWaiting.setTextColor(getColor(
                isWaiting ? com.example.honbabapp.R.color.primary_100
                        : com.example.honbabapp.R.color.text_200
        ));
        binding.btnFilterApproved.setTextColor(getColor(
                isWaiting ? com.example.honbabapp.R.color.text_200
                        : com.example.honbabapp.R.color.primary_100
        ));
    }

    // ─────────────────────────────────────────────────────────────
    // RecyclerView — CampaignApplicantAdapter (4개 콜백)
    // ─────────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        campaignApplicantAdapter = new CampaignApplicantAdapter(
                this::onApproveClick,
                this::onRejectClick,
                this::onChatClick,
                this::onDetailClick
        );
        binding.rvCampaignApplicants.setAdapter(campaignApplicantAdapter);
    }

    // ─────────────────────────────────────────────────────────────
    // 신청자 목록 API 호출 — MeetingApplicantListActivity 동일 패턴
    // ─────────────────────────────────────────────────────────────

    private void loadApplicants(String status) {
        apiService.getApplicants(campaignId, hostId, status)
                .enqueue(new Callback<ApiResponse<ApplicantsResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ApplicantsResponse>> call,
                                           Response<ApiResponse<ApplicantsResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            List<ApplicantsResponse.Applicant> applicants =
                                    response.body().getData().getApplicants();

                            Log.i(TAG, status + " 신청자 로드: " + applicants.size() + "명");
                            updateUI(applicants);

                        } else {
                            Log.e(TAG, "API 실패: " + response.code());
                            Toast.makeText(CampaignApplicantListActivity.this,
                                    "신청자 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ApplicantsResponse>> call,
                                          Throwable throwable) {
                        Log.e(TAG, "네트워크 오류: " + throwable.getMessage());
                        Toast.makeText(CampaignApplicantListActivity.this,
                                "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(
            List<ApplicantsResponse.Applicant> applicants) {
        if (applicants.isEmpty()) {
            binding.rvCampaignApplicants.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.tvApplicantCount.setText("0명");
        } else {
            binding.rvCampaignApplicants.setVisibility(View.VISIBLE);
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.tvApplicantCount.setText(applicants.size() + "명");
            campaignApplicantAdapter.updateApplicants(applicants);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 승인 버튼 클릭 — approveMeeting("approve")
    // ─────────────────────────────────────────────────────────────

    private void onApproveClick(ApplicantsResponse.Applicant applicant) {
        new AlertDialog.Builder(this)
                .setTitle("신청 승인")
                .setMessage(applicant.getUser_nickname() + "님의 체험단 신청을 승인하시겠습니까?")
                .setPositiveButton("승인", (dialog, which) -> approveApplicant(applicant, "approve"))
                .setNegativeButton("취소", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────────
    // 거절 버튼 클릭 — approveMeeting("reject")
    // ─────────────────────────────────────────────────────────────

    private void onRejectClick(ApplicantsResponse.Applicant applicant) {
        new AlertDialog.Builder(this)
                .setTitle("신청 거절")
                .setMessage(applicant.getUser_nickname() + "님의 신청을 거절하시겠습니까?")
                .setPositiveButton("거절", (dialog, which) -> approveApplicant(applicant, "reject"))
                .setNegativeButton("취소", null)
                .show();
    }

    private void approveApplicant(ApplicantsResponse.Applicant applicant, String action) {
        ApproveMeetingRequest request = new ApproveMeetingRequest(
                applicant.getParticipant_id(), action, hostId
        );

        apiService.approveMeeting(request)
                .enqueue(new Callback<ApiResponse<ApproveResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ApproveResponse>> call,
                                           Response<ApiResponse<ApproveResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            String resultMessage = "approve".equals(action)
                                    ? applicant.getUser_nickname() + "님을 승인했습니다."
                                    : applicant.getUser_nickname() + "님을 거절했습니다.";

                            Toast.makeText(CampaignApplicantListActivity.this,
                                    resultMessage, Toast.LENGTH_SHORT).show();

                            // 목록 새로고침
                            loadApplicants(currentStatusFilter);
                            setResult(RESULT_OK);

                        } else {
                            String message = response.body() != null
                                    ? response.body().getMessage() : "처리에 실패했습니다.";
                            Toast.makeText(CampaignApplicantListActivity.this,
                                    message, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ApproveResponse>> call,
                                          Throwable throwable) {
                        Log.e(TAG, "승인/거절 오류: " + throwable.getMessage());
                        Toast.makeText(CampaignApplicantListActivity.this,
                                "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────
    // 채팅하기 — 1:1 채팅방 생성 후 ChatRoomActivity 이동
    // 체험단은 그룹채팅 없이 1:1만 (approve.php 자동생성 스킵)
    // ─────────────────────────────────────────────────────────────

    private void onChatClick(ApplicantsResponse.Applicant applicant) {
        // 1:1 생성자: (owner_id, targetUserId, room_name)
        CreateRoomRequest request = new CreateRoomRequest(
                hostId,
                applicant.getUser_id(),
                applicant.getUser_nickname()
        );

        apiService.createRoom(request)
                .enqueue(new Callback<ApiResponse<CreateRoomResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<CreateRoomResponse>> call,
                                           Response<ApiResponse<CreateRoomResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            CreateRoomResponse data = response.body().getData();
                            Log.i(TAG, "채팅방 생성/조회 완료: room_id=" + data.getRoom_id());

                            Intent intent = new Intent(
                                    CampaignApplicantListActivity.this, ChatRoomActivity.class
                            );
                            intent.putExtra("room_id", data.getRoom_id());
                            intent.putExtra("room_name", applicant.getUser_nickname());
                            intent.putExtra("room_type", "direct");
                            intent.putExtra("room_image", applicant.getUser_profile_image());
                            startActivity(intent);

                        } else {
                            String message = response.body() != null
                                    ? response.body().getMessage() : "채팅방 생성에 실패했습니다.";
                            Toast.makeText(CampaignApplicantListActivity.this,
                                    message, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<CreateRoomResponse>> call,
                                          Throwable throwable) {
                        Log.e(TAG, "채팅방 생성 오류: " + throwable.getMessage());
                        Toast.makeText(CampaignApplicantListActivity.this,
                                "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────
    // 상세보기 — CampaignApplicantDetailBottomSheet (조회 전용)
    // ─────────────────────────────────────────────────────────────

    private void onDetailClick(ApplicantsResponse.Applicant applicant) {
        CampaignApplicantDetailBottomSheet bottomSheet =
                new CampaignApplicantDetailBottomSheet(applicant);
        bottomSheet.show(getSupportFragmentManager(), "campaignApplicantDetail");
    }

    // ─────────────────────────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}