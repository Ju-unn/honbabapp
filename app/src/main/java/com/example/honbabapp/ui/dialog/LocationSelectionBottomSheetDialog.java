package com.example.honbabapp.ui.dialog;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.honbabapp.BuildConfig;
import com.example.honbabapp.api.NaverMapsApiService;
import com.example.honbabapp.api.NaverMapsRetrofitClient;
import com.example.honbabapp.api.data.response.naver.NaverReverseGeoResponse;
import com.example.honbabapp.databinding.BottomsheetLocationSelectionBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationSelectionBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String TAG = "LocationBottomSheet";

    // HomeFragment에서 세팅
    public String currentLocationText;

    // 위치 선택 완료 콜백
    public interface OnLocationSelectedListener {
        void onLocationSelected(String sigungu, String dong);
    }

    public OnLocationSelectedListener listener;
    private BottomsheetLocationSelectionBinding binding;
    private FusedLocationProviderClient fusedClient;
    private NaverMapsApiService naverMapsApiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomsheetLocationSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        naverMapsApiService = NaverMapsRetrofitClient.getInstance().getApiService();

        setupCurrentLocation();
        setupGpsButton();
    }

    /**
     * 현재 저장된 위치 텍스트 표시
     */
    private void setupCurrentLocation() {
        if (currentLocationText != null) {
            binding.tvCurrentLocation.setText("기존 위치 : " + currentLocationText);
        }
    }

    /**
     * GPS 버튼: FusedLocation → Geocoder → 콜백 (HomeFragment 패턴 재사용)
     */
    @SuppressLint("MissingPermission")
    private void setupGpsButton() {
        binding.btnGpsDetect.setOnClickListener(v -> {
            binding.btnGpsDetect.setEnabled(false);
            binding.btnGpsDetect.setText("위치 검색중");

            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location == null) {
                            Log.w(TAG, "location null");
                            resetButton();
                            return;
                        }

                        String coords = location.getLongitude() + "," + location.getLatitude();
                        Log.d(TAG, "좌표: " + coords);

                        Call<NaverReverseGeoResponse> call = naverMapsApiService.reverseGeocode(
                                BuildConfig.NAVER_MAPS_CLIENT_ID,
                                BuildConfig.NAVER_MAPS_CLIENT_SECRET,
                                coords, "json", "admcode"
                        );

                        call.enqueue(new Callback<NaverReverseGeoResponse>() {
                            @Override
                            public void onResponse(Call<NaverReverseGeoResponse> call,
                                                   Response<NaverReverseGeoResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    String address = response.body().getFormattedAddress();
                                    Log.d(TAG, "주소: " + address);

                                    if (address != null && !address.isEmpty()) {
                                        // 주소 파싱: "서울특별시 관악구 신림동" → sigungu="관악구", dong="신림동"
                                        String[] parts = address.trim().split(" ");
                                        String sigungu = parts[0];
                                        String dong    = parts.length >= 2 ? parts[1] : parts[0];

                                        if (listener != null) {
                                            listener.onLocationSelected(sigungu, dong);
                                        }
                                        dismiss();
                                    } else {
                                        resetButton();
                                    }
                                } else {
                                    Log.e(TAG, "Naver API 실패: " + response.code());
                                    resetButton();
                                }
                            }

                            @Override
                            public void onFailure(Call<NaverReverseGeoResponse> call, Throwable t) {
                                Log.e(TAG, "네트워크 오류: " + t.getMessage());
                                resetButton();
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "GPS 실패: " + e.getMessage());
                        resetButton();
                    });
        });
    }

    private void resetButton() {
        if (!isAdded()) return;
        binding.btnGpsDetect.setEnabled(true);
        binding.btnGpsDetect.setText("현재 위치로 설정하기");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}