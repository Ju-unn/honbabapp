package com.example.honbabapp.activity.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.honbabapp.databinding.PageProfileSupportBinding;
import com.example.honbabapp.util.InsetsUtils;

/**
 * 고객센터 안내 Activity
 * - 고객문의 전화번호, 이용 가능 시간 안내
 */
public class ProfileSupportActivity extends AppCompatActivity {

    // ponytail: 고객센터 번호 미정 상태의 임시 하드코딩 — 실제 번호 확정되면 교체
    private static final String SUPPORT_PHONE_NUMBER = "01000000000";

    private PageProfileSupportBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageProfileSupportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsets(binding.getRoot());

        // 뒤로가기
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // 고객문의 안내 카드 전체 클릭 시 다이얼러 연결
        binding.cardSupportPhone.setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + SUPPORT_PHONE_NUMBER))));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
