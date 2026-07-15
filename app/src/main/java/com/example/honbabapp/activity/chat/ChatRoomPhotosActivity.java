package com.example.honbabapp.activity.chat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.api.data.chat.ChatPhotoData;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.chat.ChatPhotosResponse;
import com.example.honbabapp.databinding.PageChatPhotosBinding;
import com.example.honbabapp.repository.ChatRepository;
import com.example.honbabapp.ui.adapter.ChatPhotoGridAdapter;
import com.example.honbabapp.util.InsetsUtils;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 채팅방 사진 전체보기 (3열 그리드)
 *
 * 진입 경로:
 *   ChatRoomDetailActivity 의 "사진 > 더보기" 클릭
 *
 * 구조:
 *   - GridLayoutManager(this, 3)
 *   - ChatPhotoGridAdapter (rv_chat_photo_grid 사용)
 *   - 페이지당 30장 무한 스크롤
 *   - 빈 상태 placeholder
 *
 * Intent extra:
 *   - room_id : int
 *
 */
public class ChatRoomPhotosActivity extends AppCompatActivity {

    private static final String TAG = "ChatRoomPhotosActivity";

    private static final String PREF_NAME = "honbabapp_prefs";
    private static final String KEY_USER_ID = "user_id";

    // 페이지 크기
    private static final int PAGE_SIZE = 30;

    // 무한 스크롤 트리거 마진 (마지막 N개 안에 도달하면 다음 페이지 로드)
    private static final int LOAD_MORE_THRESHOLD = 5;

    private PageChatPhotosBinding binding;
    private ChatPhotoGridAdapter adapter;

    private int roomId   = -1;
    private int myUserId = -1;

    // 페이징 상태
    private int currentPage  = 1;
    private boolean isLoading = false;
    private boolean hasMore   = true;


    // ═════════════════════════════════════════
    // onCreate
    // ═════════════════════════════════════════

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageChatPhotosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsets(binding.getRoot());

        roomId = getIntent().getIntExtra("room_id", -1);
        if (roomId == -1) {
            Toast.makeText(this, "잘못된 접근입니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ── 현재 user_id ──
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        myUserId = prefs.getInt(KEY_USER_ID, -1);

        // ── 뒤로가기 ──
        binding.btnBack.setOnClickListener(v -> finish());

        // ── RecyclerView 세팅 ──
        setupRecyclerView();

        // ── 첫 페이지 로드 ──
        loadPage(1, true);
    }


    // ═════════════════════════════════════════
    // RecyclerView + 무한 스크롤
    // ═════════════════════════════════════════

    private void setupRecyclerView() {
        adapter = new ChatPhotoGridAdapter();
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        binding.rvChatPhotos.setLayoutManager(gridLayoutManager);
        binding.rvChatPhotos.setAdapter(adapter);

        adapter.setOnPhotoClickListener((photos, position) -> {
            // 전체 URL 목록 추출
            ArrayList<String> urls = new ArrayList<>();
            for (ChatPhotoData p : photos) urls.add(p.getImage_url());

            Intent intent = new Intent(this, FullScreenImageActivity.class);
            intent.putStringArrayListExtra(FullScreenImageActivity.EXTRA_IMAGE_URLS, urls);
            intent.putExtra(FullScreenImageActivity.EXTRA_START_INDEX, position);
            startActivity(intent);
        });

        // 무한 스크롤 리스너
        binding.rvChatPhotos.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);

                // 스크롤 아래 방향일 때만 검사 (dy > 0)
                if (dy <= 0) return;

                GridLayoutManager glm = (GridLayoutManager) rv.getLayoutManager();
                if (glm == null) return;

                int total = glm.getItemCount();
                int lastVisible = glm.findLastVisibleItemPosition();

                // 마지막 LOAD_MORE_THRESHOLD 개 안에 도달 + 추가 로드 가능
                if (!isLoading && hasMore && lastVisible >= total - LOAD_MORE_THRESHOLD) {
                    loadPage(currentPage + 1, false);
                }
            }
        });
    }


    // ═════════════════════════════════════════
    // API 호출
    // ═════════════════════════════════════════

    /**
     * 사진 페이지 로드
     *
     * @param page    요청할 페이지 번호
     * @param isFirst true 면 setItems (전체 교체), false 면 addItems (덧붙임)
     */
    private void loadPage(int page, boolean isFirst) {
        if (isLoading) return;
        isLoading = true;

        ChatRepository.getInstance().getChatPhotos(roomId, myUserId, page, PAGE_SIZE,
                new Callback<ApiResponse<ChatPhotosResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ChatPhotosResponse>> call,
                                           @NonNull Response<ApiResponse<ChatPhotosResponse>> response) {
                        isLoading = false;

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            ChatPhotosResponse data = response.body().getData();
                            currentPage = data.getPage();
                            hasMore     = data.isHas_more();

                            if (isFirst) {
                                adapter.setItems(data.getPhotos());
                                updateEmptyState(data.getTotal_count() == 0);
                            } else {
                                adapter.addItems(data.getPhotos());
                            }

                        } else {
                            Log.e(TAG, "사진 페이지 로드 실패");
                            if (isFirst) updateEmptyState(true);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ChatPhotosResponse>> call,
                                          @NonNull Throwable t) {
                        isLoading = false;
                        Log.e(TAG, "사진 네트워크 오류: " + t.getMessage());
                        if (isFirst) updateEmptyState(true);
                    }
                });
    }


    // ═════════════════════════════════════════
    // 빈 상태 toggle
    // ═════════════════════════════════════════

    private void updateEmptyState(boolean empty) {
        if (empty) {
            binding.rvChatPhotos.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.rvChatPhotos.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }
}