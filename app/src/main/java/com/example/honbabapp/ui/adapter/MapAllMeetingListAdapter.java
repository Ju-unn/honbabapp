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

    //아이템 현재 정렬 상태 - 기본값은 전체
    OrderBy orderBy = OrderBy.All;
    //아이템 현재 필터 상태 - 기본값은 모두 null(선택되지 않음)
    Filter filter = new Filter();


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
        Meeting meeting = getMeetingList().get(position);

        holder.rvMeetingCardBinding.tvMeetingTitle.setText(meeting.meeting_title);
        holder.rvMeetingCardBinding.tvMeetingLocation.setText(meeting.meeting_location.location_store_address);
        holder.rvMeetingCardBinding.tvMeetingDescription.setText(meeting.meeting_description);
    }

    //TODO : 해야될부분을 정의
    //allMettings, filter, orderBy를 이용해서 표시될 meetingList를 가져오는 함수
    public ArrayList<Meeting> getMeetingList() {

        ArrayList<Meeting> result = new ArrayList<>();
        for (Meeting m : allMeetings) {

            // "전체"이거나 null이면 모든 Meeting 추가
            if (filter.foodType == null || filter.foodType.equals("전체")) {
                result.add(m);
            }
            // 특정 카테고리면 해당 타입만 추가
            else if (filter.foodType.equals(m.meeting_category)) {
                result.add(m);
            }

        }
        return result;
    }

    @Override
    public int getItemCount() {
        return getMeetingList().size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        RvMeetingCardBinding rvMeetingCardBinding;

        public ViewHolder(RvMeetingCardBinding rvMeetingCardBinding) {
            super(rvMeetingCardBinding.getRoot());
            this.rvMeetingCardBinding = rvMeetingCardBinding;
        }
    }

    public void setOrderByAndFilter(OrderBy orderBy , String filter) {
        this.orderBy = orderBy;
        this.filter.foodType  = filter;
    }


    //enum클래스 활용하여 아이템 정렬 상태 표시
    public enum OrderBy {
        All, Hot, Distance
    }

    public class Filter {
        String foodType;
        int price;
        String location;
    }
}
