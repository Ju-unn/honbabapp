package com.example.honbabapp.activity.meeting;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.request.ApproveMeetingRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.ApplicantsResponse;
import com.example.honbabapp.api.data.response.meeting.ApproveResponse;
import com.example.honbabapp.databinding.PageMeetingApplicantListBinding;
import com.example.honbabapp.ui.adapter.ApplicantListAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 신청자 목록 화면 (호스트 전용)
 *
 * 기능:
 * 1. 모임 신청자 목록 표시
 * 2. 승인/거절 기능
 */
public class MeetingApplicantListActivity extends AppCompatActivity {

    private String TAG = "ApplicantListActivity";
    private PageMeetingApplicantListBinding binding;
    private ApiService apiService;
    private ApplicantListAdapter adapter;

    private int meetingId;
    private int hostId;
    private String meetingTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageMeetingApplicantListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // API 서비스 초기화
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // Intent에서 데이터 가져오기
        meetingId = getIntent().getIntExtra("meeting_id" , -1);
        meetingTitle = getIntent().getStringExtra("meeting_title");

        if (meetingId == -1) {
            Toast.makeText(this, "잘못된 접근입니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 현재 사용자 ID (호스트)
        SharedPreferences prefs = getSharedPreferences("honbabapp_prefs" , MODE_PRIVATE);
        hostId = prefs.getInt("user_id", -1);

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener(v -> finish());

        // recyclerView
        setupRecyclerView();

        // 신청자 목록 로드
        loadAppLicants();

    }


    /**
     * RecyclerView 설정
     */
    private void setupRecyclerView() {
        adapter = new ApplicantListAdapter(new ArrayList<>() , this::onApproveClick , this::onRejctClick);
        binding.rvApplicants.setAdapter(adapter);
    }

    /**
     * 신청자 목록 API 호출
     */
    private void loadAppLicants() {
        Call<ApiResponse<ApplicantsResponse>> call = apiService.getApplicants(
                meetingId ,
                hostId ,
                "대기"
        );

        call.enqueue(new Callback<ApiResponse<ApplicantsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApplicantsResponse>> call, Response<ApiResponse<ApplicantsResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<ApplicantsResponse> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        ApplicantsResponse data = apiResponse.getData();
                        List<ApplicantsResponse.Applicant> applicants = data.getApplicants();

                        Log.i(TAG, "신청자 목록 로드 성공: " + applicants.size() + "명");

                        // UI 업데이트
                        updateUI(applicants);
                    } else {
                        Log.e(TAG, "API 실패: " + apiResponse.getMessage());
                        Toast.makeText(MeetingApplicantListActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "HTTP 오류: " + response.code());
                    Toast.makeText(MeetingApplicantListActivity.this,
                            "신청자 목록을 불러오는데 실패했습니다.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ApplicantsResponse>> call, Throwable t) {
                Log.e(TAG, "네트워크 오류: " + t.getMessage());
                Toast.makeText(MeetingApplicantListActivity.this,
                        "네트워크 오류가 발생했습니다.",
                        Toast.LENGTH_SHORT).show();
            }
        });

    }

    /**
     * UI 업데이트
     */
    private void updateUI(List<ApplicantsResponse.Applicant> applicants) {
        if (applicants.isEmpty()) {
            // 빈 화면 표시
            binding.rvApplicants.setVisibility(View.GONE);
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.tvApplicantCount.setText(" 0명");
        } else {
            // 리스트 표시
            binding.rvApplicants.setVisibility(View.VISIBLE);
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.tvApplicantCount.setText("" + applicants.size() + "명");

            // Adapter 업데이트
            adapter.updateApplicants(applicants);
        }
    }

    /**
     * 승인 버튼 클릭
     */
    private void onApproveClick(ApplicantsResponse.Applicant applicant) {
        new AlertDialog.Builder(this)
                .setTitle("침여 승인")
                .setMessage(applicant.getUser_nickname() + "님의 참여를 승인하시겠습니까?")
                .setPositiveButton("승인" , (dialog , which) -> {
                    approveApplicant(applicant , "apporve");
                })
                .setNegativeButton("취소" , null)
                .show();
    }

    /**
     * 거절 버튼 클릭
     */
    private void onRejctClick(ApplicantsResponse.Applicant applicant) {
        new AlertDialog.Builder(this)
                .setTitle("참여 거절")
                .setMessage(applicant.getUser_nickname() + "님의 참여를 거절하시겠습니까?1")
                .setPositiveButton("거절" , (dialog , which) -> {
                    approveApplicant(applicant , "reject");
                })
                .setNegativeButton("취소" , null)
                .show();
    }

    /**
     * 승인/거절 API 호출
     */
    private void approveApplicant(ApplicantsResponse.Applicant applicant , String action) {
        ApproveMeetingRequest request = new ApproveMeetingRequest(
                applicant.getParticipant_id() ,
                action ,
                hostId
        );

        Call<ApiResponse<ApproveResponse>> call = apiService.approveMeeting(request);

        call.enqueue(new Callback<ApiResponse<ApproveResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApproveResponse>> call, Response<ApiResponse<ApproveResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<ApproveResponse> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        Log.i(TAG, action + " 성공");

                        // 성공 메시지
                        String message = action.equals("approve") ?
                                "참여를 승인했습니다" : "참여를 거절했습니다";
                        Toast.makeText(MeetingApplicantListActivity.this,
                                message,
                                Toast.LENGTH_SHORT).show();

                        // 신청자 목록 다시 로드
                        loadAppLicants();
                    } else {
                        Log.e(TAG, action + " 실패: " + apiResponse.getMessage());
                        Toast.makeText(MeetingApplicantListActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "HTTP 오류: " + response.code());
                    Toast.makeText(MeetingApplicantListActivity.this,
                            action.equals("approve") ?
                                    "승인에 실패했습니다" : "거절에 실패했습니다",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ApproveResponse>> call, Throwable t) {
                Log.e(TAG, "네트워크 오류: " + t.getMessage());
                Toast.makeText(MeetingApplicantListActivity.this,
                        "네트워크 오류가 발생했습니다.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

}
