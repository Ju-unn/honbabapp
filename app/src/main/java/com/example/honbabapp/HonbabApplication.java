package com.example.honbabapp;

import android.app.Application;
import android.content.Context;

import com.kakao.sdk.common.KakaoSdk;

public class HonbabApplication extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();

        appContext = getApplicationContext();

        // 카카오 SDK 초기화
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY);
    }

    /**
     * 앱 전역에서 사용할 Application Context
     * RetrofitClient처럼 Context가 필요한 static 클래스에서 사용
     */
    public static Context getAppContext() {
        return appContext;
    }
}