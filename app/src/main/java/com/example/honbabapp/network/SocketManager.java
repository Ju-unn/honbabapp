package com.example.honbabapp.network;

import android.util.Log;

import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {

    private static final String TAG = "SocketManager";
    private static final String SERVER_URL = "http://15.164.213.194:3000";

    private static class LazyHolder {
        static final SocketManager INSTANCE = new SocketManager();
    }

    public static SocketManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private Socket socket;
    // 그래서 이 플래그 없이는 connect()를 여러 번 호출하면 리스너가 중복 등록될 수 있음
    private boolean isConnecting = false;

    private SocketManager() {
        try {
            socket = IO.socket(SERVER_URL);
            Log.d(TAG, "SocketManager 초기화 완료");
        } catch (Exception e) {
            Log.e(TAG, "소켓 초기화 실패: " + e.getMessage());
        }
    }

    public void connect() {
        // connected() 체크만으로는 부족 — 연결 시도 중에도 false를 반환하기 때문
        // isConnecting까지 함께 체크해야 중복 connect() 호출 방지
        if (socket != null && !socket.connected() && !isConnecting) {
            isConnecting = true;

            // 연결 성공
            socket.on(Socket.EVENT_CONNECT, args -> {
                isConnecting = false; // 연결 완료 → 플래그 해제
                Log.d(TAG, "서버 연결 성공! ID: " + socket.id());
            });

            // 연결 실패
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                isConnecting = false; // 실패해도 플래그 해제 → 재시도 가능하게
                Log.e(TAG, "연결 실패: " + args[0].toString());
            });

            // 연결 종료
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d(TAG, "연결 종료");
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

    public void joinRoom(int roomId, int userId) {
        try {
            JSONObject data = new JSONObject();
            data.put("room_id", roomId);
            data.put("user_id", userId);
            socket.emit("join", data);
            Log.d(TAG, "join 전송! room:" + roomId);
        } catch (Exception e) {
            Log.e(TAG, "join 전송 실패: " + e.getMessage());
        }
    }

    public void sendMessage(int roomId, int userId, String content) {
        try {
            JSONObject data = new JSONObject();
            data.put("room_id", roomId);
            data.put("user_id", userId);
            data.put("content", content);
            socket.emit("message", data);
            Log.d(TAG, "메시지 전송! room:" + roomId + " 내용:" + content);
        } catch (Exception e) {
            Log.e(TAG, "메시지 전송 실패: " + e.getMessage());
        }
    }

    public void emitMemberJoined(int roomId, int userId) {
        try {
            JSONObject data = new JSONObject();
            data.put("room_id", roomId);
            data.put("user_id", userId);
            socket.emit("member_joined", data);
            Log.d(TAG, "member_joined 전송! room:" + roomId + " user:" + userId);
        } catch (Exception e) {
            Log.e(TAG, "member_joined 전송 실패: " + e.getMessage());
        }
    }

    public void sendRead(int roomId, int userId) {
        try {
            JSONObject data = new JSONObject();
            data.put("room_id", roomId);
            data.put("user_id", userId);
            socket.emit("read", data);
            Log.d(TAG, "읽음 전송! room:" + roomId);
        } catch (Exception e) {
            Log.e(TAG, "읽음 전송 실패: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (socket != null && socket.connected()) {
            socket.disconnect();
            isConnecting = false; //연결 끊길 때도 플래그 초기화
        }
    }

    public Socket getSocket() {
        return socket;
    }
}