package com.example.honbabapp.api;

import com.example.honbabapp.api.data.chat.ChatRoomData;
import com.example.honbabapp.api.data.request.chat.CreateRoomRequest;
import com.example.honbabapp.api.data.request.chat.InviteUserRequest;
import com.example.honbabapp.api.data.request.chat.KickMemberRequest;
import com.example.honbabapp.api.data.request.chat.LeaveRoomRequest;
import com.example.honbabapp.api.data.request.chat.MarkReadRequest;
import com.example.honbabapp.api.data.request.meeting.DeleteMeetingRequest;
import com.example.honbabapp.api.data.request.meeting.UpdateMeetingRequest;
import com.example.honbabapp.api.data.request.user.ChangePasswordRequest;
import com.example.honbabapp.api.data.request.user.CheckCurrentPasswordRequest;
import com.example.honbabapp.api.data.request.user.DeleteAccountRequest;
import com.example.honbabapp.api.data.request.user.UpdateLocationRequest;
import com.example.honbabapp.api.data.request.user.UpdateProfileRequest;
import com.example.honbabapp.api.data.response.MyMeetingItem;
import com.example.honbabapp.api.data.response.chat.ChatPhotosResponse;
import com.example.honbabapp.api.data.response.chat.CreateRoomResponse;
import com.example.honbabapp.api.data.response.chat.KickMemberResponse;
import com.example.honbabapp.api.data.response.chat.LeaveRoomResponse;
import com.example.honbabapp.api.data.response.chat.MessageListResponse;
import com.example.honbabapp.api.data.response.user.UserProfileResponse;
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
import com.example.honbabapp.api.data.request.meeting.ApproveMeetingRequest;
import com.example.honbabapp.api.data.request.meeting.JoinMeetingRequest;
import com.example.honbabapp.api.data.request.LoginRequest;
import com.example.honbabapp.api.data.request.meeting.MeetingCreateRequest;
import com.example.honbabapp.api.data.request.RegisterRequest;
import com.example.honbabapp.api.data.request.SendVerificationRequest;
import com.example.honbabapp.api.data.request.VerifyCodeRequest;
import com.example.honbabapp.api.data.response.ApiResponse;

