package com.example.honbabapp.repository;

import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.ImageUploadData;
import com.example.honbabapp.api.data.chat.ChatRoomData;
import com.example.honbabapp.api.data.request.chat.CreateRoomRequest;
import com.example.honbabapp.api.data.request.chat.InviteUserRequest;
import com.example.honbabapp.api.data.request.chat.KickMemberRequest;
import com.example.honbabapp.api.data.request.chat.LeaveRoomRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.chat.ChatPhotosResponse;
import com.example.honbabapp.api.data.response.chat.ChatRoomMembersResponse;
import com.example.honbabapp.api.data.response.chat.CreateRoomResponse;
import com.example.honbabapp.api.data.response.chat.KickMemberResponse;
import com.example.honbabapp.api.data.response.chat.LeaveRoomResponse;
import com.example.honbabapp.api.data.response.chat.MessageListResponse;

import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Callback;
import retrofit2.http.Part;

public class ChatRepository {

    // ─────────────────────────────────────────
    // 싱글톤
    // ─────────────────────────────────────────
    private ChatRepository() {
        apiService = RetrofitClient.getClient().create(ApiService.class);
    }

    private static class LazyHolder {
        private static final ChatRepository INSTANCE = new ChatRepository();
    }

    public static ChatRepository getInstance() {
        return LazyHolder.INSTANCE;
    }

    // ─────────────────────────────────────────
    // 내부 상태
    // ─────────────────────────────────────────
    private final ApiService apiService;


    // ─────────────────────────────────────────
    // 채팅방 목록 조회
    // ChatFragment.onResume() 에서 호출
    // ─────────────────────────────────────────

    /**
     * 내가 속한 채팅방 목록을 가져온다
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param chatRoomDb    성공 시 List<ChatRoomData> 반환
     */
    public void getChatRooms(int userId, Callback<ApiResponse<List<ChatRoomData>>> chatRoomDb) {
        apiService.getChatRooms().enqueue(chatRoomDb);
    }

    // ─────────────────────────────────────────
    // 메시지 목록 조회 (페이징)
    // ChatRoomActivity.onCreate() 에서 호출
    // ─────────────────────────────────────────

    /**
     * 채팅방의 메시지 히스토리를 페이지 단위로 가져온다
     * 한 페이지에 30개씩 가져온다
     *
     * @param roomId 채팅방 ID
     * @param page   페이지 번호 (1부터 시작)
     * @param chatRoomDb     성공 시 MessageListResponse 반환
     */
    public void getMessages(int roomId , int userId , int page, Callback<ApiResponse<MessageListResponse>> chatRoomDb) {
        apiService.getMessages(roomId , page , 30).enqueue(chatRoomDb);
    }

    // ─────────────────────────────────────────
    // 채팅방 생성
    // ─────────────────────────────────────────

    /**
     * 1:1 채팅방을 생성한다
     * 이미 채팅방이 있으면 서버가 기존 room_id 를 반환한다 (already_exists: true)
     *
     * @param myUserId     내 사용자 ID
     * @param targetUserId 상대방 사용자 ID
     * @param roomName     방 이름 (보통 상대방 닉네임)
     * @param chatRoomDb           성공 시 CreateRoomResponse 반환 (room_id 포함)
     */
    public void createDirectRoom(int myUserId , int targetUserId , String roomName ,
                                 Callback<ApiResponse<CreateRoomResponse>> chatRoomDb) {
        CreateRoomRequest request = new CreateRoomRequest(targetUserId , roomName);
        apiService.createRoom(request).enqueue(chatRoomDb);
    }

    /**
     * 그룹 채팅방을 생성한다
     * 모임 승인 시 자동으로 호출된다
     *
     * @param myUserId   방 생성자(호스트) ID
     * @param memberIds  초대할 멤버 ID 목록
     * @param meetingId  연결된 모임 ID
     * @param roomName   방 이름 (모임 제목)
     * @param chatRoomDb         성공 시 CreateRoomResponse 반환 (room_id 포함)
     */
    public void createGroupRoom(int myUserId , List<Integer> memberIds , int meetingId,
                                String roomName , Callback<ApiResponse<CreateRoomResponse>> chatRoomDb) {
        CreateRoomRequest request = new CreateRoomRequest(memberIds , meetingId , roomName);
        apiService.createRoom(request).enqueue(chatRoomDb);
    }

    // ─────────────────────────────────────────
    // 읽음 처리
    // ChatRoomActivity 진입 시 호출
    // ─────────────────────────────────────────

    // ─────────────────────────────────────────
    // 멤버 초대 (그룹 채팅 전용)
    // ─────────────────────────────────────────

