package com.example.honbabapp.activity.chat;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.data.request.review.ReviewSubmitRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.review.ReviewSubmitData;
import com.example.honbabapp.api.data.review.ReviewTarget;
import com.example.honbabapp.api.data.review.ReviewTargetsData;
import com.example.honbabapp.databinding.DialogReviewBinding;
import com.example.honbabapp.ui.adapter.ReviewTargetAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 모임 후기(매너점수) 작성 다이얼로그 헬퍼.
 *
 * MeetingQrHelper 와 동일한 구조:
 *   - 생성자에서 Activity / ApiService / meetingId / myUserId 주입
 *   - onReviewButtonClicked() — 채팅방의 "후기작성" 버튼 클릭 시 호출
 *     1) getReviewTargets API 호출
 *     2) is_eligible=false → 거부 토스트
 *     3) targets 비어있음 → "남길 수 있는 멤버 없음" 토스트
 *     4) 그 외 → DialogReviewBinding inflate 후 AlertDialog 표시
 */
public class ReviewDialogHelper {

    private static final String TAG = "ReviewDialogHelper";

    private final Activity activity;
    private final ApiService apiService;
    private final int meetingId;
    private final int myUserId;

    public ReviewDialogHelper(Activity activity, ApiService apiService, int meetingId, int myUserId) {
        this.activity = activity;
        this.apiService = apiService;
        this.meetingId = meetingId;
        this.myUserId = myUserId;
    }

    // ─────────────────────────────────────────────────────────────
    // 1. "후기작성" 버튼 클릭 — 후기 대상 조회
    // ─────────────────────────────────────────────────────────────

    public void onReviewButtonClicked() {
        apiService.getReviewTargets(meetingId, myUserId)
                .enqueue(new Callback<ApiResponse<ReviewTargetsData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ReviewTargetsData>> call,
                                           Response<ApiResponse<ReviewTargetsData>> response) {
                        if (!response.isSuccessful()
                                || response.body() == null
                                || !response.body().isSuccess()) {
                            String errorMessage = (response.body() != null)
                                    ? response.body().getMessage()
                                    : "후기 대상을 불러오지 못했습니다";
                            Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        ReviewTargetsData data = response.body().getData();

                        if (!data.isIs_eligible()) {
                            Toast.makeText(activity,
                                    "QR 인증을 하지 않아 후기를 작성할 수 없습니다",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        List<ReviewTarget> targets = data.getTargets();
                        if (targets == null || targets.isEmpty()) {
                            Toast.makeText(activity,
                                    "후기를 남길 수 있는 멤버가 없습니다",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        showReviewDialog(targets);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ReviewTargetsData>> call, Throwable throwable) {
                        Toast.makeText(activity,
                                "네트워크 오류: " + throwable.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────
    // 2. 다이얼로그 표시
    // ─────────────────────────────────────────────────────────────

    private void showReviewDialog(List<ReviewTarget> targets) {
        DialogReviewBinding dialogBinding = DialogReviewBinding.inflate(
                LayoutInflater.from(activity)
        );

        // 가로 RecyclerView 세팅
        ReviewTargetAdapter[] adapterHolder = new ReviewTargetAdapter[1];
        ReviewTarget[] selectedTargetHolder = new ReviewTarget[1];
        int[] selectedPositionHolder = { RecyclerView.NO_POSITION };

        ReviewTargetAdapter adapter = new ReviewTargetAdapter(targets, (target, position) -> {
            Log.i(TAG, "클릭됨: position=" + position
                    + ", 이전 selectedPosition=" + selectedPositionHolder[0]
                    + ", 이전 target=" + (selectedTargetHolder[0] != null ? selectedTargetHolder[0].getUser_id() : "null"));

            selectedTargetHolder[0] = target;
            selectedPositionHolder[0] = position;
            adapterHolder[0].setSelectedPosition(position);

            // 선택 영역 표시 + 입력값 초기화
            dialogBinding.layoutReviewInput.setVisibility(View.VISIBLE);
            dialogBinding.tvSelectedNickname.setText(target.getUser_nickname());
            dialogBinding.ratingBar.setRating(0);
            dialogBinding.etReviewContent.setText("");
        });
        adapterHolder[0] = adapter;

        dialogBinding.rvReviewTargets.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        dialogBinding.rvReviewTargets.setAdapter(adapter);
        dialogBinding.rvReviewTargets.setItemAnimator(null);

        AlertDialog reviewDialog = new AlertDialog.Builder(activity)
                .setView(dialogBinding.getRoot())
                .setCancelable(true)
                .create();

        // 제출 버튼
        dialogBinding.btnReviewSubmit.setOnClickListener(view -> {
            ReviewTarget selectedTarget = selectedTargetHolder[0];
            if (selectedTarget == null) {
                Toast.makeText(activity, "멤버를 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            int rating = (int) dialogBinding.ratingBar.getRating();
            if (rating == 0) {
                Toast.makeText(activity, "별점을 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            String content = dialogBinding.etReviewContent.getText().toString().trim();

            submitReview(selectedTarget, rating, content, adapter,
                    selectedPositionHolder[0], dialogBinding);
        });

        // 닫기 버튼
        dialogBinding.btnReviewDialogClose.setOnClickListener(view -> reviewDialog.dismiss());

        reviewDialog.show();
    }

    // ─────────────────────────────────────────────────────────────
    // 3. 후기 제출
    // ─────────────────────────────────────────────────────────────

    private void submitReview(ReviewTarget target, int rating, String content,
                              ReviewTargetAdapter adapter, int position,
                              DialogReviewBinding dialogBinding) {

        ReviewSubmitRequest request = new ReviewSubmitRequest(
                meetingId, myUserId, target.getUser_id(), rating, content
        );

        apiService.submitReview(request)
                .enqueue(new Callback<ApiResponse<ReviewSubmitData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ReviewSubmitData>> call,
                                           Response<ApiResponse<ReviewSubmitData>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            Toast.makeText(activity, "후기가 등록되었습니다", Toast.LENGTH_SHORT).show();

                            // 해당 대상 already_reviewed=true 갱신
                            adapter.markReviewed(position);

                            // 입력부 초기화 + 숨김 (다이얼로그는 유지 → 다른 멤버 계속 작성 가능)
                            dialogBinding.layoutReviewInput.setVisibility(View.GONE);
                            dialogBinding.ratingBar.setRating(0);
                            dialogBinding.etReviewContent.setText("");

                        } else {
                            String errorMessage = (response.body() != null)
                                    ? response.body().getMessage()
                                    : "후기 등록에 실패했습니다";
                            Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ReviewSubmitData>> call, Throwable throwable) {
                        Log.e(TAG, "후기 제출 실패: " + throwable.getMessage());
                        Toast.makeText(activity,
                                "네트워크 오류: " + throwable.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}