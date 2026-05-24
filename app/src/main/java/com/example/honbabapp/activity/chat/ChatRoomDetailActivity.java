package com.example.honbabapp.activity.chat;

import android.app.AlertDialog;
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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.activity.meeting.MeetingDetailActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.chat.ChatMemberData;
import com.example.honbabapp.api.data.chat.ChatPhotoData;
import com.example.honbabapp.api.data.chat.ChatRoomData;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.meeting.MeetingParticipant;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.chat.ChatPhotosResponse;
import com.example.honbabapp.api.data.response.chat.KickMemberResponse;
import com.example.honbabapp.api.data.response.chat.LeaveRoomResponse;
import com.example.honbabapp.databinding.PageChatRoomDetailBinding;
import com.example.honbabapp.repository.ChatRepository;
import com.example.honbabapp.ui.adapter.ChatParticipantAdapter;
import com.example.honbabapp.ui.adapter.ChatPhotoPreviewAdapter;
import com.example.honbabapp.ui.dialog.ChatParticipantActionBottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 채팅방 상세 (햄버거) 메인 페이지
 *
 * 진입 경로:
 *   ChatRoomActivity 의 햄버거 버튼 → startActivityForResult
 *
 * 구성:
 *   1) 상단 헤더 카드 (방 이미지/이름/부제/인원수)
 *      → 클릭 시 group=MeetingDetailActivity / direct=ChatRoomParticipantDetailActivity
 *   2) 사진 가로 미리보기 (최대 10장, 더보기 → ChatRoomPhotosActivity)
 *   3) 참가자 세로 전체 리스트 (행 클릭 → ChatRoomParticipantDetailActivity)
 *   4) 나가기 버튼 (다이얼로그 → leaveRoom API → finish with RESULT_OK)
 *
 * 나갈 때:
 *   setResult(RESULT_OK, Intent.putExtra("left", true)) + finish()
 *   → ChatRoomActivity.onActivityResult 에서 본인도 finish
 */
public class ChatRoomDetailActivity extends AppCompatActivity {

    private static final String TAG = "ChatRoomDetailActivity";

    // SharedPreferences
    private static final String PREF_NAME = "honbabapp_prefs";
    private static final String KEY_USER_ID = "user_id";

    // ─────────────────────────────────────────
    // ViewBinding & API
    // ─────────────────────────────────────────
    private PageChatRoomDetailBinding binding;
    private ApiService apiService;

    // ─────────────────────────────────────────
    // 어댑터
    // ─────────────────────────────────────────
    private ChatParticipantAdapter participantAdapter;
    private ChatPhotoPreviewAdapter photoAdapter;

    // ─────────────────────────────────────────
    // Intent extra (ChatRoomActivity 에서 전달)
    // ─────────────────────────────────────────
    private int    roomId      = -1;
    private String roomName    = "";
    private String roomType    = "group";   // "group" or "direct"
    private String roomImage   = null;
    private int    meetingId   = -1;        // direct 면 0 또는 미사용

    // ─────────────────────────────────────────
    // 런타임 상태 (API 응답 후 확정됨)
    // ─────────────────────────────────────────
    private int    myUserId    = -1;
    private Meeting meeting;             // getMeetingDetail 응답
    private int    otherUserId = -1;     // direct 방 상대방 user_id

    // 화면 흐름 결과 플래그
    public static final String EXTRA_LEFT = "left";


    // ═════════════════════════════════════════
    // onCreate
    // ═════════════════════════════════════════

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageChatRoomDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ── Intent extra 받기 ──
        roomId    = getIntent().getIntExtra("room_id", -1);
        roomName  = getIntent().getStringExtra("room_name");
        roomType  = getIntent().getStringExtra("room_type");
        roomImage = getIntent().getStringExtra("room_image");
        meetingId = getIntent().getIntExtra("meeting_id", -1);

