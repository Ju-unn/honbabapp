package com.example.honbabapp.activity.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.honbabapp.activity.LoginActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.request.user.DeleteAccountRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.databinding.PageProfileDeleteAccountBinding;
import com.example.honbabapp.util.InsetsUtils;
import com.example.honbabapp.util.ToastUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileDeleteAccountActivity extends AppCompatActivity {

    private static final String TAG = "ProfileDeleteAccount";
    private PageProfileDeleteAccountBinding binding;
    private ApiService apiService;

    private static final String PREF_NAME = "honbabapp_prefs";
    private static final String KEY_USER_ID = "user_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageProfileDeleteAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsetsWithIme(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        // 뒤로가기
        binding.btnBack.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed());

        // 취소 버튼
        binding.btnCancel.setOnClickListener(v -> finish());

        // 체크박스 탈퇴 버튼 화성 / 비활성
        binding.checkboxAgree.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 색상은 button_danger_background 상태 selector가 처리 (alpha 트릭 제거)
                binding.btnDeleteAccount.setEnabled(isChecked);
            }
        });

        // 기타 선택시 기타 입력란 표시
        binding.radioGroupReason.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == binding.radioReason4.getId()) {
                    // 기타 선택시 edittext 표시
                    binding.etOtherReason.setVisibility(View.VISIBLE);
                } else {
                    binding.etOtherReason.setVisibility(View.GONE);
                }
            }
        });

        // 탈퇴하기 버튼
        binding.btnDeleteAccount.setOnClickListener(v -> deleteAccount());
    }

    private void deleteAccount() {
        binding.btnDeleteAccount.setEnabled(false);

        DeleteAccountRequest request = new DeleteAccountRequest();

        apiService.deleteAccount(request)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            // SharedPreferences 초기화
                            getSharedPreferences(PREF_NAME , 0)
                                    .edit()
                                    .clear()
                                    .apply();

                            ToastUtil.makeText(ProfileDeleteAccountActivity.this,
                                    "계정이 삭제되었습니다", Toast.LENGTH_SHORT).show();

                            // LoginActivity로 이동 모든 스택 클리어
                            Intent intent = new Intent(ProfileDeleteAccountActivity.this , LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            binding.btnDeleteAccount.setEnabled(true);
                            String message = response.body() != null
                                    ? response.body().getMessage() : "탈퇴 실패";
                            // 진행 중 모임/체험단 차단은 사용자가 조치해야 하므로 다이얼로그+바로가기.
                            // 그 외 단순 실패는 Toast 유지(불필요한 다이얼로그화 방지).
                            if (message != null && message.contains("탈퇴할 수 없습니다")) {
                                showBlockedDialog(message);
                            } else {
                                ToastUtil.makeText(ProfileDeleteAccountActivity.this,
                                        message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        binding.btnDeleteAccount.setEnabled(true);
                        Log.e(TAG, "탈퇴 실패: " + t.getMessage());
                        ToastUtil.makeText(ProfileDeleteAccountActivity.this,
                                "네트워크 오류", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** 진행 중 모임/체험단 때문에 탈퇴가 차단됐을 때: 스쳐가는 Toast 대신
     *  다이얼로그로 안내하고 [바로가기]로 내 모임 관리(진행중 탭)로 이동시킨다. */
    private void showBlockedDialog(String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("지금은 탈퇴할 수 없어요")
                .setMessage(message)
                .setNegativeButton("취소", null)
                .setPositiveButton("바로가기", (d, w) ->
                        startActivity(new Intent(ProfileDeleteAccountActivity.this,
                                ProfileMyMeetingsActivity.class)))
                .show();
    }

}
