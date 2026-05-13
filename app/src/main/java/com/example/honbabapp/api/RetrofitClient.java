package com.example.honbabapp.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static String BASE_URL = "http://3.34.98.113/";
    private RetrofitClient() {} // 외부 생성 방지

    private static class LazyHolder {
        private static final Retrofit INSTANCE =
                new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
    }

    public static Retrofit getClient() {
        return LazyHolder.INSTANCE;
    }
}
