package com.example.honbabapp.network;

import android.util.Log;

import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {

    private static final String TAG = "SocketManager";
    private static final String SERVER_URL = "http://3.37.127.69:3000";

    private static class LazyHolder {
        static final SocketManager INSTANCE = new SocketManager();
    }

    public static SocketManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private Socket socket;

    private SocketManager() {
        try {
            socket = IO.socket(SERVER_URL);
            Log.d(TAG, "SocketManager 초기화 완료");
        } catch (Exception e) {
            Log.e(TAG, "소켓 초기화 실패: " + e.getMessage());
        }
    }

    public void connect() {
        if (socket != null && !socket.connected()) {
            // 연결 성공
            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d(TAG, "서버 연결 성공! ID: " + socket.id());
            });

            // 연결 실패
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                Log.e(TAG, "연결 실패: " + args[0].toString());
            });

            // 연결 종료
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d(TAG, "연결 종료");
            });

            socket.on("welcome", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    Log.d(TAG, "📩 서버에서 왔어요: " + data.getString("content"));
                } catch (Exception e) {
                    Log.e(TAG, "welcome 파싱 실패: " + e.getMessage());
                }
            });

            // echo 응답 받기
            socket.on("echo", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    Log.d(TAG, "echo 돌아왔어요: " + data.getString("text"));
                } catch (Exception e) {
                    Log.e(TAG, "echo 파싱 실패: " + e.getMessage());
                }
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

            // 메시지 수신
            socket.on("message", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    Log.d(TAG, "메시지 수신! " +
                            "room:" + data.getInt("room_id") +
                            " 내용:" + data.getString("content"));
                } catch (Exception e) {
                    Log.e(TAG, "message 파싱 실패: " + e.getMessage());
                }
            });

            // 읽음 업데이트 수신
            socket.on("read_update", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    Log.d(TAG, "읽음 업데이트! reader_id:" + data.getInt("reader_id")
                            + " room:" + data.getInt("room_id"));
                } catch (Exception e) {
                    Log.e(TAG, "read_update 파싱 실패: " + e.getMessage());
                }
            });

            socket.connect();
        }
    }

    public void sendPing() {
        try {
            JSONObject data = new JSONObject();
            data.put("text", "테스트 메시지");
            socket.emit("ping", data);
            Log.d(TAG, "ping 전송!");
        } catch (Exception e) {
            Log.e(TAG, "ping 전송 실패: " + e.getMessage());
        }
    }

    public void sendEcho() {
        try {
            JSONObject data = new JSONObject();
            data.put("text", "echo 테스트!");
            socket.emit("echo", data);
            Log.d(TAG, "echo 전송!");
        } catch (Exception e) {
            Log.e(TAG, "echo 전송 실패: " + e.getMessage());
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
        }
    }

    public Socket getSocket() {
        return socket;
    }
}