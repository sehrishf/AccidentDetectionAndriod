package com.sehrish.iad.service;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.sehrish.iad.http.APIClient;
import com.sehrish.iad.http.APIInterface;
import com.sehrish.iad.http.model.AccidentData;
import com.sehrish.iad.http.model.LocationData;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationTrack extends Service implements LocationListener {

    private Context mContext = null;
    boolean checkGPS = false;

    boolean checkNetwork = false;
    boolean canGetLocation = false;

    Location loc;
    double latitude;
    double longitude;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 5;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 15;
    protected LocationManager locationManager;
    private APIInterface apiInterface;
    private double mLastLat = 0;
    private double mLastLon = 0;

    public LocationTrack() {

    }

    public LocationTrack(Context mContext) {
        this.mContext = mContext;
        getLocation();
        apiInterface = APIClient.getClient().create(APIInterface.class);
    }

    private Location getLocation() {

        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            // get GPS status
            checkGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // get network provider status
            checkNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!checkGPS && !checkNetwork) {
                Toast.makeText(mContext, "No Service Provider is available", Toast.LENGTH_SHORT).show();
            } else {
                this.canGetLocation = true;

                // if GPS Enabled get lat/long using GPS Services
                if (checkGPS) {

                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        //  Toast.makeText(mContext, "No permission", Toast.LENGTH_SHORT).show();

                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                    }
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    if (locationManager != null) {
                        loc = locationManager
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (loc != null) {
                            latitude = loc.getLatitude();
                            longitude = loc.getLongitude();
                        }
                    }
                }

                /*if (checkNetwork) {


                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                    }
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                    if (locationManager != null) {
                        loc = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                    }

                    if (loc != null) {
                        latitude = loc.getLatitude();
                        longitude = loc.getLongitude();
                    }
                }*/

            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return loc;
    }

    public double getLongitude() {
        if (loc != null) {
            longitude = loc.getLongitude();
        }
        return longitude;
    }

    public double getLatitude() {
        if (loc != null) {
            latitude = loc.getLatitude();
        }
        return latitude;
    }

    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setTitle("GPS is not Enabled!");
        alertDialog.setMessage("Do you want to turn on GPS?");

        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }


    public void stopListener() {
        if (locationManager != null) {

            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.removeUpdates(LocationTrack.this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {

        Location currentLocation = getLocation();

        if(currentLocation.getLatitude() != mLastLat || currentLocation.getLongitude() != mLastLon) {

            mLastLat = currentLocation.getLatitude();
            mLastLon = currentLocation.getLongitude();

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //Toast.makeText(mContext, "Continious lat: " + currentLocation.getLatitude() + " long: " + currentLocation.getLongitude(), Toast.LENGTH_SHORT).show();
                    sendLocation(currentLocation.getLongitude() + "", currentLocation.getLatitude() + "");
                }
            });
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public void sendLocation(String longitude, String latitude) {
        //Toast.makeText(mContext, "Sending data to db long"+longitude+" latt "+ latitude, Toast.LENGTH_LONG).show();

        SharedPreferences sharedPreferences = mContext.getSharedPreferences("user_details", MODE_PRIVATE);
        int loggedInUserId = Integer.parseInt(sharedPreferences.getString("userid", ""));

        LocationData locationData = new LocationData(0, latitude, longitude, loggedInUserId);
        Call<LocationData> call1 = apiInterface.sendCurrentLocationData(locationData);

        call1.enqueue(new Callback<LocationData>() {
            @Override
            public void onResponse(Call<LocationData> call, Response<LocationData> response) {

                LocationData respondLocationData = response.body();
                //Toast.makeText(mContext, "location saved with id: " + respondLocationData.getLocationId(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<LocationData> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Check internet connection or Server might be in maintanence", Toast.LENGTH_SHORT).show();
                call.cancel();
            }
        });

    }

    public void sendAccidentLocation(String longitude, String latitude, int userId) {

        //Toast.makeText(mContext, "Sending data to db long"+longitude+" latt "+ latitude, Toast.LENGTH_LONG).show();
        AccidentData locationData = new AccidentData(0, latitude, longitude, userId);
        Call<AccidentData> call1 = apiInterface.sendAccidentLocationData(locationData);

        call1.enqueue(new Callback<AccidentData>() {
            @Override
            public void onResponse(Call<AccidentData> call, Response<AccidentData> response) {
                AccidentData respondLocationData = response.body();
            }

            @Override
            public void onFailure(Call<AccidentData> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Check internet connection or Server might be in maintanence", Toast.LENGTH_SHORT).show();
                call.cancel();
            }
        });

    }
}

