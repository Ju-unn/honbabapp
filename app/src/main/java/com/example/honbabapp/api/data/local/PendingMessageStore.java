package com.example.honbabapp.api.data.local;

import android.content.Context;
import android.net.Uri;

import com.example.honbabapp.api.data.chat.MessageData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PendingMessageStore {

    private final Context appContext;
    private final PendingMessageDao dao;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();
    private final File pendingImageRoot;

    /** Gson 으로 List<String> 을 복원할 때 필요한 타입 정보 */
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();


    public PendingMessageStore(Context context) {
        this.appContext = context.getApplicationContext();
        this.dao = AppDatabase.getInstance(appContext).pendingMessageDao();
        this.pendingImageRoot = new File(appContext.getFilesDir(), "pending");
    }

    /** 작업 직렬화를 위해 같은 Execut
     * or 를 복사·복원에서도 재사용 */
    public ExecutorService getDatabaseExecutor() {
        return databaseExecutor;
    }

    // ── 저장 / 갱신 / 삭제 ──────────────────────

    /** optimistic 메시지를 DB 에 저장(또는 덮어쓰기) */
    public void save(MessageData message) {
        PendingMessageEntity entity = toEntity(message);
        databaseExecutor.execute(() -> dao.upsert(entity));
    }

    /** 실패 상태로 갱신 */
    public void markFailed(String clientMessageId) {
        databaseExecutor.execute(
                () -> dao.updateStatus(clientMessageId, MessageData.STATUS_FAILED));
    }

    /** S3 업로드 성공 URL 갱신 (echo 만 실패한 경우, 재전송 시 재업로드 생략 가능) */
    public void updateServerImageUrls(String clientMessageId , List<String> serverImageUrls) {
        String json = gson.toJson(serverImageUrls);
        databaseExecutor.execute(() -> dao.updateServerImmageUrls(clientMessageId, json));
    }

    /** row 삭제 + 복사 이미지 파일 정리 (성공 확정 또는 사용자 삭제 시) */
    public void remove(String clientMessageId) {
        databaseExecutor.execute(() -> {
            dao.deleteById(clientMessageId);
            deleteImageDirectory(clientMessageId);
        });
    }

    // ── 복원 ────────────────────────────────────

    /**
     * 방의 대기 메시지를 MessageData 로 복원한다.
     * 복원된 항목은 모두 STATUS_FAILED, is_mine=true 로 세팅(자동 전송 방지).
     */

    public List<MessageData> loadForRoom(int roomId) {
        List<PendingMessageEntity> rows = dao.getByRoom(roomId);
        List<MessageData> result = new ArrayList<>();
        for (PendingMessageEntity row : rows) {
            result.add(toMessageData(row));
        }
        return result;
    }

    // ── 이미지 파일 관리 ────────────────────────

    /**
     * 선택된 원본 Uri 들을 앱 내부(getFilesDir/pending/<clientMessageId>/)로 복사하고
     * 복사본 Uri 목록을 반환한다. content:// 원본은 앱 재시작 후 접근 권한이 사라질 수 있으므로,
     */

    public List<Uri> copyImagesToInternal(String clientMessageId, List<Uri> sourceUris) {
        List<Uri> copiedUris = new ArrayList<>();
        File directory = new File(pendingImageRoot , clientMessageId);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        for (int index = 0; index < sourceUris.size(); index++) {
            File target = new File(directory, index + ".jpg");
            try (InputStream input = appContext.getContentResolver()
                    .openInputStream(sourceUris.get(index));
            FileOutputStream output = new FileOutputStream(target)){
                if (input == null) continue;
                byte[] buffer = new byte[4096];
                int length;
                while ((length = input.read(buffer)) != -1) {
                    output.write(buffer , 0 , length);
                }
                copiedUris.add(Uri.fromFile(target));
            } catch (Exception e) {
                // 한장 실패해도 나머지 계속 복사
            }
        }
        return copiedUris;
    }

    private void deleteImageDirectory(String clientMessageId) {
        File directory = new File(pendingImageRoot, clientMessageId);
        if  (!directory.exists()) return;
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }

        directory.delete();
    }


    // ── 변환 (MessageData ↔ Entity) ─────────────
    private PendingMessageEntity toEntity(MessageData message) {
        PendingMessageEntity entity = new PendingMessageEntity();
        entity.clientMessageId = message.getClient_msg_id();
        entity.roomId = message.getRoom_id();
        entity.senderId = message.getSender_id();
        entity.messageType = message.getMsg_type();
        entity.content = message.getContent();
        entity.createdAt = message.getCreated_at();
        entity.status = message.getStatus();

        List<String> serverImageUrls = message.getImage_urls();
        if (serverImageUrls != null && !serverImageUrls.isEmpty()) {
            entity.serverImageUrlsJson = gson.toJson(serverImageUrls);
        }

        List<Uri> localUris = message.getLocalUris();
        if (localUris != null && !localUris.isEmpty()) {
            List<String> localPaths = new ArrayList<>();
            for (Uri uri : localUris) {
                localPaths.add(uri.toString());
            }
            entity.localFilePathsJson = gson.toJson(localPaths);
        }
        return entity;
    }

    private MessageData toMessageData(PendingMessageEntity row) {
        MessageData message = new MessageData();
        message.setClient_msg_id(row.clientMessageId);
        message.setRoom_id(row.roomId);
        message.setSender_id(row.senderId);
        message.setMsg_type(row.messageType);
        message.setContent(row.content);
        message.setCreated_at(row.createdAt);
        message.setIs_mine(true);
        // 복원 시 항상 FAILED — 사용자가 재전송 버튼을 눌러야만 전송된다(자동 전송 금지)
        message.setStatus(MessageData.STATUS_FAILED);

        if (row.serverImageUrlsJson != null) {
            List<String> serverImageUrls = gson.fromJson(row.serverImageUrlsJson, STRING_LIST_TYPE);
            message.setImage_urls(serverImageUrls);
        }
        if (row.localFilePathsJson != null) {
            List<String> localPaths = gson.fromJson(row.localFilePathsJson, STRING_LIST_TYPE);
            List<Uri> localUris = new ArrayList<>();
            for (String path : localPaths) {
                localUris.add(Uri.parse(path));
            }
            message.setLocalUris(localUris);
        }
        return message;
    }







}
