package com.example.honbabapp.activity.chat;

import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;

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
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.BuildConfig;
import com.example.honbabapp.R;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.chat.ChatMemberData;
import com.example.honbabapp.api.data.chat.ChatRoomData;
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
import com.example.honbabapp.util.InsetsUtils;
import com.example.honbabapp.ui.dialog.ChatAttachBottomSheetDialog;
import com.example.honbabapp.util.ToastUtil;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

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

    /**
     * 현재 화면에 떠 있는 채팅방의 room_id.
     *
     * FirebaseInstanceIdService.onMessageReceived 에서 'chat' 타입 푸시를 받았을 때,
     * 이 값과 푸시의 room_id 가 같으면 알림을 표시하지 않는다
     * (소켓이 이미 화면을 갱신했으므로 중복 알림 방지).
     *
     * onResume 에서 roomId 로 설정, onPause 에서 -1 로 초기화.
     */
    public static volatile int currentOpenRoomId = -1;

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

    private MeetingQrHelper meetingQrHelper;
    private ReviewDialogHelper reviewDialogHelper;

    private final ActivityResultLauncher<ScanOptions> qrScanLauncher =
            registerForActivityResult(
                    new ScanContract(),
                    result -> {
                        if (result.getContents() != null && meetingQrHelper != null ) {
                            meetingQrHelper.onScanResult(result.getContents());
                        }
                    }
            );

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
                            // QR 스캔 목적이면 스캐너 실행, 아니면 일반 카메라
                            if (meetingQrHelper != null && meetingQrHelper.isWaitingForCameraPermission()) {
                                meetingQrHelper.launchQrScanner();
                            } else {
                                launchCamera();
                            }
                        } else {
                            ToastUtil.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show();
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
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsetsWithIme(binding.getRoot());

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
            public void onMemberJoined(int remaining) {
                runOnUiThread(() -> {
                    loadMessageHistory();
                    memberCount = (remaining >= 0) ? remaining : memberCount + 1;
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
                        updateSendInputEnabled();
                    }
                });
            }

            @Override
            public void onHostChanged(int newHostId) {
                runOnUiThread(() -> {
                    if (newHostId == myUserId) {
                        ToastUtil.makeText(ChatRoomActivity.this,
                                "방장으로 위임되었습니다", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onKicked() {
                runOnUiThread(() -> {
                    ToastUtil.makeText(ChatRoomActivity.this,
                            "방장에 의해 강퇴되었습니다", Toast.LENGTH_LONG).show();
                    finish();
                });
            }

            @Override
            public void onMessageError(String clientMsgId, String content) {
                runOnUiThread(() -> {
                    if (clientMsgId != null) {
                        Handler handler = pendingTimeoutHandlers.remove(clientMsgId);
                        if (handler != null) handler.removeCallbacksAndMessages(null);
                        failLocally(clientMsgId);
                    }
                    ToastUtil.makeText(ChatRoomActivity.this,
                            content == null || content.isEmpty() ? "메시지를 보낼 수 없습니다" : content,
                            Toast.LENGTH_SHORT).show();
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

        setupQrAndReviewIfNeeded();

        // 알림 딥링크 등으로 room_name/room_image 없이 room_id만 들고 들어온 경우
        // (정상적인 채팅 목록 진입 시에는 항상 채워져 있음) → 서버에서 방 정보를 다시 조회
        if (roomName.isEmpty() && roomImage == null) {
            refreshRoomInfoFromServer();
        }
    }

    /**
     *  그룹 채팅방 + meeting_id 있을 때만 QR/후기 기능 활성화.
     *
     *  알림 딥링크로 들어와 roomType/meetingId 가 아직 기본값(group/-1)인 상태로
     *  한 번 호출되었다가, refreshRoomInfoFromServer() 로 실제 값을 받아온 뒤
     *  다시 호출될 수 있다 (두 번 호출되어도 안전 — 헬퍼 인스턴스만 재생성됨).
     */
    private void setupQrAndReviewIfNeeded() {
        if ("group".equals(roomType) && meetingId != -1) {
            binding.btnQr.setVisibility(View.VISIBLE);
            meetingQrHelper = new MeetingQrHelper(
                    this,
                    RetrofitClient.getClient()
                            .create(ApiService.class),
                    meetingId,
                    myUserId,
                    qrScanLauncher,
                    cameraPermissionLauncher
            );
            meetingQrHelper.loadQrStatus();

            // 후기 다이얼로그 헬퍼 + 어댑터 콜백 등록
            reviewDialogHelper = new ReviewDialogHelper(
                    this,
                    RetrofitClient.getClient()
                            .create(ApiService.class),
                    meetingId,
                    myUserId
            );
            adapter.setReviewPromptClickListener(() -> reviewDialogHelper.onReviewButtonClicked());
        } else {
            binding.btnQr.setVisibility(View.GONE);
        }
    }

    /**
     * 알림 딥링크로 room_id만 받아 들어온 경우, 채팅방 목록 API(getChatRooms)에서
     * room_id가 일치하는 항목을 찾아 roomName/roomImage/roomType/memberCount/meetingId를
     * 다시 채우고 화면을 갱신한다.
     *
     * myUserId가 없으면(prefs에 user_id 없음 = 로그인 정보 없음) 호출하지 않는다.
     * 조회 실패 시에는 기존 placeholder(ic_profile)/빈 제목이 그대로 유지된다.
     */
    private void refreshRoomInfoFromServer() {
        if (roomId == -1 || myUserId == -1) {
            Log.w("ChatRoomActivity", "방 정보 재조회 생략: roomId=" + roomId + ", myUserId=" + myUserId);
            return;
        }

        RetrofitClient.getClient().create(ApiService.class)
                .getChatRooms()
                .enqueue(new Callback<ApiResponse<List<ChatRoomData>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<ChatRoomData>>> call,
                                           Response<ApiResponse<List<ChatRoomData>>> response) {
                        if (!response.isSuccessful() || response.body() == null
                                || !response.body().isSuccess() || response.body().getData() == null) {
                            Log.e("ChatRoomActivity", "방 정보 재조회 실패: HTTP " + response.code());
                            return;
                        }

                        for (ChatRoomData room : response.body().getData()) {
                            if (room.getRoom_id() == roomId) {
                                roomName = room.getRoom_name() != null ? room.getRoom_name() : "";
                                roomImage = resolveRoomImage(room);
                                roomType = room.getRoom_type() != null ? room.getRoom_type() : "group";
                                memberCount = room.getParticipants() != null ? room.getParticipants().size() : 0;
                                meetingId = room.getMeeting_id() != null ? room.getMeeting_id() : -1;

                                runOnUiThread(() -> {
                                    applyRoomInfoToToolbar();
                                    setupQrAndReviewIfNeeded();
                                });
                                return;
                            }
                        }

                        Log.w("ChatRoomActivity",
                                "접근 불가 채팅방 — roomId=" + roomId
                                        + " 는 myUserId=" + myUserId + " 의 방 목록에 없음");

                        runOnUiThread(() -> {
                            ToastUtil.makeText(ChatRoomActivity.this,
                                    "접근할 수 없는 채팅방입니다",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<ChatRoomData>>> call, Throwable t) {
                        Log.e("ChatRoomActivity", "방 정보 재조회 네트워크 오류: " + t.getMessage(), t);
                    }
                });
    }

    /**
     * 1:1 방은 room_image 컬럼이 항상 비어있으므로(서버가 채워주지 않음) 상대방의
     * 프로필 이미지를 참여자 목록에서 직접 찾는다. ChatFragment/ChatRoomListAdapter의
     * 동일 로직과 맞춰야 한다 — 안 그러면 FCM 알림 딥링크로 들어왔을 때만 기본이미지가 뜬다.
     */
    private String resolveRoomImage(ChatRoomData room) {
        if (room.isGroup()) {
            String groupImage = room.getRoom_image();
            if (groupImage != null && !groupImage.isEmpty()) {
                return groupImage;
            }
            if (room.getParticipants() != null && !room.getParticipants().isEmpty()) {
                return room.getParticipants().get(0).getUser_profile_image();
            }
            return null;
        }

        if (room.getParticipants() != null) {
            for (ChatMemberData member : room.getParticipants()) {
                if (member.getUser_id() != myUserId) {
                    return member.getUser_profile_image();
                }
            }
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 이 방을 보고 있는 동안에는 FCM 'chat' 알림을 표시하지 않도록 등록
        currentOpenRoomId = roomId;

        if (roomId != -1 && myUserId != -1) {
            markRead();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 화면을 벗어나면 이 방에 대한 FCM 알림 억제를 해제
        if (currentOpenRoomId == roomId) {
            currentOpenRoomId = -1;
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
        applyRoomInfoToToolbar();

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnMenu.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatRoomDetailActivity.class);
            intent.putExtra("room_id", roomId);
            intent.putExtra("room_name", roomName);
            intent.putExtra("room_type", roomType);
            intent.putExtra("room_image", roomImage);
            intent.putExtra("meeting_id", meetingId);
            startActivityForResult(intent, REQ_DETAIL);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        binding.btnQr.setOnClickListener(v -> {
            if (meetingQrHelper != null) {
                meetingQrHelper.onQrButtonClicked();
            }
        });
    }

    /**
     * roomName/roomImage/roomType/memberCount 값을 툴바(제목, 프로필 이미지, 인원수)에 반영한다.
     *
     * onCreate 최초 1회 호출되며, 알림 딥링크로 들어와 이 값들이 비어있던 경우에는
     * refreshRoomInfoFromServer()가 값을 채운 뒤 다시 호출한다.
     */
    private void applyRoomInfoToToolbar() {
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

        updateSendInputEnabled();
    }

    /**
     * 1:1 방에서 상대방이 나가서(탈퇴 등) 나 혼자만 남으면 입력창/전송 버튼을 막는다.
     * 서버(isRoomStillSendable)도 같은 조건으로 막지만, 눌러보고서야 실패 토스트가
     * 뜨는 것보다 애초에 못 누르게 하는 게 UX상 낫다.
     * memberCount가 아직 0(로딩 전)일 땐 오탐 방지를 위해 막지 않는다.
     */
    private void updateSendInputEnabled() {
        boolean blocked = "direct".equals(roomType) && memberCount == 1;
        binding.etMessage.setEnabled(!blocked);
        binding.btnSend.setEnabled(!blocked);
        binding.etMessage.setHint(blocked ? "상대방이 나간 채팅방입니다" : "메시지를 입력하세요");
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
                LinearLayoutManager layoutManager = (LinearLayoutManager) rv.getLayoutManager();
                if (!isLoadingMore && hasMorePages
                        && layoutManager != null && layoutManager.findFirstVisibleItemPosition() == 0) {
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
                ToastUtil.makeText(this, "사진 전송 중입니다", Toast.LENGTH_SHORT).show();
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
                ToastUtil.makeText(this, "카메라 준비 실패", Toast.LENGTH_SHORT).show();
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
            ToastUtil.makeText(this, "카메라 실행 실패", Toast.LENGTH_SHORT).show();
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
        optimistic.setCreated_at(MessageData.nowKstString());

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
                    public void onResponse(Call<ApiResponse<MessageListResponse>>
                                                   call,
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