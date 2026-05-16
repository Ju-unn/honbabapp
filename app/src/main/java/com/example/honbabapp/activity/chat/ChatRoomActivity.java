package com.example.honbabapp.activity.chat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.api.data.chat.MessageData;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.chat.MessageListResponse;
import com.example.honbabapp.databinding.PageChatDetailBinding;
import com.example.honbabapp.network.SocketManager;
import com.example.honbabapp.repository.ChatRepository;
import com.example.honbabapp.ui.adapter.MessageAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import io.socket.emitter.Emitter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRoomActivity extends AppCompatActivity {
    private PageChatDetailBinding binding;
    private MessageAdapter adapter;

    // ChatRoomDetailActivity 결과 받기용 requestCode
    private static final int REQ_DETAIL = 1001;

    // 햄버거 → ChatRoomDetailActivity 로 넘길 meeting_id
    private int meetingId = -1;
    private int myUserId = -1;
    private int roomId = -1;
    private String roomName = "";
    private String roomType = "group";
    private String roomImage = null;
    private int memberCount = 0;

    // 무한 스크롤 페이징
    private int currentPage = 1;
    private boolean isLoadingMore = false;
    private boolean hasMorePages = true;

    // 리스너를 인스턴스 변수로 보관
    // off(이벤트명)는 해당 이벤트의 모든 리스너를 제거함 → SocketManager 리스너까지 날아감
    // off(이벤트명, 특정리스너) 형태로 호출해야 이 Activity가 등록한 것만 제거할 수 있음
    // 맴버가 나갔을때
    // 나갔을때 호스트가 바뀌면
    private Emitter.Listener messageListener;
    private Emitter.Listener readUpdateListener;
    private Emitter.Listener memberJoinedListener;
    private Emitter.Listener memberLeftListener;
    private Emitter.Listener hostChangedListener;



    // ─────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        roomId   = getIntent().getIntExtra("room_id", -1);
        roomName = getIntent().getStringExtra("room_name");
        roomType = getIntent().getStringExtra("room_type");
        memberCount = getIntent().getIntExtra("member_count", 0);
        roomImage = getIntent().getStringExtra("room_image");
        meetingId = getIntent().getIntExtra("meeting_id", -1);
        if (roomName == null) roomName = "";
        if (roomType == null) roomType = "group";

        SharedPreferences prefs = getSharedPreferences("honbabapp_prefs", Context.MODE_PRIVATE);
        myUserId = prefs.getInt("user_id", -1);

        initToolbar();
        initRecyclerView();
        initInputArea();
        loadMessageHistory();
        connectWebSocket();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (roomId != -1 && myUserId != -1) {
            markRead();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // off(이벤트명)는 해당 이벤트의 모든 리스너를 제거 → SocketManager 리스너까지 날아감
        // off(이벤트명, 리스너)로 이 Activity가 등록한 것만 정확히 제거
        if (messageListener != null)
            SocketManager.getInstance().getSocket().off("message", messageListener);
        if (readUpdateListener != null)
            SocketManager.getInstance().getSocket().off("read_update", readUpdateListener);
        if (memberJoinedListener != null)
            SocketManager.getInstance().getSocket().off("member_joined", memberJoinedListener);
        if (memberLeftListener != null)
            SocketManager.getInstance().getSocket().off("member_left", memberLeftListener);
        if (hostChangedListener != null)
            SocketManager.getInstance().getSocket().off("host_changed", hostChangedListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // ChatRoomDetailActivity 에서 "나가기" 했을 때
        if (requestCode == REQ_DETAIL
                && resultCode == RESULT_OK
                && data != null
                && data.getBooleanExtra("left", false)) {
            // 본인도 함께 닫아서 ChatFragment 로 자연스럽게 복귀
            finish();
        }
    }


    // ─────────────────────────────────────────
    // 초기화
    // ─────────────────────────────────────────

    private void initToolbar() {
        binding.tvChatTitle.setText(roomName);

        // 채팅방 이미지
        Glide.with(this)
                .load(roomImage)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(binding.imChatAvatar);

        if ("group".equals(roomType) && memberCount > 0) {
            binding.tvChatStatus.setVisibility(View.VISIBLE);
            binding.tvChatStatus.setText(memberCount + "명");
        } else {
            binding.tvChatStatus.setVisibility(View.GONE);
        }

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnMenu.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatRoomDetailActivity.class);
            intent.putExtra("room_id", roomId);
            intent.putExtra("room_name", roomName);
            intent.putExtra("room_type", roomType);
            intent.putExtra("room_image", roomImage);
            intent.putExtra("meeting_id", meetingId);
            startActivityForResult(intent, REQ_DETAIL);
        });
    }

    private void initRecyclerView() {
        adapter = new MessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);

        // 위로 스크롤해서 첫 번째 아이템이 보이면 이전 페이지 로드
        binding.rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (!isLoadingMore && hasMorePages
                        && lm != null && lm.findFirstVisibleItemPosition() == 0) {
                    loadMoreMessages();
                }
            }
        });
    }

    private void initInputArea() {
        binding.btnSend.setOnClickListener(v -> sendMessage());

        binding.etMessage.setOnEditorActionListener((v, actionID, event) -> {
            if (actionID == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        binding.btnAttach.setOnClickListener(v -> {
            // TODO: 이미지 첨부 기능
        });
    }

    // ─────────────────────────────────────────
    // 메시지 히스토리 로드
    // ─────────────────────────────────────────

    private void loadMessageHistory() {
        if (roomId == -1) return;

        // member_joined 등으로 재호출 시 페이징 상태 초기화
        currentPage = 1;
        hasMorePages = true;

        ChatRepository.getInstance().getMessages(roomId, myUserId, 1,
                new Callback<ApiResponse<MessageListResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<MessageListResponse>> call, Response<ApiResponse<MessageListResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            MessageListResponse data = response.body().getData();
                            if (data != null && data.getMessages() != null) {
                                List<MessageData> history = data.getMessages();
                                for (MessageData msg : history) {
                                    msg.setIs_mine(msg.getSender_id() == myUserId);
                                }
                                adapter.setMessages(history);
                                hasMorePages = data.getTotal_pages() > 1;
                                scrollToBottom();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<MessageListResponse>> call, Throwable t) {
                        // 히스토리 로드 실패 — 빈 화면으로 진입
                    }
                });
    }

    // ─────────────────────────────────────────
    // WebSocket 연결 및 메시지 처리
    // ─────────────────────────────────────────

    private void connectWebSocket() {
        if (myUserId == -1 || roomId == -1) return;

        // 소켓 연결 여부 확인 후 connect()
        SocketManager.getInstance().connect();

        // 람다를 인스턴스 변수에 저장 → onDestroy에서 이 리스너만 정확히 제거 가능
        // 새 메시지 수신
        messageListener = args -> {
            try {
                JSONObject json = (JSONObject) args[0];

                // room_id 가드: 지금 열려있는 방 메시지가 아니면 무시
                // fan-out 구조상 내 모든 방 메시지가 들어오므로 필터 필수
                if (json.getInt("room_id") != roomId) return;

                MessageData msg = new MessageData();
                msg.setMessage_id(json.getInt("message_id"));
                msg.setRoom_id(json.getInt("room_id"));
                msg.setSender_id(json.getInt("user_id"));
                msg.setSender_name(json.optString("sender_name", ""));
                msg.setSender_profile_image(json.optString("sender_profile_image", ""));
                msg.setMsg_type(json.optString("msg_type", "text"));
                msg.setContent(json.getString("content"));
                msg.setCreated_at(json.getString("created_at"));
                msg.setUnread_count(json.optInt("unread_count", 0)); //실시간 unread_count 파싱
                msg.setIs_mine(msg.getSender_id() == myUserId);

                runOnUiThread(() -> {
                    adapter.addMessage(msg);
                    scrollToBottom();
                    markRead();
                });
            } catch (Exception e) {
                // 파싱 실패
            }
        };

        // ─── 읽음 처리 수신 ───
        // newly_read_message_ids 기반으로 해당 메시지만 정확하게 -1
        // reader_id 체크 제거 → B 자신의 read_update도 처리
        readUpdateListener = args -> {
            try {
                JSONObject json = (JSONObject) args[0];

                // room_id 가드: 다른 방의 read_update는 무시
                if (json.getInt("room_id") != roomId) return;

                JSONArray idsArray = json.optJSONArray("newly_read_message_ids");
                if (idsArray == null || idsArray.length() == 0) return;

                Set<Integer> ids = new HashSet<>();
                for (int i = 0; i < idsArray.length(); i++) {
                    ids.add(idsArray.getInt(i));
                }
                runOnUiThread(() -> adapter.decrementUnreadForMessages(ids));
            } catch (Exception e) {}
        };

        // ─── 멤버 참여 수신 ───
        // invite 후 member_joined 수신 → unread_count 재계산 + 인원 수 업데이트
        memberJoinedListener = args -> {
            try {
                JSONObject json = (JSONObject) args[0];

                // room_id 가드: 지금 열려있는 방 이벤트가 아니면 무시
                if (json.getInt("room_id") != roomId) return;

                runOnUiThread(() -> {
                    loadMessageHistory();
                    memberCount++;
                    if ("group".equals(roomType)) {
                        binding.tvChatStatus.setVisibility(View.VISIBLE);
                        binding.tvChatStatus.setText(memberCount + "명");
                    }
                });
            } catch (Exception e) {}
        };


        // ─── 멤버 나감 수신 ───
        // 누군가 채팅방에서 나갔을 때 인원수 갱신
        memberLeftListener = args -> {
            try {
                JSONObject json = (JSONObject) args[0];

                // room_id 가드 : 다른 방 이벤트면 무시
                if (json.getInt("room_id") != roomId) return;

                int remaining = json.optInt("remaining", -1);
                int leftUserId = json.optInt("user_id", -1);

                runOnUiThread(() -> {
                    // 본인이 나간 거면 onActivityResult 가 처리하므로 여기선 다른 사람만
                    if (leftUserId != myUserId) {
                        if (remaining >= 0) {
                            memberCount = remaining;
                        } else {
                            memberCount = Math.max(0, memberCount - 1);
                        }
                        if ("group".equals(roomType)) {
                            binding.tvChatStatus.setText(memberCount + "명");
                        }
                    }
                });
            } catch (Exception e) { /* 파싱 실패 무시 */ }
        };


        // ─── 호스트 변경 수신 ───
        // 위임 발생 시 본인이 새 방장이면 Toast 안내
        hostChangedListener = args -> {
            try {
                JSONObject json = (JSONObject) args[0];

                // room_id 가드
                if (json.getInt("room_id") != roomId) return;

                int newHostId = json.optInt("new_host_id", -1);

                runOnUiThread(() -> {
                    if (newHostId == myUserId) {
                        Toast.makeText(ChatRoomActivity.this,
                                "방장으로 위임되었습니다",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) { /* 파싱 실패 무시 */ }
        };


        SocketManager.getInstance().getSocket().on("host_changed", hostChangedListener);
        SocketManager.getInstance().getSocket().on("member_left", memberLeftListener);
        SocketManager.getInstance().getSocket().on("message", messageListener);
        SocketManager.getInstance().getSocket().on("read_update", readUpdateListener);
        SocketManager.getInstance().getSocket().on("member_joined", memberJoinedListener);
    }

    // ─────────────────────────────────────────
    // 메시지 전송
    // ─────────────────────────────────────────

    private void sendMessage() {
        String content = binding.etMessage.getText().toString().trim();
        if (content.isEmpty()) return;

        binding.etMessage.setText("");
        SocketManager.getInstance().sendMessage(roomId, myUserId, content);
    }

    // ─────────────────────────────────────────
    // 읽음 처리
    // ─────────────────────────────────────────

    private void markRead() {
        SocketManager.getInstance().sendRead(roomId, myUserId);
    }

    // ─────────────────────────────────────────
    // 무한 스크롤 — 이전 페이지 로드
    // ─────────────────────────────────────────

    private void loadMoreMessages() {
        isLoadingMore = true;
        int nextPage = currentPage + 1;

        ChatRepository.getInstance().getMessages(roomId, myUserId, nextPage,
                new Callback<ApiResponse<MessageListResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<MessageListResponse>> call,
                                           Response<ApiResponse<MessageListResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            MessageListResponse data = response.body().getData();
                            if (data != null && data.getMessages() != null
                                    && !data.getMessages().isEmpty()) {

                                List<MessageData> older = data.getMessages();
                                for (MessageData m : older) {
                                    m.setIs_mine(m.getSender_id() == myUserId);
                                }

                                // 삽입 전 첫 번째 아이템 위치 저장 → 삽입 후 스크롤 유지
                                int firstVisible = ((LinearLayoutManager) binding.rvMessages
                                        .getLayoutManager()).findFirstVisibleItemPosition();

                                adapter.prependMessages(older);

                                // 이전 메시지가 삽입되면 스크롤이 맨 위로 튀지 않게 보정
                                binding.rvMessages.scrollToPosition(
                                        older.size() + firstVisible);

                                currentPage = nextPage;
                                hasMorePages = nextPage < data.getTotal_pages();
                            } else {
                                hasMorePages = false;
                            }
                        } else {
                            hasMorePages = false;
                        }
                        isLoadingMore = false;
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<MessageListResponse>> call, Throwable t) {
                        isLoadingMore = false;
                    }
                });
    }

    // ─────────────────────────────────────────
    // 유틸
    // ─────────────────────────────────────────

    private void scrollToBottom() {
        int count = adapter.getItemCount();
        if (count > 0) {
            binding.rvMessages.scrollToPosition(count - 1);
        }
    }
}