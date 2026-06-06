package com.example.honbabapp.data;

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

}
