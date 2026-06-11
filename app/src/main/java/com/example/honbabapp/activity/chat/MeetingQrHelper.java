package com.example.honbabapp.activity.chat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.data.meeting.QrCodeData;
import com.example.honbabapp.api.data.meeting.QrScanResultData;
import com.example.honbabapp.api.data.meeting.QrStatusData;
import com.example.honbabapp.api.data.request.meeting.QrGenerateRequest;
import com.example.honbabapp.api.data.request.meeting.QrScanRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.databinding.DialogMeetingQrBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.ScanOptions;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * QR 출석 인증 관련 로직을 ChatRoomActivity 로부터 분리한 헬퍼 클래스
 *
 * 담당 역할:
 *   - qr_status API 호출 → 버튼 동작 결정
 *   - 호스트: qr_generate API → ZXing 비트맵 생성 → 다이얼로그 표시
 *   - 참여자: ZXing 카메라 스캔 → FusedLocation 위치 획득 → qr_scan API 호출
 *
 * 사용법 (ChatRoomActivity 에서):
 *   meetingQrHelper = new MeetingQrHelper(this, apiService, meetingId, myUserId, scanLauncher, cameraPermissionLauncher);
 *   meetingQrHelper.loadQrStatus();
 *   binding.btnQr.setOnClickListener(v -> meetingQrHelper.onQrButtonClicked());
 *
 *   // cameraPermissionLauncher 콜백에서:
 *   if (granted) {
 *       if (meetingQrHelper != null && meetingQrHelper.isWaitingForCameraPermission()) {
 *           meetingQrHelper.launchQrScanner();
 *       } else {
 *           launchCamera();
 *       }
 *   }
 */
public class MeetingQrHelper {

    private static final String TAG = "MeetingQrHelper";

    // QR 이미지 크기 (픽셀)
    private static final int QR_IMAGE_SIZE = 600;

    private final Activity activity;
    private final ApiService apiService;
    private final int meetingId;
    private final int myUserId;

    // ZXing 카메라 스캔 런처 (ChatRoomActivity 에서 주입)
    private final ActivityResultLauncher<ScanOptions> scanLauncher;

    // 카메라 권한 런처 (ChatRoomActivity 의 기존 런처 재사용)
    private final ActivityResultLauncher<String> cameraPermissionLauncher;

    // qr_status API 응답 캐시 (버튼 클릭 시 재사용)
    private QrStatusData cachedQrStatus = null;

    // 위치 관련
    private final FusedLocationProviderClient fusedLocationClient;

    // 스캔 후 인증 처리 중에 쓸 토큰 임시 저장
    private String pendingScannedToken = null;

    // 카메라 권한 허용 후 QR 스캔을 실행해야 하는지 여부
    // true : 권한 요청이 QR 스캔 목적 → 허용 시 launchQrScanner() 호출
    // false: 권한 요청이 일반 카메라 목적 → 허용 시 launchCamera() 호출 (Activity 담당)
    private boolean isWaitingForCameraPermission = false;

    public MeetingQrHelper(
            Activity activity,
            ApiService apiService,
            int meetingId,
            int myUserId,
            ActivityResultLauncher<ScanOptions> scanLauncher,
            ActivityResultLauncher<String> cameraPermissionLauncher
    ) {
        this.activity = activity;
        this.apiService = apiService;
        this.meetingId = meetingId;
        this.myUserId = myUserId;
        this.scanLauncher = scanLauncher;
        this.cameraPermissionLauncher = cameraPermissionLauncher;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
    }

    // ─────────────────────────────────────────────────────────────
    // 1. 상태 조회 — onCreate 에서 호출
    //    qr_status API → cachedQrStatus 에 저장
    // ─────────────────────────────────────────────────────────────

