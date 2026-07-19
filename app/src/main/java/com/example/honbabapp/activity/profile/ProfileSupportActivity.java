package com.example.honbabapp.activity.profile;

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
