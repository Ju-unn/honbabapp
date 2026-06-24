package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.api.data.review.ReceivedReview;
import com.example.honbabapp.databinding.RvReceivedReviewBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 받은 후기 목록 어댑터
 * ChatRoomParticipantDetailActivity 의 rv_received_reviews 에 연결.
 *
 * - 익명 표시: reviewer 정보 없음
 * - 별점(RatingBar, 읽기 전용) / 내용(nullable) / 날짜 표시
 * - content 가 null 이거나 비어있으면 tv_review_content 를 GONE 처리
 */
public class ReceivedReviewAdapter extends RecyclerView.Adapter<ReceivedReviewAdapter.ReviewViewHolder> {

    private final List<ReceivedReview> reviewList = new ArrayList<>();

    // ─────────────────────────────────────────
    // 데이터 갱신
    // ─────────────────────────────────────────

    /**
     * 외부에서 리뷰 목록 전체를 교체할 때 호출.
     * Activity 의 loadReceivedReviews() 성공 콜백에서 사용.
     */
    public void submitReviewList(List<ReceivedReview> newReviewList) {
        reviewList.clear();
        if (newReviewList != null) {
            reviewList.addAll(newReviewList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvReceivedReviewBinding binding = RvReceivedReviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ReviewViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        holder.bind(reviewList.get(position));
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    // ─────────────────────────────────────────
    // ViewHolder
    // ─────────────────────────────────────────

    static class ReviewViewHolder extends RecyclerView.ViewHolder {

        private final RvReceivedReviewBinding binding;

        ReviewViewHolder(RvReceivedReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ReceivedReview review) {
            // 별점 (1~5 정수 → float 변환, RatingBar 는 float 사용)
            binding.ratingBarReceived.setRating((float) review.getRating());

            // 날짜 — "2026-06-24 19:00:00" → "2026-06-24" 앞 10자만 표시
            String createdAt = review.getCreated_at();
            if (createdAt != null && createdAt.length() >= 10) {
                binding.tvReviewDate.setText(createdAt.substring(0, 10));
            } else {
                binding.tvReviewDate.setText(createdAt != null ? createdAt : "");
            }

            // 내용 — null 또는 빈 문자열이면 숨김
            String content = review.getContent();
            if (content != null && !content.isEmpty()) {
                binding.tvReviewContent.setVisibility(View.VISIBLE);
                binding.tvReviewContent.setText(content);
            } else {
                binding.tvReviewContent.setVisibility(View.GONE);
            }
        }
    }
}