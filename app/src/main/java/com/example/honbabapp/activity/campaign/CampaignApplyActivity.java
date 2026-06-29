package com.example.honbabapp.activity.campaign;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.activity.profile.ProfileProfileSettingActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.request.meeting.JoinMeetingRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.ParticipantResponse;
import com.example.honbabapp.databinding.PageCampaignApplyBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 체험단 신청 폼 Activity
 *
 * CampaignDetailActivity → (SNS 체크 통과) → 이 화면
 *
 * 흐름:
 * 1. Intent에서 campaignId, instagramId, blogId 수신
 * 2. tv_instagram_id / tv_blog_id prefill (읽기 전용 표시)
 * 3. tv_instagram_edit_hint / tv_blog_edit_hint 클릭 → ProfileProfileSettingActivity(focus_sns)
 * 4. et_applicant_intro 입력 → tv_intro_char_count 카운터
 * 5. btn_submit_apply → joinMeeting(applicant_intro) → RESULT_OK → 상세 새로고침
 */
public class CampaignApplyActivity extends AppCompatActivity {

    private static final String TAG = "CampaignApplyActivity";
    private static final int MAX_INTRO_LENGTH = 500;

    // Intent Extra 키 (CampaignDetailActivity에서 전달)
    public static final String EXTRA_CAMPAIGN_ID    = "campaign_id";
    public static final String EXTRA_CAMPAIGN_TITLE = "campaign_title";
    public static final String EXTRA_INSTAGRAM_ID   = "instagram_id";
    public static final String EXTRA_BLOG_ID        = "blog_id";

    private PageCampaignApplyBinding binding;
    private ApiService apiService;

    private int campaignId;
    private int currentUserId;

    // ─────────────────────────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageCampaignApplyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        SharedPreferences prefs = getSharedPreferences("honbabapp_prefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);

        campaignId = getIntent().getIntExtra(EXTRA_CAMPAIGN_ID, -1);
        if (campaignId == -1) {
            Toast.makeText(this, "잘못된 접근입니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String instagramId = getIntent().getStringExtra(EXTRA_INSTAGRAM_ID);
        String blogId      = getIntent().getStringExtra(EXTRA_BLOG_ID);

        setupUI(instagramId, blogId);
    }

    // ─────────────────────────────────────────────────────────────
    // UI 초기화
    // ─────────────────────────────────────────────────────────────

    private void setupUI(String instagramId, String blogId) {
        binding.btnBack.setOnClickListener(v -> finish());

        // SNS ID prefill (읽기 전용 TextView에 표시)
        bindSnsIdDisplay(instagramId, blogId);

        // "프로필에서 수정" 힌트 클릭 → ProfileProfileSettingActivity(focus_sns)
        binding.tvInstagramEditHint.setOnClickListener(v -> navigateToSnsProfileSetting());
        binding.tvBlogEditHint.setOnClickListener(v -> navigateToSnsProfileSetting());

        // 자기소개 글자수 카운터
        setupIntroCharCounter();

        // 신청하기 버튼
        binding.btnSubmitApply.setOnClickListener(v -> submitApply());
    }

    // ─────────────────────────────────────────────────────────────
    // SNS ID prefill — tv_instagram_id / tv_blog_id
    // ─────────────────────────────────────────────────────────────

    private void bindSnsIdDisplay(String instagramId, String blogId) {
        if (instagramId != null && !instagramId.isEmpty()) {
            binding.tvInstagramId.setText("@" + instagramId);
        }
        // 없으면 XML 기본 힌트 텍스트 ("인스타그램 ID를 등록해주세요") 유지

        if (blogId != null && !blogId.isEmpty()) {
            binding.tvBlogId.setText(blogId);
        }
        // 없으면 XML 기본 힌트 텍스트 ("블로그 ID를 등록해주세요") 유지
    }

    private void navigateToSnsProfileSetting() {
        Intent intent = new Intent(this, ProfileProfileSettingActivity.class);
        intent.putExtra("focus_sns", true);
        startActivity(intent);
    }

    // ─────────────────────────────────────────────────────────────
    // 자기소개 글자수 카운터 — tv_intro_char_count
    // ─────────────────────────────────────────────────────────────

    private void setupIntroCharCounter() {
        // XML 기본값 "0 / 500" 유지, 입력 시 업데이트
        binding.etApplicantIntro.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                binding.tvIntroCharCount.setText(charSequence.length() + " / " + MAX_INTRO_LENGTH);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 신청 API 호출 — btn_submit_apply
    // JoinMeetingRequest(campaignId, userId, applicantIntro) 사용
    // ─────────────────────────────────────────────────────────────

    private void submitApply() {
        String applicantIntro = binding.etApplicantIntro.getText().toString().trim();

        if (applicantIntro.isEmpty()) {
            Toast.makeText(this, "자기소개를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (applicantIntro.length() < 10) {
            Toast.makeText(this, "자기소개를 10자 이상 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSubmitApply.setEnabled(false);
        binding.btnSubmitApply.setText("신청 중...");

        JoinMeetingRequest request = new JoinMeetingRequest(campaignId, currentUserId, applicantIntro);

        apiService.joinMeeting(request)
                .enqueue(new Callback<ApiResponse<ParticipantResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ParticipantResponse>> call,
                                           Response<ApiResponse<ParticipantResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            Log.i(TAG, "체험단 신청 성공: campaignId=" + campaignId);
                            Toast.makeText(CampaignApplyActivity.this,
                                    "체험단 신청이 완료되었습니다!\n호스트의 승인을 기다려주세요.",
                                    Toast.LENGTH_LONG).show();

                            // CampaignDetailActivity.applyLauncher → RESULT_OK → 상세 새로고침
                            setResult(RESULT_OK);
                            finish();

                        } else {
                            String message = response.body() != null
                                    ? response.body().getMessage() : "신청에 실패했습니다.";
                            Log.e(TAG, "신청 실패: " + message);
                            Toast.makeText(CampaignApplyActivity.this,
                                    message, Toast.LENGTH_SHORT).show();
                            resetSubmitButton();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ParticipantResponse>> call,
                                          Throwable throwable) {
                        Log.e(TAG, "네트워크 오류: " + throwable.getMessage());
                        Toast.makeText(CampaignApplyActivity.this,
                                "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        resetSubmitButton();
                    }
                });
    }

    private void resetSubmitButton() {
        binding.btnSubmitApply.setEnabled(true);
        binding.btnSubmitApply.setText("신청하기");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}