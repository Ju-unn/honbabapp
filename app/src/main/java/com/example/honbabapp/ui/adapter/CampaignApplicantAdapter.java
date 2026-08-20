package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.api.data.response.meeting.ApplicantsResponse;
import com.example.honbabapp.databinding.RvCampaignApplicantItemBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CampaignApplicantAdapter
        extends RecyclerView.Adapter<CampaignApplicantAdapter.ViewHolder> {

    // ─────────────────────────────────────────────────────────────
    // 콜백 인터페이스
    // ─────────────────────────────────────────────────────────────

    public interface OnApproveClickListener {
        void onApproveClick(ApplicantsResponse.Applicant applicant);
    }

    public interface OnRejectClickListener {
        void onRejectClick(ApplicantsResponse.Applicant applicant);
    }

    public interface OnChatClickListener {
        void onChatClick(ApplicantsResponse.Applicant applicant);
    }

    public interface OnDetailClickListener {
        void onDetailClick(ApplicantsResponse.Applicant applicant);
    }

    public interface OnReviewClickListener {
        void onReviewClick(ApplicantsResponse.Applicant applicant);
    }

    // ─────────────────────────────────────────────────────────────
    // 상태
    // ─────────────────────────────────────────────────────────────

    private List<ApplicantsResponse.Applicant> applicantList = new ArrayList<>();

    private final OnApproveClickListener approveClickListener;
    private final OnRejectClickListener rejectClickListener;
    private final OnChatClickListener chatClickListener;
    private final OnDetailClickListener detailClickListener;
    private final OnReviewClickListener reviewClickListener;

    public CampaignApplicantAdapter(
            OnApproveClickListener approveClickListener,
            OnRejectClickListener rejectClickListener,
            OnChatClickListener chatClickListener,
            OnDetailClickListener detailClickListener,
            OnReviewClickListener reviewClickListener
    ) {
        this.approveClickListener = approveClickListener;
        this.rejectClickListener  = rejectClickListener;
        this.chatClickListener    = chatClickListener;
        this.detailClickListener  = detailClickListener;
        this.reviewClickListener  = reviewClickListener;
    }

    // ─────────────────────────────────────────────────────────────
    // RecyclerView.Adapter 구현
    // ─────────────────────────────────────────────────────────────

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvCampaignApplicantItemBinding binding = RvCampaignApplicantItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(applicantList.get(position));
    }

    @Override
    public int getItemCount() {
        return applicantList.size();
    }

    public void updateApplicants(List<ApplicantsResponse.Applicant> newApplicants) {
        if (newApplicants == null) return;
        applicantList.clear();
        applicantList.addAll(newApplicants);
        notifyDataSetChanged();
    }

    // ─────────────────────────────────────────────────────────────
    // ViewHolder
    // ─────────────────────────────────────────────────────────────

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final RvCampaignApplicantItemBinding binding;

        public ViewHolder(@NonNull RvCampaignApplicantItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ApplicantsResponse.Applicant applicant) {
            bindBasicInfo(applicant);
            bindButtons(applicant);
        }

        // ── 기본 정보 ──────────────────────────────────────────
        private void bindBasicInfo(ApplicantsResponse.Applicant applicant) {
            binding.tvApplicantNickname.setText(applicant.getUser_nickname());
            binding.tvApplicantManner.setText(
                    "매너점수 " + String.format("%.1f°C", applicant.getUser_manner_score())
            );
            binding.tvApplyTime.setText(formatApplyTime(applicant.getApplied_at()));
            Glide.with(binding.getRoot().getContext())
                    .load(applicant.getUser_profile_image())
                    .placeholder(R.drawable.ic_profile)
                    .circleCrop()
                    .into(binding.ivApplicantAvatar);
        }

        private void bindButtons(ApplicantsResponse.Applicant applicant) {
            String status = applicant.getParticipation_status();
            boolean isWaiting = "대기".equals(status);

            // 승인/거절 버튼 — 대기 상태에서만 표시 (승인/취소된 신청자는 숨김)
            binding.layoutActionButtons.setVisibility(isWaiting ? View.VISIBLE : View.INVISIBLE);

            if (isWaiting) {
                binding.btnApprove.setOnClickListener(view -> {
                    if (approveClickListener != null) {
                        approveClickListener.onApproveClick(applicant);
                    }
                });
                binding.btnReject.setOnClickListener(view -> {
                    if (rejectClickListener != null) {
                        rejectClickListener.onRejectClick(applicant);
                    }
                });
            }

            // 채팅/상세보기는 상태 무관 항상 표시
            binding.btnChat.setOnClickListener(view -> {
                if (chatClickListener != null) chatClickListener.onChatClick(applicant);
            });
            binding.btnViewDetail.setOnClickListener(view -> {
                if (detailClickListener != null) detailClickListener.onDetailClick(applicant);
            });

            // 리뷰작성 — 취소 탭에서, 승인 후 취소(노쇼)인 경우에만 노출
            // (approved_at이 있으면 한때 승인됐다가 취소된 것 = 대기 중 자진 취소와 구분)
            boolean isNoShowCancel = "취소".equals(status) && applicant.getApproved_at() != null;
            binding.btnWriteReview.setVisibility(isNoShowCancel ? View.VISIBLE : View.GONE);
            if (isNoShowCancel) {
                binding.btnWriteReview.setOnClickListener(view -> {
                    if (reviewClickListener != null) reviewClickListener.onReviewClick(applicant);
                });
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 유틸
    // ─────────────────────────────────────────────────────────────

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
            if (diffMinutes < 60) return diffMinutes + "분 전";
            long diffHours = diffMinutes / 60;
            if (diffHours < 24) return diffHours + "시간 전";
            return (diffHours / 24) + "일 전";
        } catch (Exception exception) {
            return appliedAt;
        }
    }
}