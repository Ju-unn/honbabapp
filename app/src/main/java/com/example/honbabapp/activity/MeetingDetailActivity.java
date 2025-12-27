package com.example.honbabapp.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.meeting.MeetingParticipant;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.databinding.PageMeetingDetailBinding;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 모임 상세 화면
 *
 * 기능:
 * 1. 모임 상세 정보 표시
 * 2. 참여 신청 버튼
 * 3. 호스트인 경우 관리 기능
 */

public class MeetingDetailActivity extends AppCompatActivity {

    private String TAG = "MeetingDetailActivity";
    private PageMeetingDetailBinding binding;
    private ApiService apiService;

    private int meetingId;
    private int currentUserId;
    private Meeting meeting;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageMeetingDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // API 초기화
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // Intent에서 meeting_id 가져오기
        meetingId = getIntent().getIntExtra("meeting_id" , -1);

        if (meetingId == -1) {
            Toast.makeText(this, "잘못된 접근입니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 현재 사용자 ID 가져오기
        SharedPreferences prefs = getSharedPreferences("honbabapp_prefs" , MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id" , -1);

        // 뒤로가기 버튼
        setupBackButton();

        // 모임 상세 정보 로드
        loadMeetingDetail();
    }

    /**
     * 뒤로가기 버튼 설정
     */
    private void setupBackButton() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    /**
     * 모임 상세 정보 API 호출
     */
    private void loadMeetingDetail() {
        Call<ApiResponse<Meeting>> call = apiService.getMeetingDetail(meetingId);

        call.enqueue(new Callback<ApiResponse<Meeting>>() {
            @Override
            public void onResponse(Call<ApiResponse<Meeting>> call, Response<ApiResponse<Meeting>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Meeting> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        meeting = apiResponse.getData();
                        Log.i(TAG, "모임 상세 정보 로드 성공: " + meeting.getMeeting_title());

                        // UI 업데이트
                        bindMeetingData();
                        setupJoinButton();
                    } else {
                        Log.e(TAG, "API 실패: " + apiResponse.getMessage());
                        Toast.makeText(MeetingDetailActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "HTTP 오류: " + response.code());
                    Toast.makeText(MeetingDetailActivity.this,
                            "모임 정보를 불러오는데 실패했습니다.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Meeting>> call, Throwable t) {
                Log.e(TAG, "네트워크 오류: " + t.getMessage());
                Toast.makeText(MeetingDetailActivity.this,
                        "네트워크 오류가 발생했습니다.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Meeting 데이터를 UI에 바인딩
     */
    private void bindMeetingData() {
        // 가게 이름 (장소 이름)
        if (meeting.getLocation() != null) {
            binding.tvMeetingTitle.setText(meeting.getMeeting_title());
            binding.tvStoreName.setText(meeting.getLocation().getPlace_name());

            // 주소
            String address = formatAddress(meeting);
            binding.tvStoreAddress.setText(address);
        }

        // 가격대
        binding.tvPriceRange.setText(meeting.getMeeting_price_range());

        // 날짜
        String date = formatDate(meeting.getMeeting_date());
        binding.tvMeetingDate.setText(date);

        // 시간
        String time = formatTime(meeting.getMeeting_date());
        binding.tvMeetingTime.setText(time);

        // 참여 인원
        String participants = meeting.getMeeting_current_participants() + "/" +
                meeting.getMeeting_max_participants() + "명";
        binding.tvParticipantCount.setText(participants);

        // 호스트 메시지 (모임 설명)
        binding.tvHostMessage.setText(meeting.getMeeting_description());

        // 호스트 정보
        if (meeting.getHost() != null) {
            binding.hostName.setText(meeting.getHost().getUser_nickname());
        }

        // 참여자 목록
        if (meeting.getParticipants() != null && !meeting.getParticipants().isEmpty()) {
            // TODO: RecyclerView로 참여자 목록 표시
            // 지금은 간단하게 참여자 수만 표시
        }

        // 9. 이미지 (TODO: 이미지 불러오기)
    }

    /**
     * 주소 포맷팅
     */
    private String formatAddress(Meeting meeting) {
        if (meeting.getLocation() == null) return "";

        String sigungu = meeting.getLocation().getSigungu();
        String dong = meeting.getLocation().getDong();

        if (dong != null && !dong.isEmpty()) {
            return sigungu + " " + dong;
        }
        return sigungu;
    }

    /**
     * 날짜 포맷팅
     * "2025-12-25 19:00:00" → "12월 25일"
     */
    private String formatDate(String datetime) {
        try {
            String[] parts = datetime.split(" ");
            if (parts.length >= 1) {
                String date = parts[0];  // "2025-12-25"
                String[] dateParts = date.split("-");
                int month = Integer.parseInt(dateParts[1]);
                int day = Integer.parseInt(dateParts[2]);

                return month + "월 " + day + "일";
            }
        } catch (Exception e) {
            Log.e(TAG, "날짜 파싱 오류: " + e.getMessage());
        }

        return datetime;
    }

    /**
     * 시간 포맷팅
     * "2025-12-25 19:00:00" → "오후 7:00"
     */
    private String formatTime(String datetime) {
        try {
            String[] parts = datetime.split(" ");
            if (parts.length >= 2) {
                String time = parts[1];  // "19:00:00"
                String[] timeParts = time.split(":");
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);

                String ampm = hour >= 12 ? "오후" : "오전";
                int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);

                return String.format("%s %d:%02d", ampm, displayHour, minute);
            }
        } catch (Exception e) {
            Log.e(TAG, "시간 파싱 오류: " + e.getMessage());
        }

        return datetime;
    }

    /**
     * 참여 신청 버튼 설정
     */
    private void setupJoinButton() {
        // 호스트 인 경우 버튼 숨김
        if (meeting.getHost() != null && meeting.getHost().getUser_id() == currentUserId) {
            binding.joinButtonContainer.setVisibility(View.GONE);
            return;

            //TODO: 호스트인경우 "참여자 목록" 보여주기 버튼을 보여주기
        }

        // 이미 참여 신청했는지 확인
        boolean alreadyJoined = checkIfAlreadyJoined();

        if (alreadyJoined) {
            binding.btnJoinMeeting.setText("신청 완료");
            binding.btnJoinMeeting.setEnabled(false);
        } else if (meeting.getMeeting_status().equals("모집완료")) {
            binding.btnJoinMeeting.setText("모집 완료");
            binding.btnJoinMeeting.setEnabled(false);
        }  else {
            // 참여 신청 버튼 클릭
            binding.btnJoinMeeting.setOnClickListener(v -> {
                // TODO: join.php API 연동
                Toast.makeText(this, "준비중",
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * 이미 참여 신청했는지 확인
     */
    private boolean checkIfAlreadyJoined() {
        if (meeting.getParticipants() == null) return false;

        for (MeetingParticipant participant : meeting.getParticipants()) {
            if (participant.getUser_id() == currentUserId) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