    /**
     * 그룹 채팅방에 새 멤버를 초대한다
     */
    public void inviteUser(int roomId , int inviterId , int targetUserId ,
                           Callback<ApiResponse<Void>> chatRoomDb) {
        InviteUserRequest request = new InviteUserRequest(roomId , targetUserId);
        apiService.inviteUser(request).enqueue(chatRoomDb);
    }

    // ─────────────────────────────────────────
    // 채팅방 나가기
    // ─────────────────────────────────────────


    /**
     * 채팅방에서 나간다
     *
     * 응답에 포함되는 정보:
     *  - 호스트 위임 발생 여부 (is_host_transferred / new_host_id)
     *  - 채팅방/모임 삭제 여부
     *  - 적용된 패널티 점수 (penalty_applied)
     *  - 변경된 매너점수 (new_manner_score)
     */
    public void leaveRoom(int roomId, int userId, Callback<ApiResponse<LeaveRoomResponse>> chatRoomDb) {
        LeaveRoomRequest request = new LeaveRoomRequest(roomId);
        apiService.leaveRoom(request).enqueue(chatRoomDb);
    }

    // ─────────────────────────────────────────
    // 채팅방 멤버(참가자) 조회 — chat_room_members 기준
    // ChatRoomDetailActivity 에서 direct/group 방 공통으로 호출
    // ─────────────────────────────────────────

    /**
     * 채팅방의 현재 멤버 목록을 가져온다 (chat_room_members 기준)
     *
     * @param roomId     채팅방 ID
     * @param chatRoomDb 성공 시 ChatRoomMembersResponse (owner_id, participants) 반환
     */
    public void getChatRoomMembers(int roomId,
                                   Callback<ApiResponse<ChatRoomMembersResponse>> chatRoomDb) {
        apiService.getChatRoomMembers(roomId).enqueue(chatRoomDb);
    }

    // ─────────────────────────────────────────
    // 강퇴 (그룹 방 방장 전용)
    // ChatRoomDetailActivity 의 참가자 행 3-dot 메뉴 → 바텀시트 → 확인 다이얼로그 후 호출
    // ─────────────────────────────────────────

    /**
     * 방장이 특정 멤버를 강퇴한다
     * @param roomId       채팅방 ID
     * @param hostId       방장 user_id (= 현재 로그인 사용자)
     * @param targetUserId 강퇴할 대상 user_id
     * @param chatRoomDb   완료 콜백 (KickMemberResponse 반환)
     */
    public void kickMember(int roomId, int hostId, int targetUserId,
                           Callback<ApiResponse<KickMemberResponse>> chatRoomDb) {
        KickMemberRequest request = new KickMemberRequest(roomId, targetUserId);
        apiService.kickMember(request).enqueue(chatRoomDb);
    }

    // ─────────────────────────────────────────
    // 채팅방 사진 조회
    // ChatRoomDetailActivity / ChatRoomPhotosActivity 에서 호출
    // ─────────────────────────────────────────

    /**
     * 채팅방의 사진 목록을 페이지 단위로 가져온다
     *
     * @param roomId     채팅방 ID
     * @param userId     요청자 user_id (멤버 권한 체크용)
     * @param page       페이지 번호 (1부터)
     * @param limit      페이지당 개수 (미리보기 10, 그리드 30)
     * @param chatRoomDb 성공 시 ChatPhotosResponse 반환
     */
    public void getChatPhotos(int roomId, int userId, int page, int limit,
                              Callback<ApiResponse<ChatPhotosResponse>> chatRoomDb) {
        apiService.getChatPhotos(roomId, page, limit).enqueue(chatRoomDb);
    }

    // ─────────────────────────────────────────
    // 채팅 이미지 업로드 (S3 업로드만, DB INSERT 안 함)
    // ChatRoomActivity 에서 갤러리/카메라 결과 후 호출
    // ─────────────────────────────────────────

    /**
     * 채팅 이미지를 S3 에 업로드한다
     *
     * @param roomId     채팅방 ID
     * @param userId     업로드하는 user_id (멤버 권한 검증용)
     * @param imagePart  MultipartBody.Part (파라미터명: "chat_image")
     * @param chatRoomDb 성공 시 ImageUploadData (image_url) 반환
     */
    public void uploadChatImage(int roomId, int userId,
                                MultipartBody.Part imagePart,
                                Callback<ApiResponse<ImageUploadData>> chatRoomDb) {
        RequestBody roomIdBody = RequestBody.create(
                MediaType.parse("text/plain"), String.valueOf(roomId));
        apiService.uploadChatImage(imagePart, roomIdBody).enqueue(chatRoomDb);
    }


}