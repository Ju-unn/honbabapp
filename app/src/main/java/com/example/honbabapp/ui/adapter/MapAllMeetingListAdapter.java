package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.data.MockData;
import com.example.honbabapp.databinding.RvMapMeetingCardBinding;
import com.example.honbabapp.databinding.RvMeetingCardBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapAllMeetingListAdapter extends RecyclerView.Adapter<MapAllMeetingListAdapter.ViewHolder> {

    private List<Meeting> meetings = new ArrayList<>();

    public void updateMeetings(List<Meeting> newMeetings) {
        this.meetings = newMeetings;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public MapAllMeetingListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvMeetingCardBinding binding = RvMeetingCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MapAllMeetingListAdapter.ViewHolder holder, int position) {
        holder.bind(meetings.get(position));
    }

    @Override
    public int getItemCount() {
        return meetings.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final RvMeetingCardBinding binding;

        public ViewHolder(RvMeetingCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Meeting meeting) {
            binding.tvMeetingTitle.setText(meeting.getMeeting_title());
            binding.tvMeetingDescription.setText(meeting.getMeeting_description());

            // location null 체크 — DB에 location이 없는 모임 방어
            if (meeting.getLocation() != null) {
                binding.tvMeetingLocation.setText(meeting.getLocation().getPlace_name() + "" + meeting.getLocation().getRoad_address());
            } else {
                binding.tvMeetingLocation.setText("");
            }
        }
    }
}



























