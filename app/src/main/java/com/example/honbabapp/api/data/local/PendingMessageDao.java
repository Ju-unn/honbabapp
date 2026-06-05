package com.example.honbabapp.api.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO(Data Access Object) = "데이터에 접근하는 객체".
 * DB 에 대고 실제로 무슨 명령을 날릴지(저장/조회/삭제/갱신)를 메서드로 선언
 */
@Dao
public interface PendingMessageDao {

    /**
     * upsert = update + insert.
     * onConflict = REPLACE → 같은 기본키(clientMessageId)가 이미 있으면 통째로 덮어쓴다.
     * 그래서 "최초 저장"과 "상태 갱신 후 재저장"을 같은 메서드로 처리할 수 있다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(PendingMessageEntity entity);

    @Query("DELETE FROM pending_messages WHERE clientMessageId = :clientMessageId")
    void deleteById(String clientMessageId);

    /** 방의 대기 메시지를 시간 오름차순으로 — 복원 시 순서 보장 */
    @Query("SELECT * FROM pending_messages WHERE roomId = :roomId ORDER BY createdAt ASC")
    List<PendingMessageEntity> getByRoom(int roomId);

    @Query("UPDATE pending_messages SET status = :status WHERE clientMessageId = :clientMessageId")
    void updateStatus(String clientMessageId , int status);

    @Query("UPDATE pending_messages SET serverImageUrlsJson = :serverImageUrlsJson "
            + "WHERE clientMessageId  = :clientMessageId")
    void updateServerImmageUrls(String clientMessageId, String serverImageUrlsJson);
}
