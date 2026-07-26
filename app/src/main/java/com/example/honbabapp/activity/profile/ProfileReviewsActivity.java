package com.example.honbabapp.activity.profile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.review.ReceivedReview;
import com.example.honbabapp.api.data.review.ReceivedReviewsData;
import com.example.honbabapp.api.data.review.WrittenReview;
import com.example.honbabapp.api.data.review.WrittenReviewsData;
import com.example.honbabapp.databinding.PageProfileReviewsBinding;
import com.example.honbabapp.ui.adapter.ReceivedReviewAdapter;
import com.example.honbabapp.ui.adapter.WrittenReviewAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 내 리뷰 Activity (프로필 > 내 리뷰)
 * - 탭1 "받은 리뷰": 기존 received.php 재사용 — 익명 방침 유지 (작성자 미표시)
 * - 탭2 "내가 쓴 리뷰": written.php — 후기 대상/모임명 포함
 *
 * ProfileMyMeetingsActivity 와 동일 패턴 (TabLayout + RecyclerView 1개,
 * 탭 전환 시 어댑터 교체).
 */
public class ProfileReviewsActivity extends AppCompatActivity {

    private static final String TAG = "ProfileReviewsActivity";
    private static final String PREF_NAME   = "honbabapp_prefs";
    private static final String KEY_USER_ID = "user_id";

    private static final int TAB_RECEIVED = 0;
    private static final int TAB_WRITTEN  = 1;

    private PageProfileReviewsBinding binding;
    private ApiService apiService;

    private ReceivedReviewAdapter receivedReviewAdapter;
    private WrittenReviewAdapter writtenReviewAdapter;

    private final List<ReceivedReview> receivedReviews = new ArrayList<>();
    private final List<WrittenReview> writtenReviews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageProfileReviewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        receivedReviewAdapter = new ReceivedReviewAdapter();
        writtenReviewAdapter = new WrittenReviewAdapter();
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReviews.setAdapter(receivedReviewAdapter);

        // 뒤로가기
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

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

        loadReceivedReviews();
        loadWrittenReviews();
    }

    /**
     * 받은 리뷰 로드 — 기존 received.php 재사용 (내 user_id로 조회)
     */
    private void loadReceivedReviews() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        int myUserId = prefs.getInt(KEY_USER_ID, -1);
        if (myUserId <= 0) {
            Log.e(TAG, "user_id 없음 — 받은 리뷰 조회 불가");
            return;
        }

        apiService.getReceivedReviews(myUserId)
                .enqueue(new Callback<ApiResponse<ReceivedReviewsData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ReceivedReviewsData>> call,
                                           Response<ApiResponse<ReceivedReviewsData>> response) {
                        if (binding == null) return;

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()
                                && response.body().getData() != null) {

                            receivedReviews.clear();
                            List<ReceivedReview> reviews = response.body().getData().getReviews();
                            if (reviews != null) receivedReviews.addAll(reviews);

                            refreshIfCurrentTab(TAB_RECEIVED);
                        } else {
                            Log.e(TAG, "받은 리뷰 로드 실패: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ReceivedReviewsData>> call, Throwable t) {
                        Log.e(TAG, "받은 리뷰 네트워크 오류: " + t.getMessage());
                    }
                });
    }

    /**
     * 내가 쓴 리뷰 로드 — written.php (토큰 기반, 파라미터 없음)
     */
    private void loadWrittenReviews() {
        apiService.getWrittenReviews()
                .enqueue(new Callback<ApiResponse<WrittenReviewsData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<WrittenReviewsData>> call,
                                           Response<ApiResponse<WrittenReviewsData>> response) {
                        if (binding == null) return;

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()
                                && response.body().getData() != null) {

                            writtenReviews.clear();
                            List<WrittenReview> reviews = response.body().getData().getReviews();
                            if (reviews != null) writtenReviews.addAll(reviews);

                            refreshIfCurrentTab(TAB_WRITTEN);
                        } else {
                            Log.e(TAG, "쓴 리뷰 로드 실패: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<WrittenReviewsData>> call, Throwable t) {
                        Log.e(TAG, "쓴 리뷰 네트워크 오류: " + t.getMessage());
                    }
                });
    }

    /**
     * 로드 완료된 탭이 현재 선택된 탭이면 화면 갱신
     */
    private void refreshIfCurrentTab(int loadedTab) {
        int selectedTab = binding.tabLayout.getSelectedTabPosition();
        if (selectedTab < 0) selectedTab = TAB_RECEIVED;
        if (selectedTab == loadedTab) {
            updateListByTab(selectedTab);
        }
    }

    private void updateListByTab(int position) {
        if (position == TAB_WRITTEN) {
            binding.rvReviews.setAdapter(writtenReviewAdapter);
            writtenReviewAdapter.submitReviewList(writtenReviews);
            showEmpty(writtenReviews.isEmpty());
        } else {
            binding.rvReviews.setAdapter(receivedReviewAdapter);
            receivedReviewAdapter.submitReviewList(receivedReviews);
            showEmpty(receivedReviews.isEmpty());
        }
    }

    private void showEmpty(boolean isEmpty) {
        binding.rvReviews.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
