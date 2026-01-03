package com.example.honbabapp.api;

import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NaverRetrofitClient {

    private static String BASE_URL = "https://openapi.naver.com/";
    private static Retrofit retrofit = null;


    /**
     * Retrofit 인스턴스 반환 (싱글톤)
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create()) // json 변환
                    .build();
        }
        return retrofit;
    }
}
