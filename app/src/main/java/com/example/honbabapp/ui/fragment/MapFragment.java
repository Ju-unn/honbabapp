package com.example.honbabapp.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.honbabapp.activity.MainActivity;
import com.example.honbabapp.activity.SearchViewActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.MeetingListResponse;
import com.example.honbabapp.databinding.FragmentMapBinding;
import com.example.honbabapp.ui.adapter.MapAllMeetingListAdapter;
import com.example.honbabapp.ui.dialog.MapCategoryBottomSheetDialog;
import com.example.honbabapp.ui.dialog.MapFilterBottomSheetDialog;
import com.example.honbabapp.ui.dialog.MapSortBottomSheetDialog;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapFragment";

    /// Naver /////////////////////////////////////////////////////////////////////////////////
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    // 지도 드래그 후 API 재조회 반경 (km)
    private static final double DEFAULT_RADIUS_KM = 3.0;
    private MapView mapView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;

    //위치
    private FusedLocationProviderClient fusedClient;

    //현재 위치 중심 좌표 (서울 시청 기본값)
    private double currentLat = 37.5665;
    private double currentLng = 126.9780;

    // onMapReady에서 호출 , map 초기화 안 될 수도 있으니 바로 호출하면 에러 날 확률방지
    private String pendingCategory = null;

    // isInitialLoad: 지도 초기 렌더링 시 CameraIdleListener가 즉시 발동해서
    // API를 두 번 호출하는 문제 방지용 플래그
    private boolean isInitialLoad = true;

    /// Naver /////////////////////////////////////////////////////////////////////////////////



    /// 상태관리 /////////////////////////////////////////////////////////////////////////////////
    private String currentCategory = "전체";
    private String currentSortOption = "전체";

    /// 상태관리 /////////////////////////////////////////////////////////////////////////////////



    /// ViewBinding /////////////////////////////////////////////////////////////////////////////////
    private FragmentMapBinding binding;

    /// ViewBinding /////////////////////////////////////////////////////////////////////////////////



    /// API  Adapter /////////////////////////////////////////////////////////////////////////////////
    private ApiService apiService;
    private MapAllMeetingListAdapter allMeetingListAdapter;

    // 현재 지도에 표시된 마커 목록 - 새 데이터 로드 시 기 존 마커 제거용
    private List<Marker> activeMarkers = new ArrayList<>();

    /// API  Adapter /////////////////////////////////////////////////////////////////////////////////

    public MapFragment() {
        Log.i(TAG, "생성");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        binding = FragmentMapBinding.inflate(inflater, container , false);

        // API 서비스 초기화
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // Google 위치 클라이언트 초기화
        fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Naver 위치 소스 초기화
        locationSource = new FusedLocationSource(this , LOCATION_PERMISSION_REQUEST_CODE);

        // MapView 초기화 - XML의 map_view와 연결
        mapView = binding.mapView;
        mapView.onCreate(savedInstanceState);

        // getMapAsync 지도가 준비되면 onMapReady() 콜백 호출
        mapView.getMapAsync(this);


        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "onViewCreated");

        initRecyclerView();
        setupSearchBar();
        onBackButtonClick();
        initSortButton();
    }


    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        Log.i(TAG, "onMapReady");
        Log.i(TAG, "Client ID: " + com.example.honbabapp.BuildConfig.NAVER_MAPS_CLIENT_ID);
        this.naverMap = naverMap;

        // Naver SDK에 위치 소스 연결
        naverMap.setLocationSource(locationSource);

        // 내 위치 버튼 활성
        naverMap.getUiSettings().setLocationButtonEnabled(true);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        // 지도 드래그 후 멈추면 새 중심 좌표로 API 재조회
        naverMap.addOnCameraIdleListener(() -> {
            if (isInitialLoad) {
                isInitialLoad = false;
                return;
            }

            // 현재 카메라 중심 좌표로 업데이트 후 재조회
            LatLng center = naverMap.getCameraPosition().target;
            currentLat = center.latitude;
            currentLng = center.longitude;
            loadMeetingsOnMap ();
        });

        // pendingCategory setFilet()가 onMapReady() 전에 호출됐다면 여기서 처리
        if (pendingCategory != null) {
            currentCategory = pendingCategory;
            pendingCategory = null;
            showCategoryDialog(currentCategory);
        }

        // 현재 위치로 카메라 이동 후 첫 api 호출
        moveToCurrentLocation();
    }

    // ──────────────────────────────────────────────
    // 위치 이동
    // ──────────────────────────────────────────────

    private void moveToCurrentLocation() {
        try {
            fusedClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    Log.i(TAG, "현재 위치: " + currentLat + ", " + currentLng);
                } else {
                    // 위치를 못 받으면 서울 시청 기본값 유지
                    Log.w(TAG, "위치 없음 → 서울 기본값 사용");
                }
                // 카메라 이동 후 API 호출
                moveCameraTo(currentLat, currentLng);
                loadMeetingsOnMap ();
            });
        } catch (SecurityException e) {
            // 위치 권한이 없을 때 — 기본값으로 진행
            Log.e(TAG, "위치 권한 없음: " + e.getMessage());
            moveCameraTo(currentLat, currentLng);
            loadMeetingsOnMap ();
        }
    }

    /**
     * 지도 카메라를 특정 좌표로 이동
     * zoom 15 = 약 500m 반경이 한눈에 보이는 적절한 초기 줌 레벨
     */
    private void moveCameraTo(double lat , double lng) {
        if (naverMap == null) return;;
        CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(
                new LatLng(lat , lng), 15
        );
        naverMap.moveCamera(cameraUpdate);
    }

    // ──────────────────────────────────────────────
    // API 호출 & 마커
    // ──────────────────────────────────────────────

    /**
     * 현재 좌표 + 카테고리로 모임 목록 API 호출
     */

    private void loadMeetingsOnMap () {
        // "전체"는 서버에서 category 피라미터 없이 처리
        String categoryParam = "전체".equals(currentCategory) ? null : currentCategory;

        // 정렬 옵션 변환
        String orderByParam = "거리순".equals(currentSortOption) ? "distance" : null ;

        Call<ApiResponse<MeetingListResponse>> call = apiService.getMeetingList(
                null,
                categoryParam,
                "모집중",
                1,
                50,
                currentLat,
                currentLng,
                DEFAULT_RADIUS_KM,
                orderByParam
        );

        call.enqueue(new Callback<ApiResponse<MeetingListResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingListResponse>> call, Response<ApiResponse<MeetingListResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Meeting> meetings = response.body().getData().getMeetings();
                    Log.i(TAG, "모임 로드: " + meetings.size() + "개");

                    // 마커 업데이트
                    updateMarkersOnMap(meetings);

                    // BottomSheet 리스트 업데이트
                    allMeetingListAdapter.updateMeetings(meetings);
                } else {
                    Log.e(TAG, "API 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingListResponse>> call, Throwable t) {
                Log.e(TAG, "네트워크 오류: " + t.getMessage());
                Toast.makeText(getContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 지도 마커 업데이트
     * 기존 마커를 모두 제거하고 새 마커를 추가
     *
     */

    private void updateMarkersOnMap(List<Meeting> meetings) {
        if (naverMap == null) return;

        // 기존 마커 전부 지도에서 제거
        for (Marker marker : activeMarkers) {
            marker.setMap(null);
        }
        activeMarkers.clear();

        // 새 마커 추가
        for (Meeting meeting : meetings) {
            if (meeting.getLocation() == null) continue;

            String mapX = meeting.getLocation().getMap_x(); //경도
            String mapY = meeting.getLocation().getMap_y(); //위도

            // map_x, map_y가 String으로 저장되므로 Double로 변환 필요
            // try-catch: DB에 잘못된 값이 있을 때 앱 크래시 방지

            try {
                double lng = Double.parseDouble(mapX);
                double lat = Double.parseDouble(mapY);

                Marker marker = new Marker();
                marker.setPosition(new LatLng(lat , lng));
                marker.setCaptionText(meeting.getMeeting_title()); // 마커 위에 모임 제목 표시
                marker.setMap(naverMap); // 지도에 마커 표시

                activeMarkers.add(marker); // 나중에 제거할 수 있도록 목록에 추가

            } catch (NumberFormatException e) {
                Log.w(TAG, "좌표 파싱 실패: " + mapX + ", " + mapY);
            }
        }
    }

    // ──────────────────────────────────────────────
    // 카테고리 Dialog
    // ──────────────────────────────────────────────

    /**
     * 카테고리 선택 다이얼로그 표시
     * HomeFragment에서 카테고리 클릭 시 자동 오픈
     * @param category 사전 선택할 카테고리
     */

    private void showCategoryDialog(String category) {
        MapCategoryBottomSheetDialog dialog = new MapCategoryBottomSheetDialog();
        dialog.currentCategory = category; // 사전 선택 설정

        dialog.listener = selectedCategory -> {
            currentCategory = selectedCategory;
            loadMeetingsOnMap (); // 선택된 카테고리로 재조회
        };

        dialog.show(getChildFragmentManager() , "mapCategoryDialog");

    }

    // ──────────────────────────────────────────────
    // 외부에서 호출되는 메서드
    // ──────────────────────────────────────────────

    /**
     * HomeFragment 카테고리 클릭 → MainActivity → 여기 호출
     * naverMap이 준비됐으면 바로 처리, 아니면 pendingCategory에 저장
     */

    public void setFilter(String category) {
        if (naverMap == null) {
            // 지도가 아직 초기화하면
            pendingCategory = category;
            return;
        }
        currentCategory = category;
        showCategoryDialog(category);
        loadMeetingsOnMap ();
    }

    // ──────────────────────────────────────────────
    // UI 초기화
    // ──────────────────────────────────────────────
    private void initRecyclerView() {
        allMeetingListAdapter = new MapAllMeetingListAdapter();
        binding.rvMeetingCard.setAdapter(allMeetingListAdapter);
    }

    private void setupSearchBar() {
        binding.searchInputLayout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity() , SearchViewActivity.class);
            startActivity(intent);
        });
    }

    private void onBackButtonClick() {
        binding.btnBack.setOnClickListener(v -> {
            MainActivity mainActivity = (MainActivity) requireActivity();
            mainActivity.navigationToHome();
        });
    }

    private void initSortButton() {
        // 정렬 버튼
        binding.btnSort.setOnClickListener(v -> showSortButtonSheet());

        // 필터 버튼
        binding.btnFilter.setOnClickListener(v -> {
            MapFilterBottomSheetDialog dialog = new MapFilterBottomSheetDialog();
            dialog.show(getChildFragmentManager(), "mapFilterDialog");
        });
    }

    private void showSortButtonSheet() {
        MapSortBottomSheetDialog dialog = new MapSortBottomSheetDialog();
        dialog.currentSortOption = currentSortOption;
        dialog.listener = sortOption -> {
            currentSortOption = sortOption;
            updateSortButtonText();
            loadMeetingsOnMap  ();
        };
        dialog.show(getChildFragmentManager(), "mapSortDialog");
    }

    private void updateSortButtonText() {
        if (binding != null) {
            binding.tvSortOption.setText(currentSortOption);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // MapView 파괴 — OpenGL 컨텍스트 해제
        mapView.onDestroy();
        binding = null;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}

