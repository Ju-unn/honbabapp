package com.example.honbabapp.activity.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.honbabapp.api.data.chat.MessageData;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.chat.MessageListResponse;
import com.example.honbabapp.databinding.PageChatDetailBinding;
import com.example.honbabapp.network.SocketManager;
import com.example.honbabapp.repository.ChatRepository;
import com.example.honbabapp.ui.adapter.MessageAdapter;

import org.json.JSONObject;

import io.socket.emitter.Emitter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRoomActivity extends AppCompatActivity {
    private PageChatDetailBinding binding;
    private MessageAdapter adapter;

    private int myUserId = -1;
    private int roomId = -1;
    private String roomName = "";
    private String roomType = "group";

    // 리스너를 인스턴스 변수로 보관
    // off(이벤트명)는 해당 이벤트의 모든 리스너를 제거함 → SocketManager 리스너까지 날아감
    // off(이벤트명, 특정리스너) 형태로 호출해야 이 Activity가 등록한 것만 제거할 수 있음
    private Emitter.Listener messageListener;
    private Emitter.Listener readUpdateListener;

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
    }

    // ─────────────────────────────────────────
    // 초기화
    // ─────────────────────────────────────────

    private void initToolbar() {
        binding.tvChatStatus.setText(roomName);
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnMenu.setOnClickListener(v -> {
            // TODO: 채팅방 메뉴 팝업 (나가기, 초대 등)
        });
    }

    private void initRecyclerView() {
        adapter = new MessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);
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

        // 소켓 연결 여부 확인 후 connect() — 이미 연결됐으면 SocketManager 내부에서 무시됨
        SocketManager.getInstance().connect();
        SocketManager.getInstance().joinRoom(roomId, myUserId);

        // 람다를 인스턴스 변수에 저장 → onDestroy에서 이 리스너만 정확히 제거 가능
        // 새 메시지 수신
        messageListener = args -> {
            try {
                JSONObject json = (JSONObject) args[0];
                MessageData msg = new MessageData();
                msg.setMessage_id(json.getInt("message_id"));
                msg.setRoom_id(json.getInt("room_id"));
                msg.setSender_id(json.getInt("user_id"));
                msg.setSender_name(json.optString("sender_name", ""));
                msg.setSender_profile_image(json.optString("sender_profile_image", ""));
                msg.setMsg_type(json.optString("msg_type", "text"));
                msg.setContent(json.getString("content"));
                msg.setCreated_at(json.getString("created_at"));
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

        // 읽음 업데이트 수신 — 상대방이 읽었을 때 내 메시지 읽음 숫자 실시간 감소
        readUpdateListener = args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                int readerId = data.getInt("reader_id");

                // 내가 읽은 건 이미 반영됨 — 상대방이 읽었을 때만 처리
                if (readerId != myUserId) {
                    runOnUiThread(() -> adapter.decreaseUnreadCount(myUserId));
                }
            } catch (Exception e) {
                // 파싱 실패
            }
        };

        SocketManager.getInstance().getSocket().on("message", messageListener);
        SocketManager.getInstance().getSocket().on("read_update", readUpdateListener);
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
    // 유틸
    // ─────────────────────────────────────────

    private void scrollToBottom() {
        int count = adapter.getItemCount();
        if (count > 0) {
            binding.rvMessages.scrollToPosition(count - 1);
        }
    }
}