package com.example.honbabapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.honbabapp.R;
import com.example.honbabapp.data.MockData;
import com.example.honbabapp.data.meeting.Meeting;
import com.example.honbabapp.data.user.User;
import com.example.honbabapp.databinding.ActivityMainBinding;
import com.example.honbabapp.ui.adapter.MainFragmentAdapter;
import com.example.honbabapp.ui.fragment.HomeFragment;
import com.example.honbabapp.ui.fragment.MapFragment;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainFragmentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        testCode();
        initViewPager();
        initNavigation();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // 새로운 intent 처리
        handleIntent(intent);
    }

    /**
     * Intent 처리 (모임 생성 후 새로고침)
     */
    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("refresh_home", false)) {
            // HomeFragment로 이동
            binding.viewPager.setCurrentItem(0, false);

            // HomeFragment 찾기
            Fragment fragment = getSupportFragmentManager()
                    .findFragmentByTag("f" + 0);  // ViewPager2의 Fragment Tag

            //  HomeFragment 새로고침
            if (fragment instanceof HomeFragment) {
                ((HomeFragment) fragment).refreshMeetingList();
            }

            // Toast 표시
            int newMeetingId = intent.getIntExtra("new_meeting_id", -1);
            if (newMeetingId != -1) {
                Toast.makeText(this, "모임이 생성되었습니다!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /// mapPage로 이동
    public void navigationToMap(String category) {
        binding.viewPager.setCurrentItem(1 , false);
        //fragmentList에서 MapFragment를 가져오기 (객체)
        //객체 선언 하고 해당 객체를 fragmentList안에있는 MapFragment 받아오기

        MapFragment mapFragment = (MapFragment) adapter.fragmentList.get(1);
        if (mapFragment instanceof MapFragment) {
            ((MapFragment) mapFragment).setFilter(category);
        }
    }

    /// homePage로 이동
    public void navigationToHome() {
        binding.viewPager.setCurrentItem(0);
    }

    private void testCode() {
        ArrayList<Meeting> meetingArrayList = MockData.getMeetingList();
        String user0InMeeting0;
        String user0InMeeeting1;
        User user0Meeting0;
        User user0Meeting1;

        user0InMeeting0 = meetingArrayList
                .get(0)
                .meeting_user_list
                .get(0)
                .user_nickname;
        user0InMeeeting1 = meetingArrayList.get(1).meeting_user_list.get(0).user_nickname;

        user0Meeting0 = meetingArrayList.get(0).meeting_user_list.get(0);
        user0Meeting1 = meetingArrayList.get(1).meeting_user_list.get(0);


        boolean isSameUser = user0Meeting0 == user0Meeting1;

        Log.i("TestLog","값 : " + isSameUser + " // " + user0Meeting0.user_nickname + " // " + user0Meeting1.user_nickname );


    }


    private void initViewPager(){
        adapter = new MainFragmentAdapter(this);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setUserInputEnabled(false);

    }

    private void initNavigation(){
        binding.bottomNavigationBar.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int position = 0;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    position = 0;
                }
                else if (itemId == R.id.nav_map) {
                    position = 1;
                }
                else if (itemId == R.id.nav_chat) {
                    position = 2;
                }
                else if (itemId == R.id.nav_profile) {
                    position = 3;
                }

                binding.viewPager.setCurrentItem(position);

                Log.i("MainActivity" , "onNavigationItemSelected position = " + position);
                return true;
            }
        });

        //viewPager의 표시되는 fragment가 변경되었을 때 호출되는코드
        //setCurrentItem으로 변경되면 bottomNavigationBar의 아이템이 선택된 상태로 보여지도록 변경하는 코드가 포함되어있음
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                int item = 0;

                switch (position) {
                    case 0 : item = R.id.nav_home;
                    break;
                    case 1 : item = R.id.nav_map;
                    break;
                    case 2 : item = R.id.nav_chat;
                    break;
                    case 3 : item = R.id.nav_profile;
                }

                binding.bottomNavigationBar.setSelectedItemId(item);
                Log.i("MainActivity" , "onPageSelected position = " + position);

            }
        });
    }

}