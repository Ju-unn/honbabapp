package com.example.honbabapp.activity.chat;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.user.UserProfileResponse;
import com.example.honbabapp.databinding.PageChatParticipantDetailBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 채팅방 참가자 프로필 상세 (view-only)
 *
 * 진입 경로:
 *   - ChatRoomDetailActivity 의 참가자 행 클릭
 *   - 1:1 방에서 헤더 카드 클릭
 *
 * 표시:
 *   - 아바타 / 닉네임 / 상태메시지 / 매너점수 / 참여횟수
 *   - 액션 버튼 없음 (개인채팅/팔로우/강퇴는 V2)
 *
 * Intent extra:
 *   - user_id : int
 *
 */
public class ChatRoomParticipantDetailActivity extends AppCompatActivity {

    private static final String TAG = "ChatParticipantDetail";

    private PageChatParticipantDetailBinding binding;
    private ApiService apiService;
    private int userId = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageChatParticipantDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ── Intent extra ──
        userId = getIntent().getIntExtra("user_id", -1);
        if (userId == -1) {
            Toast.makeText(this, "잘못된 접근입니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ── API 초기화 ──
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // ── 뒤로가기 ──
        binding.btnBack.setOnClickListener(v -> finish());

        // ── 프로필 로드 ──
        loadUserProfile();
    }

    private void loadUserProfile() {
        apiService.getUserProfile(userId)
                .enqueue(new Callback<ApiResponse<UserProfileResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<UserProfileResponse>> call,
                                           @NonNull Response<ApiResponse<UserProfileResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {
                            bindProfile(response.body().getData());
                        } else {
                            String msg = (response.body() != null)
                                    ? response.body().getMessage()
                                    : "프로필을 불러올 수 없습니다";
                            Toast.makeText(ChatRoomParticipantDetailActivity.this,
                                    msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<UserProfileResponse>> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "프로필 로드 실패: " + t.getMessage());
                        Toast.makeText(ChatRoomParticipantDetailActivity.this,
                                "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * UserProfileResponse → UI 바인딩
     * (ProfileFragment.bindProfie() 와 동일 패턴)
     */
    private void bindProfile(UserProfileResponse profile) {
        // 닉네임
        binding.tvProfileNickname.setText(profile.getUser_nickname());

        // 상태 메세지
        String status = profile.getUser_status_message();
        binding.tvProfileStatus.setText(
                (status != null && !status.isEmpty()) ? status : "상태 메세지가 없습니다"
        );

        // 매너 점수
        binding.tvMannerScore.setText(String.valueOf(profile.getUser_manner_score()));

        // 참여 횟수
        binding.tvParticipationCount.setText(profile.getUser_participation_count() + "회");

        // 아바타
        Glide.with(this)
                .load(profile.getUser_profile_image())
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(binding.ivProfileAvatar);
    }
}