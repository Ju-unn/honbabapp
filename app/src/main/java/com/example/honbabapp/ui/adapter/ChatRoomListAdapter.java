package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.api.data.chat.ChatRoomData;
import com.example.honbabapp.databinding.RvChatListBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatRoomListAdapter extends RecyclerView.Adapter<ChatRoomListAdapter.ViewHolder> {

    // 채팅방 목록 데이터
    private List<ChatRoomData> rooms = new ArrayList<>();

    // 아이템 클릭 리스너
    public interface OnRoomClickListener {
        void onRoomClick(ChatRoomData room);
    }

    private final OnRoomClickListener listener;

    public ChatRoomListAdapter(OnRoomClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // rv_chat_list.xml → ViewBinding 클래스명: RvChatListBinding
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

    /**
     * 마지막 메시지 업데이트
     * WebSocket 으로 새 메시지 수신 시 목록의 해당 방만 갱신
     */
    public void updateLastMessage(int roomId, String message, String time) {
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).getRoom_id() == roomId) {
                rooms.get(i).setLast_message(message);
                rooms.get(i).setLast_msg_time(time);
                notifyItemChanged(i); // 해당 아이템만 다시 그림
                break;
            }
        }
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
                String profileUrl = room.getParticipants().get(0).getUser_profile_image();
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
                Date date = inputFormat.parse(timeStr);

                Calendar msgCal = Calendar.getInstance();
                msgCal.setTime(date);
                Calendar now = Calendar.getInstance();

                boolean isToday = msgCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                        && msgCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);

                SimpleDateFormat outputFormat = isToday
                        ? new SimpleDateFormat("HH:mm", Locale.KOREA)
                        : new SimpleDateFormat("MM/dd", Locale.KOREA);

                return outputFormat.format(date);
            } catch (Exception e) {
                return "";
            }
        }
    }
}