        if (roomId == -1) {
            Toast.makeText(this, "잘못된 접근입니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ── 현재 user_id ──
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        myUserId = prefs.getInt(KEY_USER_ID, -1);

        // ── API 초기화 ──
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // ── UI 세팅 (즉시 보일 수 있는 부분 먼저) ──
        setupHeader();
        setupRecyclerViews();
        setupClickListeners();

        // ── API 호출 ──
        if ("group".equals(roomType) && meetingId != -1) {
            loadMeetingDetail();
        } else {
            // direct 방: 채팅방 멤버 정보 조회 → 참가자 섹션 표시
            loadChatRoomMembers();
        }

        loadChatPhotos();
    }


    // ═════════════════════════════════════════
    // 초기 UI 세팅
    // ═════════════════════════════════════════

    /** 헤더 카드 초기 표시 (Intent extra 만으로 일단 채움) */
    private void setupHeader() {
        binding.tvRoomTitle.setText(roomName != null ? roomName : "");

        // 방 이미지 (Glide 로 로드, 실패 시 기본 프로필)
        Glide.with(this)
                .load(roomImage)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .centerCrop()
                .into(binding.ivRoomImage);
    }

    /** RecyclerView 2개 세팅 */
    private void setupRecyclerViews() {
        // 참가자 (세로)
        participantAdapter = new ChatParticipantAdapter(participant -> this.onParticipantClick(participant));
        //   - currentUserId : 내가 누군지 (방장인지 판단용)
        //   - menu listener : 3-dot 눌렸을 때 호출될 콜백
        participantAdapter.setCurrentUserId(myUserId);
        participantAdapter.setOnMenuClickListener(participant -> this.onKickMenuClick(participant));

        binding.rvParticipants.setLayoutManager(new LinearLayoutManager(this));
        binding.rvParticipants.setAdapter(participantAdapter);

        // 사진 (가로)
        photoAdapter = new ChatPhotoPreviewAdapter();
        binding.rvPhotoPreview.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvPhotoPreview.setAdapter(photoAdapter);
    }

    /** 클릭 리스너 일괄 등록 */
    private void setupClickListeners() {
        // 뒤로가기
        binding.btnBack.setOnClickListener(v -> finish());

        // 헤더 카드 → group=MeetingDetail / direct=상대방 프로필
        binding.cardRoomHeader.setOnClickListener(v -> onHeaderCardClick());

        // 사진 섹션 헤더 (더보기) → ChatRoomPhotosActivity
        binding.layoutPhotoHeader.setOnClickListener(v -> openPhotosActivity());

        // 나가기 버튼 → 다이얼로그
        binding.btnLeaveRoom.setOnClickListener(v -> showLeaveDialog());
    }


    // ═════════════════════════════════════════
    // API 호출
    // ═════════════════════════════════════════

    /**
     * 모임 상세 API → 참가자 리스트, 호스트, meeting_date, 한줄소개 등 채움
     * (group 방에서만 호출)
     */
    private void loadMeetingDetail() {
        apiService.getMeetingDetail(meetingId, myUserId)
                .enqueue(new Callback<ApiResponse<Meeting>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Meeting>> call,
                                           @NonNull Response<ApiResponse<Meeting>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            meeting = response.body().getData();
                            bindMeetingData();

                        } else {
                            Log.e(TAG, "모임 상세 조회 실패: "
                                    + (response.body() != null ? response.body().getMessage() : "null"));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Meeting>> call, @NonNull Throwable t) {
                        Log.e(TAG, "모임 상세 네트워크 오류: " + t.getMessage());
                    }
                });
    }

    /**
     * 채팅방 사진 미리보기 (최대 10장)
     */
    private void loadChatPhotos() {
        ChatRepository.getInstance().getChatPhotos(roomId, myUserId, 1, 10,
                new Callback<ApiResponse<ChatPhotosResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ChatPhotosResponse>> call,
                                           @NonNull Response<ApiResponse<ChatPhotosResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            List<ChatPhotoData> photos = response.body().getData().getPhotos();
                            bindPhotosData(photos);

                        } else {
                            // 실패해도 빈 상태로 표시
                            bindPhotosData(null);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ChatPhotosResponse>> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "사진 조회 네트워크 오류: " + t.getMessage());
                        bindPhotosData(null);
                    }
                });
    }
    /**
     * 1:1 (direct) 방의 멤버 정보 조회 (참가자 섹션 표시용)
     *
     * 별도 API 없이 ChatRepository.getChatRooms() 응답에서 현재 room 을 필터링한다.
     *  - 방장 뱃지는 direct 방에선 의미 없음 → hostId = -1
     */
    private void loadChatRoomMembers() {
        ChatRepository.getInstance().getChatRooms(myUserId,
                new Callback<ApiResponse<List<ChatRoomData>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<ChatRoomData>>> call,
                                           @NonNull Response<ApiResponse<List<ChatRoomData>>> response) {
                        if (!response.isSuccessful()
                                || response.body() == null
                                || !response.body().isSuccess()
                                || response.body().getData() == null) {
                            Log.e(TAG, "채팅방 목록 조회 실패");
                            return;
                        }

                        // 응답에서 현재 room 찾기
                        ChatRoomData currentRoom = null;
                        for (ChatRoomData r : response.body().getData()) {
                            if (r.getRoom_id() == roomId) {
                                currentRoom = r;
                                break;
                            }
                        }

                        if (currentRoom == null
                                || currentRoom.getParticipants() == null
                                || currentRoom.getParticipants().isEmpty()) {
                            Log.w(TAG, "현재 채팅방 또는 참가자 정보 없음");
                            return;
                        }

                        // ChatMemberData → MeetingParticipant 변환
                        List<MeetingParticipant> participants = new ArrayList<>();
                        for (ChatMemberData m : currentRoom.getParticipants()) {
                            participants.add(toMeetingParticipant(m));
                            // 상대방 user_id 저장 (헤더 카드 클릭 시 사용)
                            if (m.getUser_id() != myUserId) {
                                otherUserId = m.getUser_id();
                            }
                        }

                        // 어댑터에 주입
                        participantAdapter.setHostId(-1);  // direct 방은 방장 없음
                        participantAdapter.setItems(participants);

                        // 헤더 "참가자 (N)"
                        binding.tvParticipantsTitle.setText("참가자 (" + participants.size() + ")");
                        binding.tvParticipantsTitle.setVisibility(View.VISIBLE);
                        binding.rvParticipants.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<ChatRoomData>>> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "채팅방 멤버 조회 네트워크 오류: " + t.getMessage());
                    }
                });
    }



    // ═════════════════════════════════════════
    // 데이터 → UI 바인딩
    // ═════════════════════════════════════════

    /** Meeting 응답을 헤더 + 참가자 RecyclerView 에 반영 */
    private void bindMeetingData() {
        if (meeting == null) return;

        // 부제: 모임 설명 (없으면 위치)
        String subtitle = meeting.getMeeting_description();
        if (subtitle == null || subtitle.isEmpty()) {
            // location 객체에서 가져오기 (없으면 빈 문자열)
            subtitle = (meeting.getLocation() != null)
                    ? meeting.getLocation().getPlace_name()
                    : "";
        }
        binding.tvRoomSubtitle.setText(subtitle);

        // 인원수 (group 전용)
        String count = meeting.getMeeting_current_participants() + "/"
                + meeting.getMeeting_max_participants() + "명";
        binding.tvMemberCount.setText(count);
        binding.tvMemberCount.setVisibility(View.VISIBLE);

        // 참가자 리스트
        List<MeetingParticipant> participants = meeting.getParticipants();
        if (participants != null) {
            // 호스트 ID 주입 → 어댑터가 방장 뱃지 표시
            int hostId = (meeting.getHost() != null) ? meeting.getHost().getUser_id() : -1;
            participantAdapter.setHostId(hostId);
            participantAdapter.setItems(participants);

            // 헤더 "참가자 (N)"
            binding.tvParticipantsTitle.setText("참가자 (" + participants.size() + ")");
        }
    }

    /** 사진 응답을 미리보기 RecyclerView 에 반영 */
    private void bindPhotosData(@Nullable List<ChatPhotoData> photos) {
        boolean empty = (photos == null || photos.isEmpty());
        if (empty) {
            binding.rvPhotoPreview.setVisibility(View.GONE);
            binding.tvPhotoEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.rvPhotoPreview.setVisibility(View.VISIBLE);
            binding.tvPhotoEmpty.setVisibility(View.GONE);
            photoAdapter.setItems(photos);
        }
    }

    /**
     * ChatMemberData → MeetingParticipant 변환 (1:1 방 멤버 표시용)
     *
     * ChatParticipantAdapter는 MeetingParticipant 만 받기 때문에
     * direct 방에서 ChatMemberData 를 어댑터에 넣으려면 변환이 필요하다.
     *
     * 매너점수는 rooms.php 응답에 없으므로 기본값(0.0) 사용 — direct 방 어댑터 UI 에서는 안 보임
     */
    private MeetingParticipant toMeetingParticipant(ChatMemberData member) {
        MeetingParticipant meetingParticipant = new MeetingParticipant();
        meetingParticipant.setUser_id(member.getUser_id());
        meetingParticipant.setUser_nickname(member.getUser_nickname());
        meetingParticipant.setUser_profile_image(member.getUser_profile_image());
        return meetingParticipant;
    }


    // ═════════════════════════════════════════
    // 클릭 핸들러
    // ═════════════════════════════════════════

    /** 헤더 카드 클릭: group=모임상세, direct=상대방 프로필 */
    private void onHeaderCardClick() {
        if ("group".equals(roomType)) {
            if (meetingId == -1) return;
            Intent intent = new Intent(this, MeetingDetailActivity.class);
            intent.putExtra("meeting_id", meetingId);
            startActivity(intent);
        } else {
            // direct: 상대방 user_id 추출 → 프로필 상세
            // 1:1 방은 멤버 2명, 본인 제외하면 상대방
            // (참가자 리스트가 비어있으므로 별도 API 또는 ChatRoomData 전달 필요)
            if (otherUserId == -1) {
                Toast.makeText(this, "상대방 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, ChatRoomParticipantDetailActivity.class);
            intent.putExtra("user_id", otherUserId);
            startActivity(intent);
        }
    }

    /** 참가자 행 클릭 → 프로필 상세 */
    private void onParticipantClick(MeetingParticipant participant) {
        Intent intent = new Intent(this, ChatRoomParticipantDetailActivity.class);
        intent.putExtra("user_id", participant.getUser_id());
        startActivity(intent);
    }

    /** 사진 더보기 클릭 → 그리드 페이지 */
    private void openPhotosActivity() {
        Intent intent = new Intent(this, ChatRoomPhotosActivity.class);
        intent.putExtra("room_id", roomId);
        startActivity(intent);
    }


    // ═════════════════════════════════════════
    // 나가기 다이얼로그 + API
    // ═════════════════════════════════════════

    /** 나가기 확인 다이얼로그 표시 */
    private void showLeaveDialog() {
        // 본인이 호스트인가?
        boolean isHost = false;
        int hoursUntil = 999;          // 모임 정보 없으면 D-3 이전으로 간주
        int otherMembers = 0;

        if ("group".equals(roomType) && meeting != null) {
            if (meeting.getHost() != null) {
                isHost = (meeting.getHost().getUser_id() == myUserId);
            }
            hoursUntil = calculateHoursUntilMeeting(meeting.getMeeting_date());
            // 본인 제외한 다른 멤버 수
            otherMembers = meeting.getMeeting_current_participants() - 1;
            if (otherMembers < 0) otherMembers = 0;
        }

        String message = buildLeaveDialogMessage(isHost, hoursUntil, otherMembers);

        new AlertDialog.Builder(this)
                .setTitle("채팅방 나가기")
                .setMessage(message)
                .setPositiveButton("나가기", (dialog, which) -> performLeave())
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 다이얼로그 문구 매트릭스 (정책 7종)
     *
     *   is_host=true  + D-3 이전 + 참여자≥1 : "방장이 위임됩니다. 정말 나가시겠어요?"
     *   is_host=true  + D-3 이전 + 참여자 0 : "모임이 삭제됩니다. 그래도 괜찮나요?"
     *   is_host=true  + D-2              : "방장이 위임되고 매너점수 -0.3이 차감됩니다."
     *   is_host=true  + D-1 이내          : "방장이 위임되고 매너점수 -1.0이 차감됩니다."
     *   is_host=false + D-3 이전          : "채팅방을 나가면 모임을 나가게 됩니다. ..."
     *   is_host=false + D-2              : "매너점수 -0.3이 차감됩니다. ..."
     *   is_host=false + D-1 이내          : "매너점수 -1.0이 차감됩니다. ..."
     */
    private String buildLeaveDialogMessage(boolean isHost, int hoursUntil, int otherMembers) {
        if (isHost) {
            // 호스트 혼자인 경우 → 모임 삭제, 매너점수 차감 없음 (날짜 무관)
            if (otherMembers == 0) {
                return "채팅방 참여자가 혼자일때 나가면 모임이 삭제됩니다. 그래도 괜찮나요?";
            }
            // ─── 아래는 "다른 멤버가 있는" 경우 ───
            if (hoursUntil >= 48) {
                return "방장이 위임됩니다. 정말 나가시겠어요?";
            }
            if (hoursUntil >= 24) {
                return "방장이 위임되고 매너점수 -0.3이 차감됩니다.";
            }
            return "방장이 위임되고 매너점수 -1.0이 차감됩니다.";
        } else {
            if (hoursUntil >= 48) {
                return "채팅방을 나가면 모임을 나가게 됩니다. 그래도 괜찮나요?";
            }
            if (hoursUntil >= 24) {
                return "매너점수 -0.3이 차감됩니다. 정말 나가시겠어요?";
            }
            return "매너점수 -1.0이 차감됩니다. 정말 나가시겠어요?";
        }
    }

    /**
     * 모임 시작까지 남은 시간 (시간 단위)
     * - 파싱 실패 시 999 (D-3 이전으로 간주)
     */
    private int calculateHoursUntilMeeting(String meetingDateStr) {
        if (meetingDateStr == null || meetingDateStr.isEmpty()) return 999;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(meetingDateStr);
            if (date == null) return 999;
            long diffMillis = date.getTime() - System.currentTimeMillis();
            return (int) (diffMillis / (1000L * 60 * 60));
        } catch (Exception e) {
            Log.e(TAG, "meeting_date 파싱 실패: " + meetingDateStr, e);
            return 999;
        }
    }

    /** 실제 leaveRoom API 호출 */
    private void performLeave() {
        ChatRepository.getInstance().leaveRoom(roomId, myUserId,
                new Callback<ApiResponse<LeaveRoomResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<LeaveRoomResponse>> call,
                                           @NonNull Response<ApiResponse<LeaveRoomResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            LeaveRoomResponse data = response.body().getData();

                            // 사용자 피드백 (간단한 Toast)
                            String msg = "채팅방을 나갔습니다.";
                            if (data.getPenalty_applied() > 0) {
                                msg = String.format(Locale.KOREA,
                                        "채팅방을 나갔습니다. (매너점수 -%.1f)",
                                        data.getPenalty_applied());
                            }
                            Toast.makeText(ChatRoomDetailActivity.this, msg, Toast.LENGTH_SHORT).show();

                            // ChatRoomActivity 로 결과 전달 → 그쪽도 finish 하도록
                            Intent result = new Intent();
                            result.putExtra(EXTRA_LEFT, true);
                            setResult(RESULT_OK, result);
                            finish();

                        } else {
                            String errMsg = (response.body() != null)
                                    ? response.body().getMessage()
                                    : "나가기에 실패했습니다";
                            Toast.makeText(ChatRoomDetailActivity.this, errMsg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<LeaveRoomResponse>> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "leaveRoom 네트워크 오류: " + t.getMessage());
                        Toast.makeText(ChatRoomDetailActivity.this,
                                "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ═════════════════════════════════════════
    // 강퇴 흐름 (3-dot 메뉴 → 바텀시트 → 확인 → API)
    // ═════════════════════════════════════════

    /**
     * 1단계 : 3-dot 메뉴 클릭 → 바텀시트 띄움
     */
    private void onKickMenuClick(MeetingParticipant participant) {
        ChatParticipantActionBottomSheetDialog dialog =
                new ChatParticipantActionBottomSheetDialog();
        dialog.participant = participant;
        dialog.listener    = this::confirmKick;   // 강퇴 버튼 누르면 confirmKick 호출
        dialog.show(getSupportFragmentManager(), "participantAction");
    }

    /**
     * 2단계 : "강퇴하기" 클릭 → 확인 다이얼로그
     *
     * 강퇴는 되돌릴 수 없는 행위라 한 번 더 확인.
     * 매너점수 차감 정책은 없지만 "재신청 불가" 라는 영구적 영향이 있음.
     */
    private void confirmKick(MeetingParticipant participant) {
        new AlertDialog.Builder(this)
                .setTitle("강퇴 확인")
                .setMessage(participant.getUser_nickname()
                        + "님을 정말 강퇴하시겠어요?\n\n강퇴된 멤버는 같은 모임에 다시 신청할 수 없습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("강퇴", (dialog, which) -> performKick(participant))
                .show();
    }

    /**
     * 3단계 : 실제 강퇴 API 호출 (Repository 경유)
     */
    private void performKick(MeetingParticipant participant) {
        ChatRepository.getInstance().kickMember(
                roomId,
                myUserId,
                participant.getUser_id(),
                new Callback<ApiResponse<KickMemberResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<KickMemberResponse>> call,
                                           @NonNull Response<ApiResponse<KickMemberResponse>> response) {

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            Toast.makeText(ChatRoomDetailActivity.this,
                                    participant.getUser_nickname() + "님을 강퇴했습니다.",
                                    Toast.LENGTH_SHORT).show();

                            // 참가자 리스트 + 인원수 갱신
                            loadMeetingDetail();

                        } else {
                            String errMsg = (response.body() != null)
                                    ? response.body().getMessage()
                                    : "강퇴에 실패했습니다";
                            Toast.makeText(ChatRoomDetailActivity.this,
                                    errMsg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<KickMemberResponse>> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "kickMember 네트워크 오류: " + t.getMessage());
                        Toast.makeText(ChatRoomDetailActivity.this,
                                "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    }
                });
    }


}