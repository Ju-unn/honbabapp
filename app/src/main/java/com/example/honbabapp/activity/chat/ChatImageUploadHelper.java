package com.example.honbabapp.activity.chat;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.honbabapp.api.data.ImageUploadData;
import com.example.honbabapp.api.data.chat.MessageData;
import com.example.honbabapp.api.data.local.PendingMessageStore;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.network.SocketManager;
import com.example.honbabapp.repository.ChatRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatImageUploadHelper {

    /** 업로드 진행에 따른 UI/영속 처리를 Activity 로 위임 (구현부에서 runOnUiThread 처리). */
    public interface Callbacks {
        /** 낙관적 메시지(SENDING) 를 어댑터·DB 에 추가하고 하단 스크롤. */
        void onOptimisticMessage(MessageData optimisticMessage);
        /** 첨부 버튼 활성/비활성 토글. */
        void onAttachButtonEnabled(boolean enabled);
        /** echo 미수신 대비 10초 타임아웃 무장. */
        void onArmSendTimeout(String clientMsgId);
        /** 업로드 실패 확정(어댑터 markFailed + DB markFailed). */
        void onFailLocally(String clientMsgId);
    }

    private final Context applicationContext;
    private final int roomId;
    private final int myUserId;
    private final PendingMessageStore pendingStore;
    private final Callbacks callbacks;

    /** 업로드 중 첨부 버튼 잠금 (다중 트리거 방지). */
    private boolean isUploading = false;

    public ChatImageUploadHelper(@NonNull Context context, int roomId, int myUserId,
                                 @NonNull PendingMessageStore pendingStore,
                                 @NonNull Callbacks callbacks) {
        this.applicationContext = context.getApplicationContext();
        this.roomId = roomId;
        this.myUserId = myUserId;
        this.pendingStore = pendingStore;
        this.callbacks = callbacks;
    }

    public boolean isUploading() {
        return isUploading;
    }

    /**
     * 선택/촬영한 이미지들을 순차 업로드한다.
     * 원본 Uri 는 앱 내부로 복사(content:// 권한 만료 대비) 후 복사본으로 미리보기/업로드/재전송.
     */
    public void startSequentialUpload(@NonNull List<Uri> uris) {
        isUploading = true;
        callbacks.onAttachButtonEnabled(false);

        String clientMsgId = UUID.randomUUID().toString();

        pendingStore.getDatabaseExecutor().execute(() -> {
            List<Uri> copiedUris = pendingStore.copyImagesToInternal(clientMsgId, uris);
            List<Uri> usableUris = copiedUris.isEmpty() ? uris : copiedUris;

            MessageData optimistic = new MessageData();
            optimistic.setClient_msg_id(clientMsgId);
            optimistic.setMsg_type("image");
            optimistic.setContent("사진");
            optimistic.setIs_mine(true);
            optimistic.setStatus(MessageData.STATUS_SENDING);
            optimistic.setLocalUris(new ArrayList<>(usableUris));
            optimistic.setSender_id(myUserId);
            optimistic.setRoom_id(roomId);
            optimistic.setCreated_at(MessageData.nowKstString());

            callbacks.onOptimisticMessage(optimistic);
            uploadAllAndSend(clientMsgId, usableUris);
        });
    }

    /** 최초 전송과 재전송(retryUpload)이 공유. emit 직전 isConnected 가드로 오프라인 자동전송 차단. */
    public void uploadAllAndSend(String clientMsgId, List<Uri> uris) {
        int count = uris.size();
        String[] results = new String[count];
        int[] done = {0};

        for (int i = 0; i < count; i++) {
            final int index = i;
            uploadOneToResult(uris.get(index), s3Url -> {
                results[index] = s3Url;  // 실패면 null
                done[0]++;
                if (done[0] != count) return;

                isUploading = false;
                callbacks.onAttachButtonEnabled(true);

                List<String> successUrls = new ArrayList<>();
                for (String url : results) {
                    if (url != null) successUrls.add(url);
                }

                if (successUrls.isEmpty()) {
                    Log.e("CHAT_UPLOAD", "모든 업로드 실패 → failLocally");
                    callbacks.onFailLocally(clientMsgId);
                    return;
                }

                pendingStore.updateServerImageUrls(clientMsgId, successUrls);

                if (!SocketManager.getInstance().isConnected()) {
                    callbacks.onFailLocally(clientMsgId);
                    return;
                }

                SocketManager.getInstance().sendImageMessage(roomId, myUserId, successUrls, clientMsgId);
                callbacks.onArmSendTimeout(clientMsgId);
            });
        }
    }

    /**
     * 단일 Uri → S3 업로드, 결과 URL 을 콜백으로 반환(실패 시 null).
     * onResult 는 항상 호출된다(다음 장 진행 보장).
     */
    private void uploadOneToResult(Uri uri, Consumer<String> onResult) {
        try {
            InputStream inputStream = applicationContext.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                onResult.accept(null);
                return;
            }

            File tempFile = new File(applicationContext.getCacheDir(),
                    "chat_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int readLength;
            while ((readLength = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, readLength);
            }
            fileOutputStream.close();
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
        } catch (Exception exception) {
            Log.e("CHAT_UPLOAD", "업로드 예외: " + exception.getMessage());
            onResult.accept(null);
        }
    }
}
