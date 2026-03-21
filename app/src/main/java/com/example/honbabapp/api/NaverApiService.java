package com.example.honbabapp.api;

import com.example.honbabapp.api.data.response.naver.NaverImageSearchResponse;
import com.example.honbabapp.api.data.response.naver.NaverLocalSearchResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/**
 * Naver Open API Service
 *
 * Base URL: https://openapi.naver.com/
 */

public interface NaverApiService {
    /**
     * 지역 검색 (Local Search)
     *
     * @param clientId Naver Client ID
     * @param clientSecret Naver Client Secret
     * @param query 검색어 (예: "돼지 먹고 싶은날")
     * @param display 검색 결과 개수 (기본 5, 최대 5)
     * @param start 검색 시작 위치 (기본 1)
     * @param sort 정렬 옵션 (random: 정확도순, comment: 업체 및 기관 리뷰 개수순)
     * @return NaverLocalSearchResponse
     */

    @GET("v1/search/local.json")
    Call<NaverLocalSearchResponse> searchLocal (
            @Header("X-Naver-Client-Id") String clientId,
            @Header("X-Naver-Client-Secret") String clientSecret,
            @Query("query") String query,
            @Query("display") int display,
            @Query("start") int start,
            @Query("sort") String sort
    );

    @GET("v1/search/image")
    Call<NaverImageSearchResponse> searchImage (
            @Header("X-Naver-Client-Id") String clientId,
            @Header("X-Naver-Client-Secret") String clientSecret,
            @Query("query") String query,
            @Query("display") int display,
            @Query("sort") String sort
    );

}
