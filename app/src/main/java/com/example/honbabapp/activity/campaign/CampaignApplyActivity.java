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
import androidx.core.view.WindowCompat;

import com.example.honbabapp.activity.profile.ProfileProfileSettingActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.request.meeting.JoinMeetingRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.ParticipantResponse;
import com.example.honbabapp.api.data.response.user.UserProfileResponse;
import com.example.honbabapp.databinding.PageCampaignApplyBinding;
import com.example.honbabapp.util.InsetsUtils;
import com.example.honbabapp.util.ToastUtil;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 체험단 신청 폼 Activity
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
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsetsWithIme(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        SharedPreferences prefs = getSharedPreferences("honbabapp_prefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);

        campaignId = getIntent().getIntExtra(EXTRA_CAMPAIGN_ID, -1);
        if (campaignId == -1) {
            ToastUtil.makeText(this, "잘못된 접근입니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String instagramId = getIntent().getStringExtra(EXTRA_INSTAGRAM_ID);
        String blogId      = getIntent().getStringExtra(EXTRA_BLOG_ID);

        setupUI(instagramId, blogId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSnsIds();
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

    private void refreshSnsIds() {
        if (currentUserId == -1) return;

        apiService.getUserProfile(currentUserId).enqueue(new Callback<ApiResponse<UserProfileResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserProfileResponse>> call,
                                   Response<ApiResponse<UserProfileResponse>> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {

                    UserProfileResponse profile = response.body().getData();
                    bindSnsIdDisplay(profile.getUser_instagram_id(), profile.getUser_blog_id());
                    Log.i(TAG, "SNS ID 갱신 완료");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserProfileResponse>> call, Throwable throwable) {
                // 재조회 실패는 조용히 무시 — 기존 표시값 유지
                Log.w(TAG, "SNS ID 재조회 실패 (무시): " + throwable.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // SNS ID prefill — tv_instagram_id / tv_blog_id
    // ─────────────────────────────────────────────────────────────

    private void bindSnsIdDisplay(String instagramId, String blogId) {
        if (instagramId != null && !instagramId.isEmpty()) {
            binding.tvInstagramId.setText("@" + instagramId);
        } else {
            binding.tvInstagramId.setText("");
        }

        if (blogId != null && !blogId.isEmpty()) {
            binding.tvBlogId.setText(blogId);
        } else {
            binding.tvBlogId.setText("");
        }
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
        binding.etApplicantIntro.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence sequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence sequence, int start, int before, int count) {
                int currentLength = sequence.length();
                binding.tvIntroCharCount.setText(currentLength + " / " + MAX_INTRO_LENGTH);
            }

            @Override public void afterTextChanged(Editable editable) {}
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 신청하기
    // ─────────────────────────────────────────────────────────────

    private void submitApply() {
        String intro = binding.etApplicantIntro.getText().toString().trim();
        if (intro.isEmpty()) {
            ToastUtil.makeText(this, "자기소개를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSubmitApply.setEnabled(false);
        binding.btnSubmitApply.setText("신청 중...");

        JoinMeetingRequest request = new JoinMeetingRequest(campaignId, intro);

        apiService.joinMeeting(request).enqueue(new Callback<ApiResponse<ParticipantResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ParticipantResponse>> call,
                                   Response<ApiResponse<ParticipantResponse>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    ToastUtil.makeText(CampaignApplyActivity.this,
                            "체험단 신청이 완료되었습니다!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    String message = response.body() != null
                            ? response.body().getMessage() : "신청에 실패했습니다.";
                    ToastUtil.makeText(CampaignApplyActivity.this,
                            message, Toast.LENGTH_SHORT).show();
                    binding.btnSubmitApply.setEnabled(true);
                    binding.btnSubmitApply.setText("신청하기");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ParticipantResponse>> call, Throwable throwable) {
                Log.e(TAG, "신청 네트워크 오류: " + throwable.getMessage());
                ToastUtil.makeText(CampaignApplyActivity.this,
                        "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                binding.btnSubmitApply.setEnabled(true);
                binding.btnSubmitApply.setText("신청하기");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}