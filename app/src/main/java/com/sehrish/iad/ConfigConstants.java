package com.sehrish.iad;

public class ConfigConstants {

    //millisecond * second * minute, 1000 millisecond = 1 second
    public static final int TIME_DELAY_FOR_ACCIDENT_CALL_IN_MILLIS = 1000 * 20;

    //10 Seconds
    public static final int TIME_TO_WAIT = 1000 * 30;

    public static final int TIME_TO_CLOSE_ALERT = TIME_TO_WAIT + 3000;

    public static String baseUrl = "http://192.168.0.88:8085/";
}
