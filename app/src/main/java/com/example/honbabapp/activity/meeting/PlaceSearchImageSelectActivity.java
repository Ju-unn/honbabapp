package com.example.honbabapp.activity.meeting;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.honbabapp.BuildConfig;
import com.example.honbabapp.api.NaverApiService;
import com.example.honbabapp.api.NaverRetrofitClient;
import com.example.honbabapp.api.data.response.naver.NaverImageSearchResponse;
import com.example.honbabapp.databinding.PageMeetingCardImageSelectBinding;
import com.example.honbabapp.databinding.RvImageSelectBinding;
import com.example.honbabapp.ui.adapter.ImageSelectAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 가게 대표 이미지 선택 Activity
 *
 * PlaceSearchActivity 에서 장소 선택 후 진입
 * 네이버 이미지 검색 결과 5개를 2열 그리드로 표시
 * 사용자가 선택 or 갤러리에서 직접 선택 → RESULT_OK 로 반환
 *
 * Intent 수신 (from PlaceSearchActivity):
 *   place_name, address, road_address, sigungu, dong, map_x, map_y, category, phone
 *
 * Intent 반환 (to MeetingCardWritingActivity):
 *   위 place 정보 그대로 + selected_image_url (String, nullable)
 */
public class PlaceSearchImageSelectActivity extends AppCompatActivity {

    private static final String TAG = "PlaceSearchImageSelect";

    private PageMeetingCardImageSelectBinding binding;
    private NaverApiService naverApiService;
    private ImageSelectAdapter adapter;

    // PlaceSearchActivity 에서 받은 장소 정보
    private String placeName;
    private String address;
    private String roadAddress;
    private String sigungu;
    private String dong;
    private String mapX;
    private String mapY;
    private String category;
    private String phone;

    // 갤러리에서 선택한 이미지 uri (선택시 저장)
    private Uri galleryImageUri = null;

    // 갤러리 런처
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri == null) return;

                        galleryImageUri = uri;

                        // 갤러리 이미지를 selelcted_image_url 대신 Uri로 전달
                        // S3 업로드는 MeetingCardWritingActivity에서 처리
                        returnResult("gallery" , uri.toString());
                    }
            );


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageMeetingCardImageSelectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        naverApiService = NaverRetrofitClient.getClient().create(NaverApiService.class);

        // 장소 정보 수신
        receivePlaceData();

        // UI 설정
        setupUI();

        // 네이버 이미지 검색 시작
        searchImages(placeName);
    }

    // PlaceSearchActivity 에서 넘어온 장소 데이터 수신
    private void receivePlaceData() {
        Intent intent = getIntent();
        placeName   = intent.getStringExtra("place_name");
        address     = intent.getStringExtra("address");
        roadAddress = intent.getStringExtra("road_address");
        sigungu     = intent.getStringExtra("sigungu");
        dong        = intent.getStringExtra("dong");
        mapX        = intent.getStringExtra("map_x");
        mapY        = intent.getStringExtra("map_y");
        category    = intent.getStringExtra("category");
        phone       = intent.getStringExtra("phone");

        Log.i(TAG, "장소: " + placeName + " / 구: " + sigungu);
    }

    private void setupUI() {
        // 뒤로가기
        binding.btnBack.setOnClickListener(v -> finish());

        // 그리드 2열 RecyclerView
        adapter = new ImageSelectAdapter();
        binding.rvImages.setLayoutManager(new GridLayoutManager(this , 2));
        binding.rvImages.setAdapter(adapter);

        // 이미지 선택 리스너 -> 확인 버튼 활성화
        adapter.setOnImageSelectListener(imageUrl -> {
            galleryImageUri = null;
            binding.btnConfirm.setEnabled(true);
            Log.i(TAG, "이미지 선택: " + imageUrl);
        });

        // 확인 버튼
        binding.btnConfirm.setOnClickListener(v -> {
            String selectedUrl = adapter.getSelectedUrl();
            if (selectedUrl != null) {
                returnResult("naver" , selectedUrl);
            }
        });

        // 갤러리 버튼
        binding.btnGallery.setOnClickListener(v -> {
            galleryLauncher.launch("image/*");
        });
    }

    // 네이버 이미지 검색
    private void searchImages(String query) {

        naverApiService.searchImage(
                BuildConfig.NAVER_CLIENT_ID,
                BuildConfig.NAVER_CLIENT_SECRET,
                query,
                5,
                "sim"
        ).enqueue(new Callback<NaverImageSearchResponse>() {
            @Override
            public void onResponse(Call<NaverImageSearchResponse> call, Response<NaverImageSearchResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "이미지 검색 실패: " + response.code());
                    showEmpty();
                    return;
                }

                List<NaverImageSearchResponse.ImageItem> items = response.body().getItems();
                if (items == null || items.isEmpty()) {
                    showEmpty();
                    return;
                }

                // url 리스트 추출
                List<String> urls = new ArrayList<>();
                for (NaverImageSearchResponse.ImageItem item : items) {
                    urls.add(item.getLink());
                }

                adapter.setImageUrls(urls);
                binding.layoutEmpty.setVisibility(View.GONE);
                Log.i(TAG, "이미지 " + urls.size() + "개 로드 완료");
            }

            @Override
            public void onFailure(Call<NaverImageSearchResponse> call, Throwable t) {
                Log.e(TAG, "이미지 검색 네트워크 오류: " + t.getMessage());
                showEmpty();
            }
        });
    }

    /**
     * RESULT_OK 로 결과 반환
     *
     * @param source "naver" | "gallery"
     * @param imageValue naver → 이미지 URL / gallery → content:// Uri 문자열
     */
    private void returnResult(String source, String imageValue) {
        Intent result = new Intent();

        // 장소 정보 그대로 전달
        result.putExtra("place_name",   placeName);
        result.putExtra("address",      address);
        result.putExtra("road_address", roadAddress);
        result.putExtra("sigungu",      sigungu);
        result.putExtra("dong",         dong);
        result.putExtra("map_x",        mapX);
        result.putExtra("map_y",        mapY);
        result.putExtra("category",     category);
        result.putExtra("phone",        phone);

        // 이미지 정보 추가
        result.putExtra("image_source", source);       // "naver" or "gallery"
        result.putExtra("selected_image_url", imageValue); // URL or content:// Uri

        setResult(RESULT_OK, result);
        finish();
    }

    private void showEmpty() {
        binding.layoutEmpty.setVisibility(View.VISIBLE);
        binding.rvImages.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }





}
