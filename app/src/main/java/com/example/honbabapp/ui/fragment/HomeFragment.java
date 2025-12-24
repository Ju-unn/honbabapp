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
import android.widget.Toast;

import com.example.honbabapp.activity.MainActivity;
import com.example.honbabapp.activity.NotificationActivity;
import com.example.honbabapp.activity.profile.MeetingCardWritingActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.MeetingListResponse;
import com.example.honbabapp.databinding.FragmentHomeBinding;
import com.example.honbabapp.ui.adapter.HomeAllMeetingListAdapter;
import com.example.honbabapp.ui.adapter.HomeHotMeetingListAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private ApiService apiService;
    private HomeHotMeetingListAdapter hotMeetingListAdapter;
    private HomeAllMeetingListAdapter allMeetingListAdapter;

    // 모임 데이터
    private List<Meeting> allMeetings = new ArrayList<>();


    public HomeFragment() {
        Log.i( TAG, "생성");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("HomeFragment" , "onCreateView");
        binding = FragmentHomeBinding.inflate(inflater , container , false);

        //API 서비스 초기화
        apiService = RetrofitClient.getClient().create(ApiService.class);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i("HomeFragment" , "onViewCreated");

        // recyclerViews 설정
        initRecyclerViews();

        // API 호출해서 데이터 로드
        loadMeetings();

        // 카테고리 클릭 이벤트 설정
        initCategoryButtons();

        // floating버튼 클릭 이벤트
        meetingCardWritingOnClick();

        // 알림버튼 클릭
        notificationButtonOnClick();
    }

    /**
     * RecyclerView 초기화
     */
    public void initRecyclerViews() {
        // Hot Meetings RecyclerView
        hotMeetingListAdapter = new HomeHotMeetingListAdapter();
        binding.rvHotMeetings.setAdapter(hotMeetingListAdapter);

        // All Meetings RecyclerView
        allMeetingListAdapter = new HomeAllMeetingListAdapter();
        binding.rvMeetings.setAdapter(allMeetingListAdapter);
    }

    /**
     * 실제 API 호출해서 모임 목록 가져오기
     */
    private void loadMeetings() {
        // TODO: 나중에 네이버 API로 내 위치 정보 가져와서 DB저장 (회원가입 할때 db에 location 값 넣기)
        // TODO : DB에 저장된 user_location에서 sigungu 추출 or db location 3개로 분리

        // 현재는 임시로 하드코딩
        String userSigungu = "관악구";

        // API 호출
        Call<ApiResponse<MeetingListResponse>> call = apiService.getMeetingList(
                userSigungu ,
                "전체" ,
                "모집중" ,
                1 ,
                10
        );

        call.enqueue(new Callback<ApiResponse<MeetingListResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingListResponse>> call, Response<ApiResponse<MeetingListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<MeetingListResponse> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        // 데이터 받아오기 성공
                        MeetingListResponse data = apiResponse.getData();
                        allMeetings = data.getMeetings();

                        Log.i(TAG, "모임 목록 로드 성공: " + allMeetings.size() + "개");

                        // Adapter에 데이터 전달
                        updateAdapters();
                    } else {
                        Log.e(TAG , "API 실패: " + apiResponse.getMessage());
                        Toast.makeText(getContext() ,
                                apiResponse.getMessage() ,
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "HTTP 오류: " + response.code());
                    Toast.makeText(getContext(),
                            "모임 목록을 불러오는데 실패했습니다.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingListResponse>> call, Throwable t) {
                Log.e(TAG, "네트워크 오류: " + t.getMessage());
                Toast.makeText(getContext(),
                        "네트워크 오류가 발생했습니다.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Adapter에 데이터 업데이트
     */
    private void updateAdapters() {
        // All meetings Adapter 업데이트
        allMeetingListAdapter.updateMeetings(allMeetings);

        // Hot Meetings Adapter 업데이트
        List<Meeting> hotMeetings = new ArrayList<>();
        for (Meeting meeting : allMeetings) {
            if (meeting.isMeeting_is_hot()) {
                hotMeetings.add(meeting);
            }
        }
        hotMeetingListAdapter.updateMeetings(hotMeetings);
    }
    public void initCategoryButtons() {
        // 전체
        binding.categoryAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               navigateToMap("전체");
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
        mainActivity.navigationToMap(category);
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