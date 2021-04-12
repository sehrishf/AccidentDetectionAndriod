package com.sehrish.iad.http.model;

import com.google.gson.annotations.SerializedName;

public class RemoveContact {

    @SerializedName("userId")
    private long userId;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getFriendId() {
        return friendId;
    }

    public void setFriendId(long friendId) {
        this.friendId = friendId;
    }

    @SerializedName("friendId")
    private long friendId;
}
