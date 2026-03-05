package com.example.honbabapp.api;

import com.example.honbabapp.api.data.response.meeting.ApplicantsResponse;
import com.example.honbabapp.api.data.response.meeting.ApproveResponse;
import com.example.honbabapp.api.data.AvailabilityData;
import com.example.honbabapp.api.data.ImageUploadData;
import com.example.honbabapp.api.data.login.LoginData;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.response.meeting.MeetingCreateResponse;
import com.example.honbabapp.api.data.response.meeting.MeetingListResponse;
import com.example.honbabapp.api.data.response.meeting.ParticipantResponse;
import com.example.honbabapp.api.data.VerificationData;
import com.example.honbabapp.api.data.VerifiedData;
import com.example.honbabapp.api.data.request.ApproveMeetingRequest;
import com.example.honbabapp.api.data.request.JoinMeetingRequest;
import com.example.honbabapp.api.data.request.LoginRequest;
import com.example.honbabapp.api.data.request.MeetingCreateRequest;
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
    @POST("api/auth/upload_profile_image.php")
    Call<ApiResponse<ImageUploadData>> uploadProfileImage(
            @Part MultipartBody.Part image,
            @Part("user_id") RequestBody userId
            );

    // ==================== Meeting API ====================

    /**
     * 1. 모임 생성
     * @param request 모임 정보 (제목, 설명, 카테고리, 날짜, 인원, 가격, 장소)
     * @return ApiResponse<MeetingCreateData> - meeting_id, location_id
     */
    @POST("api/meeting/create.php")
    Call<ApiResponse<MeetingCreateResponse>> createMeeting(@Body MeetingCreateRequest request);

    /**
     * 2. 모임 목록 조회 (지역 필터링)
     * @param sigungu 시/군/구 (예: "관악구", "강남구")
     * @param category 카테고리 (예: "한식", "중식", "전체")
     * @param status 모임 상태 (기본값: "모집중")
     * @param page 페이지 번호 (기본값: 1)
     * @param limit 페이지당 개수 (기본값: 10)
     * @return ApiResponse<MeetingListData> - meetings 배열, total_count, page, total_pages
     */
    @GET("api/meeting/list.php")
    Call<ApiResponse<MeetingListResponse>> getMeetingList(
            @Query("sigungu") String sigungu,
            @Query("category") String category,
            @Query("status") String status,
            @Query("page") int page,
            @Query("limit") int limit
    );

    /**
     * 3. 모임 상세 조회
     * @param meetingId 모임 ID
     * @return ApiResponse<Meeting> - 모임 상세 정보 (location, host, participants 포함)
     */
    @GET("api/meeting/detail.php")
    Call<ApiResponse<Meeting>> getMeetingDetail(@Query("meeting_id") int meetingId , @Query("user_id") int userId);

    /**
     * 4. 모임 참여 신청
     * @param request meeting_id, user_id
     * @return ApiResponse<ParticipantData> - participant_id, participation_status
     */
    @POST("api/meeting/join.php")
    Call<ApiResponse<ParticipantResponse>> joinMeeting(@Body JoinMeetingRequest request);

    /**
     * 5. 신청자 목록 조회 (호스트 전용)
     * @param meetingId 모임 ID
     * @param hostId 호스트 ID (권한 확인)
     * @param status 참여 상태 ("대기", "승인", "거절")
     * @return ApiResponse<ApplicantsData> - applicants 배열, total_count
     */
    @GET("api/meeting/applicants.php")
    Call<ApiResponse<ApplicantsResponse>> getApplicants(
            @Query("meeting_id") int meetingId,
            @Query("host_id") int hostId,
            @Query("status") String status
    );

    /**
     * 6. 신청 승인/거절
     * @param request participant_id, action ("approve" or "reject"), host_id
     * @return ApiResponse<ApproveData> - participant_id, participation_status, meeting_current_participants
     */
    @POST("api/meeting/approve.php")
    Call<ApiResponse<ApproveResponse>> approveMeeting(@Body ApproveMeetingRequest request);





}

