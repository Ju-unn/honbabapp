package com.example.honbabapp.ui.dialog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.ApplicantsResponse;
import com.example.honbabapp.api.data.review.ReceivedReviewsData;
import com.example.honbabapp.databinding.BottomsheetCampaignApplicantDetailBinding;
import com.example.honbabapp.ui.adapter.ReceivedReviewAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 체험단 신청자 상세보기 바텀시트 (조회 전용)
 *
 * 1단계에서 ApplicantsResponse.Applicant에 추가된 필드:
 * - user_instagram_id, user_blog_id
 * - applicant_intro
 */
public class CampaignApplicantDetailBottomSheet extends BottomSheetDialogFragment {

    // ─────────────────────────────────────────
    // 외부 주입
    // ─────────────────────────────────────────

    public ApplicantsResponse.Applicant applicant;

    // ─────────────────────────────────────────
    // 생성자 (CampaignApplicantListActivity에서 사용하는 방식)
    // ─────────────────────────────────────────

    public CampaignApplicantDetailBottomSheet(ApplicantsResponse.Applicant applicant) {
        this.applicant = applicant;
    }

    public CampaignApplicantDetailBottomSheet() {}

    // ─────────────────────────────────────────
    // 내부 상태
    // ─────────────────────────────────────────

    private BottomsheetCampaignApplicantDetailBinding binding;

    private static final int REVIEW_PAGE_SIZE = 5;
    private ApiService apiService;
    private ReceivedReviewAdapter reviewAdapter;
    private int reviewPage = 1;
    private boolean reviewHasMore = false;
    private boolean isLoadingReviews = false;

