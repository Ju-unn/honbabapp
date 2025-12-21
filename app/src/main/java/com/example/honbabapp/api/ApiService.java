package com.example.honbabapp.api;

import com.example.honbabapp.api.data.AvailabilityData;
import com.example.honbabapp.api.data.LoginData;
import com.example.honbabapp.api.data.VerificationData;
import com.example.honbabapp.api.data.VerifiedData;
import com.example.honbabapp.api.data.request.LoginRequest;
import com.example.honbabapp.api.data.request.RegisterRequest;
import com.example.honbabapp.api.data.request.SendVerificationRequest;
import com.example.honbabapp.api.data.request.VerifyCodeRequest;
import com.example.honbabapp.api.data.response.ApiResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {


    // ==================== 로그인 ====================
    /**
     * 로그인
     * @param loginRequest 이메일, 비밀번호
     * @return LoginResponse
     */
    @POST("api/auth/login.php")
    Call<ApiResponse<LoginData>> login(@Body LoginRequest loginRequest);

    // ==================== 회원가입 ====================

    /**
     * 1. 닉네임 중복 확인
     * @param nickname 확인할 닉네임
     * @return ApiResponse<AvailabilityData> - available: true/false
     */
    @GET("api/auth/check_nickname.php")
    Call<ApiResponse<AvailabilityData>> checkNickname(@Query("nickname") String nickname);

    /**
     * 2. 이메일 중복 확인
     * @param email 확인할 이메일
     * @return ApiResponse<AvailabilityData> - available: true/false
     */
    @GET("api/auth/check_email.php")
    Call<ApiResponse<AvailabilityData>> checkEmail(@Query("email") String email);

    /**
     * 3. 이메일 인증 코드 발송
     * @param request 이메일 주소
     * @return ApiResponse<VerificationData> - code, expires_in
     */
    @POST("api/auth/send_verification.php")
    Call<ApiResponse<VerificationData>> sendVerification(@Body SendVerificationRequest request);

    /**
     * 4. 인증 코드 검증
     * @param request 이메일, 인증 코드
     * @return ApiResponse<VerifiedData> - verified: true/false
     */
    @POST("api/auth/verify_code.php")
    Call<ApiResponse<VerifiedData>> verifyCode(@Body VerifyCodeRequest request);

    /**
     * 5. 전화번호 중복 확인
     * @param phone 확인할 전화번호 (010-1234-5678)
     * @return ApiResponse<AvailabilityData> - available: true/false
     */
    @GET("api/auth/check_phone.php")
    Call<ApiResponse<AvailabilityData>> checkPhone(@Query("phone") String phone);

    /**
     * 6. 회원가입 최종 등록
     * @param request 닉네임, 이메일, 비밀번호, 전화번호, 생년월일, 성별, 위치
     * @return ApiResponse<User> - 생성된 사용자 정보
     */
    @POST("api/auth/register.php")
    Call<ApiResponse<Void>> register(@Body RegisterRequest request);

    /**
     * Retrofit 라이브러리를 이용해서 통신을 하는데
     * MultiPart타입, POST(HTTP Method) 형식으로 통신한다
     * MultiPart는 이미지같은 형식을 보내기위해 사용하며 byte형식으로 통신하여 파일등을 주고받을 수 있도록
     * 지원한다
     */
    @Multipart
    @POST("")
    Call<ApiResponse<Void>> uploadProfileImage(
            @Part MultipartBody.Part image,
            @Part("user_id") RequestBody userId
            );
}

