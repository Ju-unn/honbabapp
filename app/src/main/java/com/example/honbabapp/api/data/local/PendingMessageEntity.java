package com.example.honbabapp.api.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_messages")
public class PendingMessageEntity {

    /** client_msg_id — optimistic dedupe 키이자 기본키 */
    @PrimaryKey
    @NonNull
    public String clientMessageId = "";

    public int roomId;
    public int senderId;

    /** "text" | "image" */
    public String messageType;

    public String content;

    /** "yyyy-MM-dd HH:mm:ss" (UTC) — 복원 정렬 기준 */
    public String createdAt;

    /** 저장은 하되, 복원 시에는 항상 FAILED 로 표시한다(활성 전송 컨텍스트가 없으므로) */
    public int status;

    /** S3 업로드 성공 URL 목록(Gson JSON), 없으면 null */
    public String serverImageUrlsJson;

    /** 앱 내부 복사본 경로 목록(Gson JSON), 없으면 null */
    public String localFilePathsJson;
}
