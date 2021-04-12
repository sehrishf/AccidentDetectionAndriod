package com.sehrish.iad.http.model;

import com.google.gson.annotations.SerializedName;

public class UserFriend {
    @SerializedName("id")
    int id;
    @SerializedName("mobileno")
    String mobileno;
    @SerializedName("userid")
    String userid;

    public void setId(int id) { this.id = id; }
    public int getId() {
        return id;
    }
    public void setUserId(String userId) { this.userid = userId; }
    public String getUserId() {
        return userid;
    }

    public String getMobileno() {
        return mobileno;
    }
    public UserFriend(String userId, String mobileno, int id) {
        this.userid = userId;
        this.mobileno = mobileno;
        this.id = id; }
}
