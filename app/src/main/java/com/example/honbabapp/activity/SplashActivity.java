package com.example.honbabapp.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Thread thread;

        ///new Runnable() alt + enter를 누르면 람다식으로 변경 된다.
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000L);
                    Intent intent = new Intent(SplashActivity.this , LoginActivity.class);
                    startActivity(intent);
                    finish();

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        thread.start();


    }
}