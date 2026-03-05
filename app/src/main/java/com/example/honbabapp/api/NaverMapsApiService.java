package com.example.honbabapp.api;

import com.example.honbabapp.api.data.response.naver.NaverReverseGeoResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface NaverMapsApiService {

    @GET("map-reversegeocode/v2/gc")
    Call<NaverReverseGeoResponse> reverseGeocode(
            @Header("X-NCP-APIGW-API-KEY-ID") String keyId,    // Client ID
            @Header("X-NCP-APIGW-API-KEY")    String key,      // Client Secret
            @Query("coords")  String coords,   // "경도,위도" 형식 (예: "126.978,37.566")
            @Query("output")  String output,   // "json" 고정
            @Query("orders")  String orders    // "admcode" 고정 (행정구역 기준)
    );
}
