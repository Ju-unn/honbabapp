package com.example.honbabapp.ui.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.honbabapp.activity.MainActivity;
import com.example.honbabapp.activity.NotificationActivity;
import com.example.honbabapp.activity.meeting.MeetingCardWritingActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.MeetingListResponse;
import com.example.honbabapp.databinding.FragmentHomeBinding;
import com.example.honbabapp.ui.adapter.HomeAllMeetingListAdapter;
import com.example.honbabapp.ui.adapter.HomeHotMeetingListAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    // 위치 관련
    private FusedLocationProviderClient fusedClient;
    private String currentSigungu = "관악구"; // 위치 로드 전 기본값

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

        //GPS 클라이언트 초기화
        fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i("HomeFragment" , "onViewCreated");

        // recyclerViews 설정
        initRecyclerViews();

        // GPS 위치 먼저 가져온뒤 API 호출
        loadCurrentLocationAndMeetings();

        // 카테고리 클릭 이벤트 설정
        initCategoryButtons();

        // floating버튼 클릭 이벤트
        meetingCardWritingOnClick();

        // 알림버튼 클릭
        notificationButtonOnClick();

        // 더보기 버튼 클릭 이벤트
        binding.tvHotMeetingsMore.setOnClickListener(v -> navigateToMapHot());
        binding.tvAllMeetingsMore.setOnClickListener(v -> navigateToMap("전체"));
    }

    /**
     * 모임 목록 새로고침 (외부에서 호출)
     */
    public void refreshMeetingList() {
        Log.i(TAG, "모임 목록 새로고침 시작");
        loadCurrentLocationAndMeetings();  // 기존 loadMeetings() 재호출
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

        // API 호출 (currentSigungu는 loadCurrentLocationAndMeetings에서 설정됨)
        Call<ApiResponse<MeetingListResponse>> call = apiService.getMeetingList(
                currentSigungu ,
                "전체" ,
                "모집중" ,
                1 ,
                30
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
        // Hot Meetings: is_hot 무시 — view_count 내림차순 정렬 후 상위 5개
        List<Meeting> hotMeetings = new ArrayList<>(allMeetings);
        hotMeetings.sort((a , b) -> b.getMeeting_view_count() - a.getMeeting_view_count());
        if (hotMeetings.size() > 5) {
            hotMeetings = hotMeetings.subList(0, 5);
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

    public void navigateToMapHot() {
        MainActivity mainActivity = (MainActivity) requireActivity();
        mainActivity.navigationToMapHot();
    }

    /**
     * GPS 위치로 구/동 가져온 뒤 loadMeetings() 호출
     * 권한 없거나 위치 못 받으면 기본값(currentSigungu = "관악구")으로 진행
     */
    private void loadCurrentLocationAndMeetings() {
        // 위치 권한 확인
        boolean hasPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        if (!hasPermission) {
            Log.w(TAG, "위치 권한 없음 → 기본값(" + currentSigungu + ")으로 로드");
            loadMeetings();
            return;
        }

        try {
            fusedClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    parseAddressFromLocation(location.getLatitude(), location.getLongitude());
                } else {
                    // 캐시된 위치 없음 — 기본값으로 진행
                    Log.w(TAG, "위치 캐시 없음 → 기본값(" + currentSigungu + ")으로 로드");
                    loadMeetings();
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "위치 가져오기 실패: " + e.getMessage());
                loadMeetings();
            });
        } catch (SecurityException e) {
            Log.e(TAG, "위치 권한 오류: " + e.getMessage());
            loadMeetings();
        }
    }

    /**
     * 위경도 → 구/동 추출 → btn_location 텍스트 업데이트 → loadMeetings()
     */

    private void parseAddressFromLocation(double lat , double lng) {
        try {
            Geocoder geocoder = new Geocoder(requireContext() , new Locale("ko" , "KR"));
            List<Address> addresses = geocoder.getFromLocation(lat , lng , 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // 구
                String sigungu = address.getSubAdminArea();
                // 동
                String dong = address.getThoroughfare();
                if (dong == null || dong.isEmpty()) {
                    dong = address.getSubLocality();
                }

                if (sigungu != null && !sigungu.isEmpty()) {
                    currentSigungu = sigungu;
                }

                // btn_location 동 표시
                String locationText = (dong != null && !dong.isEmpty()) ? dong : currentSigungu;
                binding.btnLocation.setText(locationText);

                Log.i(TAG, "위치 파싱 성공: " + currentSigungu + " / " + locationText);
            } else {
                Log.w(TAG, "주소 결과 없음 → 기본값 유지");
            }
        } catch (Exception e) {
            Log.e(TAG, "Geocoder 오류: " + e.getMessage());
        }

        // 주소 파싱 성공/실패 관계없이 loadMeetings() 호출
        loadMeetings();
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