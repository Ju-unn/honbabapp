package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.api.data.response.meeting.ApplicantsResponse;
import com.example.honbabapp.databinding.RvApplicantListBinding;

import java.util.List;

/**
 * 신청자 목록 어댑터
 */
public class ApplicantListAdapter extends RecyclerView.Adapter<ApplicantListAdapter.ViewHolder>{

    private List<ApplicantsResponse.Applicant> applicants;
    private OnApproveClickListener onApproveClickListener;
    private OnRejectClickListener onRejectClickListener;

    public interface OnApproveClickListener {
        void onApproveClick(ApplicantsResponse.Applicant applicant);
    }

    public interface OnRejectClickListener {
        void onRejectClick(ApplicantsResponse.Applicant applicant);
    }

    public ApplicantListAdapter(List<ApplicantsResponse.Applicant> applicants ,
                                OnApproveClickListener onApproveClickListener ,
                                OnRejectClickListener onRejectClickListener) {
        this.applicants = applicants;
        this.onApproveClickListener = onApproveClickListener;
        this.onRejectClickListener = onRejectClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        RvApplicantListBinding binding = RvApplicantListBinding.inflate(inflater , parent , false);
        ViewHolder viewHolder = new ViewHolder(binding);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApplicantsResponse.Applicant applicant = applicants.get(position);
        holder.bind(applicant);
    }

    @Override
    public int getItemCount() {
        return applicants.size();
    }

    /**
     * 데이터 업데이트
     */
    public void updateApplicants(List<ApplicantsResponse.Applicant> newApplicants) {
        this.applicants.clear();
        this.applicants.addAll(newApplicants);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private RvApplicantListBinding binding;
        public ViewHolder(RvApplicantListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ApplicantsResponse.Applicant applicant) {
            // 이름
            binding.tvApplicantName.setText(applicant.getUser_nickname());

            //성별 표시 (한 줄 소개 대신)
            String gender = applicant.getUser_gender();
            binding.tvApplicantIntro.setText(gender + " • 매너온도 " +
                    String.format("%.1f°C", applicant.getUser_manner_score()));

            // 신청 시간
            String applyTime = formatApplyTime(applicant.getApplied_at());
            binding.tvApplyTime.setText(applyTime);

            // 승인 버튼
            binding.btnAccept.setOnClickListener(v -> {
                if (onApproveClickListener != null) {
                    onApproveClickListener.onApproveClick(applicant);
                }
            });

            // 거절 버튼
            binding.btnReject.setOnClickListener(v -> {
                if (onRejectClickListener != null) {
                    onRejectClickListener.onRejectClick(applicant);
                }
            });

            // 프로필 이미지 (TODO: Glide)
        }
    }

    /**
     * 신청 시간 포맷팅
     * "2025-12-28 10:30:00" → "10분 전"
     */
    private String formatApplyTime(String appliedAt) {
        try {
            // 현재는 단순 표시
            // TODO: 실제 시간 차이 계산
            return "방금 전";
        } catch (Exception e) {
            return appliedAt;
        }
    }
}