    public void loadQrStatus() {
        apiService.getQrStatus(meetingId, myUserId)
                .enqueue(new Callback<ApiResponse<QrStatusData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<QrStatusData>> call,
                                           Response<ApiResponse<QrStatusData>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {
                            cachedQrStatus = response.body().getData();
                            Log.d(TAG, "QR 상태 로드 완료: " + cachedQrStatus.getState());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<QrStatusData>> call, Throwable throwable) {
                        Log.e(TAG, "QR 상태 조회 실패: " + throwable.getMessage());
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────
    // 2. QR 버튼 클릭 처리 — btnQr 클릭 리스너에서 호출
    // ─────────────────────────────────────────────────────────────

    public void onQrButtonClicked() {
        if (cachedQrStatus == null) {
            Toast.makeText(activity, "잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show();
            loadQrStatus();
            return;
        }

        if (cachedQrStatus.isIs_host()) {
            handleHostQrButtonClicked();
        } else {
            handleParticipantQrButtonClicked();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. 호스트 흐름
    // ─────────────────────────────────────────────────────────────

    private void handleHostQrButtonClicked() {
        String state = cachedQrStatus.getState();

        switch (state) {
            case "before_window":
                Toast.makeText(activity,
                        "모임 24시간 전부터 QR을 생성할 수 있습니다",
                        Toast.LENGTH_LONG).show();
                break;

            case "available":
                requestQrGenerate();
                break;

            case "generated":
                showQrDialog(
                        cachedQrStatus.getCode_token(),
                        cachedQrStatus.getExpires_at(),
                        false
                );
                break;

            case "expired":
                Toast.makeText(activity, "종료된 모임입니다", Toast.LENGTH_SHORT).show();
                break;

            default:
                Toast.makeText(activity, "알 수 없는 상태입니다", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void requestQrGenerate() {
        QrGenerateRequest generateRequest = new QrGenerateRequest(meetingId, myUserId);

        apiService.generateQrCode(generateRequest)
                .enqueue(new Callback<ApiResponse<QrCodeData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<QrCodeData>> call,
                                           Response<ApiResponse<QrCodeData>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            QrCodeData qrCodeData = response.body().getData();

                            // 상태 캐시 업데이트 (다음 클릭부터 generated 분기 탐)
                            cachedQrStatus.setState("generated");
                            cachedQrStatus.setCode_token(qrCodeData.getCode_token());
                            cachedQrStatus.setExpires_at(qrCodeData.getExpires_at());

                            showQrDialog(
                                    qrCodeData.getCode_token(),
                                    qrCodeData.getExpires_at(),
                                    qrCodeData.isIs_newly_generated()
                            );
                        } else {
                            String errorMessage = (response.body() != null)
                                    ? response.body().getMessage()
                                    : "QR 생성에 실패했습니다";
                            Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<QrCodeData>> call, Throwable throwable) {
                        Toast.makeText(activity,
                                "네트워크 오류: " + throwable.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showQrDialog(String codeToken, String expiresAt, boolean isNewlyCreated) {
        Bitmap qrBitmap = generateQrBitmap(codeToken);
        if (qrBitmap == null) {
            Toast.makeText(activity, "QR 이미지 생성에 실패했습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        DialogMeetingQrBinding dialogBinding = DialogMeetingQrBinding.inflate(
                LayoutInflater.from(activity)
        );

        dialogBinding.tvQrDialogTitle.setText(isNewlyCreated ? "QR이 생성되었습니다" : "모임 출석 QR");
        dialogBinding.ivQrCode.setImageBitmap(qrBitmap);
        dialogBinding.tvQrExpiresAt.setText("만료 시각: " + expiresAt);

        AlertDialog qrDialog = new AlertDialog.Builder(activity)
                .setView(dialogBinding.getRoot())
                .setCancelable(true)
                .create();

        dialogBinding.btnQrDialogClose.setOnClickListener(v -> qrDialog.dismiss());

        qrDialog.show();
    }

    private Bitmap generateQrBitmap(String codeToken) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            return barcodeEncoder.encodeBitmap(
                    codeToken,
                    BarcodeFormat.QR_CODE,
                    QR_IMAGE_SIZE,
                    QR_IMAGE_SIZE
            );
        } catch (Exception exception) {
            Log.e(TAG, "QR 비트맵 생성 실패: " + exception.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 4. 참여자 흐름
    // ─────────────────────────────────────────────────────────────

    private void handleParticipantQrButtonClicked() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // 권한 없음 → 권한 요청 다이얼로그 띄우기
            // isWaitingForCameraPermission = true 로 설정해서
            // ChatRoomActivity 의 cameraPermissionLauncher 콜백이
            // launchCamera() 대신 launchQrScanner() 를 호출하도록 구분
            isWaitingForCameraPermission = true;
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        launchQrScanner();
    }

    /**
     * QR 스캔 실행
     * ChatRoomActivity 의 cameraPermissionLauncher 콜백에서도 호출됨
     */
    public void launchQrScanner() {
        isWaitingForCameraPermission = false;
        ScanOptions scanOptions = new ScanOptions();
        scanOptions.setOrientationLocked(false);
        scanOptions.setBeepEnabled(false);
        scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        scanOptions.setCaptureActivity(QrScannerActivity.class);
        scanLauncher.launch(scanOptions);
    }

    /**
     * 카메라 권한 요청이 QR 스캔 목적인지 여부
     * ChatRoomActivity 의 cameraPermissionLauncher 콜백에서 분기 판단용
     */
    public boolean isWaitingForCameraPermission() {
        return isWaitingForCameraPermission;
    }

    /**
     * ZXing 스캔 결과 처리 — ChatRoomActivity 의 qrScanLauncher 콜백에서 호출
     */
    public void onScanResult(String scannedToken) {
        pendingScannedToken = scannedToken;
        requestCurrentLocationAndSubmit();
    }

    // ─────────────────────────────────────────────────────────────
    // 5. 위치 획득 → qr_scan API 호출
    // ─────────────────────────────────────────────────────────────

    private void requestCurrentLocationAndSubmit() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(activity, "위치를 확인하는 중...", Toast.LENGTH_SHORT).show();

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMaxUpdates(1)
                .build();

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        fusedLocationClient.removeLocationUpdates(this);

                        if (locationResult.getLastLocation() == null) {
                            activity.runOnUiThread(() ->
                                    Toast.makeText(activity,
                                            "위치를 가져올 수 없습니다. 다시 시도해주세요",
                                            Toast.LENGTH_SHORT).show()
                            );
                            return;
                        }

                        Location currentLocation = locationResult.getLastLocation();
                        submitQrScan(
                                pendingScannedToken,
                                currentLocation.getLatitude(),
                                currentLocation.getLongitude()
                        );
                    }
                },
                Looper.getMainLooper()
        );
    }

    private void submitQrScan(String codeToken, double latitude, double longitude) {
        QrScanRequest scanRequest = new QrScanRequest(codeToken, myUserId, latitude, longitude);

        apiService.scanQrCode(scanRequest)
                .enqueue(new Callback<ApiResponse<QrScanResultData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<QrScanResultData>> call,
                                           Response<ApiResponse<QrScanResultData>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            int updatedCount = response.body().getData().getParticipation_count();
                            activity.runOnUiThread(() -> showScanSuccessDialog(updatedCount));
                        } else {
                            String errorMessage = (response.body() != null)
                                    ? response.body().getMessage()
                                    : "출석 인증에 실패했습니다";
                            activity.runOnUiThread(() ->
                                    Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show()
                            );
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<QrScanResultData>> call, Throwable throwable) {
                        activity.runOnUiThread(() ->
                                Toast.makeText(activity,
                                        "네트워크 오류: " + throwable.getMessage(),
                                        Toast.LENGTH_SHORT).show()
                        );
                    }
                });
    }

    private void showScanSuccessDialog(int updatedParticipationCount) {
        new AlertDialog.Builder(activity)
                .setTitle("출석 인증 완료")
                .setMessage("출석이 인증되었습니다!\n누적 참여 횟수: " + updatedParticipationCount + "회")
                .setPositiveButton("확인", null)
                .show();
    }
}