package com.example.honbabapp.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.honbabapp.HonbabApplication;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static String BASE_URL = "http://3.34.98.113/";

    // LoginActivity의 SharedPreferences 키와 동일하게 맞춤
    private static final String PREF_NAME = "honbabapp_prefs";
    private static final String KEY_JWT_TOKEN = "jwt_token";

    private RetrofitClient() {} // 외부 생성 방지

    private static class LazyHolder {
        private static final Retrofit INSTANCE =
                new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(buildOkHttpClient())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
    }

    public static Retrofit getClient() {
        return LazyHolder.INSTANCE;
    }

    /**
     * JWT 토큰 자동 첨부 인터셉터가 포함된 OkHttpClient 생성
     */
    private static OkHttpClient buildOkHttpClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor())
                .build();
    }

    /**
     * 저장된 JWT 토큰이 있으면 모든 요청에 Authorization 헤더를 자동으로 첨부
     */
    private static class AuthInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();

            Context context = HonbabApplication.getAppContext();
            String token = null;

            if (context != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                token = prefs.getString(KEY_JWT_TOKEN, null);
            }

            // 토큰이 없으면 원래 요청 그대로 전송 (비로그인 API 호출 시)
            if (token == null || token.isEmpty()) {
                return chain.proceed(originalRequest);
            }

            // 토큰이 있으면 Authorization 헤더 추가해서 전송
            Request authorizedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();

            return chain.proceed(authorizedRequest);
        }
    }
}