package com.example.honbabapp.ui.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.honbabapp.activity.MainActivity;
import com.example.honbabapp.activity.NotificationActivity;
import com.example.honbabapp.activity.profile.MeetingCardWritingActivity;
import com.example.honbabapp.databinding.FragmentHomeBinding;
import com.example.honbabapp.ui.adapter.HomeAllMeetingListAdapter;
import com.example.honbabapp.ui.adapter.HomeHotMeetingListAdapter;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private HomeHotMeetingListAdapter hotMeetingListAdapter;
    private HomeAllMeetingListAdapter allMeetingListAdapter;


    public HomeFragment() {
        Log.i("HomeFragment" , "생성");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("HomeFragment" , "onCreateView");
        binding = FragmentHomeBinding.inflate(inflater , container , false);



        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i("HomeFragment" , "onViewCreated");
        // recyclerViews 설정
        initRecyclerViews();

        // 카테고리 클릭 이벤트 설정
        initCategoryButtons();

        // floating버튼 클릭 이벤트
        meetingCardWritingOnClick();

        // 알림버튼 클릭
        notificationButtonOnClick();
    }

    public void initRecyclerViews() {
        // Hot Meetings RecyclerView
        hotMeetingListAdapter = new HomeHotMeetingListAdapter();
        binding.rvHotMeetings.setAdapter(hotMeetingListAdapter);

        // All Meetings RecyclerView
        allMeetingListAdapter = new HomeAllMeetingListAdapter();
        binding.rvMeetings.setAdapter(allMeetingListAdapter);
    }

    public void initCategoryButtons() {
        // 전체
        binding.categoryAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               navigateToMap("잔체");
            }
        });

        // 한식
        binding.categoryKorean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToMap("한식");
            }
        });

        // 중식
        binding.categoryChinese.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToMap("중식");
            }
        });

        // 일식
        binding.categoryJapanese.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToMap("일식");
            }
        });


        // 오마카세
        binding.categoryOmakase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToMap("오마카세");
            }
        });

        // 양식
        binding.categoryWestern.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToMap("양식");
            }
        });

        // 술집
        binding.categoryPub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToMap("술집");
            }
        });

        // 카페
        binding.categoryCafe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToMap("카페");
            }
        });
    }

    public void navigateToMap(String category){
        MainActivity mainActivity = (MainActivity) requireActivity();
        mainActivity.navigationToMap();
    }

    /// flaoting 버트 클릭시 미팅 작성 페이지 이동
    public void meetingCardWritingOnClick() {
        binding.fabCreateMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity() , MeetingCardWritingActivity.class);
                startActivity(intent);
            }
        });
    }

    /// 알림 버튼
    public void notificationButtonOnClick() {
        binding.btnNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity() , NotificationActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}