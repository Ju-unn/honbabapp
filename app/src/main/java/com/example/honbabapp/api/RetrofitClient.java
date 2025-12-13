package com.example.honbabapp.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static String BASE_URL = "http://3.37.127.69/";
    private static Retrofit retrofit = null;

    // Retrofit 인스턴스 반환
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
