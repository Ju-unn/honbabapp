package com.example.honbabapp.activity.chat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.BuildConfig;
import com.example.honbabapp.R;
import com.example.honbabapp.api.data.chat.MessageData;
import com.example.honbabapp.api.data.local.AppDatabase;
import com.example.honbabapp.api.data.local.PendingMessageDao;
import com.example.honbabapp.api.data.local.PendingMessageStore;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.chat.MessageListResponse;
import com.example.honbabapp.databinding.PageChatDetailBinding;
import com.example.honbabapp.network.SocketManager;
import com.example.honbabapp.repository.ChatRepository;
import com.example.honbabapp.ui.adapter.MessageAdapter;
import com.example.honbabapp.ui.dialog.ChatAttachBottomSheetDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRoomActivity extends AppCompatActivity {
    private PageChatDetailBinding binding;
    private MessageAdapter adapter;

    private PendingMessageStore pendingStore;
    private ChatRoomSocketHandler socketHandler;
    private ChatImageUploadHelper uploadHelper;

    private final Map<String, Handler> pendingTimeoutHandlers = new HashMap<>();

    private static final int REQ_DETAIL = 1001;

    private int meetingId = -1;
    private int myUserId = -1;
    private int roomId = -1;
    private String roomName = "";
    private String roomType = "group";
    private String roomImage = null;
    private int memberCount = 0;

    private int currentPage = 1;
    private boolean isLoadingMore = false;
    private boolean hasMorePages = true;

    private static final int MAX_GALLERY_PICK = 10;

    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.PickMultipleVisualMedia(MAX_GALLERY_PICK),
                    uris -> {
                        if (uris == null || uris.isEmpty()) return;
                        uploadHelper.startSequentialUpload(uris);
                    }
            );

    private Uri pendingCameraUri = null;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            launchCamera();
                        } else {
                            Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    success -> {
                        if (success && pendingCameraUri != null) {
                            List<Uri> single = new ArrayList<>();
                            single.add(pendingCameraUri);
                            uploadHelper.startSequentialUpload(single);
                        } else {
                            Log.d("CHAT_UPLOAD", "카메라 촬영 취소 또는 실패");
                        }
                        pendingCameraUri = null;
                    }
            );

    // ─────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        roomId      = getIntent().getIntExtra("room_id", -1);
        roomName    = getIntent().getStringExtra("room_name");
        roomType    = getIntent().getStringExtra("room_type");
        memberCount = getIntent().getIntExtra("member_count", 0);
        roomImage   = getIntent().getStringExtra("room_image");
        meetingId   = getIntent().getIntExtra("meeting_id", -1);
        if (roomName == null) roomName = "";
        if (roomType == null) roomType = "group";

        SharedPreferences prefs = getSharedPreferences("honbabapp_prefs", Context.MODE_PRIVATE);
        myUserId = prefs.getInt("user_id", -1);

        Context appContext = getApplicationContext();
        PendingMessageDao pendingMessageDao = AppDatabase.getInstance(appContext).pendingMessageDao();
        File pendingImageRoot = new File(appContext.getFilesDir(), "pending");
        pendingStore = new PendingMessageStore(appContext, pendingMessageDao, pendingImageRoot);

        socketHandler = new ChatRoomSocketHandler(roomId, myUserId, new ChatRoomSocketHandler.Callbacks() {
            @Override
            public void onMessageReceived(MessageData message, String clientMsgId) {
                runOnUiThread(() -> {
                    if (clientMsgId != null) {
                        Handler handler = pendingTimeoutHandlers.remove(clientMsgId);
                        if (handler != null) handler.removeCallbacksAndMessages(null);
                    }
                    if (clientMsgId != null && !clientMsgId.isEmpty()) {
                        adapter.confirmMessage(clientMsgId, message);
                        pendingStore.remove(clientMsgId);
                    } else {
                        adapter.addIncomingSent(message);
                    }
                    scrollToBottom();
                    markRead();
                });
            }

            @Override
            public void onReadUpdate(Set<Integer> ids) {
                runOnUiThread(() -> adapter.decrementUnreadForMessages(ids));
            }

            @Override
            public void onMemberJoined() {
                runOnUiThread(() -> {
                    loadMessageHistory();
                    memberCount++;
                    if ("group".equals(roomType)) {
                        binding.tvChatStatus.setVisibility(View.VISIBLE);
                        binding.tvChatStatus.setText(memberCount + "명");
                    }
                });
            }

            @Override
            public void onMemberLeft(int leftUserId, int remaining) {
                runOnUiThread(() -> {
                    if (leftUserId != myUserId) {
                        memberCount = (remaining >= 0) ? remaining : Math.max(0, memberCount - 1);
                        if ("group".equals(roomType)) {
                            binding.tvChatStatus.setText(memberCount + "명");
                        }
                    }
                });
            }

            @Override
            public void onHostChanged(int newHostId) {
                runOnUiThread(() -> {
                    if (newHostId == myUserId) {
                        Toast.makeText(ChatRoomActivity.this,
                                "방장으로 위임되었습니다", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onKicked() {
                runOnUiThread(() -> {
                    Toast.makeText(ChatRoomActivity.this,
                            "방장에 의해 강퇴되었습니다", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });

        uploadHelper = new ChatImageUploadHelper(this, roomId, myUserId, pendingStore,
                new ChatImageUploadHelper.Callbacks() {
                    @Override
                    public void onOptimisticMessage(MessageData optimistic) {
                        runOnUiThread(() -> {
                            adapter.addMessage(optimistic);
                            scrollToBottom();
                            pendingStore.save(optimistic);
                        });
                    }

                    @Override
                    public void onAttachButtonEnabled(boolean enabled) {
                        runOnUiThread(() -> binding.btnAttach.setEnabled(enabled));
                    }

                    @Override
                    public void onArmSendTimeout(String clientMsgId) {
                        armSendTimeout(clientMsgId);
                    }

                    @Override
                    public void onFailLocally(String clientMsgId) {
                        runOnUiThread(() -> failLocally(clientMsgId));
                    }
                });

        initToolbar();
        initRecyclerView();
        initInputArea();
        loadMessageHistory();
        socketHandler.register();
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
        socketHandler.unregister();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_DETAIL
                && resultCode == RESULT_OK
                && data != null
                && data.getBooleanExtra("left", false)) {
            finish();
        }

        if (requestCode == REQ_DETAIL) {
            currentPage = 1;
            hasMorePages = true;
            loadMessageHistory();
        }
    }

    // ─────────────────────────────────────────
    // 초기화
    // ─────────────────────────────────────────

    private void initToolbar() {
        binding.tvChatTitle.setText(roomName);

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
        adapter = new MessageAdapter(senderId -> {
            Intent intent = new Intent(this, ChatRoomParticipantDetailActivity.class);
            intent.putExtra("user_id", senderId);
            startActivity(intent);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);

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

        adapter.setFailedMessageListener(new MessageAdapter.OnFailedMessageListener() {
            @Override
            public void onRetry(MessageData msg) {
                msg.setStatus(MessageData.STATUS_SENDING);
                adapter.notifyItemChanged(adapter.getMessages().indexOf(msg));

                if (msg.isImage()) {
                    List<Uri> localUris = msg.getLocalUris();
                    if (localUris != null && !localUris.isEmpty()) {
                        retryUpload(msg, localUris);
                    }
                } else {
                    retryText(msg);
                }
            }

            @Override
            public void onDelete(MessageData msg) {
                adapter.removeByClientMsgId(msg.getClient_msg_id());
                pendingStore.remove(msg.getClient_msg_id());
            }
        });

        adapter.setImageClickListener((urls, startIndex) -> {
            Intent intent = new Intent(this, FullScreenImageActivity.class);
            intent.putStringArrayListExtra(
                    FullScreenImageActivity.EXTRA_IMAGE_URLS, new ArrayList<>(urls));
            intent.putExtra(FullScreenImageActivity.EXTRA_START_INDEX, startIndex);
            startActivity(intent);
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
            if (uploadHelper.isUploading()) {
                Toast.makeText(this, "사진 전송 중입니다", Toast.LENGTH_SHORT).show();
                return;
            }
            showAttachBottomSheet();
        });
    }

    // ─────────────────────────────────────────
    // 이미지 첨부 — 바텀시트 / 카메라
    // ─────────────────────────────────────────

    private void showAttachBottomSheet() {
        ChatAttachBottomSheetDialog dialog = new ChatAttachBottomSheetDialog();
        dialog.listener = new ChatAttachBottomSheetDialog.OnAttachActionListener() {
            @Override
            public void onPickGallery() {
                galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            }

            @Override
            public void onPickCamera() {
                int status = ContextCompat.checkSelfPermission(
                        ChatRoomActivity.this, Manifest.permission.CAMERA);
                if (status == PackageManager.PERMISSION_GRANTED) {
                    launchCamera();
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            }
        };
        dialog.show(getSupportFragmentManager(), "chat_attach");
    }

    private void launchCamera() {
        try {
            File dir = new File(getCacheDir(), "chat_images");
            if (!dir.exists() && !dir.mkdirs()) {
                Log.e("CHAT_UPLOAD", "chat_images 디렉토리 생성 실패");
                Toast.makeText(this, "카메라 준비 실패", Toast.LENGTH_SHORT).show();
                return;
            }

            File photoFile = new File(dir, "cam_" + System.currentTimeMillis() + ".jpg");

            Uri uri = FileProvider.getUriForFile(
                    this,
                    BuildConfig.APPLICATION_ID + ".fileprovider",
                    photoFile);

            pendingCameraUri = uri;
            cameraLauncher.launch(uri);

        } catch (Exception e) {
            Log.e("CHAT_UPLOAD", "카메라 실행 실패: " + e.getMessage());
            Toast.makeText(this, "카메라 실행 실패", Toast.LENGTH_SHORT).show();
            pendingCameraUri = null;
        }
    }

    // ─────────────────────────────────────────
    // 메시지 히스토리 로드
    // ─────────────────────────────────────────

    private void loadMessageHistory() {
        if (roomId == -1) return;

        currentPage = 1;
        hasMorePages = true;

        ChatRepository.getInstance().getMessages(roomId, myUserId, 1,
                new Callback<ApiResponse<MessageListResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<MessageListResponse>> call,
                                           Response<ApiResponse<MessageListResponse>> response) {
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
                                restorePendingMessages();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<MessageListResponse>> call, Throwable t) {
                        // 히스토리 로드 실패 — 빈 화면으로 진입
                    }
                });
    }

    private void restorePendingMessages() {
        pendingStore.getDatabaseExecutor().execute(() -> {
            List<MessageData> pending = pendingStore.loadForRoom(roomId);
            if (pending.isEmpty()) return;
            runOnUiThread(() -> {
                for (MessageData message : pending) {
                    adapter.addMessage(message);
                }
                scrollToBottom();
            });
        });
    }

    // ─────────────────────────────────────────
    // 메시지 전송
    // ─────────────────────────────────────────

    private void sendMessage() {
        String content = binding.etMessage.getText().toString().trim();
        if (content.isEmpty()) return;

        binding.etMessage.setText("");

        String clientMsgId = UUID.randomUUID().toString();

        MessageData optimistic = new MessageData();
        optimistic.setClient_msg_id(clientMsgId);
        optimistic.setMsg_type("text");
        optimistic.setContent(content);
        optimistic.setIs_mine(true);
        optimistic.setStatus(MessageData.STATUS_SENDING);
        optimistic.setSender_id(myUserId);
        optimistic.setRoom_id(roomId);
        optimistic.setCreated_at(MessageData.nowUtcString());

        adapter.addMessage(optimistic);
        scrollToBottom();
        pendingStore.save(optimistic);

        if (!SocketManager.getInstance().isConnected()) {
            failLocally(clientMsgId);
            return;
        }

        SocketManager.getInstance().sendMessage(roomId, myUserId, content, clientMsgId);
        armSendTimeout(clientMsgId);
    }

    private void retryText(MessageData failedMsg) {
        String clientMsgId = failedMsg.getClient_msg_id();

        if (!SocketManager.getInstance().isConnected()) {
            failLocally(clientMsgId);
            return;
        }

        SocketManager.getInstance().sendMessage(roomId, myUserId, failedMsg.getContent(), clientMsgId);
        armSendTimeout(clientMsgId);
    }

    private void retryUpload(MessageData failedMsg, List<Uri> uris) {
        pendingStore.getDatabaseExecutor().execute(() ->
                uploadHelper.uploadAllAndSend(failedMsg.getClient_msg_id(), uris));
    }

    // ─────────────────────────────────────────
    // 읽음 처리
    // ─────────────────────────────────────────

    private void markRead() {
        SocketManager.getInstance().sendRead(roomId, myUserId);
    }

    // ─────────────────────────────────────────
    // 타임아웃 / 실패 처리
    // ─────────────────────────────────────────

    private void armSendTimeout(String clientMsgId) {
        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        pendingTimeoutHandlers.put(clientMsgId, timeoutHandler);
        timeoutHandler.postDelayed(() -> {
            if (pendingTimeoutHandlers.containsKey(clientMsgId)) {
                pendingTimeoutHandlers.remove(clientMsgId);
                failLocally(clientMsgId);
                Log.w("CHAT_SEND", "타임아웃 → failLocally: " + clientMsgId);
            }
        }, 10_000);
    }

    private void failLocally(String clientMsgId) {
        adapter.markFailed(clientMsgId);
        pendingStore.markFailed(clientMsgId);
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

                                int firstVisible = ((LinearLayoutManager) binding.rvMessages
                                        .getLayoutManager()).findFirstVisibleItemPosition();

                                adapter.prependMessages(older);

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
