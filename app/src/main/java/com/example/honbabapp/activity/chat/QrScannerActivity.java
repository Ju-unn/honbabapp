package com.example.honbabapp.activity.chat;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.databinding.ActivityQrScannerBinding;
import com.journeyapps.barcodescanner.CaptureManager;

/**
 * 커스텀 QR 스캐너 액티비티
 *
 * 역할:
 *   - DecoratedBarcodeView 로 커스텀 스캔 UI 표시
 *   - CaptureManager 로 QR 인식 처리
 *   - 플래시 토글 버튼 제공
 */
public class QrScannerActivity extends AppCompatActivity {

    private ActivityQrScannerBinding binding;
    private CaptureManager captureManager;

    // 플래시 현재 상태 (false = 꺼짐)
    private boolean isFlashOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // CaptureManager: ZXing 의 QR 인식 로직을 DecoratedBarcodeView 와 연결
        captureManager = new CaptureManager(this, binding.decoratedBarcodeView);
        captureManager.initializeFromIntent(getIntent(), savedInstanceState);
        captureManager.decode();

        // 뒤로가기 버튼
        binding.btnScannerBack.setOnClickListener(v -> finish());

        // 플래시 토글 버튼
        binding.btnScannerFlash.setOnClickListener(v -> {
            if (!isFlashOn) {
                binding.decoratedBarcodeView.setTorchOn();
                binding.btnScannerFlash.setText("플래시 끄기");
                isFlashOn = true;
            } else {
                binding.decoratedBarcodeView.setTorchOff();
                binding.btnScannerFlash.setText("플래시 켜기");
                isFlashOn = false;
            }
        });
    }

    // ─────────────────────────────────────────
    // CaptureManager 생명주기 위임 (필수)
    // 이걸 빠뜨리면 카메라가 백그라운드에서도 계속 켜져있거나
    // 앱 복귀 시 검은 화면이 뜨는 문제가 생김
    // ─────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        captureManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        captureManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        captureManager.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        captureManager.onSaveInstanceState(outState);
    }

    // 카메라 권한 요청 결과도 CaptureManager 에 전달
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        captureManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}