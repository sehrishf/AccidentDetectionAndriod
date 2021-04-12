package com.sehrish.iad.http;

import com.sehrish.iad.http.model.AccidentData;
import com.sehrish.iad.http.model.LocationData;
import com.sehrish.iad.http.model.RemoveContact;
import com.sehrish.iad.http.model.User;
import com.sehrish.iad.http.model.UserFriend;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface APIInterface {

    @POST("api/location/save")
    Call<LocationData> sendCurrentLocationData(@Body LocationData user);

    @POST("api/location/save-accident")
    Call<AccidentData> sendAccidentLocationData(@Body AccidentData user);

    @POST("api/user/save")
    Call<User> saveUserData(@Body User user);

    @POST("api/user/get-user-by-mobile")
    Call<User> getUserData(@Body User user);

    @POST("api/user/friendsinfo")
    Call<UserFriend> saveUserFriend(@Body UserFriend userFriend);

    @POST("api/user/friendslist")
    Call<List<UserFriend>> getUserFriend(@Body User user);

    @POST("api/user/removefriend")
    Call<List<UserFriend>> removeFriend(@Body RemoveContact removeContact);

}

