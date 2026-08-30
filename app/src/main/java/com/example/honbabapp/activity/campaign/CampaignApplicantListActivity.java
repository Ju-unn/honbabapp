package com.example.honbabapp.activity.campaign;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.request.meeting.ApproveMeetingRequest;
import com.example.honbabapp.api.data.request.chat.CreateRoomRequest;
import com.example.honbabapp.api.data.request.review.ReviewSubmitRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.chat.CreateRoomResponse;
import com.example.honbabapp.api.data.response.meeting.ApplicantsResponse;
import com.example.honbabapp.api.data.response.meeting.ApproveResponse;
import com.example.honbabapp.api.data.review.ReviewSubmitData;
import com.example.honbabapp.activity.chat.ChatRoomActivity;
import com.example.honbabapp.databinding.DialogReviewBinding;
import com.example.honbabapp.databinding.PageCampaignApplicantListBinding;
import com.example.honbabapp.util.InsetsUtils;
import com.example.honbabapp.ui.adapter.CampaignApplicantAdapter;
import com.example.honbabapp.ui.dialog.CampaignApplicantDetailBottomSheet;
import com.example.honbabapp.util.SnackbarUtil;
import com.example.honbabapp.util.ToastUtil;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 체험단 신청자 목록 Activity (호스트 전용)
 */
public class CampaignApplicantListActivity extends AppCompatActivity {

    private static final String TAG = "CampaignApplicantList";

    public static final String EXTRA_CAMPAIGN_ID = "campaign_id";
    public static final String EXTRA_CAMPAIGN_TITLE = "campaign_title";

    private PageCampaignApplicantListBinding binding;
    private ApiService apiService;
    private CampaignApplicantAdapter campaignApplicantAdapter;

    private int campaignId;
    private int hostId;

    private String currentStatusFilter = "대기";
    private boolean isProcessing = false; // 승인/거절 인플라이트 가드(연타 중복요청 방지)

    // ─────────────────────────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageCampaignApplicantListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsets(binding.getRoot());

        setResult(RESULT_OK);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        campaignId = getIntent().getIntExtra(EXTRA_CAMPAIGN_ID, -1);
        if (campaignId == -1) {
            ToastUtil.makeText(this, "잘못된 접근입니다", Toast.LENGTH_SHORT).show();
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
    // 필터 탭 (대기 / 승인 / 취소)
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

        binding.btnFilterCanceled.setOnClickListener(v -> {
            if (!"취소".equals(currentStatusFilter)) {
                currentStatusFilter = "취소";
                updateTabStyle("취소");
                loadApplicants("취소");
            }
        });
    }

    private void updateTabStyle(String activeStatus) {
        binding.btnFilterWaiting.setSelected("대기".equals(activeStatus));
        binding.btnFilterApproved.setSelected("승인".equals(activeStatus));
        binding.btnFilterCanceled.setSelected("취소".equals(activeStatus));
    }

