package com.example.honbabapp.fcm;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.honbabapp.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FirebaseInstanceIdService extends FirebaseMessagingService {

    // firebase에서 특정 디바이스를 구분하기 위한 값
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        SharedPreferences sharedPreferences = this.getSharedPreferences("token" , Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("token" , token).apply();
        editor.commit();

        Log.i("Test_log" , "FirebaseInstanceIdService.OnNewToken token :" + token);
    }

    // firebase에서 메세지를 줬을때 전달 받을 곳
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        Map<String, String> data = message.getData();


        Bundle bundleData = message.toIntent().getExtras();
        String BundleTitle = bundleData.getString("gcm.notification.title");
        String BundleBody = bundleData.getString("gcm.notification.body");
        Log.i("Test_log" , "bundleMessage title :" + BundleTitle + "\n BundleBody :" + BundleBody);
        showNotification(BundleTitle , BundleBody);

    }

    // 상단 알림영역을 그리는 역할
    public void sendNotification(RemoteMessage remoteMessage) {

    }

    // 알림 채널 생성 (Android 8.0 이상 필수)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "CHANNEL_ID",
                    "CHANNEL_NAME",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("혼밥앱의 알림을 받습니다");

            NotificationManager manager = getBaseContext().getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    // 기본 알림 표시
    public void showNotification(String title, String message) {
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext(), "CHANNEL_ID")
                .setSmallIcon(R.drawable.ic_notification) // 알림 아이콘
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true); // 클릭 시 자동으로 사라짐

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getBaseContext());
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1000, builder.build());
    }
}
