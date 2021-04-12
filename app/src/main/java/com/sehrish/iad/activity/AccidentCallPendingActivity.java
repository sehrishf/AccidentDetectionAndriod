package com.sehrish.iad.activity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.sehrish.iad.ConfigConstants;
import com.sehrish.iad.R;
import com.sehrish.iad.http.APIClient;
import com.sehrish.iad.http.APIInterface;
import com.sehrish.iad.http.model.User;
import com.sehrish.iad.http.model.UserFriend;
import com.sehrish.iad.service.LocationTrack;
import com.sehrish.iad.service.ShakeService;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccidentCallPendingActivity extends AppCompatActivity {

    Handler waitAndCallAccidentHandler;
    Handler waitAndAutoCloseProcessesHandler;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeService mShakeService;
    private SharedPreferences sharedPreferences;
    private APIInterface apiInterface;
    public static int loggedInUserId = 0;
    Uri notification;
    Ringtone ringtone;
    Vibrator vibrator;
    AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog);

        showCustomDialog();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sharedPreferences = getSharedPreferences("user_details", MODE_PRIVATE);
        loggedInUserId = Integer.parseInt(sharedPreferences.getString("userid", ""));

        //todo; to put it on reboot
        apiInterface = APIClient.getClient().create(APIInterface.class);
        notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        try {
            if(detectVibrate(getApplicationContext())) {
                vibrator.vibrate(ConfigConstants.TIME_TO_WAIT);
            }
            ringtone.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

        waitAndCallAccidentHandler = new Handler(Looper.getMainLooper());
        waitAndCallAccidentHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isNeedToCallForAccident();
            }
        }, ConfigConstants.TIME_TO_WAIT);

        waitAndAutoCloseProcessesHandler = new Handler(Looper.getMainLooper());
        waitAndAutoCloseProcessesHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopAllProcesses();
            }
        }, ConfigConstants.TIME_TO_CLOSE_ALERT);

    }

    public static boolean detectVibrate(Context context){
        boolean status = false;
        AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if(am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE){
            status = true;
        } else if (1 == Settings.System.getInt(context.getContentResolver(), "vibrate_when_ringing", 0))
            status = true;
        return status;
    }

    public void onClickStop(View view) {
        stopAllProcesses();
    }

    private void stopAllProcesses() {
        waitAndAutoCloseProcessesHandler.removeCallbacksAndMessages(null);
        waitAndCallAccidentHandler.removeCallbacksAndMessages(null);
        ringtone.stop();
        vibrator.cancel();
        alertDialog.dismiss();
        finish();
    }


    private void showCustomDialog() {

        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.my_dialog, viewGroup, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        alertDialog = builder.create();

        alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        alertDialog.show();

        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                stopAllProcesses();
            }
        });
    }

    private void isNeedToCallForAccident() {

        Long lastCallDateTimeInMillis = sharedPreferences.getLong("lastAccidentCallDateTime", 0);
        Long currentDateTimeInMillis = System.currentTimeMillis();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Long diffInMillis = currentDateTimeInMillis - lastCallDateTimeInMillis;

        if (lastCallDateTimeInMillis == 0 || (diffInMillis > ConfigConstants.TIME_DELAY_FOR_ACCIDENT_CALL_IN_MILLIS)) {
            editor.putLong("lastAccidentCallDateTime", currentDateTimeInMillis);
            editor.commit();
            Log.d("shake_detected", "sending accident call");
            sendAccidentCall();
        }
    }

    public void sendAccidentCall() {
        LocationTrack locationTrack = new LocationTrack(getApplicationContext());

        if (locationTrack.canGetLocation() == true) {
            double longitude = locationTrack.getLongitude();
            double latitude = locationTrack.getLatitude();
            locationTrack.sendAccidentLocation(longitude + "", latitude + "", loggedInUserId);
            sendSMSToFriends(latitude, longitude);
            Log.d("shake_detected", "sending sms lat log" + latitude + " " + longitude);
        } else {
            Log.d("shake_detected", "can not get location");
        }
    }

    private String getAddress(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            Address obj = addresses.get(0);

            String street =  obj.getThoroughfare();
            String houseNumber = obj.getSubThoroughfare();
            String postalCode = obj.getPostalCode();
            String address = street;

            if(houseNumber != null) address = address + " " + houseNumber;
            if(postalCode != null) address = address + " " + postalCode;

            Log.v("IGA", "Address" + address);
            return address;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void sendSMSToFriends(double lat, double lon) {

        if (lat != 0.0 || lat != 0) {
            User user = new User(loggedInUserId, "", "", "", "");
            Call<List<UserFriend>> call = apiInterface.getUserFriend(user);
            Log.d("shake_detected", "sending sms lat log" + lat + " " + lon);


            call.enqueue(new Callback<List<UserFriend>>() {
                @Override
                public void onResponse(Call<List<UserFriend>> call, Response<List<UserFriend>> response) {

                    List<UserFriend> responseFriendList = response.body();
                    for (UserFriend userFriend : responseFriendList) {

                        Intent intent = new Intent(getApplicationContext(), AccidentCallPendingActivity.class);
                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

                        SmsManager smsManager = SmsManager.getDefault();
                        String toPersonMobileNo = userFriend.getMobileno();
                        String accidentAddress = getAddress(lat, lon);

                        if(accidentAddress != null) {
                            accidentAddress = " in " + accidentAddress;
                        }

                        String smsText = "Hello\n" +
                                "Your friend had an accident " + accidentAddress + "\n" +
                               // "Please see in map\n" +
                               "www." + ConfigConstants.baseUrl + "/" + "user/friend-accident?lat=" + lat + "&lon=" + lon + "&uid=" + loggedInUserId;

                        //smsManager.sendTextMessage(toPersonMobileNo, "01793477312", "Test", pendingIntent, null);

                        smsManager.sendTextMessage(toPersonMobileNo, null, smsText, null, null);


                        Log.d("shake_detected", "sent sms lat log" + lat + " " + lon);
                    }
                }

                @Override
                public void onFailure(Call<List<UserFriend>> call, Throwable t) {

                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}