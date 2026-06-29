package com.example.honbabapp.ui.adapter;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.activity.campaign.CampaignDetailActivity;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.meeting.MeetingParticipant;
import com.example.honbabapp.databinding.RvCampaignCardBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 체험단 목록 어댑터
 *
 * Issue #3 수정:
 * - bindDeadlineBadge() 제거 — rv_campaign_card.xml 에서 tv_deadline_badge 뷰 삭제됨
 * - bindLocationAndDate() 에서 tv_campaign_date 참조 제거 — 위치(tv_campaign_location)만 표시
 */
public class CampaignListAdapter extends RecyclerView.Adapter<CampaignListAdapter.ViewHolder> {

    private static final String TAG = "CampaignListAdapter";
    private List<Meeting> campaignMeetingList = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvCampaignCardBinding binding = RvCampaignCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(campaignMeetingList.get(position));
    }

    @Override
    public int getItemCount() {
        return campaignMeetingList.size();
    }

    public void updateCampaignList(List<Meeting> meetings) {
        if (meetings == null) {
            Log.w(TAG, "updateCampaignList: meetings is null");
            return;
        }
        campaignMeetingList.clear();
        campaignMeetingList.addAll(meetings);
        notifyDataSetChanged();
        Log.i(TAG, "체험단 목록 업데이트 완료: " + meetings.size() + "개");
    }

    // ─────────────────────────────────────────────────────────────
    // ViewHolder
    // ─────────────────────────────────────────────────────────────

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final RvCampaignCardBinding binding;

        public ViewHolder(@NonNull RvCampaignCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Meeting meeting) {
            bindClickEvent(meeting);
            bindImage(meeting);
            // bindDeadlineBadge 제거 — Issue #3: tv_deadline_badge 뷰 XML에서 삭제됨
            bindTitle(meeting);
            bindCampaignProvided(meeting);
            bindCampaignChannels(meeting);
            bindLocation(meeting);        // 위치만 표시 (날짜 행 삭제됨)
            bindParticipantsRow(meeting);
            bindDeadlineText(meeting);
        }

        // ── 클릭 → CampaignDetailActivity ────────────────────────
        private void bindClickEvent(Meeting meeting) {
            binding.getRoot().setOnClickListener(view -> {
                Intent intent = new Intent(view.getContext(), CampaignDetailActivity.class);
                intent.putExtra("meeting_id", meeting.getMeeting_id());
                view.getContext().startActivity(intent);
            });
        }

        // ── 상단 이미지 ───────────────────────────────────────────
        private void bindImage(Meeting meeting) {
            Glide.with(binding.getRoot().getContext())
                    .load(meeting.getMeeting_image())
                    .placeholder(R.drawable.placeholder_meeting_image)
                    .error(R.drawable.placeholder_meeting_image)
                    .centerCrop()
                    .into(binding.ivCampaignImage);
        }

        // ── 제목 (왼쪽) + 체험단 뱃지 (오른쪽) ──────────────────
        private void bindTitle(Meeting meeting) {
            binding.tvCampaignTitle.setText(meeting.getMeeting_title());
        }

        // ── 제공 내역 (주황 굵게) ─────────────────────────────────
        private void bindCampaignProvided(Meeting meeting) {
            String provided = meeting.getCampaign_provided();
            if (provided != null && !provided.isEmpty()) {
                binding.tvCampaignProvided.setText(provided);
                binding.tvCampaignProvided.setVisibility(View.VISIBLE);
            } else {
                binding.tvCampaignProvided.setVisibility(View.GONE);
            }
        }

        // ── 홍보 채널 ─────────────────────────────────────────────
        private void bindCampaignChannels(Meeting meeting) {
            String channels = meeting.getCampaign_channels();
            if (channels != null && !channels.isEmpty()) {
                binding.tvCampaignChannels.setText(channels.replace(",", " · "));
                binding.tvCampaignChannels.setVisibility(View.VISIBLE);
            } else {
                binding.tvCampaignChannels.setVisibility(View.GONE);
            }
        }

        /**
         * Issue #3: 위치만 표시
         * tv_campaign_date 뷰가 rv_campaign_card.xml 에서 삭제되었으므로
         * 날짜 바인딩(binding.tvCampaignDate) 코드 제거
         */
        private void bindLocation(Meeting meeting) {
            if (meeting.getLocation() != null) {
                String locationText = meeting.getLocation().getPlace_name();
                if (locationText == null || locationText.isEmpty()) {
                    locationText = meeting.getLocation().getDong();
                }
                binding.tvCampaignLocation.setText(locationText);
            }
        }

        // ── 하단 참여자 아바타 + 남은 인원 ───────────────────────
        private void bindParticipantsRow(Meeting meeting) {
            List<MeetingParticipant> participants = meeting.getParticipants();

            ImageView[] avatars = {
                    binding.ivAvatar1,
                    binding.ivAvatar2,
                    binding.ivAvatar3
            };

            int participantCount = (participants != null) ? participants.size() : 0;

            for (int index = 0; index < avatars.length; index++) {
                if (index < participantCount) {
                    avatars[index].setVisibility(View.VISIBLE);
                    Glide.with(binding.getRoot().getContext())
                            .load(participants.get(index).getUser_profile_image())
                            .placeholder(R.drawable.ic_profile)
                            .circleCrop()
                            .into(avatars[index]);
                } else {
                    avatars[index].setVisibility(View.GONE);
                }
            }

            int needed = meeting.getMeeting_max_participants() - meeting.getMeeting_current_participants();
            binding.tvCampaignParticipantCount.setText(
                    needed > 0 ? "+" + needed + "명 더 모집중" : "정원 마감"
            );
        }

        // ── 마감일 텍스트 (하단 오른쪽) ──────────────────────────
        private void bindDeadlineText(Meeting meeting) {
            String deadline = meeting.getCampaign_deadline();
            if (deadline == null || deadline.isEmpty()) {
                binding.tvCampaignDeadline.setVisibility(View.GONE);
                return;
            }
            try {
                SimpleDateFormat inputFormat  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
                SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd", Locale.KOREA);
                Date deadlineDate = inputFormat.parse(deadline);
                if (deadlineDate != null) {
                    binding.tvCampaignDeadline.setText("~" + outputFormat.format(deadlineDate) + " 마감");
                    binding.tvCampaignDeadline.setVisibility(View.VISIBLE);
                } else {
                    binding.tvCampaignDeadline.setVisibility(View.GONE);
                }
            } catch (ParseException e) {
                Log.w(TAG, "마감일 파싱 실패: " + deadline);
                binding.tvCampaignDeadline.setVisibility(View.GONE);
            }
        }
    }
}