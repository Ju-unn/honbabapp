package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.api.data.chat.ChatMemberData;
import com.example.honbabapp.api.data.chat.ChatRoomData;
import com.example.honbabapp.databinding.RvChatListBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ChatRoomListAdapter extends RecyclerView.Adapter<ChatRoomListAdapter.ViewHolder> {

    // 채팅방 목록 데이터
    private List<ChatRoomData> rooms = new ArrayList<>();

    // 아이템 클릭 리스너
    public interface OnRoomClickListener {
        void onRoomClick(ChatRoomData room);
    }

    private final OnRoomClickListener listener;
    private final int myUserId;

    public ChatRoomListAdapter(OnRoomClickListener listener, int myUserId) {
        this.listener = listener;
        this.myUserId = myUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvChatListBinding binding = RvChatListBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(rooms.get(position));
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    // ─────────────────────────────────────────
    // 데이터 갱신 메서드
    // ─────────────────────────────────────────

    /**
     * 채팅방 목록 전체 교체
     * ChatFragment 에서 API 응답 받으면 호출
     */
    public void setRooms(List<ChatRoomData> newRooms) {
        this.rooms = newRooms;
        notifyDataSetChanged();
    }

    // ─────────────────────────────────────────
    // ViewHolder
    // ─────────────────────────────────────────
    class ViewHolder extends RecyclerView.ViewHolder {

        private final RvChatListBinding binding;

        ViewHolder(RvChatListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatRoomData room) {

            // 방 이름
            binding.textRoomName.setText(room.getRoom_name());

            // 마지막 메시지
            String lastMsg = room.getLast_message();
            binding.textLastMessage.setText(lastMsg != null ? lastMsg : "");

            // 시간 포맷 (오늘이면 "HH:mm", 아니면 "MM/dd")
            binding.textTime.setText(formatTime(room.getLast_msg_time()));

            // 읽지 않은 메시지 수 뱃지
            int unread = room.getUnread_count();
            if (unread > 0) {
                binding.badgeUnread.setVisibility(android.view.View.VISIBLE);
                binding.badgeUnread.setText(unread > 999 ? "999+" : String.valueOf(unread));
            } else {
                binding.badgeUnread.setVisibility(android.view.View.GONE);
            }

            // 프로필 이미지
            if (room.getParticipants() != null && !room.getParticipants().isEmpty()) {
                String profileUrl = null;

                if (room.isDirect()) {
                    // 내 userId와 다른 멤버를 직접 찾아야 함
                    for (ChatMemberData member : room.getParticipants()) {
                        if (member.getUser_id() != myUserId) {
                            profileUrl = member.getUser_profile_image();
                            break;
                        }
                    }
                } else {
                    // 그룹 채팅방: room_image 우선, 없으면 0번째 유저 이미지
                    String groupImage = room.getRoom_image();
                    if (groupImage != null && !groupImage.isEmpty()) {
                        profileUrl = groupImage;
                    } else {
                        profileUrl = room.getParticipants().get(0).getUser_profile_image();
                    }
                }

                // profileUrl이 null이면 Glide의 error()가 ic_profile 기본 이미지 표시
                Glide.with(binding.getRoot().getContext())
                        .load(profileUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(binding.imgRoomProfile);
            }

            // 클릭 이벤트
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onRoomClick(room);
            });
        }

        /**
         * "2026-04-17 10:00:00" → 오늘이면 "10:00", 아니면 "04/17"
         */
        private String formatTime(String timeStr) {
            if (timeStr == null || timeStr.isEmpty()) return "";
            try {
                SimpleDateFormat inputFormat =
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = inputFormat.parse(timeStr);

                Calendar msgCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"));
                msgCal.setTime(date);
                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"));

                boolean isToday = msgCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                        && msgCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);

                SimpleDateFormat outputFormat = isToday
                        ? new SimpleDateFormat("HH:mm", Locale.KOREA)
                        : new SimpleDateFormat("MM/dd", Locale.KOREA);
                outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

                return outputFormat.format(date);
            } catch (Exception e) {
                return "";
            }
        }
    }
}