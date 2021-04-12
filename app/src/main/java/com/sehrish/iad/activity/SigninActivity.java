package com.sehrish.iad.activity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.PusherEvent;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import com.sehrish.iad.ConfigConstants;
import com.sehrish.iad.R;
import com.sehrish.iad.http.APIClient;
import com.sehrish.iad.http.APIInterface;
import com.sehrish.iad.http.model.User;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SigninActivity extends AppCompatActivity {

    private APIInterface apiInterface;
    SharedPreferences sharedPreferences;
    int loggedInUserId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.signin_activity);
        checkLoginValidity();
    }

    private void checkLoginValidity() {
        apiInterface = APIClient.getClient().create(APIInterface.class);
        sharedPreferences = getSharedPreferences("user_details", MODE_PRIVATE);

        if (sharedPreferences.contains("userphone") && sharedPreferences.contains("password")) {

            loggedInUserId = Integer.parseInt(sharedPreferences.getString("userid", ""));
            String phoneText = sharedPreferences.getString("userphone", "");
            String passText = sharedPreferences.getString("password", "");
            String ipText = sharedPreferences.getString("ipText", "192.168.0.1");

            signInCallToServer(phoneText, passText, ipText);
        }
    }

    public void goToHomeActivity() {
        initializeAccidentNotification();
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    public void goToHomeActivity(View v) {
        initializeAccidentNotification();
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    public void goToSignUpActivity(View v) {
        Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);
    }

    private void signInCallToServer(String phoneText, String passText, String ipText) {

        ConfigConstants.baseUrl = "http://" + ipText+":8085";
        apiInterface = APIClient.getClient().create(APIInterface.class);

        Toast.makeText(getBaseContext(), "Checking User...", Toast.LENGTH_SHORT).show();
        User userData = new User(0, phoneText, "", "", passText);
        Call<User> call1 = apiInterface.getUserData(userData);

        call1.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {

                User respondData = response.body();
                if (respondData != null && respondData.getUserId() > 0) {

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("userphone", phoneText);
                    editor.putString("password", passText);
                    editor.putString("userid", respondData.getUserId() + "");
                    editor.putString("ipText", ipText + "");
                    editor.commit();

                    loggedInUserId = respondData.getUserId();
                    goToHomeActivity();
                } else {
                    Toast.makeText(getBaseContext(), "User is invalid:", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(getBaseContext(), "Check internet connection or IP address", Toast.LENGTH_LONG).show();
                call.cancel();
            }
        });
    }

    public void signIn(View view) {
        EditText editPhone = (EditText) findViewById(R.id.editPhone);
        EditText editPass = (EditText) findViewById(R.id.editPass);
        EditText editIp = (EditText) findViewById(R.id.editip);

        String phoneText = editPhone.getText().toString();
        String passText = editPass.getText().toString();
        String ipText = editIp.getText().toString();

        if(ipText.trim().isEmpty() == false) {
            signInCallToServer(phoneText, passText, ipText);
        } else {
            Toast.makeText(getBaseContext(), "Give your server IP", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeAccidentNotification() {

        PusherOptions options = new PusherOptions();
        options.setCluster("eu");

        Pusher pusher = new Pusher("a9e662200a7506256f55", options);
        pusher.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange change) {
                Log.i("Pusher", "State changed from " + change.getPreviousState() +
                        " to " + change.getCurrentState());
            }

            @Override
            public void onError(String message, String code, Exception e) {
                Log.i("Pusher", "There was a problem connecting! " +
                        "\ncode: " + code +
                        "\nmessage: " + message +
                        "\nException: " + e
                );
            }
        }, ConnectionState.ALL);

        Channel channel = pusher.subscribe("my-channel");
        channel.bind("my-event-" + loggedInUserId, new SubscriptionEventListener() {
            @Override
            public void onEvent(PusherEvent event) {
                Log.d("sehrish_pusher", event.getData());
                createNotificationChannel(event.getData().toString());
            }
        });
    }

    private void createNotificationChannel(String message) {

        CharSequence name = "accident";
        String CHANNEL_ID = "Accidents";
        String[] messagesParts = message.split("#");
        String title = messagesParts[0];
        String text = title;

        if(messagesParts.length > 2) {
            double lat = Double.parseDouble(messagesParts[1]);

            String strLng = messagesParts[2].replace("\"", "");
            double lng = Double.parseDouble(strLng);
            String address = getAddress(lat, lng);
            if(address != null) {
                text = address;
            }
        }

        Intent intent = new Intent(this, HomeActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(text);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(1, notificationBuilder.build());//integer id, to distinguish between different notifications.
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

    @Override
    public void onResume() {
        super.onResume();
        checkLoginValidity();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}