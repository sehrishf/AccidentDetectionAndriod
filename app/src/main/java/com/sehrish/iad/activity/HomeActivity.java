package com.sehrish.iad.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.sehrish.iad.ConfigConstants;
import com.sehrish.iad.MyListAdapter;
import com.sehrish.iad.R;
import com.sehrish.iad.http.APIClient;
import com.sehrish.iad.http.APIInterface;
import com.sehrish.iad.http.model.User;
import com.sehrish.iad.http.model.UserFriend;
import com.sehrish.iad.service.LocationTrack;
import com.sehrish.iad.service.ShakeService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    //The following are used for the shake detection
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeService mShakeService;
    private SharedPreferences sharedPreferences;
    private APIInterface apiInterface;
    private ListView list;
    public MyListAdapter listAdapter;
    public static int loggedInUserId = 0;
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    public List<String> listViewPhoneNumbers = new ArrayList<>();
    public List<Integer> listViewPhoneNumberIds = new ArrayList<>();
    private LocationTrack locationTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_main);

        //never delete this line
        locationTrack = new LocationTrack(HomeActivity.this);

        sharedPreferences = getSharedPreferences("user_details", MODE_PRIVATE);
        loggedInUserId = Integer.parseInt(sharedPreferences.getString("userid", ""));
        apiInterface = APIClient.getClient().create(APIInterface.class);

        listAdapter = new MyListAdapter(this, listViewPhoneNumbers, listViewPhoneNumberIds);
        list = (ListView) findViewById(R.id.list);
        list.setAdapter(listAdapter);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeService = new ShakeService();

        detectShake();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                int REQUEST_CODE = 101;
                Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                myIntent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(myIntent, REQUEST_CODE);
            }
        }

        if(checkAndRequestPermissions() == false) {
            Toast.makeText(this, "Without permission you are not allowed to use this app", Toast.LENGTH_LONG).show();
            finish();
        }

        startService(new Intent(this, LocationTrack.class));

        Intent serviceIntent = new Intent(this, ShakeService.class);
        serviceIntent.putExtra("inputExtra", "Your accident detection background services");
        ContextCompat.startForegroundService(this, serviceIntent);

    }

    private  boolean checkAndRequestPermissions() {

        int fineLocation = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        int sendSms = ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS);
        int internet = ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET);
        int vibration = ContextCompat.checkSelfPermission(this, android.Manifest.permission.VIBRATE);

        List<String> listPermissionsNeeded = new ArrayList<>();

        if (fineLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (sendSms != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.SEND_SMS);
        }
        if (internet != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.INTERNET);
        }
        if (vibration != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.VIBRATE);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Do the stuff that requires permission...
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    //Show permission explanation dialog...
                } else {
                    //Never ask again selected, or device policy prohibits the app from having that permission.
                    //So, disable that feature, or fall back to another situation...
                }
            }
        }
    }

    private void detectShake() {

        mShakeService.setOnShakeListener(new ShakeService.OnShakeListener() {
            @Override
            public void onShake(int count) {

                Long lastCallDateTimeInMillis = sharedPreferences.getLong("lastAccidentCallDateTime", 0);
                Long currentDateTimeInMillis = System.currentTimeMillis();
                Long diffInMilliSeconds = currentDateTimeInMillis - lastCallDateTimeInMillis;

                if(lastCallDateTimeInMillis == 0 || (diffInMilliSeconds > ConfigConstants.TIME_DELAY_FOR_ACCIDENT_CALL_IN_MILLIS)) {

                    Intent dialogIntent = new Intent(getBaseContext(), AccidentCallPendingActivity.class);
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(dialogIntent);

                } else {
                    Log.d("shake_detected", "Not sending accident call for delay");
                }
            }
        });
    }

    public void goToSignInActivity() {
        Intent intent = new Intent(this, SigninActivity.class);
        startActivity(intent);
    }

    public void saveFriendDetail(View view) {

        String userId = sharedPreferences.getString("userid", "");
        EditText editPhone = (EditText) findViewById(R.id.friend_number_input);

        String phoneText = editPhone.getText().toString();
        UserFriend userFriend = new UserFriend(userId, phoneText, 0);
        Call<UserFriend> callSaveFriend = apiInterface.saveUserFriend(userFriend);

        callSaveFriend.enqueue(new Callback<UserFriend>() {
            @Override
            public void onResponse(Call<UserFriend> call, Response<UserFriend> response) {
                UserFriend respondLocationData = response.body();
                fillFriendsList();
            }

            @Override
            public void onFailure(Call<UserFriend> call, Throwable t) {
                Toast.makeText(HomeActivity.this,
                        "Check Internet connection or Server", Toast.LENGTH_LONG).show();
                call.cancel();
            }
        });
    }

    public void fillFriendsList() {
        
        User user = new User(loggedInUserId, "", "", "", "");
        Call<List<UserFriend>> call = apiInterface.getUserFriend(user);
        call.enqueue(new Callback<List<UserFriend>>() {
            @Override
            public void onResponse(Call<List<UserFriend>> call, Response<List<UserFriend>> response) {
                List<UserFriend> responseFriendList = response.body();
                for (UserFriend userFriend : responseFriendList) {
                    if(listViewPhoneNumberIds.contains(userFriend.getId()) == false) {
                        listViewPhoneNumbers.add(userFriend.getMobileno());
                        listViewPhoneNumberIds.add(userFriend.getId());
                    }
                }
                listAdapter.notifyDataSetChanged();
            }
            @Override
            public void onFailure(Call<List<UserFriend>> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Check internet connection or Server might be in maintanence", Toast.LENGTH_LONG).show();
                call.cancel();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        fillFriendsList();
        mSensorManager.registerListener(mShakeService, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {

            this.locationTrack.stopListener();

            Intent location = new Intent(this, LocationTrack.class);
            this.stopService(location);

            Intent shake = new Intent(this, ShakeService.class);
            this.stopService(shake);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.commit();
            goToSignInActivity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}