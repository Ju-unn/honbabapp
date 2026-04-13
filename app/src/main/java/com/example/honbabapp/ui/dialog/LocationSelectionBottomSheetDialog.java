package com.example.honbabapp.ui.dialog;

import android.annotation.SuppressLint;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.honbabapp.databinding.BottomsheetLocationSelectionBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;
import java.util.Locale;

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

            fusedClient.getLastLocation().addOnSuccessListener(location -> {
                if (location == null) {
                    resetButton();
                    return;
                }

                try {
                    Geocoder geocoder = new Geocoder(requireContext(), new Locale("ko", "KR"));
                    List<Address> addresses = geocoder.getFromLocation(
                            location.getLatitude(), location.getLongitude(), 1);

                    if (addresses == null || addresses.isEmpty()) {
                        resetButton();
                        return;
                    }

                    Address address = addresses.get(0);
                    String sigungu = address.getSubAdminArea(); // 예: 관악구
                    String dong = address.getThoroughfare(); // 예: 신림동
                    if (dong == null || dong.isEmpty()) {
                        dong = address.getSubLocality();
                    }
                    if (sigungu == null || sigungu.isEmpty()) {
                        resetButton();
                        return;
                    }

                    Log.i(TAG, "GPS 위치: " + sigungu + " " + dong);

                    if (listener != null) {
                        listener.onLocationSelected(sigungu, dong != null ? dong : sigungu);
                    }
                    dismiss();

                } catch (Exception e) {
                    Log.e(TAG, "Geocoder 오류: " + e.getMessage());
                    resetButton();
                }
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