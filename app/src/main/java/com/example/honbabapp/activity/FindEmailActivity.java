package com.example.honbabapp.activity;

import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.FindEmailData;
import com.example.honbabapp.api.data.request.FindEmailRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.databinding.ActivityFindEmailBinding;
import com.example.honbabapp.util.InsetsUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 아이디(이메일) 찾기 Activity
 * 휴대폰 번호 입력 → 마스킹된 이메일 즉시 표시 (인증코드 없음)
 * (이 앱은 이메일이 곧 아이디라서 이메일 인증으로 아이디를 찾는 건 순환이 됨)
 */
public class FindEmailActivity extends AppCompatActivity {

    private static final String TAG = "FindEmailActivity";

    private ActivityFindEmailBinding binding;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFindEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsets(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        // 뒤로가기
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // 전화번호 하이픈 자동 생성 (010-1234-5678) — 회원가입 화면과 동일 방식
        binding.etPhone.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

        // 아이디 찾기
        binding.btnFindEmail.setOnClickListener(v -> findEmail());
    }

    private void findEmail() {
        String phoneNumber = binding.etPhone.getText().toString().trim();

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "휴대폰 번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnFindEmail.setEnabled(false);

        FindEmailRequest request = new FindEmailRequest(phoneNumber);
        apiService.findEmail(request).enqueue(new Callback<ApiResponse<FindEmailData>>() {
            @Override
            public void onResponse(Call<ApiResponse<FindEmailData>> call,
                                   Response<ApiResponse<FindEmailData>> response) {
                if (binding == null) return;
                binding.btnFindEmail.setEnabled(true);

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {

                    binding.tvMaskedEmail.setText(response.body().getData().getMasked_email());
                    binding.layoutResult.setVisibility(View.VISIBLE);
                } else {
                    binding.layoutResult.setVisibility(View.GONE);
                    String message = response.body() != null
                            ? response.body().getMessage() : "아이디 찾기에 실패했습니다";
                    Toast.makeText(FindEmailActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<FindEmailData>> call, Throwable t) {
                if (binding == null) return;
                binding.btnFindEmail.setEnabled(true);
                Log.e(TAG, "아이디 찾기 네트워크 오류: " + t.getMessage());
                Toast.makeText(FindEmailActivity.this,
                        "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