    // ─────────────────────────────────────────────────────────────
    // RecyclerView
    // ─────────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        campaignApplicantAdapter = new CampaignApplicantAdapter(
                this::onApproveClick,
                this::onRejectClick,
                this::onChatClick,
                this::onDetailClick,
                this::onReviewClick
        );
        binding.rvCampaignApplicants.setAdapter(campaignApplicantAdapter);
    }

    // ─────────────────────────────────────────────────────────────
    // 신청자 목록 API 호출
    // ─────────────────────────────────────────────────────────────

    private void loadApplicants(String status) {
        apiService.getApplicants(campaignId, status)
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
                            ToastUtil.makeText(CampaignApplicantListActivity.this,
                                    "신청자 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ApplicantsResponse>> call,
                                          Throwable throwable) {
                        Log.e(TAG, "네트워크 오류: " + throwable.getMessage());
                        SnackbarUtil.showRetry(binding.getRoot(),
                                "네트워크 오류가 발생했습니다.", () -> loadApplicants(status));
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
        if (isProcessing) return; // 이미 처리 중이면 무시(연타 방지)
        isProcessing = true;

        ApproveMeetingRequest request = new ApproveMeetingRequest(
                applicant.getParticipant_id(), action
        );

        apiService.approveMeeting(request)
                .enqueue(new Callback<ApiResponse<ApproveResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ApproveResponse>> call,
                                           Response<ApiResponse<ApproveResponse>> response) {
                        isProcessing = false;
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            String message = "approve".equals(action) ? "승인되었습니다." : "거절되었습니다.";
                            ToastUtil.makeText(CampaignApplicantListActivity.this,
                                    message, Toast.LENGTH_SHORT).show();
                            loadApplicants(currentStatusFilter);
                        } else {
                            String message = response.body() != null
                                    ? response.body().getMessage() : "처리에 실패했습니다.";
                            ToastUtil.makeText(CampaignApplicantListActivity.this,
                                    message, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ApproveResponse>> call,
                                          Throwable throwable) {
                        isProcessing = false;
                        Log.e(TAG, "승인/거절 오류: " + throwable.getMessage());
                        SnackbarUtil.showRetry(binding.getRoot(),
                                "네트워크 오류가 발생했습니다.", () -> approveApplicant(applicant, action));
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────
    // 채팅하기
    // ─────────────────────────────────────────────────────────────

    private void onChatClick(ApplicantsResponse.Applicant applicant) {
        CreateRoomRequest request = new CreateRoomRequest(
                applicant.getUser_id(), applicant.getUser_nickname()
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
                            ToastUtil.makeText(CampaignApplicantListActivity.this,
                                    message, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<CreateRoomResponse>> call,
                                          Throwable throwable) {
                        Log.e(TAG, "채팅방 생성 오류: " + throwable.getMessage());
                        SnackbarUtil.showRetry(binding.getRoot(),
                                "네트워크 오류가 발생했습니다.", () -> onChatClick(applicant));
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

    // ─────────────────────────────────────────────────────────────
    // 노쇼(승인 후 취소) 리뷰작성
    //
    // ReviewDialogHelper(채팅방용)는 QR 인증 대상자 중에서 고르는 선택형
    // 다이얼로그라 이 화면(대상이 이미 확정된 단건)에는 맞지 않는다.
    // 같은 dialog_review.xml 레이아웃을 재사용하되 대상 선택 목록은 숨기고
    // 바로 별점 입력 영역을 보여준다.
    // ─────────────────────────────────────────────────────────────

    private void onReviewClick(ApplicantsResponse.Applicant applicant) {
        DialogReviewBinding dialogBinding = DialogReviewBinding.inflate(LayoutInflater.from(this));

        dialogBinding.tvReviewDialogTitle.setText("노쇼 리뷰 작성");
        dialogBinding.tvReviewDialogDescription.setText(
                applicant.getUser_nickname() + "님에 대한 별점과 후기를 남겨주세요");
        dialogBinding.rvReviewTargets.setVisibility(View.GONE);
        dialogBinding.layoutReviewInput.setVisibility(View.VISIBLE);
        dialogBinding.tvSelectedNickname.setText(applicant.getUser_nickname());

        AlertDialog reviewDialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setCancelable(true)
                .create();

        dialogBinding.btnReviewSubmit.setOnClickListener(v -> {
            int rating = (int) dialogBinding.ratingBar.getRating();
            if (rating == 0) {
                ToastUtil.makeText(this, "별점을 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            dialogBinding.btnReviewSubmit.setEnabled(false);
            String content = dialogBinding.etReviewContent.getText().toString().trim();
            submitNoShowReview(applicant, rating, content, reviewDialog, dialogBinding.btnReviewSubmit);
        });

        dialogBinding.btnReviewDialogClose.setOnClickListener(v -> reviewDialog.dismiss());

        reviewDialog.show();
    }

    private void submitNoShowReview(ApplicantsResponse.Applicant applicant, int rating,
                                    String content, AlertDialog reviewDialog, Button submitButton) {
        ReviewSubmitRequest request = new ReviewSubmitRequest(
                campaignId, applicant.getUser_id(), rating, content
        );

        apiService.submitReview(request)
                .enqueue(new Callback<ApiResponse<ReviewSubmitData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ReviewSubmitData>> call,
                                           Response<ApiResponse<ReviewSubmitData>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            ToastUtil.makeText(CampaignApplicantListActivity.this,
                                    "리뷰가 등록되었습니다.", Toast.LENGTH_SHORT).show();
                            reviewDialog.dismiss();
                            loadApplicants(currentStatusFilter);
                        } else {
                            submitButton.setEnabled(true);
                            String message = response.body() != null
                                    ? response.body().getMessage() : "리뷰 등록에 실패했습니다.";
                            ToastUtil.makeText(CampaignApplicantListActivity.this,
                                    message, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ReviewSubmitData>> call, Throwable throwable) {
                        submitButton.setEnabled(true);
                        Log.e(TAG, "리뷰 등록 오류: " + throwable.getMessage());
                        ToastUtil.makeText(CampaignApplicantListActivity.this,
                                "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}