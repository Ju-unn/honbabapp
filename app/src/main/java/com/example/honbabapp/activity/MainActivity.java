package com.example.honbabapp.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.honbabapp.R;
import com.example.honbabapp.databinding.ActivityMainBinding;
import com.example.honbabapp.network.SocketManager;
import com.example.honbabapp.ui.adapter.MainFragmentAdapter;
import com.example.honbabapp.ui.fragment.CampaignFragment;
import com.example.honbabapp.ui.fragment.HomeFragment;
import com.example.honbabapp.ui.fragment.MapFragment;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 2001;

    private ActivityMainBinding binding;
    private MainFragmentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViewPager();
        initNavigation();
        handleIntent(getIntent());
        requestNotificationPermissionIfNeeded();

        SharedPreferences prefs =
                getSharedPreferences("honbabapp_prefs", android.content.Context.MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);

        SocketManager.getInstance().login(userId);
    }

    /**
     * Android 13(API 33) 이상에서 푸시 알림을 표시하려면
     * POST_NOTIFICATIONS 런타임 권한이 필요하다.
     */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_NOTIFICATION_PERMISSION
                );
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        if (intent.getBooleanExtra("refresh_home", false)) {
            Fragment homeFragment = getSupportFragmentManager().findFragmentByTag("f0");
            if (homeFragment instanceof HomeFragment) {
                ((HomeFragment) homeFragment).refreshMeetingList();
            }
        }

        // 체험단 탭으로 이동 + 목록 새로고침
        if (intent.getBooleanExtra("refresh_campaign", false)) {
            binding.viewPager.setCurrentItem(2, false); // CampaignFragment = index 2
            CampaignFragment campaignFragment =
                    (CampaignFragment) getSupportFragmentManager().findFragmentByTag("f2");
            if (campaignFragment != null) {
                campaignFragment.refreshCampaignList();
            }
        }

        // 지도 탭으로 이동
        if (intent.getBooleanExtra("move_to_map", false)) {
            double latitude = intent.getDoubleExtra("map_lat", 37.5665);
            double longitude = intent.getDoubleExtra("map_lng", 126.9780);

            binding.viewPager.setCurrentItem(1, false); // 지도 탭 = index 1 (불변)
            MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentByTag("f1");
            if (mapFragment != null) {
                mapFragment.moveToLocation(latitude, longitude);
            }
        }
    }

    /**
     * 지도 탭으로 이동 + 카테고리 필터 설정
     * index 1로 고정 (계획서: 지도=1 유지로 navigationToMap() 영향 없음)
     */
    public void navigationToMap(String category) {
        binding.viewPager.setCurrentItem(1, false);
        MapFragment mapFragment = (MapFragment) adapter.fragmentList.get(1);
        mapFragment.setFilter(category);
    }

    public void navigationToMapHot() {
        binding.viewPager.setCurrentItem(1, false);
        MapFragment mapFragment = (MapFragment) adapter.fragmentList.get(1);
        mapFragment.setFilterHot();
    }

    public void navigationToHome() {
        binding.viewPager.setCurrentItem(0);
    }

    private void initViewPager() {
        adapter = new MainFragmentAdapter(this);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setUserInputEnabled(false);
    }

    /**
     * 바텀 네비게이션 탭 ↔ ViewPager2 인덱스 매핑
     *
     * 0 - nav_home    → HomeFragment
     * 1 - nav_map     → MapFragment
     * 2 - nav_campaign → CampaignFragment
     * 3 - nav_chat    → ChatFragment
     * 4 - nav_profile → ProfileFragment
     */
    private void initNavigation() {
        binding.bottomNavigationBar.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int targetPosition = 0;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    targetPosition = 0;
                } else if (itemId == R.id.nav_map) {
                    targetPosition = 1;
                } else if (itemId == R.id.nav_campaign) {
                    targetPosition = 2;
                } else if (itemId == R.id.nav_chat) {
                    targetPosition = 3;
                } else if (itemId == R.id.nav_profile) {
                    targetPosition = 4;
                }

                binding.viewPager.setCurrentItem(targetPosition);
                Log.i(TAG, "onNavigationItemSelected targetPosition = " + targetPosition);
                return true;
            }
        });

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                int selectedItemId = R.id.nav_home;

                switch (position) {
                    case 0:
                        selectedItemId = R.id.nav_home;
                        break;
                    case 1:
                        selectedItemId = R.id.nav_map;
                        break;
                    case 2:
                        selectedItemId = R.id.nav_campaign;
                        break;
                    case 3:
                        selectedItemId = R.id.nav_chat;
                        break;
                    case 4:
                        selectedItemId = R.id.nav_profile;
                        break;
                }

                binding.bottomNavigationBar.setSelectedItemId(selectedItemId);
                Log.i(TAG, "onPageSelected position = " + position);
            }
        });
    }
}