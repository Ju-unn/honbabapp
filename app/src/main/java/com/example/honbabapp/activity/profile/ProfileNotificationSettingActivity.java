package com.example.honbabapp.activity.profile;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.databinding.PageProfileNotificationSettingsBinding;

public class ProfileNotificationSettingActivity extends AppCompatActivity {

    private PageProfileNotificationSettingsBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageProfileNotificationSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 뒤로가기
        binding.btnBack.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed());


    }

}