package com.example.honbabapp.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * WebSocket 연결을 전역에서 관리하는 싱글톤 클래스
 * OkHttp WebSocket 기반으로 연결/재연결/전송/해제를 담당한다
 */
public class WebSocketManager {

    private static final String TAG = "WebSocketManager";
    private static final String WS_URL = "ws://3.37.127.69:8080";
    private static final int MAX_RETRY = 3;          // 최대 재연결 횟수
    private static final long RETRY_DELAY_MS = 3000; // 재연결 대기 시간 (3초)

    // ─────────────────────────────────────────
    // 싱글톤
    // ─────────────────────────────────────────
    private WebSocketManager() {}

    private static class LazyHolder {
        private static final WebSocketManager INSTANCE = new WebSocketManager();
    }

    public static WebSocketManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    // ─────────────────────────────────────────
    // 내부 상태
    // ─────────────────────────────────────────
    private WebSocket webSocket;
    private OkHttpClient client;
    private int currentUserId;
    private MessageListener listener;
    private int retryCount = 0;
    private boolean isConnected = false; // 연결 상태 플래그

    // ─────────────────────────────────────────
    // 콜백 인터페이스
    // ─────────────────────────────────────────

    /**
     * 외부(Activity/Fragment)에서 WebSocket 이벤트를 수신하는 콜백
     */
    public interface MessageListener {
        /** 서버로부터 JSON 문자열 수신 */
        void onMessage(String json);
        /** WebSocket 연결 성공 */
        void onConnected();
        /** WebSocket 연결 끊김 (재연결 포기 포함) */
        void onDisconnected();
    }

    // ─────────────────────────────────────────
    // 공개 메서드
    // ─────────────────────────────────────────

    /**
     * WebSocket 서버에 연결한다
     * 이미 연결된 상태라면 기존 연결을 종료하고 재연결한다
     *
     * @param userId   현재 로그인한 사용자 ID
     * @param listener 이벤트 수신 콜백
     */
    public void connect(int userId, MessageListener listener) {
        // 이미 연결된 경우 기존 연결 정리 후 재연결
        if (isConnected && webSocket != null) {
            Log.d(TAG, "기존 연결 종료 후 재연결");
            webSocket.cancel(); // 즉시 종료 (onClosed 콜백 없이)
        }

        this.currentUserId = userId;
        this.listener = listener;

        // OkHttpClient 재생성 (이전 인스턴스 누수 방지)
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
        client = new OkHttpClient();

        String url = WS_URL + "?user_id=" + userId;
        Log.d(TAG, "WebSocket 연결 시도: " + url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(WebSocket ws, Response response) {
                // 연결 성공
                retryCount = 0;
                isConnected = true;
                Log.d(TAG, "WebSocket 연결 성공");
                // 콜백은 메인 스레드에서 전달
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (listener != null) listener.onConnected();
                });
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                // 메시지 수신 — 파싱은 호출자(Activity)에서 처리
                Log.d(TAG, "메시지 수신: " + text);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (listener != null) listener.onMessage(text);
                });
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                // 정상 종료 (disconnect() 호출 등)
                isConnected = false;
                Log.d(TAG, "WebSocket 정상 종료. code=" + code + ", reason=" + reason);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (listener != null) listener.onDisconnected();
                });
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                // 비정상 종료 — 재연결 시도
                isConnected = false;
                Log.e(TAG, "WebSocket 오류 발생: " + t.getMessage());

                if (retryCount < MAX_RETRY) {
                    retryCount++;
                    Log.d(TAG, "재연결 시도 " + retryCount + "/" + MAX_RETRY);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        connect(currentUserId, WebSocketManager.this.listener);
                    }, RETRY_DELAY_MS);
                } else {
                    // 최대 재연결 횟수 초과 → 포기
                    Log.e(TAG, "재연결 포기 (최대 횟수 초과)");
                    retryCount = 0;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (listener != null) listener.onDisconnected();
                    });
                }
            }
        });
    }

    /**
     * 채팅방에 입장한다 (서버에 join 타입 전송)
     *
     * @param roomId 입장할 채팅방 ID
     * @param userId 입장하는 사용자 ID
     */
    public void joinRoom(int roomId, int userId) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", "join");
        obj.addProperty("room_id", roomId);
        obj.addProperty("user_id", userId);
        send(obj.toString());
        Log.d(TAG, "joinRoom 전송: roomId=" + roomId);
    }

    /**
     * 채팅 메시지를 전송한다
     *
     * @param roomId  전송할 채팅방 ID
     * @param userId  발신자 ID
     * @param content 메시지 내용
     */
    public void sendMessage(int roomId, int userId, String content) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", "message");
        obj.addProperty("room_id", roomId);
        obj.addProperty("user_id", userId);
        obj.addProperty("content", content);
        obj.addProperty("msg_type", "text");
        send(obj.toString());
        Log.d(TAG, "sendMessage 전송: roomId=" + roomId + ", content=" + content);
    }

    /**
     * 읽음 처리를 서버에 전송한다
     *
     * @param roomId 읽음 처리할 채팅방 ID
     * @param userId 읽은 사용자 ID
     */
    public void markRead(int roomId, int userId) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", "read");
        obj.addProperty("room_id", roomId);
        obj.addProperty("user_id", userId);
        send(obj.toString());
        Log.d(TAG, "markRead 전송: roomId=" + roomId);
    }

    /**
     * 채팅방에서 나간다 (서버에 leave 타입 전송)
     *
     * @param roomId 나갈 채팅방 ID
     * @param userId 나가는 사용자 ID
     */
    public void leaveRoom(int roomId, int userId) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", "leave");
        obj.addProperty("room_id", roomId);
        obj.addProperty("user_id", userId);
        send(obj.toString());
        Log.d(TAG, "leaveRoom 전송: roomId=" + roomId);
    }

    /**
     * WebSocket 연결을 끊는다
     * 앱 종료 또는 로그아웃 시 호출
     */
    public void disconnect() {
        if (webSocket != null) {
            // 1000 = 정상 종료 코드
            webSocket.close(1000, "사용자 연결 종료");
            webSocket = null;
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client = null;
        }
        isConnected = false;
        listener = null;
        retryCount = 0;
        Log.d(TAG, "WebSocket 연결 종료 완료");
    }

    /**
     * 현재 WebSocket 연결 상태를 반환한다
     */
    public boolean isConnected() {
        return isConnected;
    }

    // ─────────────────────────────────────────
    // 내부 전송 메서드
    // ─────────────────────────────────────────

    /**
     * WebSocket으로 JSON 문자열을 전송한다
     * 연결이 없는 경우 로그만 남기고 무시한다
     */
    private void send(String json) {
        if (webSocket == null || !isConnected) {
            Log.w(TAG, "WebSocket 미연결 상태 — 전송 무시: " + json);
            return;
        }
        boolean result = webSocket.send(json);
        if (!result) {
            Log.e(TAG, "전송 실패 (버퍼 초과 또는 연결 끊김): " + json);
        }
    }
}