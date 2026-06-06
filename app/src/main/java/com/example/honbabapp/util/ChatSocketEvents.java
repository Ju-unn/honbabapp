package com.example.honbabapp.util;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 채팅 소켓 이벤트 payload 안전 파싱 헬퍼.
 *
 * Socket.IO 리스너의 args[0] 캐스팅, room_id 가드, 필드 추출을 한곳으로 모은다.
 * 모든 메서드는 예외를 던지지 않고 기본값을 반환한다(원본의 silent-ignore 동작 보존).
 */
public final class ChatSocketEvents {

    private static final int NO_ROOM = Integer.MIN_VALUE;

    private ChatSocketEvents() {
    }

    /**
     * 소켓 콜백 인자 배열의 첫 요소를 JSONObject 로 안전 변환한다.
     *
     * @return JSONObject 또는 (비어있거나 타입 불일치면) null
     */
    public static JSONObject asJsonObject(Object[] socketArguments) {
        if (socketArguments == null || socketArguments.length == 0) {
            return null;
        }
        if (socketArguments[0] instanceof JSONObject) {
            return (JSONObject) socketArguments[0];
        }
        return null;
    }

    /**
     * payload 의 room_id 가 현재 열려있는 방과 일치하는지 검사한다.
     * payload 가 null 이거나 room_id 키가 없으면 false (→ 리스너에서 조기 return).
     */
    public static boolean isForRoom(JSONObject payload, int currentRoomId) {
        if (payload == null) {
            return false;
        }
        return payload.optInt("room_id", NO_ROOM) == currentRoomId;
    }

    /** 문자열 필드 추출 (없으면 빈 문자열). */
    public static String getString(JSONObject payload, String key) {
        if (payload == null) {
            return "";
        }
        return payload.optString(key, "");
    }

    /** 정수 필드 추출 (없으면 defaultValue). */
    public static int getInt(JSONObject payload, String key, int defaultValue) {
        if (payload == null) {
            return defaultValue;
        }
        return payload.optInt(key, defaultValue);
    }

    /** 정수 배열 필드 추출 (없으면 null — 호출부에서 length 체크). */
    public static JSONArray getIntArray(JSONObject payload, String key) {
        if (payload == null) {
            return null;
        }
        return payload.optJSONArray(key);
    }
}
