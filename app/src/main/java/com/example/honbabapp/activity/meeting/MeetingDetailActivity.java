package com.example.honbabapp.activity.meeting;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.bumptech.glide.Glide;
import com.example.honbabapp.api.data.request.meeting.DeleteMeetingRequest;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.UiSettings;
import com.example.honbabapp.R;
import com.example.honbabapp.activity.MainActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.meeting.MeetingParticipant;
import com.example.honbabapp.api.data.request.meeting.JoinMeetingRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.ParticipantResponse;
import com.example.honbabapp.databinding.PageMeetingDetailBinding;
import com.example.honbabapp.ui.adapter.ParticipantListAdapter;
import com.example.honbabapp.util.InsetsUtils;
import com.example.honbabapp.util.ToastUtil;

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
    private ParticipantListAdapter participantAdapter;

    private int meetingId;
    private int currentUserId;
    private Meeting meeting;
    private NaverMap miniNaverMap;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageMeetingDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsets(binding.getRoot());

        // Naver Mini Map
        binding.miniMap.onCreate(savedInstanceState);

        // API 초기화
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // Intent에서 meeting_id 가져오기
        meetingId = getIntent().getIntExtra("meeting_id" , -1);

        if (meetingId == -1) {
            ToastUtil.makeText(this, "잘못된 접근입니다", Toast.LENGTH_SHORT).show();
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
        Call<ApiResponse<Meeting>> call = apiService.getMeetingDetail(meetingId , currentUserId);

        call.enqueue(new Callback<ApiResponse<Meeting>>() {
            @Override
            public void onResponse(Call<ApiResponse<Meeting>> call, Response<ApiResponse<Meeting>> response) {
                if (binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Meeting> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        meeting = apiResponse.getData();
                        Log.i(TAG, "모임 상세 정보 로드 성공: " + meeting.getMeeting_title());

                        if (meeting.getParticipants() != null) {
                            for (MeetingParticipant p : meeting.getParticipants()) {
                                Log.i(TAG, "참여자: " + p.getUser_nickname() + " / 이미지: " + p.getUser_profile_image());
                            }
                        }

                        // UI 업데이트
                        bindMeetingData();
                        setupJoinButton();
                    } else {
                        Log.e(TAG, "API 실패: " + apiResponse.getMessage());
                        ToastUtil.makeText(MeetingDetailActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "HTTP 오류: " + response.code());
                    ToastUtil.makeText(MeetingDetailActivity.this,
                            "모임 정보를 불러오는데 실패했습니다.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Meeting>> call, Throwable t) {
                Log.e(TAG, "네트워크 오류: " + t.getMessage());
                ToastUtil.makeText(MeetingDetailActivity.this,
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


        // 참여자 제목 동적 업데이트
        binding.tvParticipantsTitle.setText(
                "참여자 (" + meeting.getMeeting_current_participants() + "/" + meeting.getMeeting_max_participants() + ")"
        );

        // 카테고리 바인딩
        binding.tvStoreCategory.setText(meeting.getMeeting_category());

        // 참여자 목록
        setupParticipantsRecyclerView();

        // 9. 이미지
        String imageUrl = meeting.getMeeting_image();
        Log.e(TAG , "모임 이미지 : " + imageUrl  );
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_meeting_image)
                    .centerCrop()
                    .into(binding.ivMeetingImage);
        }

        // 10. MiniMap
        if (meeting.getLocation() != null) {
            try {
                double lat = Double.parseDouble(meeting.getLocation().getMap_y());
                double lng = Double.parseDouble(meeting.getLocation().getMap_x());

                binding.miniMap.getMapAsync(naverMap -> {
                    miniNaverMap = naverMap;

                    // 인터랙션 비활성화 (미니맵이므로)
                    UiSettings ui = naverMap.getUiSettings();
                    ui.setZoomControlEnabled(false);
                    ui.setCompassEnabled(false);
                    ui.setScaleBarEnabled(false);
                    ui.setScrollGesturesEnabled(false);
                    ui.setZoomGesturesEnabled(false);
                    ui.setRotateGesturesEnabled(false);
                    ui.setTiltGesturesEnabled(false);
                    ui.setLogoClickEnabled(false);

                    // 마커 추가
                    LatLng position = new LatLng(lat, lng);
                    Marker marker = new Marker();
                    marker.setPosition(position);
                    marker.setIcon(OverlayImage.fromResource(R.drawable.orange_pin));
                    marker.setMap(naverMap);

                    // 카메라 이동
                    naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(position, 15));
                });

                // 클릭 시 MapFragment로 이동
                // MapView 자체는 내부 제스처 처리기가 터치를 먼저 소비해 클릭이 거의 발동하지 않으므로
                // 지도 위에 얹은 투명 오버레이(mini_map_click_overlay)가 클릭을 대신 받는다
                binding.miniMapClickOverlay.setOnClickListener(v -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("move_to_map", true);
                    intent.putExtra("map_lat", lat);
                    intent.putExtra("map_lng", lng);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                });
            } catch (NumberFormatException e) {
                Log.e(TAG, "좌표 파싱 실패" + e.getMessage());
            }
        }
    }

    /**
     * 참여자 RecyclerView 설정
     */
    private void setupParticipantsRecyclerView() {
        if (meeting.getParticipants() == null || meeting.getParticipants().isEmpty()) return;

        participantAdapter = new ParticipantListAdapter(meeting.getParticipants(), meeting.getHost().getUser_nickname());
        binding.rvParticipants.setAdapter(participantAdapter);
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

        // 만료된 미팅 처리 — 호스트/참여자 구분 없이 버튼 숨김
        if (meeting.isExpired()) {
            binding.joinButtonContainer.setVisibility(View.GONE);
            binding.btnMoreOptions.setVisibility(View.GONE);
            return;  // 이후 로직 실행 안 함
        }

        // 호스트 인 경우 버튼 숨김
        if (meeting.getHost() != null && meeting.getHost().getUser_id() == currentUserId) {

            // 참여 신청 버튼 숨김
            binding.joinButtonContainer.setVisibility(View.GONE);

            showHostManagermentButton();
            return;
        }

        // 현재 사용자의 참여 상태 확인
        String participationStatus = getParticipationStatus();

        // "취소" 상태(이전에 채팅방을 나간 사용자)는 신규 사용자와 동일하게 취급 → 재신청 허용
        if ("취소".equals(participationStatus)) {
            participationStatus = null;
        }

        if (participationStatus != null) {
            // 참여 신청한 경우 - 상태에 따라 버튼 텍스트 변경
            if (participationStatus.equals("승인")) {
                binding.btnJoinMeeting.setText("참여 확정");
                binding.btnJoinMeeting.setEnabled(false);
            } else if (participationStatus.equals("대기")) {
                binding.btnJoinMeeting.setText("승인 대기 중");
                binding.btnJoinMeeting.setEnabled(false);
            } else if (participationStatus.equals("거절")) {
                binding.btnJoinMeeting.setText("참여 거절됨");
                binding.btnJoinMeeting.setEnabled(false);
            }
        } else if (meeting.getMeeting_status().equals("모집완료")) {
            binding.btnJoinMeeting.setText("모집 완료");
            binding.btnJoinMeeting.setEnabled(false);
        } else if (meeting.getMeeting_status().equals("취소")) {   // ← 추가
            binding.btnJoinMeeting.setText("취소된 모임");
            binding.btnJoinMeeting.setEnabled(false);
        } else {
            // 참여 신청 버튼 클릭
            binding.btnJoinMeeting.setOnClickListener(v -> {
                showJoinConfirmDialog();
            });
        }
    }

    /**
     * 현재 사용자의 참여 상태 확인
     */
    private String getParticipationStatus() {
        return meeting.getMy_participation_status();
    }

    /**
     * 이미 참여 신청했는지 확인
     */
    private boolean checkIfAlreadyJoined() {
        if (meeting.getParticipants() == null) return false;

        for (MeetingParticipant participant : meeting.getParticipants()) {
            if (participant.getUser_id() == currentUserId ) {
                return true;
            }
        }

        return false;
    }

    /**
     * 참여 신청 확인 다이얼로그 표시
     */
    private void showJoinConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("모임 참여 신청")
                .setMessage("이 모임에 참여 신청하시겠습니까?\n호스트의 승인 후 참여가 확정됩니다.")
                .setPositiveButton("신청하기" , (dialog, which) -> {
                    // 참여 신청 api 호출
                    joinMeeting();
                })
                .setNegativeButton("취소" , null)
                .show();
    }

    /**
     * 참여 신청 API 호출
     */
    private void joinMeeting() {
        if (currentUserId == -1) {
            ToastUtil.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // 버튼 비활성화 (중복 클릭 방지)
        binding.btnJoinMeeting.setEnabled(false);
        binding.btnJoinMeeting.setText("신청 중...");

        // API 호출
        JoinMeetingRequest request = new JoinMeetingRequest(meetingId);
        Call<ApiResponse<ParticipantResponse>> call = apiService.joinMeeting(request);

        call.enqueue(new Callback<ApiResponse<ParticipantResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ParticipantResponse>> call, Response<ApiResponse<ParticipantResponse>> response) {
                if (binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<ParticipantResponse> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        // 신청 성공
                        ParticipantResponse data = apiResponse.getData();
                        Log.i(TAG, "참여 신청 성공: " + data.getParticipation_status());

                        // UI 업데이트
                        binding.btnJoinMeeting.setText("신청 완료");
                        binding.btnJoinMeeting.setEnabled(false);

                        // 성공 메세지
                        ToastUtil.makeText(MeetingDetailActivity.this,
                                "참여 신청이 완료되었습니다.\n호스트의 승인을 기다려주세요.",
                                Toast.LENGTH_LONG).show();

                        // 모임 정보 다시 로드 (참여자 목록 업데이트)
                        loadMeetingDetail();
                    } else {
                        // API 실패
                        Log.e(TAG, "참여 신청 실패: " + apiResponse.getMessage());
                        ToastUtil.makeText(MeetingDetailActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();

                        // 버튼 다시 활성화
                        binding.btnJoinMeeting.setEnabled(true);
                        binding.btnJoinMeeting.setText("참여 신청하기");
                    }
                } else {
                    Log.e(TAG, "HTTP 오류: " + response.code());
                    ToastUtil.makeText(MeetingDetailActivity.this,
                            "참여 신청에 실패했습니다.",
                            Toast.LENGTH_SHORT).show();

                    // 버튼 다시 활성화
                    binding.btnJoinMeeting.setEnabled(true);
                    binding.btnJoinMeeting.setText("참여 신청하기");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ParticipantResponse>> call, Throwable t) {
                Log.e(TAG, "네트워크 오류: " + t.getMessage());
                ToastUtil.makeText(MeetingDetailActivity.this,
                        "네트워크 오류가 발생했습니다.",
                        Toast.LENGTH_SHORT).show();

                // 버튼 다시 활성화
                binding.btnJoinMeeting.setEnabled(true);
                binding.btnJoinMeeting.setText("참여 신청하기");
            }
        });
    }

    /**
     * ActivityResultLauncher
     */
    private final ActivityResultLauncher<Intent> applicantListLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult() ,
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            loadMeetingDetail();
                        }
                    }
            );

    /**
     * editMeetingLauncher
     */

    private final ActivityResultLauncher<Intent> editMeetingLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult() ,
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            // 상세 화면에 머물며 재로딩 (체험단 상세와 동일한 동작)
                            // 홈 목록은 돌아갈 때 HomeFragment.onResume이 갱신함
                            loadMeetingDetail();
                        }
                    }
            );

    /**
     * 호스트용 관리 버튼 표시
     */
    private void showHostManagermentButton() {
        // 버튼 컨테이너 다시 표시
        binding.joinButtonContainer.setVisibility(View.VISIBLE);
        // 버튼 텍스트 변경
        binding.btnJoinMeeting.setText("신청자 목록");
        binding.btnJoinMeeting.setEnabled(true);

        // 클릭 이벤트
        binding.btnJoinMeeting.setOnClickListener(v -> {
            // 신청자 목록 화면으로 이동
            Intent intent = new Intent(this , MeetingApplicantListActivity.class);
            intent.putExtra("meeting_id" , meetingId);
            intent.putExtra("meeting_title" , meeting.getMeeting_title());
            applicantListLauncher.launch(intent);
        });

        // 삭제 / 편집 버튼
        binding.btnMoreOptions.setVisibility(View.VISIBLE);
        binding.btnMoreOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this , binding.btnMoreOptions);
            popup.getMenuInflater().inflate(R.menu.menu_meeting_detail , popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_edit_meeting) {
                    Intent intent = new Intent(this, MeetingEditActivity.class);
                    intent.putExtra("meeting_id", meetingId);
                    editMeetingLauncher.launch(intent);
                    return true;
                } else if (id == R.id.action_delete_meeting) {
                    showDeleteConfirmDialog();
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("모임 삭제")
                .setMessage("모임을 삭제하시겠습니까?\n대기 중인 신청은 모두 취소되며, 채팅방도 함께 삭제됩니다.\n(승인된 참여자가 있으면 삭제할 수 없습니다)")
                .setPositiveButton("삭제", (dialog, which) -> deleteMeeting())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteMeeting() {
        DeleteMeetingRequest request = new DeleteMeetingRequest(meetingId);
        apiService.deleteMeeting(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (binding == null) return;

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ToastUtil.makeText(MeetingDetailActivity.this, "모임이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MeetingDetailActivity.this , MainActivity.class);
                    intent.putExtra("refresh_home" ,true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    String msg = (response.body() != null) ? response.body().getMessage() : "삭제에 실패했습니다.";
                    ToastUtil.makeText(MeetingDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                ToastUtil.makeText(MeetingDetailActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }

        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        binding.miniMap.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.miniMap.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.miniMap.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        binding.miniMap.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) binding.miniMap.onDestroy();
        binding = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        binding.miniMap.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        binding.miniMap.onLowMemory();
    }
}