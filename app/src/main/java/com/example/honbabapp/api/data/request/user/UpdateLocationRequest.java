package com.example.honbabapp.api.data.request.user;

public class UpdateLocationRequest {
    private int user_id;
    private String user_location;

    public UpdateLocationRequest(int user_id, String user_location) {
        this.user_id = user_id;
        this.user_location = user_location;
    }
}
