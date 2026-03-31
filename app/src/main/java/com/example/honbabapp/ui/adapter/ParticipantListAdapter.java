package com.example.honbabapp.ui.adapter;

import static com.bumptech.glide.load.resource.bitmap.TransformationUtils.circleCrop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.api.data.meeting.MeetingParticipant;
import com.example.honbabapp.databinding.RvMeetingParticipantItemBinding;

import java.util.List;

public class ParticipantListAdapter extends RecyclerView.Adapter<ParticipantListAdapter.ViewHolder> {

    private final List<MeetingParticipant> participants;
    private final String hostNickName;

    public ParticipantListAdapter(List<MeetingParticipant> participants , String hostNickName) {
        this.participants = participants;
        this.hostNickName = hostNickName;
    }


    @NonNull
    @Override
    public ParticipantListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        RvMeetingParticipantItemBinding binding = RvMeetingParticipantItemBinding.inflate(layoutInflater , parent , false);
        ViewHolder viewHolder = new ViewHolder(binding);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantListAdapter.ViewHolder holder, int position) {
        holder.bind(participants.get(position));
    }

    @Override
    public int getItemCount() {
        return participants != null ? participants.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        RvMeetingParticipantItemBinding binding;
        public ViewHolder(RvMeetingParticipantItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(MeetingParticipant participant) {

            binding.tvParticipantName.setText(participant.getUser_nickname());
            binding.tvParticipantManner.setText("매너점수 " + String.format("%.1f", participant.getUser_manner_score()));

            // 호스트 배지
            if (participant.getUser_nickname().equals(hostNickName)) {
                binding.tvHostBadge.setVisibility(View.VISIBLE);
            } else {
                binding.tvHostBadge.setVisibility(View.GONE);
            }

            Glide.with(binding.getRoot().getContext())
                    .load(participant.getUser_profile_image())
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.ivParticipantAvatar);
        }
    }
}
