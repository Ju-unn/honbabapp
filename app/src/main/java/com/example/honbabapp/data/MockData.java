package com.example.honbabapp.data;

import com.example.honbabapp.data.chat.ChatRoom;
import com.example.honbabapp.data.chat.Message;
import com.example.honbabapp.data.location.Location;
import com.example.honbabapp.data.meeting.Meeting;
import com.example.honbabapp.data.user.User;

import java.util.ArrayList;
import java.util.Date;

public class MockData {

    static public ArrayList<User> getUserList() {
        ArrayList<User> userList = new ArrayList<>();

        for(int i = 0; i < 50; i++) {
            userList.add(new User(i ,
                    "eamil" + i ,
                    "password" + i ,
                    "nickname" + i ,
                    "userStatus" + i ,
                    "userBirthday" + i ,
                    "남성" ,
                    "userPhoneNumer" + i ,
                    3.0,
                    0 ,
                    "서울" ,
                    true ,
                    new Date() ,
                    new Date()));
        }

        return userList;
    }


    static public ArrayList<Location> getLocationList() {
        ArrayList<Location> locationList = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            locationList.add(new Location(
                    "" + i ,
                    "가게이름" + i + "번" ,
                    "" + i ,
                    "" + i ,
                    (double)i ,
                    (double)i ,
                    (double)i
                    ));
        }

