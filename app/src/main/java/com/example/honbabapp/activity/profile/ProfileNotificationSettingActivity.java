package com.example.honbabapp.activity.profile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.request.fcm.UpdatePushSettingRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.user.UserProfileResponse;
import com.example.honbabapp.databinding.PageProfileNotificationSettingsBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 알림 설정 화면
 */
public class ProfileNotificationSettingActivity extends AppCompatActivity {

    private static final String TAG = "ProfileNotificationSetting";

    private static final String PREF_NAME = "honbabapp_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_PUSH_ENABLED = "push_enabled";

    private PageProfileNotificationSettingsBinding binding;
    private ApiService apiService;
    private SharedPreferences sharedPreferences;
    private int userId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageProfileNotificationSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        userId = sharedPreferences.getInt(KEY_USER_ID, -1);

        // 뒤로가기
        binding.btnBack.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed());

        // 화면 진입 시: prefs 값으로 먼저 즉시 표시(깜빡임 방지) → 서버 값으로 동기화
        // (기본값 true = DB push_enabled 기본값 1 과 동일)
        boolean cachedPushEnabled = sharedPreferences.getBoolean(KEY_PUSH_ENABLED, true);
        binding.switchPushEnabled.setChecked(cachedPushEnabled);

        binding.switchPushEnabled.setOnCheckedChangeListener((switchView, isChecked) ->
                updatePushSetting(isChecked));

        loadPushSettingFromServer();
    }

    /**
     * 서버(profile.php)의 push_enabled 값을 조회해 스위치 상태를 동기화한다.
     * 다른 기기에서 알림을 켜고/끄고 온 경우에도 이 화면에 들어오면 최신 값으로 갱신된다.
     *
     * 조회 실패 시에는 위에서 이미 표시한 prefs 캐시 값을 그대로 유지한다 (오프라인 대응).
     */
    private void loadPushSettingFromServer() {
        if (userId == -1) {
            Log.w(TAG, "user_id 없음 - 알림 설정 서버 조회 생략");
            return;
        }

        apiService.getUserProfile(userId).enqueue(new Callback<ApiResponse<UserProfileResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserProfileResponse>> call,
                                   Response<ApiResponse<UserProfileResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    UserProfileResponse profile = response.body().getData();
                    if (profile == null) return;

                    boolean serverPushEnabled = profile.getPush_enabled() == 1;
                    sharedPreferences.edit().putBoolean(KEY_PUSH_ENABLED, serverPushEnabled).apply();

                    // setChecked가 다시 리스너를 트리거해 update_push_setting.php를
                    // 호출하지 않도록, 서버 동기화 중에는 리스너를 잠시 해제한다.
                    binding.switchPushEnabled.setOnCheckedChangeListener(null);
                    binding.switchPushEnabled.setChecked(serverPushEnabled);
                    binding.switchPushEnabled.setOnCheckedChangeListener((switchView, isChecked) ->
                            updatePushSetting(isChecked));
                } else {
                    Log.e(TAG, "알림 설정 서버 조회 실패: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserProfileResponse>> call, Throwable t) {
                Log.e(TAG, "알림 설정 서버 조회 네트워크 오류: " + t.getMessage(), t);
            }
        });
    }

    /**
     * 알림 on/off 변경을 서버(update_push_setting.php)에 반영하고,
     * 성공 시 SharedPreferences에도 저장한다.
     *
     * 실패하면 사용자에게 안내하고 스위치를 이전 상태로 되돌린다.
     */
    private void updatePushSetting(boolean enabled) {
        if (userId == -1) {
            Log.w(TAG, "user_id 없음 - 알림 설정 변경 생략");
            return;
        }

        int pushEnabledValue = enabled ? 1 : 0;
        UpdatePushSettingRequest request = new UpdatePushSettingRequest(userId, pushEnabledValue);

        apiService.updatePushSetting(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    sharedPreferences.edit().putBoolean(KEY_PUSH_ENABLED, enabled).apply();
                    Log.d(TAG, "알림 설정 변경 완료: push_enabled=" + pushEnabledValue);
                } else {
                    Log.e(TAG, "알림 설정 변경 실패: HTTP " + response.code());
                    showUpdateFailed(enabled);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "알림 설정 변경 네트워크 오류: " + t.getMessage(), t);
                showUpdateFailed(enabled);
            }
        });
    }

    /**
     * 서버 반영 실패 시 안내 토스트를 띄우고, 스위치를 이전 상태로 되돌린다.
     * setChecked 호출이 다시 리스너를 트리거하지 않도록 잠시 해제한다.
     */
    private void showUpdateFailed(boolean attemptedValue) {
        Toast.makeText(this, "알림 설정 변경에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();

        binding.switchPushEnabled.setOnCheckedChangeListener(null);
        binding.switchPushEnabled.setChecked(!attemptedValue);
        binding.switchPushEnabled.setOnCheckedChangeListener((switchView, isChecked) ->
                updatePushSetting(isChecked));
    }
}