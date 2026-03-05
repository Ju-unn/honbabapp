package com.example.honbabapp.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.honbabapp.R;
import com.example.honbabapp.data.MockData;
import com.example.honbabapp.data.meeting.Meeting;
import com.example.honbabapp.data.user.User;
import com.example.honbabapp.databinding.ActivityMainBinding;
import com.example.honbabapp.ui.adapter.MainFragmentAdapter;
import com.example.honbabapp.ui.fragment.MapFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.messaging.FirebaseMessaging;

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
        getToken();
        askNotificationPermission();
    }

    /// mapPage로 이동
    public void navigationToMap(String category) {
        binding.viewPager.setCurrentItem(1);
        //fragmentList에서 MapFragment를 가져오기 (객체)
        //객체 선언 하고 해당 객체를 fragmentList안에있는 MapFragment 받아오기
        MapFragment mapFragment;
        mapFragment = (MapFragment) adapter.fragmentList.get(1);
        if (mapFragment.isDetached()) { //뷰가 그려진 상태에서는 카테고리만 변경해서 해당부분만 새로 그리기
            mapFragment.setFilter(category);
        } else {    //초기값으로 설정 (뷰가 아직 그려지기전)
            mapFragment.pendingCategory = category;
        }
    }

    // Declare the launcher at the top of your Activity/Fragment:
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // FCM SDK (and your app) can post notifications.
                } else {
                    // TODO: Inform user that that your app will not show notifications.
                }
            });

    private void askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    public void getToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        Log.i("Test_log" , "getToken : " + token);
                    }
                });
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