package com.sehrish.iad.http.model;

import com.google.gson.annotations.SerializedName;

public class AccidentData {

        @SerializedName("id")
        int locationId;

        public int getLocationId() {
        return locationId;
    }

        public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

        public String getLat() {
        return lat;
    }

        public void setLat(String lat) {
        this.lat = lat;
    }

        public String getLon() {
        return lon;
    }

        public void setLon(String lon) {
        this.lon = lon;
    }

        public int getUserId() {
        return userId;
    }

        public void setUserId(int userId) {
        this.userId = userId;
    }

    public AccidentData(int locationId, String lat, String lon, int userId) {
        this.locationId = locationId;
        this.lat = lat;
        this.lon = lon;
        this.userId = userId;
    }

        @SerializedName("lat")
        String lat;
        @SerializedName("lon")
        String lon;
        @SerializedName("userId")
        int userId;
}
