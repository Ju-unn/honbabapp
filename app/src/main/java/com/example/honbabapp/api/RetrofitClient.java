package com.example.honbabapp.api;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.honbabapp.HonbabApplication;
import com.example.honbabapp.activity.LoginActivity;
import com.example.honbabapp.api.data.request.RefreshTokenRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.RefreshTokenResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static String BASE_URL = "http://3.34.98.113/";

    // LoginActivity의 SharedPreferences 키와 동일하게 맞춤
    private static final String PREF_NAME = "honbabapp_prefs";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";

    // 여러 요청이 동시에 401을 받아도 refresh.php는 한 번만 호출되도록 보장
    private static final Object TOKEN_REFRESH_LOCK = new Object();

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
     * JWT 토큰 자동 첨부 인터셉터 + 401 자동 갱신 Authenticator가 포함된 OkHttpClient 생성
     */
    private static OkHttpClient buildOkHttpClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor())
                .authenticator(new TokenRefreshAuthenticator())
                .build();
    }

    /**
     * 저장된 JWT 토큰이 있으면 모든 요청에 Authorization 헤더를 자동으로 첨부
     */
    private static class AuthInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            String accessToken = getStoredAccessToken();

            android.util.Log.d("AuthInterceptor", "요청 URL=" + originalRequest.url()
                    + ", 첨부되는토큰존재여부=" + (accessToken != null));

            if (accessToken == null || accessToken.isEmpty()) {
                return chain.proceed(originalRequest);
            }

            Request authorizedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .build();

            return chain.proceed(authorizedRequest);
        }
    }

    /**
     * 401 응답 수신 시 refresh token으로 access token을 재발급받아
     * 원래 요청을 새 토큰으로 재시도하는 Authenticator.
     *
     * OkHttp 계약: 여기서 새 Request를 반환하면 그 요청으로 자동 재시도하고,
     * null을 반환하면 재시도를 포기하고 원래의 401 응답을 호출부로 그대로 전달한다.
     */
    private static class TokenRefreshAuthenticator implements Authenticator {
        @Override
        public Request authenticate(Route route, Response response) throws IOException {
            android.util.Log.d("TokenRefresh", "401 감지됨, 갱신 시도 시작. 실패한 URL="
                    + response.request().url());

            if (responseChainLength(response) >= 2) {
                android.util.Log.d("TokenRefresh", "이미 재시도했는데도 401 - 포기");
                return null;
            }
            String failedAuthorizationHeader = response.request().header("Authorization");

            synchronized (TOKEN_REFRESH_LOCK) {
                String currentAccessToken = getStoredAccessToken();

                // 이 요청이 실패한 이후 다른 스레드가 이미 갱신을 완료해뒀다면,
                // refresh를 또 호출하지 않고 최신 토큰으로 바로 재시도한다.
                boolean tokenAlreadyRefreshedByAnotherThread =
                        currentAccessToken != null
                                && !("Bearer " + currentAccessToken).equals(failedAuthorizationHeader);

                if (tokenAlreadyRefreshedByAnotherThread) {
                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + currentAccessToken)
                            .build();
                }

                String storedRefreshToken = getStoredRefreshToken();
                if (storedRefreshToken == null || storedRefreshToken.isEmpty()) {
                    forceLogoutAndRedirectToLogin();
                    return null;
                }

                RefreshTokenResponse refreshResult = requestNewTokenPair(storedRefreshToken);
                if (refreshResult == null) {
                    android.util.Log.d("TokenRefresh", "refresh 실패 - 강제 로그아웃");
                    forceLogoutAndRedirectToLogin();
                    return null;
                }

                android.util.Log.d("TokenRefresh", "갱신 성공! 새 access token으로 원 요청 재시도");

                saveTokenPair(refreshResult.getAccess_token(), refreshResult.getRefresh_token());

                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + refreshResult.getAccess_token())
                        .build();
            }
        }
    }

    /**
     * 이 응답에 이르기까지 재시도가 몇 번 있었는지 계산 (priorResponse 체인 길이)
     */
    private static int responseChainLength(Response response) {
        int length = 1;
        Response prior = response.priorResponse();
        while (prior != null) {
            length++;
            prior = prior.priorResponse();
        }
        return length;
    }

    /**
     * refresh.php를 동기 호출해서 새 토큰 쌍을 받아온다.
     *
     * getClient()(AuthInterceptor + 이 Authenticator가 모두 붙은 Retrofit)를 쓰지 않고
     * 별도의 순수 OkHttpClient를 쓰는 이유: 순환 호출 방지.
     * refresh 요청 자체는 애초에 Authorization 헤더가 필요 없다.
     */
    private static RefreshTokenResponse requestNewTokenPair(String refreshToken) {
        try {
            OkHttpClient plainHttpClient = new OkHttpClient();
            Gson gson = new Gson();

            String requestJson = gson.toJson(new RefreshTokenRequest(refreshToken));
            RequestBody requestBody = RequestBody.create(
                    requestJson, MediaType.parse("application/json; charset=utf-8"));

            Request refreshRequest = new Request.Builder()
                    .url(BASE_URL + "api/auth/refresh.php")
                    .post(requestBody)
                    .build();

            try (Response refreshResponse = plainHttpClient.newCall(refreshRequest).execute()) {
                if (!refreshResponse.isSuccessful() || refreshResponse.body() == null) {
                    return null;
                }

                String responseJson = refreshResponse.body().string();
                Type responseType = TypeToken
                        .getParameterized(ApiResponse.class, RefreshTokenResponse.class)
                        .getType();
                ApiResponse<RefreshTokenResponse> apiResponse = gson.fromJson(responseJson, responseType);

                if (apiResponse == null || !apiResponse.isSuccess() || apiResponse.getData() == null) {
                    return null;
                }

                return apiResponse.getData();
            }
        } catch (IOException ioException) {
            return null;
        }
    }

    private static String getStoredAccessToken() {
        SharedPreferences prefs = getPrefs();
        return prefs != null ? prefs.getString(KEY_JWT_TOKEN, null) : null;
    }

    private static String getStoredRefreshToken() {
        SharedPreferences prefs = getPrefs();
        return prefs != null ? prefs.getString(KEY_REFRESH_TOKEN, null) : null;
    }

    private static void saveTokenPair(String accessToken, String refreshToken) {
        SharedPreferences prefs = getPrefs();
        if (prefs == null) return;

        prefs.edit()
                .putString(KEY_JWT_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    private static SharedPreferences getPrefs() {
        Context context = HonbabApplication.getAppContext();
        if (context == null) return null;
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * refresh token마저 만료/폐기된 경우 - 로컬 로그인 상태를 전부 초기화하고
     * 로그인 화면으로 강제 이동한다 (모든 액티비티 스택 제거).
     */
    private static void forceLogoutAndRedirectToLogin() {
        Context context = HonbabApplication.getAppContext();
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent loginIntent = new Intent(context, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(loginIntent);
    }
}