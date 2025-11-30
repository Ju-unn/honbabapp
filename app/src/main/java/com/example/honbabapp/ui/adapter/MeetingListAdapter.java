package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.data.MockData;
import com.example.honbabapp.data.meeting.Meeting;
import com.example.honbabapp.databinding.RvMapMeetingCardBinding;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class MeetingListAdapter extends RecyclerView.Adapter<MeetingListAdapter.ViewHolder> {
    ArrayList<Meeting> allMeetingList = MockData.getMeetingList();

    @NonNull
    @Override
    public MeetingListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        RvMapMeetingCardBinding rvMapMeetingCardBinding = RvMapMeetingCardBinding.inflate(layoutInflater , parent , false);
        ViewHolder viewHolder = new ViewHolder(rvMapMeetingCardBinding);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MeetingListAdapter.ViewHolder holder, int position) {
            Meeting allMeeting = allMeetingList.get(position);

            holder.binding.tvMeetingTitle.setText(allMeeting.meeting_title);
            holder.binding.tvMeetingLocation.setText(allMeeting.meeting_location.location_store_address);
            holder.binding.tvMeetingDescription.setText(allMeeting.meeting_description);
    }

    @Override
    public int getItemCount() {
        return allMeetingList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        RvMapMeetingCardBinding binding;
        public ViewHolder(RvMapMeetingCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
