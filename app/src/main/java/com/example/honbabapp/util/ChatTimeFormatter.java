package com.example.honbabapp.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 채팅 화면 전용 시간 포맷터.
 *
 * 서버 UTC 문자열("yyyy-MM-dd HH:mm:ss")을 Asia/Seoul 기준 라벨로 변환한다.
 * 채팅방 목록과 메시지 말풍선의 표시 규칙이 다르므로 메서드를 분리해 각각의 동작을 보존한다.
 *
 * SimpleDateFormat 은 스레드 안전하지 않으므로 원본과 동일하게 호출마다 지역 인스턴스를 만든다.
 */
public final class ChatTimeFormatter {

    private static final String SERVER_TIME_ZONE = "UTC";
    private static final String DISPLAY_TIME_ZONE = "Asia/Seoul";
    private static final String SERVER_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private ChatTimeFormatter() {
        // 유틸 클래스 — 인스턴스화 금지
    }

    /**
     * 채팅방 목록용 라벨 (ChatRoomListAdapter.formatTime 대체).
     *  - 오늘  : "오전 9:05" / "오후 3:42"
     *  - 어제  : "어제"
     *  - 그 외 : "04/17"
     *  - null·빈 문자열·파싱 실패 : ""
     */
    public static String formatRoomListTime(String serverTimeText) {
        if (serverTimeText == null || serverTimeText.isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(SERVER_PATTERN, Locale.KOREA);
            inputFormat.setTimeZone(TimeZone.getTimeZone(SERVER_TIME_ZONE));
            Date parsedDate = inputFormat.parse(serverTimeText);
            if (parsedDate == null) {
                return "";
            }

            Calendar messageCalendar = Calendar.getInstance(TimeZone.getTimeZone(DISPLAY_TIME_ZONE));
            messageCalendar.setTime(parsedDate);
            Calendar nowCalendar = Calendar.getInstance(TimeZone.getTimeZone(DISPLAY_TIME_ZONE));

            boolean isToday = messageCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR)
                    && messageCalendar.get(Calendar.DAY_OF_YEAR) == nowCalendar.get(Calendar.DAY_OF_YEAR);
            if (isToday) {
                SimpleDateFormat todayFormat = new SimpleDateFormat("a h:mm", Locale.KOREAN);
                todayFormat.setTimeZone(TimeZone.getTimeZone(DISPLAY_TIME_ZONE));
                return todayFormat.format(parsedDate);
            }

            Calendar yesterdayCalendar = Calendar.getInstance(TimeZone.getTimeZone(DISPLAY_TIME_ZONE));
            yesterdayCalendar.add(Calendar.DAY_OF_YEAR, -1);
            boolean isYesterday = messageCalendar.get(Calendar.YEAR) == yesterdayCalendar.get(Calendar.YEAR)
                    && messageCalendar.get(Calendar.DAY_OF_YEAR) == yesterdayCalendar.get(Calendar.DAY_OF_YEAR);
            if (isYesterday) {
                return "어제";
            }

            SimpleDateFormat otherDayFormat = new SimpleDateFormat("MM/dd", Locale.KOREA);
            otherDayFormat.setTimeZone(TimeZone.getTimeZone(DISPLAY_TIME_ZONE));
            return otherDayFormat.format(parsedDate);

        } catch (Exception exception) {
            return "";
        }
    }

    /**
     * 메시지 말풍선용 라벨 (MessageAdapter.formatTime 대체).
     * 항상 시:분("오전 9:05")만 표시한다.
     *  - 길이 16 미만·null : ""
     *  - 파싱 실패 : 원본의 11~16 구간("HH:mm")을 그대로 반환 (원본 동작 보존)
     */
    public static String formatMessageTime(String createdAt) {
        if (createdAt == null || createdAt.length() < 16) {
            return "";
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(SERVER_PATTERN, Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone(SERVER_TIME_ZONE));
            Date parsedDate = inputFormat.parse(createdAt);

            SimpleDateFormat outputFormat = new SimpleDateFormat("a h:mm", Locale.KOREAN);
            outputFormat.setTimeZone(TimeZone.getTimeZone(DISPLAY_TIME_ZONE));
            return outputFormat.format(parsedDate);
        } catch (Exception exception) {
            return createdAt.substring(11, 16);
        }
    }
}
