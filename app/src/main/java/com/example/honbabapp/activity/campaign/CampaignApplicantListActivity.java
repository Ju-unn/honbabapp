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

    private String currentStatusFilter = "대기";

    // ─────────────────────────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageCampaignApplicantListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setResult(RESULT_OK);

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

    private void updateTabStyle(String activeStatus) {
        binding.btnFilterWaiting.setSelected("대기".equals(activeStatus));
        binding.btnFilterApproved.setSelected("승인".equals(activeStatus));
    }

    // ─────────────────────────────────────────────────────────────
    // RecyclerView
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
    // 신청자 목록 API 호출
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
                            updateApplicantList(applicants);

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

    private void updateApplicantList(List<ApplicantsResponse.Applicant> applicants) {
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
    // 승인 / 거절
    // ─────────────────────────────────────────────────────────────

    private void onApproveClick(ApplicantsResponse.Applicant applicant) {
        new AlertDialog.Builder(this)
                .setTitle("신청 승인")
                .setMessage(applicant.getUser_nickname() + "님의 체험단 신청을 승인하시겠습니까?")
                .setPositiveButton("승인", (dialog, which) -> approveApplicant(applicant, "approve"))
                .setNegativeButton("취소", null)
                .show();
    }

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
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            String message = "approve".equals(action) ? "승인되었습니다." : "거절되었습니다.";
                            Toast.makeText(CampaignApplicantListActivity.this,
                                    message, Toast.LENGTH_SHORT).show();
                            loadApplicants(currentStatusFilter);
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
    // 채팅하기
    // ─────────────────────────────────────────────────────────────

    private void onChatClick(ApplicantsResponse.Applicant applicant) {
        CreateRoomRequest request = new CreateRoomRequest(
                hostId, applicant.getUser_id(), applicant.getUser_nickname()
        );

        apiService.createRoom(request)
                .enqueue(new Callback<ApiResponse<CreateRoomResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<CreateRoomResponse>> call,
                                           Response<ApiResponse<CreateRoomResponse>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {

                            CreateRoomResponse data = response.body().getData();
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
    // 상세보기
    // ─────────────────────────────────────────────────────────────

    private void onDetailClick(ApplicantsResponse.Applicant applicant) {
        new CampaignApplicantDetailBottomSheet(applicant)
                .show(getSupportFragmentManager(), "applicant_detail");
    }
}