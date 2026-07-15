package com.example.honbabapp.activity;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.honbabapp.databinding.PageHomeNotificationBinding;
import com.example.honbabapp.util.InsetsUtils;

public class NotificationActivity extends AppCompatActivity {

    private PageHomeNotificationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageHomeNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsets(binding.getRoot());

        // 뒤로가기 버튼
        onBackButtonClick();
    }

    /// 뒤로가기 버튼
    public void onBackButtonClick(){
        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }
}
