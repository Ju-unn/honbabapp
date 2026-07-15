package com.example.honbabapp.activity.meeting;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import com.example.honbabapp.BuildConfig;
import com.example.honbabapp.api.NaverApiService;
import com.example.honbabapp.api.NaverRetrofitClient;
import com.example.honbabapp.api.data.response.naver.NaverLocalSearchResponse;
import com.example.honbabapp.databinding.ActivityPlaceSearchBinding;
import com.example.honbabapp.ui.adapter.PlaceSearchAdapter;
import com.example.honbabapp.util.InsetsUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 장소 검색 Activity
 *
 * Naver Local Search API를 사용하여 장소 검색
 *
 */

public class PlaceSearchActivity extends AppCompatActivity {

    private String TAG   = "PlaceSearchACtivity";
    private ActivityPlaceSearchBinding binding;
    private NaverApiService naverApiService;
    private PlaceSearchAdapter adapter;

    //PlaceSearchLauncher
    private final ActivityResultLauncher<Intent> imageSelectLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            // place 정보 + selected_url 상위 전달
                            setResult(RESULT_OK, result.getData());
                            finish();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlaceSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsets(binding.getRoot());

        // Naver API 서비스 초기화
        naverApiService = NaverRetrofitClient.getClient().create(NaverApiService.class);

        // UI 설정
        setupUI();

        // MeetingCardWritingActivity에서 검색어 전달받은 경우 자동 검색
        String searchQuery = getIntent().getStringExtra("search_query");
        if (searchQuery != null && !searchQuery.isEmpty()) {
            binding.etSearch.setText(searchQuery);
            searchPlace(searchQuery);
        }
    }

    /**
     * UI 초기 설정
     */
    private void setupUI() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener(v -> finish());

        // RecyclerView 설정
        adapter = new PlaceSearchAdapter();
        binding.rvSearchResults.setAdapter(adapter);

        // 장소 클릭 리스너
        adapter.setOnPlaceClickListener(place -> {
            // 선택한 장소 정보를 MeetingCardWritingActivity로 전달
            Intent intent = new Intent(this,  PlaceSearchImageSelectActivity.class);
            intent.putExtra("place_name", place.getCleanTitle());
            intent.putExtra("address", place.getAddress());
            intent.putExtra("road_address", place.getRoadAddress());
            intent.putExtra("sido", place.getSido());
            intent.putExtra("sigungu", place.getSigungu());
            intent.putExtra("dong", place.getDong());
            intent.putExtra("map_x", place.getMapxDouble());
            intent.putExtra("map_y", place.getMapyDouble());
            intent.putExtra("category", place.getCategory());
            intent.putExtra("phone", place.getTelephone());

            //
            imageSelectLauncher.launch(intent);
        });

        // 검색 버튼
        binding.btnSearch.setOnClickListener(v -> performSearch());

        // EditText 엔터키 처리
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    /**
     * 검색 실행
     */
    private void performSearch() {
        String query = binding.etSearch.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(this, "검색어를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // Naver API 호출
        searchPlace(query);
    }

    /**
     * Naver Local Search API 호출
     */
    private void searchPlace(String query) {
        String clientId = BuildConfig.NAVER_CLIENT_ID;
        String clientSecret = BuildConfig.NAVER_CLIENT_SECRET;

        Call<NaverLocalSearchResponse> call = naverApiService.searchLocal(
                clientId,
                clientSecret,
                query,
                5,        // display: 5개
                1,        // start: 1번째부터
                "random"  // sort: 정확도순
        );

        call.enqueue(new Callback<NaverLocalSearchResponse>() {
            @Override
            public void onResponse(Call<NaverLocalSearchResponse> call, Response<NaverLocalSearchResponse> response) {
                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    NaverLocalSearchResponse result = response.body();

                    Log.i(TAG, "검색 성공: " + result.getTotal() + "개 결과");

                    if (result.getItems() != null && !result.getItems().isEmpty()) {
                        // 검색 결과 표시
                        adapter.updatePlaces(result.getItems());
                        binding.layoutEmpty.setVisibility(View.GONE);
                    } else {
                        // 검색 결과 없음
                        adapter.updatePlaces(null);
                        showEmptyState("검색 결과가 없습니다");
                    }
                }  else {
                    Log.e(TAG, "API 오류: " + response.code());
                    showEmptyState("검색에 실패했습니다");
                    Toast.makeText(PlaceSearchActivity.this,
                            "검색에 실패했습니다",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<NaverLocalSearchResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e(TAG, "네트워크 오류: " + t.getMessage());
                showEmptyState("네트워크 오류가 발생했습니다");
                Toast.makeText(PlaceSearchActivity.this,
                        "네트워크 오류가 발생했습니다",
                        Toast.LENGTH_SHORT).show();
            }
        });


    }

    /**
     * 빈 상태 표시
     */
    private void showEmptyState(String message) {
        binding.layoutEmpty.setVisibility(View.VISIBLE);
        binding.tVMessage1.setText(message);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }



}