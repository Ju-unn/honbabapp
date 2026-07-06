package com.example.honbabapp.fcm;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.example.honbabapp.R;
import com.example.honbabapp.activity.MainActivity;
import com.example.honbabapp.activity.chat.ChatRoomActivity;
import com.example.honbabapp.activity.meeting.MeetingDetailActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.request.fcm.RegisterFcmTokenRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * FCM(Firebase Cloud Messaging) 푸시 알림 수신 서비스
 *
 * - onNewToken: 디바이스 토큰을 SharedPreferences에 저장하고, 로그인 상태면
 *               즉시 서버(register_fcm_token.php)에 등록한다.
 * - onMessageReceived: data-only 메시지를 수신해 알림을 표시한다.
 *   - 현재 보고 있는 채팅방의 'chat' 알림은 표시하지 않는다
 *     (ChatRoomActivity.currentOpenRoomId 와 비교, 소켓이 이미 화면을 갱신함).
 *   - 알림 탭 시 type 에 따라 적절한 화면으로 딥링크한다.
 *
 * 알림 채널:
 *   - "chat"    : 채팅 메시지 / 후기 작성 요청
 *   - "general" : 모임 승인·거절·초대·강퇴, 후기 수신 등 그 외 전체
 */
public class FirebaseInstanceIdService extends FirebaseMessagingService {

    private static final String TAG = "FirebaseInstanceIdService";

    private static final String PREF_NAME = "honbabapp_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    public static final String CHANNEL_ID_CHAT = "chat";
    public static final String CHANNEL_ID_GENERAL = "general";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM 토큰 갱신: " + token);

        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(KEY_FCM_TOKEN, token).apply();

        int userId = sharedPreferences.getInt(KEY_USER_ID, -1);
        if (userId == -1) {
            // 로그인 전이면 서버에 등록할 user_id가 없음
            // → 로그인 시 LoginActivity.handleLoginSuccess()에서 다시 등록됨
            Log.d(TAG, "로그인 전 - FCM 토큰은 로컬에만 저장, 서버 등록은 로그인 시 수행");
            return;
        }

        registerTokenToServer(userId, token);
    }

    /**
     * FCM 토큰을 서버에 등록 (best-effort — 실패해도 앱 동작에는 영향 없음)
     */
    private void registerTokenToServer(int userId, String token) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        RegisterFcmTokenRequest request = new RegisterFcmTokenRequest(token);

        apiService.registerFcmToken(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "FCM 토큰 서버 등록 완료 (user_id=" + userId + ")");
                } else {
                    Log.e(TAG, "FCM 토큰 서버 등록 실패: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "FCM 토큰 서버 등록 네트워크 오류: " + t.getMessage(), t);
            }
        });
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        if (data.isEmpty()) {
            Log.w(TAG, "data 페이로드 없음 - 알림 생략");
            return;
        }

        String type = data.get("type");
        String title = data.get("title");
        String body = data.get("body");

        Log.d(TAG, "FCM 메시지 수신: type=" + type + ", data=" + data);

        // ─────────────────────────────────────────
        // 포그라운드 억제: 현재 보고 있는 채팅방의 'chat' 메시지는
        // 알림을 띄우지 않는다 (소켓이 이미 화면을 갱신했기 때문).
        // 다른 방의 메시지 / 백그라운드에서는 정상적으로 알림 표시.
        // ─────────────────────────────────────────
        if ("chat".equals(type)) {
            int roomId = parseIntSafely(data.get("room_id"), -1);
            if (roomId != -1 && roomId == ChatRoomActivity.currentOpenRoomId) {
                Log.d(TAG, "현재 보고 있는 채팅방(room_id=" + roomId + ") 메시지 - 알림 생략");
                return;
            }
        }

        showNotification(type, title, body, data);
    }

    /**
     * 알림 채널 생성(필요 시) 후 알림을 표시한다.
     */
    private void showNotification(String type, String title, String body, Map<String, String> data) {
        ensureNotificationChannels();

        String channelId = "chat".equals(type) || "review_prompt".equals(type)
                ? CHANNEL_ID_CHAT
                : CHANNEL_ID_GENERAL;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.app_logo2)
                .setContentTitle(title != null ? title : getString(R.string.app_name))
                .setContentText(body != null ? body : "")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(buildContentIntent(type, data));

        // SecurityException을 던지므로 사전에 체크한다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "POST_NOTIFICATIONS 권한 없음 - 알림 표시 생략");
            return;
        }

        int notificationId = (int) System.currentTimeMillis();
        NotificationManagerCompat.from(this).notify(notificationId, builder.build());
    }

    /**
     * 알림 탭 시 이동할 화면을 type 에 따라 결정한다.
     *
     *  - chat / review_prompt / chat_invite : ChatRoomActivity (room_id)
     *  - review_received / meeting_approved / meeting_rejected : MeetingDetailActivity (meeting_id)
     *  - 그 외(chat_kicked 포함) : MainActivity
     *
     * MainActivity를 부모로 하는 백스택을 TaskStackBuilder로 구성한다.
     */
    private PendingIntent buildContentIntent(String type, Map<String, String> data) {
        Intent targetIntent;
        boolean addMainAsParent = true;

        if ("chat".equals(type) || "review_prompt".equals(type) || "chat_invite".equals(type)) {
            targetIntent = new Intent(this, ChatRoomActivity.class);
            targetIntent.putExtra("room_id", parseIntSafely(data.get("room_id"), -1));
        } else if ("review_received".equals(type)
                || "meeting_approved".equals(type)
                || "meeting_rejected".equals(type)) {
            targetIntent = new Intent(this, MeetingDetailActivity.class);
            targetIntent.putExtra("meeting_id", parseIntSafely(data.get("meeting_id"), -1));
        } else {
            targetIntent = new Intent(this, MainActivity.class);
            addMainAsParent = false;
        }

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);

        if (addMainAsParent) {
            taskStackBuilder.addNextIntent(new Intent(this, MainActivity.class));
        }
        taskStackBuilder.addNextIntent(targetIntent);

        int requestCode = (int) System.currentTimeMillis();
        return taskStackBuilder.getPendingIntent(
                requestCode,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * "chat", "general" 알림 채널 생성.
     * createNotificationChannel은 이미 존재하는 채널 ID로 호출해도 안전(idempotent)하므로
     * 알림 표시 직전에 매번 호출해도 문제없다.
     */
    private void ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager == null) return;

        NotificationChannel chatChannel = new NotificationChannel(
                CHANNEL_ID_CHAT,
                "채팅 알림",
                NotificationManager.IMPORTANCE_HIGH
        );
        chatChannel.setDescription("새 채팅 메시지 및 후기 작성 요청 알림");

        NotificationChannel generalChannel = new NotificationChannel(
                CHANNEL_ID_GENERAL,
                "일반 알림",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        generalChannel.setDescription("모임 승인/거절/초대/강퇴, 후기 수신 등 일반 알림");

        notificationManager.createNotificationChannel(chatChannel);
        notificationManager.createNotificationChannel(generalChannel);
    }

    private int parseIntSafely(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}