package com.sehrish.iad.http.model;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    int userId;
    @SerializedName("mobileno")
    String mobileno;
    @SerializedName("name")
    String name;
    @SerializedName("email")
    String email;
    @SerializedName("password")
    String password;
    public void setUserId(int userId) {
        this.userId = userId;
    }
    public int getUserId() {
        return userId;
    }
    public User(int userId, String mobileno, String name, String email,String password) {
        this.userId = userId;
        this.mobileno = mobileno;
        this.name = name;
        this.email = email;
        this.password=password;
    }
}
