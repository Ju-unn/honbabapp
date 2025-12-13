package com.example.honbabapp.api;

import com.example.honbabapp.api.data.LoginRequest;
import com.example.honbabapp.api.data.LoginResponse;
import com.example.honbabapp.api.data.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    @POST("login.php")
    Call<LoginResponse> loginUser(@Body LoginRequest loginRequest);

}