    // ─────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomsheetCampaignApplicantDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (applicant == null) {
            dismiss();
            return;
        }

        apiService = RetrofitClient.getClient().create(ApiService.class);

        bindApplicantData();
        setupCloseButton();
        setupReviewsList();
        loadReviews(true);
    }

    // ─────────────────────────────────────────
    // 데이터 바인딩
    // ─────────────────────────────────────────

    private void bindApplicantData() {
        bindProfileSection();
        bindSnsSection();
        bindIntroSection();
    }

    private void bindProfileSection() {
        binding.tvSheetNickname.setText(applicant.getUser_nickname() + "님의 신청서");
        binding.tvSheetNicknameDetail.setText(applicant.getUser_nickname());

        binding.tvSheetManner.setText(
                "매너점수 " + String.format("%.1f°C", applicant.getUser_manner_score())
        );

        binding.tvSheetApplyTime.setText(formatApplyTime(applicant.getApplied_at()));

        Glide.with(requireContext())
                .load(applicant.getUser_profile_image())
                .placeholder(R.drawable.ic_profile)
                .circleCrop()
                .into(binding.ivSheetAvatar);
    }

    // ── SNS 계정 — 1단계 ApplicantsResponse.Applicant 필드 연동 ──
    private void bindSnsSection() {
        String instagramId = applicant.getUser_instagram_id();
        binding.tvSheetInstagramId.setText(
                (instagramId != null && !instagramId.isEmpty())
                        ? "@" + instagramId : "—"
        );
        binding.layoutSheetInstagram.setOnClickListener(v ->
                openSnsUrl(instagramId, "https://www.instagram.com/"));

        String blogId = applicant.getUser_blog_id();
        binding.tvSheetBlogId.setText(
                (blogId != null && !blogId.isEmpty()) ? blogId : "—"
        );
        binding.layoutSheetBlog.setOnClickListener(v ->
                openSnsUrl(blogId, "https://blog.naver.com/"));

        String youtubeId = applicant.getUser_youtube_id();
        binding.tvSheetYoutubeId.setText(
                (youtubeId != null && !youtubeId.isEmpty()) ? youtubeId : "—"
        );
        binding.layoutSheetYoutube.setOnClickListener(v ->
                openSnsUrl(youtubeId, "https://www.youtube.com/@"));
    }

    private void openSnsUrl(String id, String urlPrefix) {
        if (id == null || id.isEmpty()) return;
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlPrefix + id)));
    }

    // ── 자기소개 — 1단계 applicant_intro 필드 연동 ────────────────
    private void bindIntroSection() {
        String intro = applicant.getApplicant_intro();
        binding.tvSheetIntro.setText(
                (intro != null && !intro.isEmpty()) ? intro : "자기소개 없음"
        );
    }

    // ─────────────────────────────────────────
    // 닫기 버튼
    // ─────────────────────────────────────────

    private void setupCloseButton() {
        binding.btnClose.setOnClickListener(v -> dismiss());
    }

    // ─────────────────────────────────────────
    // 받은 리뷰 — 5개씩 페이징
    // ─────────────────────────────────────────

    private void setupReviewsList() {
        reviewAdapter = new ReceivedReviewAdapter();
        binding.rvSheetReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSheetReviews.setAdapter(reviewAdapter);
        binding.rvSheetReviews.setNestedScrollingEnabled(false);
        binding.btnSheetLoadMoreReviews.setOnClickListener(v -> loadReviews(false));
    }

    private void loadReviews(boolean isFirst) {
        if (isLoadingReviews) return;
        isLoadingReviews = true;

        int page = isFirst ? 1 : reviewPage + 1;

        apiService.getReceivedReviewsPaged(applicant.getUser_id(), page, REVIEW_PAGE_SIZE)
                .enqueue(new Callback<ApiResponse<ReceivedReviewsData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ReceivedReviewsData>> call,
                                           Response<ApiResponse<ReceivedReviewsData>> response) {
                        isLoadingReviews = false;

                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess() && response.body().getData() != null) {
                            ReceivedReviewsData data = response.body().getData();

                            if (isFirst) {
                                reviewAdapter.submitReviewList(data.getReviews());
                            } else {
                                reviewAdapter.addReviewList(data.getReviews());
                            }
                            reviewPage = data.getPage();
                            reviewHasMore = data.isHas_more();

                            boolean isEmpty = isFirst
                                    && (data.getReviews() == null || data.getReviews().isEmpty());
                            binding.tvSheetNoReviews.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                            binding.rvSheetReviews.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                            binding.btnSheetLoadMoreReviews.setVisibility(
                                    reviewHasMore ? View.VISIBLE : View.GONE);
                        } else if (isFirst) {
                            binding.tvSheetNoReviews.setVisibility(View.VISIBLE);
                            binding.rvSheetReviews.setVisibility(View.GONE);
                            binding.btnSheetLoadMoreReviews.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ReceivedReviewsData>> call, Throwable throwable) {
                        isLoadingReviews = false;
                        if (isFirst) {
                            binding.tvSheetNoReviews.setVisibility(View.VISIBLE);
                            binding.rvSheetReviews.setVisibility(View.GONE);
                            binding.btnSheetLoadMoreReviews.setVisibility(View.GONE);
                        }
                    }
                });
    }

    // ─────────────────────────────────────────
    // 유틸 — 신청 시간 포맷
    // ─────────────────────────────────────────

    private String formatApplyTime(String appliedAt) {
        if (appliedAt == null || appliedAt.isEmpty()) return "";
        try {
            SimpleDateFormat dateFormat =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
            // 서버는 항상 KST(Asia/Seoul) 문자열을 내려주므로, 기기 타임존이 달라도
            // 항상 KST로 해석하도록 명시한다 (안 하면 기기 기본 타임존으로 파싱되어 시간이 어긋남)
            dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            Date applyDate = dateFormat.parse(appliedAt);
            if (applyDate == null) return appliedAt;
            long diffMilliseconds = System.currentTimeMillis() - applyDate.getTime();
            long diffMinutes      = diffMilliseconds / 60000;
            if (diffMinutes < 60)  return diffMinutes + "분 전";
            long diffHours = diffMinutes / 60;
            if (diffHours < 24)    return diffHours + "시간 전";
            return (diffHours / 24) + "일 전";
        } catch (Exception exception) {
            return appliedAt;
        }
    }

    // ─────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}