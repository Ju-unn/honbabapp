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
import com.example.honbabapp.activity.meeting.MeetingDetailActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.MeetingListResponse;
import com.example.honbabapp.databinding.FragmentMapBinding;
import com.example.honbabapp.ui.adapter.MapAllMeetingListAdapter;
import com.example.honbabapp.ui.dialog.MapClusterBottomSheetDialog;
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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;

import com.naver.maps.map.clustering.Clusterer;
import com.naver.maps.map.clustering.ClusteringKey;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    // Clusterer: SDK 내장 클러스터링 관리자
    private Clusterer<MeetingKey> clusterer;

    // debounce용 Handler (카메라 이동 시 API 과다 호출 방지)
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    private List<Meeting> currentMapMeetings = new ArrayList<>();

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

        // ── Clusterer 초기화 ──────────────────────────────
        clusterer = new Clusterer.Builder<MeetingKey>()
                // 클러스트 마커 (여러 개 묶인 상태) 스타일
                        .clusterMarkerUpdater((info , marker) -> {
                            marker.setIcon(OverlayImage.fromBitmap(makeClusterIcon(info.getSize())));
                            marker.setCaptionText("");
                            marker.setZIndex(1);

                            final int clusterSize = info.getSize(); // 클러스터 안에 몇 개 묶였는지

                            marker.setOnClickListener(o -> {
                                LatLng clusterPos = marker.getPosition(); // 클러스터 중심 좌표
                                List<Meeting> clusterMeetings = extractClusterMeetings(clusterPos, clusterSize);

                                MapClusterBottomSheetDialog dialog = new MapClusterBottomSheetDialog();
                                dialog.meetings = clusterMeetings;
                                dialog.show(getChildFragmentManager(), "clusterDialog");
                                return true;
                            });
                        })
                // 개별 마커 스타일 + 클릭 이벤트
                        .leafMarkerUpdater((info , marker) -> {
                            Meeting meeting = ((MeetingKey) info.getKey()).getMeeting();

                            // 기본 아이콘으로 초기화 (클러스터 아이콘이 남아있는 현상 방지)
                            marker.setIcon(Marker.DEFAULT_ICON);

                            marker.setCaptionText(meeting.getMeeting_title());
                            marker.setOnClickListener(o -> {
                                Intent intent = new Intent(requireContext(), MeetingDetailActivity.class);
                                intent.putExtra("meeting_id", meeting.getMeeting_id());
                                startActivity(intent);
                                return true;
                            });
                        })
                        .build();
        clusterer.setMap(naverMap);

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

            LatLng center = naverMap.getCameraPosition().target;
            currentLat = center.latitude;
            currentLng = center.longitude;

            // ── debounce 시작 ──────────────────────

            // 1단계: 이미 예약된 게 있으면 취소
            if (debounceRunnable != null) {
                debounceHandler.removeCallbacks(debounceRunnable);
            }

            // 2단계: 새로 예약 (500ms 후에 API 호출)
            debounceRunnable = new Runnable() {
                @Override
                public void run() {
                    loadMeetingsOnMap();
                }
            };
            debounceHandler.postDelayed(debounceRunnable, 500);

            // ── debounce 끝 ──────────────────────
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

    /**
     * 클러스터 중심 좌표에서 가장 가까운 N개의 모임을 반환.
     * Naver Maps SDK가 getLeafItems()를 제공하지 않으므로
     * 거리 정렬로 해당 클러스터의 모임 목록을 추출.
     *
     * @param clusterPos  클러스터 마커 중심 좌표
     * @param clusterSize 클러스터에 포함된 모임 수
     */
    private List<Meeting> extractClusterMeetings(LatLng clusterPos, int clusterSize) {
        List<Meeting> sorted = new ArrayList<>(currentMapMeetings);

        sorted.sort((a, b) -> {
            double dA = squaredDistToCluster(a, clusterPos);
            double dB = squaredDistToCluster(b, clusterPos);
            return Double.compare(dA, dB);
        });

        int count = Math.min(clusterSize, sorted.size());
        return new ArrayList<>(sorted.subList(0, count));
    }

    /** 모임 위치 ↔ 클러스터 중심 간 제곱 거리 (비교용, sqrt 불필요) */
    private double squaredDistToCluster(Meeting m, LatLng pos) {
        if (m.getLocation() == null) return Double.MAX_VALUE;
        try {
            double lat = Double.parseDouble(m.getLocation().getMap_y());
            double lng = Double.parseDouble(m.getLocation().getMap_x());
            double dLat = lat - pos.latitude;
            double dLng = lng - pos.longitude;
            return dLat * dLat + dLng * dLng;
        } catch (NumberFormatException e) {
            return Double.MAX_VALUE;
        }
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

                    List<Meeting> rawMeetings = response.body().getData().getMeetings();

                    // 만료된 미팅 클라이언트 필터링
                    List<Meeting> meetings = new ArrayList<>();
                    for (Meeting m : rawMeetings) {
                        if (!m.isExpired()) {
                            meetings.add(m);
                        }
                    }

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
     */
    private void updateMarkersOnMap(List<Meeting> meetings) {
        currentMapMeetings = new ArrayList<>(meetings);
        if (clusterer == null) return;

        // 기존 클러스터 데이터 전부 제거
        clusterer.clear();

        // Meeting → MeetingKey 변환 후 일괄 추가
        Map<MeetingKey, Void> keyMap = new HashMap<>();
        for (Meeting meeting : meetings) {
            if (meeting.getLocation() == null) continue;
            try {
                Double.parseDouble(meeting.getLocation().getMap_y());
                Double.parseDouble(meeting.getLocation().getMap_x());
                keyMap.put(new MeetingKey(meeting), null);
            } catch (NumberFormatException e) {
                Log.w(TAG, "좌표 파싱 실패: " + meeting.getMeeting_id());
            }
        }
        clusterer.addAll(keyMap);
    }

    /**
     * 클러스터 마커 아이콘 생성
     * 숫자가 표시된 원형 배지
     */
    private Bitmap makeClusterIcon(int count) {
        int sizePx = 96;
        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // 1단계: 주황색 원 그리기
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.parseColor("#FF6B35"));
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, circlePaint);

        // 2단계: 흰색 테두리 그리기
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(5f);
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, (sizePx / 2f) - 3f, borderPaint);

        // 3단계: 숫자 텍스트 그리기
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(count >= 10 ? 28f : 34f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        float textY = sizePx / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(String.valueOf(count), sizePx / 2f, textY, textPaint);

        return bmp;
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

    // ──────────────────────────────────────────────
    // MeetingKey - Clusterer용 키 클래스
    // ──────────────────────────────────────────────
    private static class MeetingKey implements ClusteringKey {
        private final Meeting meeting;

        MeetingKey(Meeting meeting) {
            this.meeting = meeting;
        }

        @NonNull
        @Override
        public LatLng getPosition() {
            double lat = Double.parseDouble(meeting.getLocation().getMap_y());
            double lng = Double.parseDouble(meeting.getLocation().getMap_x());
            return new LatLng(lat, lng);
        }

        public Meeting getMeeting() {
            return meeting;
        }
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
        if (clusterer != null) {
            clusterer.setMap(null);  // 지도에서 분리 → 메모리 해제
            clusterer = null;
        }
        if (debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);  // 대기 중인 API 호출 취소
        }
        mapView.onDestroy();
        binding = null;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }


}



