package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.api.data.review.WrittenReview;
import com.example.honbabapp.databinding.RvWrittenReviewBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 내가 쓴 후기 목록 어댑터
 * ProfileReviewsActivity 의 "내가 쓴 리뷰" 탭에 연결.
 *
 * ReceivedReviewAdapter 와 동일 톤이지만 후기 대상(닉네임/아바타)과
 * 모임명을 함께 표시한다.
 */
public class WrittenReviewAdapter extends RecyclerView.Adapter<WrittenReviewAdapter.ReviewViewHolder> {

    private final List<WrittenReview> reviewList = new ArrayList<>();

    /**
     * 외부에서 리뷰 목록 전체를 교체할 때 호출.
     */
    public void submitReviewList(List<WrittenReview> newReviewList) {
        reviewList.clear();
        if (newReviewList != null) {
            reviewList.addAll(newReviewList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvWrittenReviewBinding binding = RvWrittenReviewBinding.inflate(
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

    static class ReviewViewHolder extends RecyclerView.ViewHolder {

        private final RvWrittenReviewBinding binding;

        ReviewViewHolder(RvWrittenReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(WrittenReview review) {
            // 후기 대상 닉네임 (탈퇴 등으로 null이면 대체 문구)
            String nickname = review.getReviewee_nickname();
            binding.tvRevieweeNickname.setText(
                    nickname != null && !nickname.isEmpty() ? nickname : "알 수 없는 사용자");

            // 후기 대상 아바타
            String profileImage = review.getReviewee_profile_image();
            if (profileImage != null && !profileImage.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(profileImage)
                        .placeholder(R.drawable.ic_profile)
                        .circleCrop()
                        .into(binding.ivRevieweeAvatar);
            } else {
                binding.ivRevieweeAvatar.setImageResource(R.drawable.ic_profile);
            }

            // 모임명 — null이면 숨김 (모임 삭제된 경우)
            String meetingTitle = review.getMeeting_title();
            if (meetingTitle != null && !meetingTitle.isEmpty()) {
                binding.tvMeetingTitle.setVisibility(View.VISIBLE);
                binding.tvMeetingTitle.setText(meetingTitle);
            } else {
                binding.tvMeetingTitle.setVisibility(View.GONE);
            }

            // 별점
            binding.ratingBarWritten.setRating((float) review.getRating());

            // 날짜 — "2026-06-24 19:00:00" → "2026-06-24"
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