        return locationList;
    }

    static public ArrayList<Meeting> getMeetingList() {
        ArrayList<Meeting> meetingList = new ArrayList<>();
        ArrayList<User> userList = getUserList();
        ArrayList<Location> locationList = getLocationList();


        /// 1번 미팅룸에 들어갈 유저
        ArrayList<User> meetingUserList0 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            meetingUserList0.add(userList.get(i));
        }

        meetingList.add(new Meeting(0,
                "1번 미팅" ,
                meetingUserList0 ,
                2 ,
                "1번미팅" ,
                "한식" ,
                new Date() ,
                4 ,
                "20000원",
                "모집중" ,
                0 ,
                true ,
                new Date(),
                locationList.get(0)));


        /// 2번 미팅룸에 들어갈 유저
        ArrayList<User> meetingUserList1 = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            meetingUserList1.add(userList.get(i));
        }

        meetingList.add(new Meeting(1,
                "2번 미팅" ,
                meetingUserList1 ,
                1,
                "2번미팅" ,
                "중식" ,
                new Date() ,
                4 ,
                "20000원",
                "모집중" ,
                0 ,
                false ,
                new Date(),
                locationList.get(1)));

        /// 3번 미팅룸에 들어갈 유저
        ArrayList<User> meetingUserList2 = new ArrayList<>();
        for (int i = 8; i < 12; i++) {
            meetingUserList2.add(userList.get(i));
        }

        meetingList.add(new Meeting(2,
                "3번 미팅"
                , meetingUserList2 ,
                3 ,
                "3번미팅" ,
                "일식" ,
                new Date() ,
                4 ,
                "20000원",
                "모집중" ,
                0 ,
                true ,
                new Date(),
                locationList.get(2)));

        /// 4번 미팅룸에 들어갈 유저
        ArrayList<User> meetingUserList3 = new ArrayList<>();
        for (int i = 12; i < 16; i++) {
            meetingUserList3.add(userList.get(i));
        }

        meetingList.add(new Meeting(3,
                "4번 미팅" ,
                meetingUserList3 ,
                2,
                "4번미팅" ,
                "오마카세" ,
                new Date() ,
                4 ,
                "50000원",
                "모집완료" ,
                0 ,
                true ,
                new Date(),
                locationList.get(3)));

        return meetingList;
    }

    static public ArrayList<ChatRoom> getChatRoomList() {
        ArrayList<ChatRoom> chatRoomList = new ArrayList<>();

        /// 1번 채팅룸
        /// 1대1 채팅
        ArrayList<User> chatRoomUserList0 = new ArrayList<>();
        chatRoomUserList0.add(getUserList().get(0));
        chatRoomUserList0.add(getUserList().get(1));

        String chatRoom0participants = "";
        for (int i = 0; i < chatRoomUserList0.size(); i++) {
            chatRoom0participants += chatRoomUserList0.get(i).user_nickname + ",";
        }

        chatRoomList.add(new ChatRoom(
                0 ,
                chatRoomUserList0.get(0).user_id ,
                null ,
                "1:1" ,
                chatRoom0participants ,
                "1번 마지막 메세지" ,
                new Date() ,
                new Date() ,
                0 ,
                chatRoomUserList0));


        /// 2번 채팅룸
        /// 1대1 채팅
        ArrayList<User> chatRoomUserList1 = new ArrayList<>();
        chatRoomUserList1.add(getUserList().get(2));
        chatRoomUserList1.add(getUserList().get(3));

        String chatRoom1participants = "";
        for (int i = 0; i < chatRoomUserList1.size(); i++) {
            chatRoom1participants += chatRoomUserList1.get(i).user_nickname + ",";
        }

        chatRoomList.add(new ChatRoom(1 ,
                chatRoomUserList1.get(0).user_id ,
                null ,
                "1:1" ,
                chatRoom1participants ,
                "2번 마지막 메세지" ,
                new Date() ,
                new Date() ,
                0 ,
                chatRoomUserList1));


        /// 3번 채팅룸
        /// group
        ArrayList<User> chatRoomUserList2 = new ArrayList<>();
        chatRoomUserList2.add(getUserList().get(4));
        chatRoomUserList2.add(getUserList().get(5));
        chatRoomUserList2.add(getUserList().get(6));

        String chatRoom2participants = "";
        for (int i = 0; i < chatRoomUserList2.size(); i++) {
            chatRoom2participants += chatRoomUserList2.get(i).user_nickname + ",";
        }

        chatRoomList.add(new ChatRoom(2 ,
                chatRoomUserList2.get(0).user_id ,
                2,
                "group" ,
                chatRoom2participants ,
                "3번 마지막 메세지" ,
                new Date() ,
                new Date() ,
                0 ,
                chatRoomUserList2));



        return chatRoomList;
    }

    static public ArrayList<Message> getMessageList() {
        ArrayList<Message> messagesList = new ArrayList<>();

        /// 1번 채팅방
        messagesList.add(new Message(0,
                getChatRoomList().get(0).chatRoom_room_id ,
                getUserList().get(0).user_id ,
                "text" ,
                "안녕하세요!" ,
                true ,
                0 ,
                new Date()));
        messagesList.add(new Message(1,
                getChatRoomList().get(0).chatRoom_room_id ,
                getUserList().get(1).user_id ,
                "text" ,
                "네 안녕하세요!" ,
                false ,
                1 ,
                new Date()));

        /// 2번 채팅방
        messagesList.add(new Message(2,
                getChatRoomList().get(1).chatRoom_room_id ,
                getUserList().get(2).user_id ,
                "text" ,
                "내일 모임 참석 가능하세요?" ,
                true ,
                0 ,
                new Date()));

        /// 3번 채팅방
        messagesList.add(new Message(4,
                getChatRoomList().get(2).chatRoom_room_id ,
                null ,
                "system" ,
                getUserList().get(3).user_id + " 님이 참가 하였습니다" ,
                true ,
                0 ,
                new Date()));
        messagesList.add(new Message(5,
                getChatRoomList().get(2).chatRoom_room_id ,
                getUserList().get(3).user_id ,
                "text" ,
                "안녕하세요 여러분" ,
                true ,
                0 ,
                new Date()));
        messagesList.add(new Message(6,
                getChatRoomList().get(2).chatRoom_room_id ,
                getUserList().get(4).user_id ,
                "text" ,
                "안녕하세요!!" ,
                true ,
                0 ,
                new Date()));
        return messagesList;


    }

}
