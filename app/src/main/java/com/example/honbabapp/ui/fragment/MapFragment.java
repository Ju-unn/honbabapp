package com.example.honbabapp.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private Double pendingLat = null;
    private Double pendingLng = null;

    // onMapReady에서 호출 , map 초기화 안 될 수도 있으니 바로 호출하면 에러 날 확률방지
    private String pendingCategory = null;

    // isInitialLoad: 지도 초기 렌더링 시 CameraIdleListener가 즉시 발동해서
    // API를 두 번 호출하는 문제 방지용 플래그
    private boolean isInitialLoad = true;

    /// Naver /////////////////////////////////////////////////////////////////////////////////



    /// 상태관리 /////////////////////////////////////////////////////////////////////////////////
    private Set<String> currentFilterCategories = new HashSet<>();
    private int currentMinPrice = 0;
    private int currentMaxPrice = 100000;
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
        setupBottomSheetCallback();
    }


    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        Log.i(TAG, "onMapReady");
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
            String cat = pendingCategory;
            pendingCategory = null;

            currentFilterCategories = new HashSet<>();
            if (!"전체".equals(cat)) {
                currentFilterCategories.add(cat);
            }
        }

        if (pendingLat != null && pendingLng != null) {
            double lat = pendingLat;
            double lng = pendingLng;
            pendingLat = null;
            pendingLng = null;
            moveCameraTo(lat , lng);
            loadMeetingsOnMap();
            return;
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
        String categories = null;
        if (!currentFilterCategories.isEmpty()) {
            categories = String.join(",", currentFilterCategories);
        }

        // 가격 필터: 기본값(0원, 100000원) 그대로면 null → 필터 비활성
        Integer minPrice = (currentMinPrice > 0) ? currentMinPrice : null;
        Integer maxPrice = (currentMaxPrice < 100000) ? currentMaxPrice : null;

        // 정렬 옵션 변환
        String orderBy = "거리순".equals(currentSortOption) ? "distance" : null ;

        Call<ApiResponse<MeetingListResponse>> call = apiService.getMeetingList(
                null,
                categories,
                "모집중",
                1,
                50,
                currentLat,
                currentLng,
                DEFAULT_RADIUS_KM,
                orderBy,
                minPrice,
                maxPrice
        );

        call.enqueue(new Callback<ApiResponse<MeetingListResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingListResponse>> call, Response<ApiResponse<MeetingListResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Meeting> meetings = response.body().getData().getMeetings();
                    Log.i(TAG, "모임 로드: " + meetings.size() + "개");

                    // 인기순
                    if ("인기순".equals(currentSortOption)) {
                        meetings.sort((a , b) -> b.getMeeting_view_count() - a.getMeeting_view_count());
                    }

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
                Log.w(TAG, "좌표 파싱 실패: " + meeting.getLocation().getMap_x());
            }
        }
    }

    // ──────────────────────────────────────────────
    // 필터 Dialog
    // ──────────────────────────────────────────────

    private void showFilterDialog(@Nullable String preSelectedCategory) {
        MapFilterBottomSheetDialog dialog = new MapFilterBottomSheetDialog();
        dialog.preSelectedCategory = preSelectedCategory;

        // 이전 필터 상태
        dialog.initialSelectedCategories = new HashSet<>(currentFilterCategories);
        dialog.initialMinPrice = currentMinPrice;
        dialog.initialMaxPrice = currentMaxPrice;


        dialog.listener = ((categories, minPrice, maxPrice, date, time) -> {
            currentFilterCategories = new HashSet<>(categories);
            currentMinPrice = minPrice;
            currentMaxPrice = maxPrice;
            loadMeetingsOnMap();
        });
        dialog.show(getChildFragmentManager(), "mapFilterDialog");
    }

    // ──────────────────────────────────────────────
    // 외부에서 호출 (HomeFragment → MainActivity → 여기)
    // ──────────────────────────────────────────────

    public void moveToLocation(double lat , double lng) {
        currentLat = lat;
        currentLng = lng;

        if (naverMap == null) {
            // 지도 아직 준비 안 됐으면 pending 처리
            pendingLat = lat;
            pendingLng = lng;
            return;
        }

        moveCameraTo(lat , lng);
        loadMeetingsOnMap();
    }

    public void setFilter(String category) {
        if (naverMap == null){
            pendingCategory = category;
            return;
        }

        // 카테고리 선택시 즉시 반영
        currentFilterCategories = new HashSet<>();
        if (!"전체".equals(category)) {
            currentFilterCategories.add(category);
        }
        loadMeetingsOnMap();
    }

    // ──────────────────────────────────────────────
    // Hot미팅
    // ──────────────────────────────────────────────
    public void setFilterHot() {
        if (naverMap == null) {
            pendingCategory = "전체";
            return;
        }
        currentFilterCategories = new HashSet<>();
        currentSortOption = "인기순";
        updateSortButtonText();
        loadMeetingsOnMap();
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
        binding.btnFilter.setOnClickListener(v -> showFilterDialog(null));
    }

    private void setupBottomSheetCallback() {
        BottomSheetBehavior<LinearLayout> behavior =
                BottomSheetBehavior.from(binding.bottomSheet);

        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (naverMap == null) return;

                // 현재 BottomSheet 실제 높이 계산
                // slideOffset : 0.0 = peekHeight, 1.0 = .완전히 펼친 상태
                int sheetHeight = bottomSheet.getHeight();
                int peekHeight = behavior.getPeekHeight();

                // 현재 보이는 높이
                int visibleHeight = (int) (peekHeight + (sheetHeight - peekHeight) * slideOffset);

                // Naver 지도 UI 버튼 위치를 visibleHeight 만큼 위로 이동
                naverMap.setContentPadding(0 , 0 , 0, visibleHeight);
            }

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }
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

