package com.example.honbabapp.activity.chat;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.user.UserProfileResponse;
import com.example.honbabapp.api.data.review.ReceivedReview;
import com.example.honbabapp.api.data.review.ReceivedReviewsData;
import com.example.honbabapp.databinding.PageChatParticipantDetailBinding;
import com.example.honbabapp.ui.adapter.ReceivedReviewAdapter;
import com.example.honbabapp.util.InsetsUtils;
import com.example.honbabapp.util.ToastUtil;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 채팅방 참가자 프로필 상세 (view-only)
 *
 * 진입 경로:
 *   - ChatRoomDetailActivity 의 참가자 행 클릭
 *   - 1:1 방에서 헤더 카드 클릭
 *
 * 표시:
 *   - 아바타 / 닉네임 / 상태메시지 / 매너점수 / 참여횟수
 *   - 받은 리뷰 목록 (익명, RecyclerView 인라인 표시)
 *
 * Intent extra:
 *   - user_id : int
 */
public class ChatRoomParticipantDetailActivity extends AppCompatActivity {

    private static final String TAG = "ChatParticipantDetail";

    private PageChatParticipantDetailBinding binding;
    private ApiService apiService;
    private ReceivedReviewAdapter receivedReviewAdapter;

    private int userId = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageChatParticipantDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsets(binding.getRoot());

        // ── Intent extra ──
        userId = getIntent().getIntExtra("user_id", -1);
        if (userId == -1) {
            ToastUtil.makeText(this, "잘못된 접근입니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ── API 초기화 ──
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // ── 뒤로가기 ──
        binding.btnBack.setOnClickListener(view -> finish());

        // ── 받은 리뷰 RecyclerView 초기화 ──
        setupReceivedReviewsList();

        // ── API 호출 (순서: 프로필 먼저, 이어서 받은 리뷰) ──
        loadUserProfile();
        loadReceivedReviews();
    }

    // ─────────────────────────────────────────────
    // RecyclerView 초기 세팅
    // ─────────────────────────────────────────────

    private void setupReceivedReviewsList() {
        receivedReviewAdapter = new ReceivedReviewAdapter();
        binding.rvReceivedReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReceivedReviews.setAdapter(receivedReviewAdapter);
        // 부모 ScrollView 가 스크롤을 처리하므로 중첩 스크롤 비활성화
        binding.rvReceivedReviews.setNestedScrollingEnabled(false);
    }

    // ─────────────────────────────────────────────
    // 프로필 로드
    // ─────────────────────────────────────────────

    private void loadUserProfile() {
        apiService.getUserProfile(userId)
                .enqueue(new Callback<ApiResponse<UserProfileResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<UserProfileResponse>> call,
                                           @NonNull Response<ApiResponse<UserProfileResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {
                            bindProfile(response.body().getData());
                        } else {
                            String message = (response.body() != null)
                                    ? response.body().getMessage()
                                    : "프로필을 불러올 수 없습니다";
                            ToastUtil.makeText(ChatRoomParticipantDetailActivity.this,
                                    message, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<UserProfileResponse>> call,
                                          @NonNull Throwable throwable) {
                        Log.e(TAG, "프로필 로드 실패: " + throwable.getMessage());
                        ToastUtil.makeText(ChatRoomParticipantDetailActivity.this,
                                "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─────────────────────────────────────────────
    // 받은 리뷰 목록 로드
    // ─────────────────────────────────────────────

    private void loadReceivedReviews() {
        apiService.getReceivedReviews(userId)
                .enqueue(new Callback<ApiResponse<ReceivedReviewsData>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ReceivedReviewsData>> call,
                                           @NonNull Response<ApiResponse<ReceivedReviewsData>> response) {
                        if (!response.isSuccessful()
                                || response.body() == null
                                || !response.body().isSuccess()) {
                            Log.e(TAG, "받은 리뷰 로드 실패: "
                                    + (response.body() != null ? response.body().getMessage() : "null"));
                            showEmptyReviewState();
                            return;
                        }

                        ReceivedReviewsData data = response.body().getData();
                        List<ReceivedReview> reviews =
                                (data != null) ? data.getReviews() : null;

                        if (reviews == null || reviews.isEmpty()) {
                            showEmptyReviewState();
                        } else {
                            showReviewList(reviews);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ReceivedReviewsData>> call,
                                          @NonNull Throwable throwable) {
                        Log.e(TAG, "받은 리뷰 네트워크 오류: " + throwable.getMessage());
                        // 리뷰 로드 실패는 프로필 자체에 영향을 주지 않으므로 Toast 없이 빈 상태 표시
                        showEmptyReviewState();
                    }
                });
    }

    // ─────────────────────────────────────────────
    // UI 상태 전환 헬퍼
    // ─────────────────────────────────────────────

    /** 리뷰가 존재할 때 — RecyclerView 표시, 빈 상태 안내 숨김 */
    private void showReviewList(List<ReceivedReview> reviews) {
        receivedReviewAdapter.submitReviewList(reviews);
        binding.rvReceivedReviews.setVisibility(View.VISIBLE);
        binding.tvNoReviews.setVisibility(View.GONE);
    }

    /** 리뷰가 없거나 로드 실패 — RecyclerView 숨김, 빈 상태 안내 표시 */
    private void showEmptyReviewState() {
        binding.rvReceivedReviews.setVisibility(View.GONE);
        binding.tvNoReviews.setVisibility(View.VISIBLE);
    }

    // ─────────────────────────────────────────────
    // 프로필 데이터 → UI 바인딩
    // (ProfileFragment.bindProfile() 과 동일 패턴)
    // ─────────────────────────────────────────────

    private void bindProfile(UserProfileResponse profile) {
        // 닉네임
        binding.tvProfileNickname.setText(profile.getUser_nickname());

        // 상태 메시지
        String statusMessage = profile.getUser_status_message();
        binding.tvProfileStatus.setText(
                (statusMessage != null && !statusMessage.isEmpty())
                        ? statusMessage
                        : "상태 메세지가 없습니다"
        );

        // 매너 점수
        binding.tvMannerScore.setText(String.valueOf(profile.getUser_manner_score()));

        // 참여 횟수
        binding.tvParticipationCount.setText(profile.getUser_participation_count() + "회");

        // 아바타
        Glide.with(this)
                .load(profile.getUser_profile_image())
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(binding.ivProfileAvatar);
    }
}