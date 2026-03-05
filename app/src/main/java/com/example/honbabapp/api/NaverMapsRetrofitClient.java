package com.example.honbabapp.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NaverMapsRetrofitClient {

    private static final String BASE_URL = "https://maps.apigw.ntruss.com/";
    private static NaverMapsRetrofitClient instance;
    private final NaverMapsApiService apiService;

    private NaverMapsRetrofitClient() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()) //json 변환
                .build();

        apiService = retrofit.create(NaverMapsApiService.class);
    }

    public static synchronized NaverMapsRetrofitClient getInstance() {
        if (instance == null) {
            instance = new NaverMapsRetrofitClient();
        }
        return instance;
    }

    public NaverMapsApiService getApiService() {
        return apiService;
    }
}
