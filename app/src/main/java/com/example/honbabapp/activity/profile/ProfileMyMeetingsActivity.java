package com.example.honbabapp.activity.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.honbabapp.activity.meeting.MeetingCardWritingActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.MyMeetingItem;
import com.example.honbabapp.databinding.PageProfileMyMeetingsBinding;
import com.example.honbabapp.ui.adapter.ProfileMyMeetingAdapter;
import com.example.honbabapp.util.InsetsUtils;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileMyMeetingsActivity extends AppCompatActivity {

    private PageProfileMyMeetingsBinding binding;
    private ApiService apiService;
    private ProfileMyMeetingAdapter adapter;

    private static final String TAG = "ProfileMyMeetings";

    // 탭별 데이터
    private List<MyMeetingItem> currentMeetings    = new ArrayList<>(); // 탭1: 진행중 전체
    private List<MyMeetingItem> pastHostMeetings   = new ArrayList<>(); // 탭2: 과거 호스트
    private List<MyMeetingItem> pastParticipantMeetings = new ArrayList<>(); // 탭3: 과거 참여자

    private static final String PREF_NAME   = "honbabapp_prefs";
    private static final String KEY_USER_ID = "user_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageProfileMyMeetingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsets(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        // RecyclerView 설정
        adapter = new ProfileMyMeetingAdapter();
        binding.rvMyMeetings.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyMeetings.setAdapter(adapter);

        // 뒤로가기
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // 모임 만들기
        binding.btnCreateMeeting.setOnClickListener(v ->
                startActivity(new Intent(this, MeetingCardWritingActivity.class)));

        // 탭 선택 이벤트
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateListByTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // API 호출
        loadMyMeetings();
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadMyMeetings();
    }

    private void loadMyMeetings() {

        // 캐시가 없는 첫 로드에서만 스피너 — 빈상태가 먼저 깜빡이는 것 방지.
        // onResume 재호출 때 이미 데이터가 있으면 그대로 목록 유지.
        if (currentMeetings.isEmpty() && pastHostMeetings.isEmpty()
                && pastParticipantMeetings.isEmpty()) {
            binding.progressLoading.setVisibility(View.VISIBLE);
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.rvMyMeetings.setVisibility(View.GONE);
        }

        apiService.getMyMeetings()
                .enqueue(new Callback<ApiResponse<List<MyMeetingItem>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<MyMeetingItem>>> call, Response<ApiResponse<List<MyMeetingItem>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {

                            List<MyMeetingItem> all = response.body().getData();

                            Log.d(TAG, "전체 모임 수: " + all.size());
                            classifyMeetings(all);

                            Log.d(TAG, "현재진행: " + currentMeetings.size());
                            Log.d(TAG, "과거호스트: " + pastHostMeetings.size());
                            Log.d(TAG, "과거참여: " + pastParticipantMeetings.size());

                            // 현재 선택된 탭을 유지한 채로 갱신
                            // (탭이 아직 선택되지 않은 최초 진입 시에는 0번 탭)
                            int selectedTab = binding.tabLayout.getSelectedTabPosition();
                            updateListByTab(selectedTab >= 0 ? selectedTab : 0);
                        }  else {
                            Log.e(TAG, "API 실패: " + (response.body() != null
                                    ? response.body().getMessage() : "body null"));
                            showEmpty();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<MyMeetingItem>>> call, Throwable t) {
                        Log.e(TAG, "네트워크 오류: " + t.getMessage());
                        showEmpty();
                    }
                });
    }

    private void classifyMeetings(List<MyMeetingItem> all) {
        currentMeetings.clear();;
        pastHostMeetings.clear();
        pastParticipantMeetings.clear();

        for (MyMeetingItem item : all) {
            // 취소된 모임은 날짜와 무관하게 "과거"로 분류
            // 취소됐는가
            boolean isCancelled = "취소".equals(item.getMeeting_status());
            // 과거인가
            boolean treatAsPast = item.isIs_past() || isCancelled;

            if (!treatAsPast) {
                // 탭1 : 진행중 (host + participant)
                currentMeetings.add(item);
            } else if ("host".equals(item.getUser_role())) {
                // 탭2 : 과거 호스트 (취소 포함)
                pastHostMeetings.add(item);
            } else {
                // 탭3 : 과거 참여자 (취소 포함)
                pastParticipantMeetings.add(item);
            }
        }
    }

    private void updateListByTab(int position) {
        List<MyMeetingItem> list;
        switch (position) {
            case 1: list = pastHostMeetings; break;
            case 2: list = pastParticipantMeetings; break;
            default: list = currentMeetings; break;
        }

        if (list.isEmpty()) {
            showEmpty();
        } else {
            showList(list);
        }
    }

    private void showList(List<MyMeetingItem> list) {
        binding.progressLoading.setVisibility(View.GONE);
        binding.rvMyMeetings.setVisibility(View.VISIBLE);
        binding.emptyStateLayout.setVisibility(View.GONE);
        adapter.updateMeetings(list);
    }

    private void showEmpty() {
        binding.progressLoading.setVisibility(View.GONE);
        binding.rvMyMeetings.setVisibility(View.GONE);
        binding.emptyStateLayout.setVisibility(View.VISIBLE);
    }


}