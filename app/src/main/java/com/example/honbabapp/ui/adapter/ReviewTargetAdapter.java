package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.api.data.review.ReviewTarget;
import com.example.honbabapp.databinding.RvReviewTargetBinding;
import com.example.honbabapp.util.ChatGlide;
import com.example.honbabapp.util.ToastUtil;

import java.util.List;

/**
 * 후기 대상 가로 목록 어댑터 (dialog_review.xml 의 rv_review_targets).
 *
 * - 프로필 클릭 시 onTargetClickListener 로 선택 이벤트 전달
 * - already_reviewed=true 인 항목은 alpha 0.4 + 체크 아이콘 표시,
 *   클릭 시 "이미 후기를 작성했습니다" 토스트만 띄우고 선택 이벤트는 전달하지 않음
 * - 선택된 항목은 selectedPosition 으로 추적해 강조 표시(alpha 1.0 유지, 다른 항목과 구분)
 */
public class ReviewTargetAdapter extends RecyclerView.Adapter<ReviewTargetAdapter.ViewHolder> {

    public interface OnTargetClickListener {
        void onTargetClick(ReviewTarget target, int position);
    }

    private final List<ReviewTarget> targets;
    private final OnTargetClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public ReviewTargetAdapter(List<ReviewTarget> targets, OnTargetClickListener listener) {
        this.targets = targets;
        this.listener = listener;
    }

    /** 제출 성공 시 호출 — 해당 대상을 already_reviewed=true 로 갱신 */
    public void markReviewed(int position) {
        if (position < 0 || position >= targets.size()) return;
        targets.get(position).setAlready_reviewed(true);
        notifyItemChanged(position);
    }

    /** 선택 상태 갱신 (선택 카드 강조 표시용) */
    public void setSelectedPosition(int position) {
        int previous = selectedPosition;
        selectedPosition = position;
        if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous);
        if (selectedPosition != RecyclerView.NO_POSITION) notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvReviewTargetBinding binding = RvReviewTargetBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReviewTarget target = targets.get(position);

        holder.binding.tvTargetNickname.setText(target.getUser_nickname());
        ChatGlide.loadCircleProfile(holder.binding.imgTargetProfile, target.getUser_profile_image());

        if (target.isAlready_reviewed()) {
            holder.binding.imgTargetChecked.setVisibility(View.VISIBLE);
            holder.binding.imgTargetProfile.setAlpha(0.4f);
        } else {
            holder.binding.imgTargetChecked.setVisibility(View.GONE);
            holder.binding.imgTargetProfile.setAlpha(1.0f);
        }

        // 선택된 카드 강조 (선택 시 살짝 진하게)
        holder.binding.getRoot().setAlpha(
                (position == selectedPosition && !target.isAlready_reviewed()) ? 1.0f
                        : target.isAlready_reviewed() ? 0.4f : 0.85f
        );

        holder.binding.getRoot().setOnClickListener(view -> {
            if (target.isAlready_reviewed()) {
                ToastUtil.makeText(view.getContext(), "이미 후기를 작성했습니다", Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) {
                listener.onTargetClick(target, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return targets.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RvReviewTargetBinding binding;

        ViewHolder(RvReviewTargetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}