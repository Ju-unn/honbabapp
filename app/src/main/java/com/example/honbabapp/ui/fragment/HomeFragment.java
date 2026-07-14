package com.example.honbabapp.ui.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.honbabapp.activity.MainActivity;
import com.example.honbabapp.activity.meeting.MeetingCardWritingActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.NaverMapsApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.request.user.UpdateLocationRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.MeetingListResponse;
import com.example.honbabapp.databinding.FragmentHomeBinding;
import com.example.honbabapp.ui.adapter.HomeAllMeetingListAdapter;
import com.example.honbabapp.ui.adapter.HomeHotMeetingListAdapter;
import com.example.honbabapp.ui.dialog.LocationSelectionBottomSheetDialog;
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

    // SharedPreferences
    private SharedPreferences prefs;
    private static final String PREFS_NAME  = "honbabapp_prefs";
    private static final String KEY_SIGUNGU = "user_sigungu";
    private static final String KEY_DONG    = "user_dong";

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

        // 위치 설정 버튼 클릭시
        binding.btnLocation.setOnClickListener(v -> openLocationSelectionDialog());

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

        // 더보기 버튼 클릭 이벤트
        binding.tvHotMeetingsMore.setOnClickListener(v -> navigateToMapHot());
        binding.tvAllMeetingsMore.setOnClickListener(v -> navigateToMap("전체"));
    }

    /**
     * 위치 설정 버튼 클릭시
     */

    private void openLocationSelectionDialog() {
        LocationSelectionBottomSheetDialog dialog = new LocationSelectionBottomSheetDialog();
        dialog.currentLocationText = prefs.getString(KEY_DONG, currentSigungu);
        dialog.listener = (sigungu, dong) -> {
            currentSigungu = sigungu;
            prefs.edit()
                    .putString(KEY_SIGUNGU, sigungu)
                    .putString(KEY_DONG, dong)
                    .apply();
            binding.btnLocation.setText(dong);
            loadMeetings();
            updateLocationInDB(sigungu + " " + dong);
        };
        dialog.show(getChildFragmentManager(), "locationDialog");
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
                null ,   // categories: null = 전체 (서버에 '전체' 특례 없음 — MapFragment와 동일 규약)
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

                        List<Meeting> rawMeetings = data.getMeetings();

                        // 만료된 미팅 클라이언트 필터링 (백엔드 이중 안전망)
                        allMeetings = new ArrayList<>();
                        for (Meeting m : rawMeetings) {
                            if (!m.isExpired()) {
                                allMeetings.add(m);
                            }
                        }

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
     * Step1: SharedPreferences에서 즉시 로드
     * Step2: GPS 백그라운드 보완
     */
    private void loadCurrentLocationAndMeetings() {
        prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Step1: 저장된 위치 즉시 읽기
        String savedSigungu = prefs.getString(KEY_SIGUNGU, null);
        String savedDong    = prefs.getString(KEY_DONG, null);

        if (savedSigungu != null) {
            // 저장된 위치 있음 → 즉시 표시 + API 호출
            currentSigungu = savedSigungu;
            binding.btnLocation.setText(savedDong != null ? savedDong : savedSigungu);
            loadMeetings();
        } else {
            // 저장된 위치 없음 (첫 실행 등) → 기본값으로 일단 호출
            Log.w(TAG, "저장된 위치 없음 → 기본값(" + currentSigungu + ")으로 로드");
            loadMeetings();
        }

        // Step2: GPS 백그라운드 보완
        requestGpsInBackground(savedSigungu);
    }

    /**
     * GPS를 백그라운드에서 요청 (권한 없으면 스킵)
     */
    private void requestGpsInBackground(String currentSaved) {
        boolean hasPermission = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        if (!hasPermission) {
            Log.w(TAG, "위치 권한 없음 → GPS 스킵");
            return;
        }

        try {
            fusedClient.getLastLocation().addOnSuccessListener(location -> {
                if (location == null) return;
                parseAddressAndUpdateIfChanged(
                        location.getLatitude(), location.getLongitude(), currentSaved
                );
            });
        } catch (SecurityException e) {
            Log.e(TAG, "GPS 오류: " + e.getMessage());
        }
    }

    /**
     * 위경도 → 구/동 파싱
     * 이전 sigungu와 다를 때만 UI + API 재조회
     */
    private void parseAddressAndUpdateIfChanged(double lat, double lng, String prevSigungu) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), new Locale("ko", "KR"));
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses == null || addresses.isEmpty()) return;

            Address address = addresses.get(0);
            String gpsSigungu = address.getSubAdminArea();
            String gpsDong    = address.getThoroughfare();
            if (gpsDong == null || gpsDong.isEmpty()) gpsDong = address.getSubLocality();

            if (gpsSigungu == null || gpsSigungu.isEmpty()) return;

            // SharedPreferences 항상 최신 GPS값으로 갱신
            prefs.edit()
                    .putString(KEY_SIGUNGU, gpsSigungu)
                    .putString(KEY_DONG, gpsDong != null ? gpsDong : gpsSigungu)
                    .apply();

            // 구가 달라진 경우에만 UI + API 재조회
            if (!gpsSigungu.equals(prevSigungu)) {
                currentSigungu = gpsSigungu;
                String finalDong = gpsDong != null ? gpsDong : gpsSigungu;
                binding.btnLocation.setText(finalDong);
                loadMeetings();
                Log.i(TAG, "GPS 위치 변경 감지: " + prevSigungu + " → " + gpsSigungu);
            }

        } catch (Exception e) {
            Log.e(TAG, "Geocoder 오류: " + e.getMessage());
        }
    }

    /**
     * 위치 업데이트
     * @param location
     */
    private void updateLocationInDB(String location) {
        UpdateLocationRequest request = new UpdateLocationRequest(location);

        apiService.updateLocation(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.i(TAG, "위치 DB 업데이트 성공: " + location);
                } else {
                    Log.e(TAG, "위치 DB 업데이트 실패");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "위치 DB 업데이트 네트워크 오류: " + t.getMessage());
            }
        });
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

    @Override
    public void onResume() {
        super.onResume();
        if (binding == null) return;
        loadMeetings(); // 채팅방에서 모임 삭제/취소 후 돌아왔을때 새로고침
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}