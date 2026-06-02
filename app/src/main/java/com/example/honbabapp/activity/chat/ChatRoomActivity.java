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
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.bumptech.glide.Glide;
import com.example.honbabapp.BuildConfig;
import com.example.honbabapp.R;
import com.example.honbabapp.api.data.ImageUploadData;
import com.example.honbabapp.api.data.chat.MessageData;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.chat.MessageListResponse;
import com.example.honbabapp.databinding.PageChatDetailBinding;
import com.example.honbabapp.network.SocketManager;
import com.example.honbabapp.repository.ChatRepository;
import com.example.honbabapp.ui.adapter.MessageAdapter;
import com.example.honbabapp.ui.dialog.ChatAttachBottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import io.socket.emitter.Emitter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRoomActivity extends AppCompatActivity {
    private PageChatDetailBinding binding;
    private MessageAdapter adapter;

    // ── Optimistic UI ──────────────────────────────
    /** 업로드 중인 이미지의 client_msg_id → 타임아웃 Handler 취소용 */
    private final Map<String , Handler> pendingTimeoutHandlers = new HashMap<>();

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
    private Emitter.Listener kickedListener;

    // ─────────────────────────────────────────
    // 이미지 첨부
    // ─────────────────────────────────────────
    /** 갤러리 한 번에 선택할 수 있는 최대 장수 */
    private static final int MAX_GALLERY_PICK = 10;

    /** 업로드 중 첨부 버튼 잠금 (다중 트리거 방지) */
    private boolean isUploading = false;

    /**
     * Photo Picker
     * 다중 선택 결과(List<Uri>) 를 받으면 순차로 업로드 (uploadOne 재귀).
     */
    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.PickMultipleVisualMedia(MAX_GALLERY_PICK) ,
                    uris -> {
                        if (uris == null || uris.isEmpty()) return;
                        startSequentialUpload(uris);
                    }
            );

    /**
     * 카메라가 사진을 저장할 위치 (FileProvider content:// URI).
     */
    private Uri pendingCameraUri = null;

    /**
     * 카메라 권한 런처
     */
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission() ,
                    granted -> {
                        if (granted) {
                            launchCamera();
                        } else {
                            Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    /**
     * 카메라 촬영 런처 (TakePicture)
     * 성공 시 pendingCameraUri 가 가리키는 파일(= 카메라 앱이 사진을 저장한 곳) 을 업로드.
     */
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    success -> {
                        if (success  && pendingCameraUri != null) {
                            // 사진 1장 = 리스트 1개로 감싸서 동일 순차 업로드 흐름 재사용
                            List<Uri> single = new ArrayList<>();
                            single.add(pendingCameraUri);
                            startSequentialUpload(single);
                        } else {
                            Log.d("CHAT_UPLOAD", "카메라 촬영 취소 또는 실패");
                        }
                        // pendingCameraUri 는 다음 촬영 때 다시 발급하므로 정리
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
        if (kickedListener != null)
            SocketManager.getInstance().getSocket().off("kicked", kickedListener);
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

        // 햄버거에서 돌아왔을 때 메시지 히스토리 재조회 (방 정보 변경 가능성 대비)
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
        adapter = new MessageAdapter(senderId -> {
            // 메시지 프로필/닉네임 클릭 → 참가자 상세
            Intent intent = new Intent(this, ChatRoomParticipantDetailActivity.class);
            intent.putExtra("user_id", senderId);
            startActivity(intent);
        });

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

        adapter.setFailedMessageListener(new MessageAdapter.OnFailedMessageListener() {
            @Override
            public void onRetry(MessageData msg) {
                // SENDING 으로 되돌리고 동일 client_msg_id 로 재업로드
                msg.setStatus(MessageData.STATUS_SENDING);
                adapter.notifyItemChanged(adapter.getMessages().indexOf(msg)); // 오버레이 즉시 숨김
                List<Uri> uris = msg.getLocalUris();
                if (uris != null && !uris.isEmpty()) {
                    retryUpload(msg, uris);
                }
            }

            @Override
            public void onDelete(MessageData msg) {
                adapter.removeByClientMsgId(msg.getClient_msg_id());
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
            // 업로드 진행 중이면 무시 (락)
            if (isUploading) {
                Toast.makeText(this, "사진 전송 중입니다", Toast.LENGTH_SHORT).show();
                return;
            }
            showAttachBottomSheet();
        });
    }

    // ─────────────────────────────────────────
    // 이미지 첨부 — 바텀시트
    // ─────────────────────────────────────────

    /**
     * 첨부 버튼 클릭 → 갤러리/카메라 바텀시트 표시
     *
     * 다이얼로그는 "어떤 액션을 선택했는가" 만 알리고
     * 실제 갤러리/카메라 인텐트 실행은 Activity 책임 (강퇴 다이얼로그와 동일 패턴)
     */
    private void showAttachBottomSheet() {
        ChatAttachBottomSheetDialog dialog = new ChatAttachBottomSheetDialog();
        dialog.listener = new ChatAttachBottomSheetDialog.OnAttachActionListener() {
            @Override
            public void onPickGallery() {
                // Photo Picker 띄위기 이미지만
                galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            }

            @Override
            public void onPickCamera() {
                // 권한 체크 — 이미 허용이면 바로 launchCamera, 아니면 권한 요청
                int status = ContextCompat.checkSelfPermission(
                        ChatRoomActivity.this , Manifest.permission.CAMERA);
                if (status == PackageManager.PERMISSION_GRANTED) {
                    launchCamera();
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            }
        };
        dialog.show(getSupportFragmentManager() , "chat_attach");
    }

    /**
     * 카메라 앱을 실행해서 촬영 → 결과를 cameraLauncher 콜백으로 받음
     *
     * 흐름:
     *   1) cache/chat_images/ 디렉토리 보장 (file_paths.xml 의 허용 경로)
     *   2) cam_<timestamp>.jpg 빈 파일 생성
     *   3) FileProvider 로 content:// URI 발급 (file:// 직접 노출 금지 — Android 7+ FileUriExposedException)
     *   4) pendingCameraUri 에 보관 (콜백에서 어떤 파일이었는지 알기 위함)
     *   5) cameraLauncher.launch(uri) → 카메라 앱이 그 URI 에 사진 저장
     *
     * 실패 시 토스트만 띄우고 종료 (촬영 흐름 자체가 시작 안 됨).
     */

    private void launchCamera() {
        try {
            // 1) 디렉토리 보장 — file_paths.xml 의 <cache-path path="chat_images/"> 와 일치해야 함
            File dir = new File(getCacheDir(), "chat_images");
            if (!dir.exists() && !dir.mkdirs()) {
                Log.e("CHAT_UPLOAD", "chat_images 디렉토리 생성 실패");
                Toast.makeText(this, "카메라 준비 실패", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2) 임시 빈 파일
            File photoFile = new File(dir, "cam_" + System.currentTimeMillis() + ".jpg");

            // 3) FileProvider URI 발급
            //    authority 는 AndroidManifest.xml 의 <provider> 와 정확히 동일해야 함
            //    BuildConfig.APPLICATION_ID + ".fileprovider" → "com.example.honbabapp.fileprovider"
            Uri uri = FileProvider.getUriForFile(
                    this,
                    BuildConfig.APPLICATION_ID + ".fileprovider",
                    photoFile);

            // 4) 보관 — 콜백에서 사용
            pendingCameraUri = uri;

            // 5) 카메라 실행
            cameraLauncher.launch(uri);

        }  catch (Exception e) {
            Log.e("CHAT_UPLOAD", "카메라 실행 실패: " + e.getMessage());
            Toast.makeText(this, "카메라 실행 실패", Toast.LENGTH_SHORT).show();
            pendingCameraUri = null;
        }
    }



    // ─────────────────────────────────────────
    // 이미지 첨부 — 순차 업로드
    // ─────────────────────────────────────────

    /**
     * 선택된 Uri 목록을 모두 S3에 올린 뒤 묶음 소켓 메시지 1개 전송.
     *
     * 흐름:
     *   1) optimistic 메시지 즉시 추가 (SENDING, localUris 세팅 → Glide 미리보기)
     *   2) 각 Uri 병렬 업로드 (uploadOne 은 그대로 활용, 결과를 results[] 배열에 기록)
     *   3) 전체 완료 후 성공 URL ≥ 1 → sendImageMessage 1회 emit
     *   4) 성공 URL = 0 → 즉시 markFailed
     *   5) 10초 타임아웃 Handler → echo 미수신 시 markFailed
     */
    /**
     * 선택된 Uri 목록을 모두 S3에 올린 뒤 묶음 소켓 메시지 1개 전송.
     *
     * 흐름:
     *   1) optimistic 메시지 즉시 추가 (SENDING, localUris 세팅 → Glide 미리보기)
     *   2) 각 Uri 병렬 업로드 (uploadOne 은 그대로 활용, 결과를 results[] 배열에 기록)
     *   3) 전체 완료 후 성공 URL ≥ 1 → sendImageMessage 1회 emit
     *   4) 성공 URL = 0 → 즉시 markFailed
     *   5) 10초 타임아웃 Handler → echo 미수신 시 markFailed
     */
    private void startSequentialUpload(@NonNull List<Uri> uris) {
        isUploading = true;
        binding.btnAttach.setEnabled(false);

        // 1) client_msg_id 생성
        String clientMsgId = UUID.randomUUID().toString();

        // 2) optimistic 메시지 즉시 추가
        MessageData optimistic = new MessageData();
        optimistic.setClient_msg_id(clientMsgId);
        optimistic.setMsg_type("image");
        optimistic.setContent("사진");
        optimistic.setIs_mine(true);
        optimistic.setStatus(MessageData.STATUS_SENDING);
        optimistic.setLocalUris(new ArrayList<>(uris));   // Glide 미리보기용
        optimistic.setSender_id(myUserId);
        optimistic.setRoom_id(roomId);
        runOnUiThread(() -> {
            adapter.addMessage(optimistic);
            scrollToBottom();
        });

        // 3) 업로드 결과 배열 (선택 순서 보존)
        int count = uris.size();
        String[] results = new String[count];
        int[] done = {0}; // 완료 카운터 (배열로 감싸서 람다에서 수정 가능)

        for (int i = 0; i < count; i++) {
            final int idx = i;
            uploadOneToResult(uris.get(idx), s3Url -> {
                results[idx] = s3Url; // 실패면 null
                done[0]++;

                if (done[0] == count) {
                    // 모든 업로드 완료
                    isUploading = false;
                    runOnUiThread(() -> binding.btnAttach.setEnabled(true));

                    // 성공 URL만 추림
                    List<String> successUrls = new ArrayList<>();
                    for (String url : results) {
                        if (url != null) successUrls.add(url);
                    }

                    if (successUrls.isEmpty()) {
                        // 전부 실패
                        Log.e("CHAT_UPLOAD", "모든 업로드 실패 → markFailed");
                        runOnUiThread(() -> adapter.markFailed(clientMsgId));
                    } else {
                        // 묶음 emit 1회
                        SocketManager.getInstance()
                                .sendImageMessage(roomId, myUserId, successUrls, clientMsgId);

                        // 10초 타임아웃: echo 안 오면 FAILED
                        Handler handler = new Handler(Looper.getMainLooper());
                        pendingTimeoutHandlers.put(clientMsgId, handler);
                        handler.postDelayed(() -> {
                            if (pendingTimeoutHandlers.containsKey(clientMsgId)) {
                                // 아직 confirm 안 됨 → failed
                                pendingTimeoutHandlers.remove(clientMsgId);
                                adapter.markFailed(clientMsgId);
                                Log.w("CHAT_UPLOAD", "타임아웃 → markFailed: " + clientMsgId);
                            }
                        }, 10_000);
                    }
                }
            });
        }
    }

    /**
     * 단일 Uri → S3 업로드, 결과 URL을 콜백으로 반환.
     * 실패 시 null 반환 (다음 장 진행 위해 onComplete 는 항상 호출).
     */
    private void uploadOneToResult(Uri uri, Consumer<String> onResult) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) { onResult.accept(null); return; }

            File tempFile = new File(getCacheDir(), "chat_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) fos.write(buffer, 0, len);
            fos.close();
            inputStream.close();

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), tempFile);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                    "chat_image", tempFile.getName(), requestFile);

            ChatRepository.getInstance().uploadChatImage(roomId, myUserId, imagePart,
                    new Callback<ApiResponse<ImageUploadData>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<ImageUploadData>> call,
                                               Response<ApiResponse<ImageUploadData>> response) {
                            if (response.isSuccessful()
                                    && response.body() != null
                                    && response.body().isSuccess()
                                    && response.body().getData() != null) {
                                String url = response.body().getData().getImage_url();
                                Log.d("CHAT_UPLOAD", "S3 성공: " + url);
                                onResult.accept(url);
                            } else {
                                Log.w("CHAT_UPLOAD", "S3 실패 응답");
                                onResult.accept(null);
                            }
                        }
                        @Override
                        public void onFailure(Call<ApiResponse<ImageUploadData>> call, Throwable t) {
                            Log.e("CHAT_UPLOAD", "S3 네트워크 실패: " + t.getMessage());
                            onResult.accept(null);
                        }
                    });
        } catch (Exception e) {
            Log.e("CHAT_UPLOAD", "업로드 예외: " + e.getMessage());
            onResult.accept(null);
        }
    }

    /**
     * 콜백 기반 비동기 직렬 처리 패턴
     */
    private void uploadOne(@NonNull List<Uri> uris , int index) {
        // 종료 조건
        if (index >= uris.size()) {
            isUploading = false;
            binding.btnAttach.setEnabled(true);
            return;
        }
        Uri uri = uris.get(index);
        uploadAndSend(uri, () -> {
            // 다음 장을 350ms 뒤에 (타다닥 속도 늦춤)
            binding.rvMessages.postDelayed(
                    () -> uploadOne(uris, index + 1), 350);
        });
    }

    /**
     * 단일 Uri 업로드 + WebSocket emit
     *
     * 흐름:
     *   1) Uri → 임시 파일 변환 (Multipart 전송에 필요)
     *   2) Multipart 구성
     *   3) ChatRepository.uploadChatImage() → S3 URL 반환
     *   4) SocketManager.sendImageMessage() → 서버가 DB 저장 + fan-out
     *   5) onComplete.run() → 다음 장으로 진행
     *
     * 실패해도 onComplete 는 호출됨 (다음 장 계속 진행).
     * → 1장이 실패해도 2,3 번째는 정상 업로드 → 사용자 경험 보호
     */

    private void uploadAndSend(Uri uri , Runnable onComplete) {
        try {
            // 1) Uri → 임시 파일
            //    cache/chat_<timestamp>.jpg 로 저장 (앱 종료 시 OS 가 정리)

            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e("CHAT_UPLOAD", "InputStream null — skip uri:" + uri);
                onComplete.run();
                return;
            }

            File tempFile = new File(getCacheDir() ,
                    "chat_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            inputStream.close();

            // 2) Multipart 구성
            //    파라미터명 "chat_image" → 백엔드의 $_FILES['chat_image'] 와 매칭
            RequestBody requestFile = RequestBody.create(
                    MediaType.parse("image/jpeg") , tempFile);

            MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                    "chat_image" , tempFile.getName() , requestFile);

            // 3) S3 업로드
            ChatRepository.getInstance().uploadChatImage(roomId, myUserId, imagePart,
                    new Callback<ApiResponse<ImageUploadData>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<ImageUploadData>> call, Response<ApiResponse<ImageUploadData>> response) {
                            if (response.isSuccessful()
                                    && response.body() != null
                                    && response.body().isSuccess()
                                    && response.body().getData() != null) {

                                String imageUrl = response.body().getData().getImage_url();
                                Log.d("CHAT_UPLOAD", "S3 업로드 성공: " + imageUrl);

                                // 4) WebSocket emit → 서버가 DB 저장 + fan-out
                                SocketManager.getInstance()
                                        .sendImageMessage(roomId, myUserId , imageUrl);
                            } else {
                                String msg = (response.body() != null)
                                        ? response.body().getMessage()
                                        : "이미지 업로드 실패";
                                Log.e("CHAT_UPLOAD", "S3 업로드 실패: " + msg);
                                Toast.makeText(ChatRoomActivity.this,
                                        "이미지 1장 업로드 실패", Toast.LENGTH_SHORT).show();
                            }
                            // 5) 임시 파일 정리 + 다음 장 진행
                            //noinspection ResultOfMethodCallIgnored
                            tempFile.delete();
                            onComplete.run();
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<ImageUploadData>> call, Throwable t) {
                            Log.e("CHAT_UPLOAD", "네트워크 실패: " + t.getMessage());
                            Toast.makeText(ChatRoomActivity.this,
                                    "네트워크 오류 — 이미지 1장 실패", Toast.LENGTH_SHORT).show();
                            //noinspection ResultOfMethodCallIgnored
                            tempFile.delete();
                            onComplete.run();
                        }
                    });



        } catch (IOException e) {
            Log.e("CHAT_UPLOAD", "이미지 처리 오류: " + e.getMessage());
            onComplete.run();
        }
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
                msg.setImage_url(json.optString("image_url", null));
                msg.setCreated_at(json.getString("created_at"));
                msg.setUnread_count(json.optInt("unread_count", 0));
                msg.setIs_mine(msg.getSender_id() == myUserId);
                msg.setStatus(MessageData.STATUS_SENT);

                // ── image_urls 파싱 ──
                JSONArray urlsArr = json.optJSONArray("image_urls");
                if (urlsArr != null && urlsArr.length() > 0) {
                    List<String> imageUrls = new ArrayList<>();
                    for (int i = 0; i < urlsArr.length(); i++) imageUrls.add(urlsArr.getString(i));
                    msg.setImage_urls(imageUrls);
                }

                // ── client_msg_id — optimistic dedupe ──
                String clientMsgId = json.optString("client_msg_id", null);
                msg.setClient_msg_id(clientMsgId);


                runOnUiThread(() -> {
                    // 타임아웃 Handler 취소
                    if (clientMsgId != null) {
                        Handler handler = pendingTimeoutHandlers.remove(clientMsgId);
                        if (handler != null) handler.removeCallbacksAndMessages(null);
                    }

                    // confirmMessage: client_msg_id 있으면 in-place 교체, 없으면 addMessage
                    if (clientMsgId != null && !clientMsgId.isEmpty()) {
                        adapter.confirmMessage(clientMsgId, msg);
                    } else {
                        adapter.addMessage(msg);
                    }
                    scrollToBottom();
                    markRead();
                });
            } catch (Exception e) {
                Log.e("CHAT_UPLOAD", "messageListener 파싱 실패: " + e.getMessage());
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

        // ─── 강퇴 수신 ───
        kickedListener = args -> {
            Log.d("KICK_DEBUG", "kicked 이벤트 수신! args=" + (args.length > 0 ? args[0].toString() : "empty"));

            try {
                JSONObject json = (JSONObject) args[0];

                Log.d("KICK_DEBUG", "json.room_id=" + json.getInt("room_id") + " / 내 roomId=" + roomId);

                if (json.getInt("room_id") != roomId) {
                    Log.d("KICK_DEBUG", "room_id 다름 → 무시");
                    return;
                }

                int targetUserId = json.optInt("target_user_id", -1);
                Log.d("KICK_DEBUG", "targetUserId=" + targetUserId + " / 내 userId=" + myUserId);

                if (targetUserId != myUserId) {
                    Log.d("KICK_DEBUG", "target_user_id 다름 → 무시");
                    return;
                }

                Log.d("KICK_DEBUG", "강퇴 확정 → finish 호출");
                runOnUiThread(() -> {
                    Toast.makeText(ChatRoomActivity.this,
                            "방장에 의해 강퇴되었습니다",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception e) {
                Log.e("KICK_DEBUG", "kicked 파싱 실패: " + e.getMessage());
            }
        };



        SocketManager.getInstance().getSocket().on("host_changed", hostChangedListener);
        SocketManager.getInstance().getSocket().on("member_left", memberLeftListener);
        SocketManager.getInstance().getSocket().on("message", messageListener);
        SocketManager.getInstance().getSocket().on("read_update", readUpdateListener);
        SocketManager.getInstance().getSocket().on("member_joined", memberJoinedListener);
        SocketManager.getInstance().getSocket().on("kicked", kickedListener);
    }

    /**
     * 실패 메시지 재전송 — 동일 client_msg_id 재사용
     */
    private void retryUpload(MessageData failedMsg, List<Uri> uris) {adapter.setImageClickListener((urls, startIndex) -> {
        Intent intent = new Intent(this, FullScreenImageActivity.class);
        intent.putStringArrayListExtra(
                FullScreenImageActivity.EXTRA_IMAGE_URLS, new ArrayList<>(urls));
        intent.putExtra(FullScreenImageActivity.EXTRA_START_INDEX, startIndex);
        startActivity(intent);
    });
        String clientMsgId = failedMsg.getClient_msg_id();
        int count = uris.size();
        String[] results = new String[count];
        int[] done = {0};

        for (int i = 0; i < count; i++) {
            final int idx = i;
            uploadOneToResult(uris.get(idx), s3Url -> {
                results[idx] = s3Url;
                done[0]++;
                if (done[0] == count) {
                    List<String> successUrls = new ArrayList<>();
                    for (String url : results) if (url != null) successUrls.add(url);

                    if (successUrls.isEmpty()) {
                        runOnUiThread(() -> adapter.markFailed(clientMsgId));
                    } else {
                        SocketManager.getInstance()
                                .sendImageMessage(roomId, myUserId, successUrls, clientMsgId);

                        Handler handler = new Handler(Looper.getMainLooper());
                        pendingTimeoutHandlers.put(clientMsgId, handler);
                        handler.postDelayed(() -> {
                            if (pendingTimeoutHandlers.containsKey(clientMsgId)) {
                                pendingTimeoutHandlers.remove(clientMsgId);
                                adapter.markFailed(clientMsgId);
                            }
                        }, 10_000);
                    }
                }
            });
        }
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