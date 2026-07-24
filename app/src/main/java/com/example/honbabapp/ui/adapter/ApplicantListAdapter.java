package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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
            binding.tvApplicantIntro.setText("매너점수 " +
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

            // 프로필 이미지
            Glide.with(binding.getRoot().getContext())
                    .load(applicant.getUser_profile_image())
                    .placeholder(R.drawable.ic_profile)
                    .circleCrop()
                    .into(binding.ivApplicantAvatar);
        }
    }

    /**
     * 신청 시간 포맷팅
     * "2025-12-28 10:30:00" → "10분 전"
     */
    private String formatApplyTime(String appliedAt) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
            // 서버는 항상 KST(Asia/Seoul) 문자열을 내려주므로, 기기 타임존이 달라도
            // 항상 KST로 해석하도록 명시한다 (안 하면 기기 기본 타임존으로 파싱되어 시간이 어긋남)
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            Date applyDate = sdf.parse(appliedAt);
            long diffMs = System.currentTimeMillis() - applyDate.getTime();
            long diffMin = diffMs / 60000;

            if (diffMin < 1) return "방금";
            if (diffMin < 60) return diffMin + "분 전";
            long diffHour = diffMin / 60;
            if (diffHour < 24) return diffHour + "시간 전";
            return (diffHour / 24) + "일 전";
        } catch (Exception e) {
            return appliedAt;
        }
    }
}
