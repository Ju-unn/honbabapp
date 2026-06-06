package com.example.honbabapp.activity.chat;

import com.example.honbabapp.api.data.chat.MessageData;
import com.example.honbabapp.network.SocketManager;
import com.example.honbabapp.util.ChatSocketEvents;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;
public class ChatRoomSocketHandler {

    /** 파싱·가드를 통과한 이벤트를 Activity 로 전달하는 콜백. 구현부에서 runOnUiThread 처리. */
    public interface Callbacks {
        /** 새 메시지 수신. clientMsgId 가 있으면 optimistic 확정 대상. */
        void onMessageReceived(MessageData message, String clientMsgId);
        /** 읽음 갱신 — 방금 읽힌 message_id 집합. */
        void onReadUpdate(Set<Integer> newlyReadMessageIds);
        /** 멤버 입장. */
        void onMemberJoined();
        /** 멤버 퇴장 (leftUserId, 남은 인원 remaining; remaining < 0 이면 미상). */
        void onMemberLeft(int leftUserId, int remaining);
        /** 방장 위임 (새 방장 user_id). */
        void onHostChanged(int newHostId);
        /** 본인이 강퇴됨. */
        void onKicked();
    }

    private final int roomId;
    private final int myUserId;
    private final Callbacks callbacks;

    private Emitter.Listener messageListener;
    private Emitter.Listener readUpdateListener;
    private Emitter.Listener memberJoinedListener;
    private Emitter.Listener memberLeftListener;
    private Emitter.Listener hostChangedListener;
    private Emitter.Listener kickedListener;

    public ChatRoomSocketHandler(int roomId, int myUserId, Callbacks callbacks) {
        this.roomId = roomId;
        this.myUserId = myUserId;
        this.callbacks = callbacks;
    }

    /** 소켓 연결 보장 후 6개 리스너 등록. */
    public void register() {
        if (myUserId == -1 || roomId == -1) return;
        SocketManager.getInstance().connect();
        Socket socket = SocketManager.getInstance().getSocket();

        // ── 새 메시지 수신 ──
        messageListener = arguments -> {
            JSONObject payload = ChatSocketEvents.asJsonObject(arguments);
            if (!ChatSocketEvents.isForRoom(payload, roomId)) return;
            try {
                MessageData message = new MessageData();
                message.setMessage_id(payload.getInt("message_id"));
                message.setRoom_id(payload.getInt("room_id"));
                message.setSender_id(payload.getInt("user_id"));
                message.setSender_name(payload.optString("sender_name", ""));
                message.setSender_profile_image(payload.optString("sender_profile_image", ""));
                message.setMsg_type(payload.optString("msg_type", "text"));
                message.setContent(payload.getString("content"));
                message.setImage_url(payload.optString("image_url", null));
                message.setCreated_at(payload.getString("created_at"));
                message.setUnread_count(payload.optInt("unread_count", 0));
                message.setIs_mine(message.getSender_id() == myUserId);
                message.setStatus(MessageData.STATUS_SENT);

                JSONArray imageUrlsArray = payload.optJSONArray("image_urls");
                if (imageUrlsArray != null && imageUrlsArray.length() > 0) {
                    List<String> imageUrls = new ArrayList<>();
                    for (int index = 0; index < imageUrlsArray.length(); index++) {
                        imageUrls.add(imageUrlsArray.getString(index));
                    }
                    message.setImage_urls(imageUrls);
                }

                String clientMsgId = payload.optString("client_msg_id", null);
                message.setClient_msg_id(clientMsgId);

                callbacks.onMessageReceived(message, clientMsgId);
            } catch (Exception exception) {
                // 필수 필드 누락 등 — 원본과 동일하게 무시
            }
        };

        // ── 읽음 처리 수신 ──
        readUpdateListener = arguments -> {
            JSONObject payload = ChatSocketEvents.asJsonObject(arguments);
            if (!ChatSocketEvents.isForRoom(payload, roomId)) return;
            JSONArray idsArray = ChatSocketEvents.getIntArray(payload, "newly_read_message_ids");
            if (idsArray == null || idsArray.length() == 0) return;
            try {
                Set<Integer> readMessageIds = new HashSet<>();
                for (int index = 0; index < idsArray.length(); index++) {
                    readMessageIds.add(idsArray.getInt(index));
                }
                callbacks.onReadUpdate(readMessageIds);
            } catch (Exception exception) {
                // 무시
            }
        };

        // ── 멤버 참여 수신 ──
        memberJoinedListener = arguments -> {
            JSONObject payload = ChatSocketEvents.asJsonObject(arguments);
            if (!ChatSocketEvents.isForRoom(payload, roomId)) return;
            callbacks.onMemberJoined();
        };

        // ── 멤버 나감 수신 ──
        memberLeftListener = arguments -> {
            JSONObject payload = ChatSocketEvents.asJsonObject(arguments);
            if (!ChatSocketEvents.isForRoom(payload, roomId)) return;
            int remaining = ChatSocketEvents.getInt(payload, "remaining", -1);
            int leftUserId = ChatSocketEvents.getInt(payload, "user_id", -1);
            callbacks.onMemberLeft(leftUserId, remaining);
        };

        // ── 호스트 변경 수신 ──
        hostChangedListener = arguments -> {
            JSONObject payload = ChatSocketEvents.asJsonObject(arguments);
            if (!ChatSocketEvents.isForRoom(payload, roomId)) return;
            int newHostId = ChatSocketEvents.getInt(payload, "new_host_id", -1);
            callbacks.onHostChanged(newHostId);
        };

        // ── 강퇴 수신 — 본인 대상일 때만 ──
        kickedListener = arguments -> {
            JSONObject payload = ChatSocketEvents.asJsonObject(arguments);
            if (!ChatSocketEvents.isForRoom(payload, roomId)) return;
            int targetUserId = ChatSocketEvents.getInt(payload, "target_user_id", -1);
            if (targetUserId != myUserId) return;
            callbacks.onKicked();
        };

        socket.on("message", messageListener);
        socket.on("read_update", readUpdateListener);
        socket.on("member_joined", memberJoinedListener);
        socket.on("member_left", memberLeftListener);
        socket.on("host_changed", hostChangedListener);
        socket.on("kicked", kickedListener);
    }

    /**
     * 등록한 리스너만 정확히 해제한다.
     * off(이벤트명)만 호출하면 SocketManager 의 전역 리스너까지 제거되므로
     * 반드시 off(이벤트명, 리스너) 형태로 제거한다.
     */
    public void unregister() {
        Socket socket = SocketManager.getInstance().getSocket();
        if (socket == null) return;
        if (messageListener != null) socket.off("message", messageListener);
        if (readUpdateListener != null) socket.off("read_update", readUpdateListener);
        if (memberJoinedListener != null) socket.off("member_joined", memberJoinedListener);
        if (memberLeftListener != null) socket.off("member_left", memberLeftListener);
        if (hostChangedListener != null) socket.off("host_changed", hostChangedListener);
        if (kickedListener != null) socket.off("kicked", kickedListener);
    }
}
