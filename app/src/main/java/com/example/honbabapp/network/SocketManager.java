package com.example.honbabapp.network;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {

    private static final String TAG = "SocketManager";
    private static final String SERVER_URL = "http://3.34.98.113:3000";

    private static class LazyHolder {
        static final SocketManager INSTANCE = new SocketManager();
    }

    public static SocketManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private Socket socket;
    private boolean isConnecting = false;

    // 연결 전에 login()이 호출됐을 때 userId를 임시 보관
    // 연결 완료되면 EVENT_CONNECT에서 꺼내서 emit
    private int pendingLoginUserId = -1;

    private SocketManager() {
        try {
            socket = IO.socket(SERVER_URL);
            Log.d(TAG, "SocketManager 초기화 완료");
        } catch (Exception e) {
            Log.e(TAG, "소켓 초기화 실패: " + e.getMessage());
        }
    }

    public void connect() {
        if (socket != null && !socket.connected() && !isConnecting) {
            isConnecting = true;

            //  중복 등록 방지: 등록 전에 먼저 제거
            socket.off(Socket.EVENT_CONNECT);
            socket.off(Socket.EVENT_CONNECT_ERROR);
            socket.off(Socket.EVENT_DISCONNECT);
            socket.off("system");

            // 연결 성공
            socket.on(Socket.EVENT_CONNECT, args -> {
                isConnecting = false;
                Log.d(TAG, "서버 연결 성공! ID: " + socket.id());

                // 연결 완료 시점에 pendingLoginUserId가 있으면 login emit
                // 재연결(네트워크 끊겼다 복구)될 때도 자동으로 재로그인됨
                if (pendingLoginUserId != -1) {
                    emitLogin(pendingLoginUserId);
                }
            });

            // 연결 실패
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                isConnecting = false;
                Log.e(TAG, "연결 실패: " + args[0].toString());
            });

            // 연결 종료
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d(TAG, "연결 종료 - user_id: " + pendingLoginUserId);
            });

            // 시스템 메시지 수신
            socket.on("system", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    Log.d(TAG, "시스템: " + data.getString("content"));
                } catch (Exception e) {
                    Log.e(TAG, "system 파싱 실패: " + e.getMessage());
                }
            });

            socket.connect();
        }
    }

    /**
     * 로그인 emit — 앱 시작 시 MainActivity에서 한 번 호출
     *
     * 동작 원리:
     * - 이미 연결됨 → 즉시 emit
     * - 아직 연결 안 됨 → pendingLoginUserId에 저장 → connect() 트리거
     *   → EVENT_CONNECT 콜백에서 자동으로 emit
     * - 재연결 시 → EVENT_CONNECT 재실행 → 자동 재로그인
     */
    public void login(int userId) {
        pendingLoginUserId = userId; // 항상 저장 (재연결 대비)

        if (socket != null && socket.connected()) {
            // 이미 연결된 상태 → 즉시 emit
            emitLogin(userId);
        } else {
            // 아직 연결 안 됨 → connect() 호출, 완료되면 EVENT_CONNECT에서 emit
            Log.d(TAG, "소켓 미연결 상태 — connect() 후 login emit 예정");
            connect();
        }
    }

    /** 실제 login emit 로직 (내부 전용) */
    private void emitLogin(int userId) {
        try {
            JSONObject data = new JSONObject();
            data.put("user_id", userId);
            socket.emit("login", data);
            Log.d(TAG, "login emit 완료 — user_id: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "login emit 실패: " + e.getMessage());
        }
    }

    public void sendMessage(int roomId, int userId, String content) {
        try {
            JSONObject data = new JSONObject();
            data.put("room_id", roomId);
            data.put("user_id", userId);
            data.put("content", content);
            socket.emit("message", data);
            Log.d(TAG, "메시지 전송! room:" + roomId);
        } catch (Exception e) {
            Log.e(TAG, "메시지 전송 실패: " + e.getMessage());
        }
    }

    /**
     * 단일 이미지 전송 — 구버전 호환용
     * @deprecated 새 코드는 sendImageMessage(roomId, userId, urls, clientMsgId) 사용
     */
    public void sendImageMessage(int roomId, int userId, String imageUrl) {
        List<String> urls = new ArrayList<>();
        urls.add(imageUrl);
        sendImageMessage(roomId, userId, urls, null);
    }

    /**
     * 다중 이미지 전송 (묶음 메시지 1개)
     *
     * @param roomId      채팅방 ID
     * @param userId      보내는 user_id
     * @param urls        S3 업로드 완료된 URL 목록 (1장 이상)
     * @param clientMsgId optimistic dedupe 키 (null 이면 생략)
     */
    public void sendImageMessage(int roomId, int userId, List<String> urls, String clientMsgId) {
        try {
            JSONObject data = new JSONObject();
            data.put("room_id", roomId);
            data.put("user_id", userId);
            data.put("msg_type", "image");
            data.put("image_url", urls.get(0));
            data.put("content", "사진");

            // image_urls 배열
            JSONArray arr = new JSONArray();
            for (String url : urls) arr.put(url);
            data.put("image_urls", arr);

            // client_msg_id — 있을 때만 포함
            if (clientMsgId != null) data.put("client_msg_id", clientMsgId);

            socket.emit("message", data);
            Log.d(TAG, "이미지 메시지 전송! room:" + roomId
                    + " urls:" + urls.size() + "장"
                    + " clientMsgId:" + clientMsgId);
        } catch (Exception e) {
            Log.e(TAG, "이미지 메시지 전송 실패: " + e.getMessage());
        }
    }

    public void sendRead(int roomId, int userId) {
        try {
            JSONObject data = new JSONObject();
            data.put("room_id", roomId);
            data.put("user_id", userId);
            socket.emit("read", data);
        } catch (Exception e) {
            Log.e(TAG, "read 전송 실패: " + e.getMessage());
        }
    }

    public void emitMemberJoined(int roomId, int userId) {
        try {
            JSONObject data = new JSONObject();
            data.put("room_id", roomId);
            data.put("user_id", userId);
            socket.emit("member_joined", data);
            Log.d(TAG, "member_joined emit! room:" + roomId + " user:" + userId);
        } catch (Exception e) {
            Log.e(TAG, "member_joined emit 실패: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (socket != null && socket.connected()) {
            socket.disconnect();
        }
    }

    public Socket getSocket() {
        return socket;
    }
}