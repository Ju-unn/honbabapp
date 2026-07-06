package com.example.honbabapp.activity.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.activity.LoginActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.request.LogoutRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.network.SocketManager;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileLogoutActivity extends AppCompatActivity {

    private static final String TAG = "ProfileLogoutActivity";
    private static final String PREF_NAME  = "honbabapp_prefs";
    private static final String KEY_USER_ID = "user_id";

    private ApiService apiService;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //별도 레이아웃 없음 - 다이얼로그만 표시
        apiService = RetrofitClient.getClient().create(ApiService.class);
        showLogoutConfirmDialog();
    }

    // 확인 다이얼로그
    private void showLogoutConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("로그아웃")
                .setMessage("로그아웃 하시겠습니까?")
                .setPositiveButton("로그아웃", (dialog, which) -> requestLogoutFromServer())
                .setNegativeButton("취소", (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    // 서버에 fcm token null 처리 요청
    private void requestLogoutFromServer() {
        int userId = getSharedPreferences(PREF_NAME, 0).getInt(KEY_USER_ID, -1);

        if (userId == -1) {
            // 로그인 정보 자체가 없으면 바로 로컬 정리
            Log.w(TAG, "user_id 없음 — 로컬 정리만 수행");
            performLocalLogout();
            return;
        }

        LogoutRequest request = new LogoutRequest();
        apiService.logout(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    Log.d(TAG, "서버 로그아웃 완료 (user_id=" + userId + ")");
                } else {
                    Log.w(TAG, "서버 로그아웃 응답 실패 — 로컬 정리는 계속 진행");
                }
                // 성공·실패 관계없이 로컬 정리 수행
                deleteFirebaseToken();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                // 네트워크 오류여도 로컬 정리는 반드시 수행
                Log.e(TAG, "서버 로그아웃 네트워크 오류: " + t.getMessage());
                deleteFirebaseToken();
            }
        });
    }

    // fireBase 기기 토큰 삭제
    private void deleteFirebaseToken() {
        FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase 토큰 삭제 완료");
                    } else {
                        Log.w(TAG, "Firebase 토큰 삭제 실패 — 로컬 정리는 계속 진행");
                    }
                    performLocalLogout();
                });
    }

    // 소캣해제 sharedPreferences 초기화 + 화면이동
    private void performLocalLogout() {
        // 소켓 연결해제
        SocketManager.getInstance().disconnect();
        Log.d(TAG, "소켓 연결 해제 완료");

        // SharedPreferences 초기화
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, 0);
        preferences.edit().clear().apply();
        Log.d(TAG, "SharedPreferences 초기화 완료");

        Toast.makeText(this, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();

        // Step 5: LoginActivity 로 이동 — 태스크 스택 전체 클리어
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
    }





}
