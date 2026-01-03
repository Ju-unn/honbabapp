package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.activity.meeting.PlaceSearchActivity;
import com.example.honbabapp.api.data.response.naver.NaverLocalSearchResponse;
import com.example.honbabapp.databinding.ActivityPlaceSearchBinding;
import com.example.honbabapp.databinding.ActivitySearchViewBinding;
import com.example.honbabapp.databinding.RvMapMeetingCardBinding;
import com.example.honbabapp.databinding.RvPlaceSearchBinding;

import java.util.ArrayList;
import java.util.List;

public class PlaceSearchAdapter extends RecyclerView.Adapter<PlaceSearchAdapter.ViewHolder> {

    private List<NaverLocalSearchResponse.LocalItem> placeList = new ArrayList<>();
    private OnPlaceClickListener listener;

    /**
     * 장소 클릭 리스너 인터페이스
     */
    public interface OnPlaceClickListener {
        void onPlaceClick(NaverLocalSearchResponse.LocalItem place);
    }

    public void setOnPlaceClickListener(OnPlaceClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        RvPlaceSearchBinding binding = RvPlaceSearchBinding.inflate(inflater , parent , false);
        ViewHolder viewHolder = new ViewHolder(binding);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceSearchAdapter.ViewHolder holder, int position) {
        NaverLocalSearchResponse.LocalItem place = placeList.get(position);
        holder.bind(place);
    }

    @Override
    public int getItemCount() {
        return placeList.size();
    }

    /**
     * 검색 결과 업데이트
     */
    public void updatePlaces(List<NaverLocalSearchResponse.LocalItem> places) {
        this.placeList.clear();
        if (places != null) {
            this.placeList.addAll(places);
        }
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private RvPlaceSearchBinding binding;
        public ViewHolder(RvPlaceSearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(NaverLocalSearchResponse.LocalItem place) {
            // 장소명
            binding.tvPlaceName.setText(place.getCleanTitle());

            //카테고리
            if (place.getCategory() != null && !place.getCategory().isEmpty()) {
                 binding.tvCategory.setText(place.getCategory());
                 binding.tvCategory.setVisibility(View.VISIBLE);
            } else {
                binding.tvCategory.setVisibility(View.GONE);
            }

            // 도로명 주소
            if (place.getRoadAddress() != null && !place.getRoadAddress().isEmpty()) {
                binding.tvRoadAddress.setText(place.getRoadAddress());
            } else {
                binding.tvRoadAddress.setText("도로명 주소 없음");
            }

            // 지번 주소
            if (place.getAddress() != null && !place.getAddress().isEmpty()) {
                binding.tvAddress.setText("(" + place.getAddress() + ")");
                binding.tvAddress.setVisibility(View.VISIBLE);
            } else {
                binding.tvAddress.setVisibility(View.GONE);
            }

            // 전화번호
            if (place.getTelephone() != null && !place.getTelephone().isEmpty()) {
                binding.tvPhone.setText(place.getTelephone());
                binding.layoutPhone.setVisibility(View.VISIBLE);
            } else {
                binding.layoutPhone.setVisibility(View.GONE);
            }

            // 클릭 이벤트
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaceClick(place);
                }
            });


        }
    }
}
