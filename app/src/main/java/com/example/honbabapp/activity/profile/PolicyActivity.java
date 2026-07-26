package com.example.honbabapp.activity.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.R;
import com.example.honbabapp.databinding.PagePolicyBinding;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 약관/개인정보처리방침 열람 Activity
 * EXTRA_POLICY_TYPE 값에 따라 res/raw 의 문서를 읽어 표시한다.
 * - "terms"   → 이용약관 (terms_of_service.txt)
 * - "privacy" → 개인정보처리방침 (privacy_policy.txt)
 */
public class PolicyActivity extends AppCompatActivity {

    private static final String TAG = "PolicyActivity";

    public static final String EXTRA_POLICY_TYPE = "policy_type";
    public static final String TYPE_TERMS = "terms";
    public static final String TYPE_PRIVACY = "privacy";

    private PagePolicyBinding binding;

    /** 호출 편의를 위한 인텐트 팩토리 */
    public static Intent newIntent(Context context, String policyType) {
        Intent intent = new Intent(context, PolicyActivity.class);
        intent.putExtra(EXTRA_POLICY_TYPE, policyType);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PagePolicyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String policyType = getIntent().getStringExtra(EXTRA_POLICY_TYPE);
        boolean isPrivacy = TYPE_PRIVACY.equals(policyType);

        binding.tvScreenTitle.setText(isPrivacy ? "개인정보처리방침" : "이용약관");
        binding.tvPolicyContent.setText(readRawTextFile(
                isPrivacy ? R.raw.privacy_policy : R.raw.terms_of_service));
    }

    /**
     * res/raw 의 UTF-8 텍스트 파일을 문자열로 읽는다.
     */
    private String readRawTextFile(int rawResId) {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getResources().openRawResource(rawResId), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
        } catch (Exception e) {
            Log.e(TAG, "문서 읽기 실패: " + e.getMessage());
            return "문서를 불러오지 못했습니다.";
        }
        return stringBuilder.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