import java.util.List;

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
     * 2. 모임 목록 조회 (지역 필터링) - Homefragment
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
     * 2-2. 모임 목록 조회 (위치 기반 필터 — MapFragment용)
     * @param sigungu  시/군/구 (선택)
     * @param categories 카테고리 (선택)
     * @param status   모임 상태
     * @param page     페이지 번호
     * @param limit    페이지당 개수
     * @param lat      중심 위도 (Bbox + Haversine 활성화)
     * @param lng      중심 경도
     * @param radius   반경 km
     * @param orderBy  "distance" → 거리순, null → 기본 정렬
     *
     * Retrofit에서 Double(박스타입) 파라미터가 null이면
     * 쿼리스트링에서 자동으로 생략됨 → 하위 호환 유지
     */
    // MapFragment용 (신규)
    @GET("api/meeting/list.php")
    Call<ApiResponse<MeetingListResponse>> getMeetingList(
            @Query("sigungu") String sigungu,
            @Query("categories") String categories,
            @Query("status") String status,
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("lat") Double lat,
            @Query("lng") Double lng,
            @Query("radius") Double radius,
            @Query("order_by") String orderBy,
             @Query("min_price") Integer minPrice,
            @Query("max_price") Integer maxPrice
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

    /**
     * 가게 선택 시 네이버 이미지 검색으로 가져온 사진을 S3에 업로드
     * - 네이버 이미지 URL → OkHttp 다운로드 → 임시 파일 → 이 API로 업로드
     * - 업로드 성공 시 반환된 meeting_image_url을 MeetingCreateRequest에 담아서 create.php로 전송
     *
     * @param image MultipartBody.Part (파라미터명: "meeting_image")
     * @return ApiResponse<ImageUploadData> - image_url (S3 URL)
     */
    @Multipart
    @POST("api/meeting/upload_meeting_image.php")
    Call<ApiResponse<ImageUploadData>> uploadMeetingImage(
            @Part MultipartBody.Part image
    );

    @POST("api/meeting/delete.php")
    Call<ApiResponse<Void>> deleteMeeting(@Body DeleteMeetingRequest request);

    @POST("api/meeting/update.php")
    Call<ApiResponse<Void>> updateMeeting(@Body UpdateMeetingRequest request);

    // ==================== User API (Profile)====================

    /**
     * 1. 프로필 조회
     */
    @GET("api/user/profile.php")
    Call<ApiResponse<UserProfileResponse>> getUserProfile(@Query("user_id") int userId);

    /**
     * 2. 프로필 수정 (닉네임, 상태메시지, 프로필이미지 선택적)
     */
    @POST("api/user/update_profile.php")
    Call<ApiResponse<Void>> updateProfile(@Body UpdateProfileRequest request);

    /**
     * 3. 비밀번호 변경
     */
    @POST("api/user/change_password.php")
    Call<ApiResponse<Void>> changePassword(@Body ChangePasswordRequest request);

    /**
     * 3-1. 현재 비밀번호 확인 (비밀번호 변경 전 실시간 검증용)
     */
    @POST("api/user/check_current_password.php")
    Call<ApiResponse<Void>> checkCurrentPassword(@Body CheckCurrentPasswordRequest request);

    /**
     * 4. 회원 탈퇴
     */
    @POST("api/user/delete_account.php")
    Call<ApiResponse<Void>> deleteAccount(@Body DeleteAccountRequest request);


    /**
     * 5. 내 모임 목록 (host + participant 전체)
     */
    @GET("api/user/my_meetings.php")
    Call<ApiResponse<List<MyMeetingItem>>> getMyMeetings(@Query("user_id") int userId);

    /**
     * 6. 내 위치 업데이트
     */
    @POST("api/user/update_location.php")
    Call<ApiResponse<Void>> updateLocation(@Body UpdateLocationRequest request);


    // ==================== Chat API ====================

    /**
     * 1. 내 채팅방 목록 조회
     */
    @GET("api/chat/rooms.php")
    Call<ApiResponse<List<ChatRoomData>>> getChatRooms(@Query("user_id") int userId);

    /**
     * 2. 채팅방 생성
     */
    @POST("api/chat/create_room.php")
    Call<ApiResponse<CreateRoomResponse>> createRoom(@Body CreateRoomRequest request);

    /**
     * 3. 메세지 히스토리 조회
     */
    @GET("api/chat/messages.php")
    Call<ApiResponse<MessageListResponse>> getMessages(
            @Query("room_id") int roomId,
            @Query("user_id") int userId,
            @Query("page") int page,
            @Query("limit") int limit
    );

    /**
     * 4. 읽음처리
     */
    @POST("api/chat/mark_read.php")
    Call<ApiResponse<Void>> markRead(@Body MarkReadRequest request);

    /**
     * 5. 멤버 초대 (그룹방 전용)
     */
    @POST("api/chat/invite.php")
    Call<ApiResponse<Void>> inviteUser(@Body InviteUserRequest request);


    /**
     * 6. 채팅방 나가기
     *
     * 응답 data 의 9개 필드:
     *   - room_id, remaining
     *   - is_host, is_host_transferred, new_host_id
     *   - room_deleted, meeting_deleted
     *   - penalty_applied, new_manner_score
     */
    @POST("api/chat/leave.php")
    Call<ApiResponse<LeaveRoomResponse>> leaveRoom(@Body LeaveRoomRequest request);

    /**
     * 7. 채팅방 사진 목록 조회 (페이지네이션)
     *
     * @param roomId 채팅방 ID
     * @param userId 요청자 user_id (멤버 권한 체크용)
     * @param page   페이지 번호 (1부터)
     * @param limit  페이지당 개수 (보통 10 또는 30)
     */
    @GET("api/chat/photos.php")
    Call<ApiResponse<ChatPhotosResponse>> getChatPhotos (
            @Query("room_id") int roomId,
            @Query("user_id") int userId,
            @Query("page")    int page,
            @Query("limit")   int limit
    );

    /**
     * 8. 채팅방 멤버 강퇴 (방장 전용)
     *
     * 응답 data:
     *   - room_id                      : 강퇴 일어난 방
     *   - target_user_id               : 강퇴된 유저
     *   - remaining                    : 남은 멤버 수
     *   - meeting_current_participants : 강퇴 후 현재 모임 인원
     *
     * 서버에서 자동 처리:
     *   - meeting_participants 상태 '강퇴' 로 변경
     *   - chat_room_members 에서 삭제
     *   - meetings.meeting_current_participants -= 1, 모집완료 → 모집중 복귀
     *   - 시스템 메시지 "OO님이 강퇴되었습니다." INSERT
     *   - WebSocket 'member_left' (reason='kicked') 방 전체 broadcast
     *   - WebSocket 'kicked' 대상자에게 직접 emit
     */
    @POST("api/chat/kick.php")
    Call<ApiResponse<KickMemberResponse>> kickMember(@Body KickMemberRequest request);

    /**
     * 채팅 이미지 업로드
     * - 갤러리/카메라에서 가져온 이미지를 S3 chat/ 폴더로 업로드
     * - 서버는 chat_messages INSERT 하지 않음 (S3 업로드 + URL 반환만)
     * - 클라이언트가 받은 URL 을 WebSocket 'message' 이벤트로 보내야 DB 저장됨
     *   (텍스트 메시지와 동일 흐름 → 단일 진실의 원천)
     *
     * @param image   MultipartBody.Part (파라미터명: "chat_image")
     * @param roomId  채팅방 ID (멤버 권한 검증용)
     * @param userId  업로드하는 user_id
     * @return ApiResponse<ImageUploadData> - image_url (S3 URL)
     */
    @Multipart
    @POST("api/chat/upload_chat_image.php")
    Call<ApiResponse<ImageUploadData>> uploadChatImage(
            @Part MultipartBody.Part image,
            @Part("room_id") RequestBody roomId,
            @Part("user_id") RequestBody userId
    );


}

