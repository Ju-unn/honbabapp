package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.data.MockData;
import com.example.honbabapp.data.meeting.Meeting;
import com.example.honbabapp.databinding.RvMapMeetingCardBinding;
import com.example.honbabapp.databinding.RvMeetingCardBinding;

import java.util.ArrayList;
import java.util.Map;

public class MapAllMeetingListAdapter extends RecyclerView.Adapter<MapAllMeetingListAdapter.ViewHolder> {
    ArrayList<Meeting> allMeetings = MockData.getMeetingList();


    @NonNull
    @Override
    public MapAllMeetingListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        /// allMeetingList
        RvMeetingCardBinding rvMeetingCardBinding = RvMeetingCardBinding.inflate(layoutInflater , parent , false);
        ViewHolder viewHolder = new ViewHolder(rvMeetingCardBinding);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MapAllMeetingListAdapter.ViewHolder holder, int position) {
        Meeting allMeeting = allMeetings.get(position);

        holder.rvMeetingCardBinding.tvMeetingTitle.setText(allMeeting.meeting_title);
        holder.rvMeetingCardBinding.tvMeetingLocation.setText(allMeeting.meeting_location.location_store_address);
        holder.rvMeetingCardBinding.tvMeetingDescription.setText(allMeeting.meeting_description);
    }

    @Override
    public int getItemCount() {
        return allMeetings.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        RvMeetingCardBinding rvMeetingCardBinding;

        public ViewHolder(RvMeetingCardBinding rvMeetingCardBinding) {
            super(rvMeetingCardBinding.getRoot());
            this.rvMeetingCardBinding = rvMeetingCardBinding;
        }
    }
}
