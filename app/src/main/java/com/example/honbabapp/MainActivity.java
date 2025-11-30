package com.example.honbabapp;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.honbabapp.data.MockData;
import com.example.honbabapp.data.meeting.Meeting;
import com.example.honbabapp.data.user.User;
import com.example.honbabapp.databinding.ActivityMainBinding;
import com.example.honbabapp.ui.adapter.MainFragmentAdapter;
import com.example.honbabapp.ui.fragment.MapFragment;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;

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

    public void navigationToMapWithCategory(String category) {
        Bundle bundle = new Bundle();
        bundle.putString("category" , category);

        Fragment mapFragment = getSupportFragmentManager().findFragmentById(R.id.nav_map);

        if (mapFragment instanceof MapFragment){
            ((MapFragment) mapFragment).setCategory(category);
        }

        binding.bottomNavigationBar.setSelectedItemId(R.id.nav_map);
    }

}