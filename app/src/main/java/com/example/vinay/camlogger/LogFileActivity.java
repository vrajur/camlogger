package com.example.vinay.camlogger;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Environment;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;



public class LogFileActivity extends AppCompatActivity implements SensorEventListener,
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public String[] sensorStrings = {"Accelerometer", "Magnetometer", "Gyroscope", "Proximity",
            "Barometer", "Gravity", "Linear Acceleration", "Rotation Vector", "Step Detector",
            "Step Counter", "Significant Motion Detector", "Game Rotation Vector",
            "Geomagnetic Rotation Vector", "Orientation" , "Tilt Detector",
            "AMD", "RMD", "Gestures", "Tap", "Facing", "Tilt", "Pedometer"};

    enum LOGGEDSENSORS {
        ACCELEROMETER,
        GYROSCOPE,
        GPS }
    int numSensors = 3; // Should be same size as number of LOGGEDSENSORS elements

    SensorManager sensorManager;
    HashMap<LOGGEDSENSORS, Sensor> sensors = new HashMap<LOGGEDSENSORS, Sensor>();
    HashMap<LOGGEDSENSORS, SensorEventListener> sensorListeners = new HashMap<LOGGEDSENSORS, SensorEventListener>();
    HashMap<LOGGEDSENSORS, FileWriter> fileWriters = new HashMap<LOGGEDSENSORS, FileWriter>();
    int activeSensors = 0;

    boolean requestingLocationUpdates = false;
    LocationManager locationManager;

//    FusedLocationProviderClient fusedLocationProviderClient;
//    LocationRequest mLocationRequest;
//    LocationCallback mLocationCallback;
//
//    GoogleApiClient mGoogleApiClient;

    private void initializeSensors() {
        try {
            sensors.put(LOGGEDSENSORS.ACCELEROMETER, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
            sensors.put(LOGGEDSENSORS.GYROSCOPE, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
            Log.d("Sensor", "Created sensor objects");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeLocationServices() {
        // Check Permissions:
        if (!PermissionsChecker.checkLocationPermissions(LogFileActivity.this)) {
            PermissionsChecker.requestLocationPermissions(LogFileActivity.this);
        }
//        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.d("LocationProvider", "Fused Location Provider Client object created");
    }

    private void createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_file);

        if (!isGooglePlayServicesAvailable()) {
            finish();
        }
        createLocationRequest();
//        mGoogleApiClient = new GoogleApiClient.Builder(this)
//                .addApi(LocationServices.API)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .build();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensors.size() < numSensors) { initializeSensors(); }
        initializeLocationServices();

        ToggleButton toggleAccelButton = (ToggleButton) findViewById(R.id.toggleButton2); // initiate a toggle button
        toggleAccelButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    // Toggle checked
                    String filename = "AccelerometerLog_" + getTimestamp() + ".txt";
                    try {
                        fileWriters.put(LOGGEDSENSORS.ACCELEROMETER, new FileWriter(filename, LogFileActivity.this));
                        activeSensors += 1;
                        Log.d("CreatedWriter", "Created File: " + filename);
                    }
                    catch (Exception e) {
                        Log.e("creatingWriter", "Failed to create writer: " + e.getMessage());
                    }
                } else {
                    // Toggle not checked
                    try {
                        fileWriters.get(LOGGEDSENSORS.ACCELEROMETER).close();
                        fileWriters.remove(LOGGEDSENSORS.ACCELEROMETER);
                        activeSensors -= 1;
                        if (!fileWriters.containsKey(LOGGEDSENSORS.ACCELEROMETER)) {
                            Log.d("WriterClosed", "Closed Accelerometer Log File");
                        }
                    }
                    catch (Exception e) {
                        Log.e("closingWriter", "Failed to close writer: " + e.getMessage());
                    }
                }
            }
        });

        ToggleButton toggleGyroButton = (ToggleButton) findViewById(R.id.toggleButton5);
        toggleGyroButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    String filename = "GyroscopeLog_" + getTimestamp() + ".txt";
                    try {
                        fileWriters.put(LOGGEDSENSORS.GYROSCOPE, new FileWriter(filename, LogFileActivity.this));
                        activeSensors += 1;
                        Log.d("CreatedWriter", "Created File: " + filename);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        fileWriters.get(LOGGEDSENSORS.GYROSCOPE).close();
                        fileWriters.remove(LOGGEDSENSORS.GYROSCOPE);
                        activeSensors -= 1;
                        if (fileWriters.containsKey(LOGGEDSENSORS.GYROSCOPE)) {
                            Log.d("WriterClosed", "Closed Gyroscope Log File");
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        ToggleButton toggleGpsButton = (ToggleButton) findViewById(R.id.toggleButton4);
        toggleGpsButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    String filename = "GpsLog_" + getTimestamp() + ".txt";
                    try {
                        if (PermissionsChecker.checkLocationPermissions(LogFileActivity.this)) {
                            fileWriters.put(LOGGEDSENSORS.GPS, new FileWriter(filename, LogFileActivity.this));
                            try {
                                startLocationUpdates();
                                Log.d("GPS", "Subscribed to Location Updates");
                            } catch (SecurityException e) {
                                e.printStackTrace();
                            }
                            activeSensors += 1;
                            requestingLocationUpdates = true;
                            Log.d("CreatedWriter", "Created File: " + filename);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        fileWriters.get(LOGGEDSENSORS.GPS).close();
                        fileWriters.remove(LOGGEDSENSORS.GPS);
                        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
                        activeSensors -= 1;
                        requestingLocationUpdates = false;
                        if (!fileWriters.containsKey(LOGGEDSENSORS.GPS)) {
                            Log.d("WriterClosed", "Closed GPS Log File");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

//        mLocationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
//
//                Log.d("Location", "Inside onLocationResult callback");
////                if (locationResult != null) {
////                    for (Location location : locationResult.getLocations()) {
////                        // TODO Write to file
////                        Log.d("Location", location.toString());
////                    }
////                }
//            }
//        };





        Intent intent = getIntent();
        TextView textView = findViewById(R.id.textView);
        String msg;
        if (PermissionsChecker.isExternalStorageWritable()) {
            msg = "Log File is Writable!";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        } else {
            msg = "Log File is Not Writable :(";
        }
        msg += "\n" + Environment.getExternalStorageState();
        textView.setText(msg);

        // Create Log File:
        try {
            FileWriter sensorWriter = new FileWriter("AvailableSensors.txt", LogFileActivity.this);

            // Get List of Available Sensors:
            List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

            for (int ii = 0; ii < deviceSensors.size(); ii++) {
                Sensor sensor = deviceSensors.get(ii);
                Log.d("SensorList", sensor.getName());

                // Write to Log File
                sensorWriter.write(sensor.getName() + "\n");
            }

            sensorWriter.close();

        } catch (Exception e) {
            textView.setText(msg + "\nError: " + e.getMessage());
            Log.e("ERR", e.getMessage());
        }
    }

    ////////////////////////

    public String getTimestamp() {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
        Date date = new Date(Calendar.getInstance().getTimeInMillis());
        Date currentTime = Calendar.getInstance().getTime();
        return dateFormat.format(date);
    }

    ///////////////////////

    private void startLocationUpdates() {
        Log.d("StartLocationUpdates", "Starting Location Updates");

        LocationResult locationResult = new LocationResult(){
            @Override
            public void gotLocation(Location location){
                //Got the location!
            }
        };
        MyLocation myLocation = new MyLocation();
        myLocation.getLocation(this, locationResult);
        // Get GPS Updates:
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, LogFileActivity.this);


//        createLocationRequest();
//        LocationSettingsRequest.Builder locationRequestBuilder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
//        SettingsClient client = LocationServices.getSettingsClient(this);
//        Task<LocationSettingsResponse> task = client.checkLocationSettings(locationRequestBuilder.build())
//                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
//                    @Override
//                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
//                        try {
//                            Log.i("onSuccess", "All location settings are satisfied.");
//                            fusedLocationProviderClient.requestLocationUpdates(mLocationRequest,
//                                    mLocationCallback, Looper.myLooper());
//                        } catch (SecurityException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                })
//                .addOnFailureListener(this, new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        int statusCode = ((ApiException) e).getStatusCode();
//                        switch (statusCode) {
//                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
//                                Log.i("FailureListener", "Location settings are not satisfied. Attempting to upgrade " +
//                                        "location settings ");
//                                try {
//                                    // Show the dialog by calling startResolutionForResult(), and check the
//                                    // result in onActivityResult().
//                                    ResolvableApiException rae = (ResolvableApiException) e;
//                                    rae.startResolutionForResult(LogFileActivity.this, 0x1);
//                                } catch (IntentSender.SendIntentException sie) {
//                                    Log.i("FailureListener", "PendingIntent unable to execute request.");
//                                }
//                                break;
//                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
//                                String errorMessage = "Location settings are inadequate, and cannot be " +
//                                        "fixed here. Fix in Settings.";
//                                Log.e("FailureListener", errorMessage);
//                                Toast.makeText(LogFileActivity.this, errorMessage, Toast.LENGTH_LONG).show();
//                                requestingLocationUpdates = false;
//                        }
//                    }
//                });
//
//        // Create LocationSettingsRequest object using location request
//        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
//        builder.addLocationRequest(mLocationRequest);
//        LocationSettingsRequest locationSettingsRequest = builder.build();
//
//        // Check whether location settings are satisfied
//        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
//        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
//        settingsClient.checkLocationSettings(locationSettingsRequest);
//
//        mLocationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
//                // do work here
//                Log.d("Location", "Inside onLocationResult callback");
//                Log.d("Location", "Location Results: " + locationResult.toString());
//                Toast.makeText(getApplicationContext(), "GPS Result Received", Toast.LENGTH_SHORT).show();
//                onLocationChanged(locationResult.getLastLocation());
//            }
//        };
//
//        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
//        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback,
//                null);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("Location", "Inside onLocationChanged callback");

    }





    ///////////////////////

    @Override
    public void onSensorChanged(SensorEvent event) {
//        Log.d("sensorevents", "SensorEvent Recieved");
        if (activeSensors > 0) {
            try {
                String logData;
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        if (fileWriters.containsKey(LOGGEDSENSORS.ACCELEROMETER)) {
                            logData = String.format("%d; ACC; %f; %f; %f; %f; %f; %f\n", event.timestamp, event.values[0], event.values[1], event.values[2], 0.f, 0.f, 0.f);
                            Log.d("Accelerometer", logData);
                            fileWriters.get(LOGGEDSENSORS.ACCELEROMETER).write(logData);
                        }
                        break;
                    case Sensor.TYPE_GYROSCOPE:
                        if (fileWriters.containsKey(LOGGEDSENSORS.GYROSCOPE)) {
                            logData = String.format("%d; GYR; %f; %f; %f; %f; %f; %f\n", event.timestamp, event.values[0], event.values[1], event.values[2], 0.f, 0.f, 0.f);
                            Log.d("Gyroscope", logData);
                            fileWriters.get(LOGGEDSENSORS.GYROSCOPE).write(logData);
                        }
                        break;
                }
            } catch (Exception e) {
                Log.e("SensorLogging", "Error Writing to Sensor Log: " + e.getMessage());
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onResume() {
        if (sensors.size() < numSensors) { initializeSensors(); }

        try {
            sensorManager.registerListener(this, sensors.get(LOGGEDSENSORS.ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(this, sensors.get(LOGGEDSENSORS.GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
            //requestLocationUpdates

        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
//        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }


    ////////////////////////////////////////////
//    private boolean isGooglePlayServicesAvailable() {
//        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
//        if (ConnectionResult.SUCCESS == status) {
//            Log.d("GooglePlayServices", "Google Play Services Available");
//            return true;
//        } else {
//            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
//            return false;
//        }
//    }
//
//    @Override
//    public void onConnected(Bundle bundle) {
//        Log.d("OnConnected", "onConnected - isConnected ...............: " + mGoogleApiClient.isConnected());
//        startLocationUpdates();
//    }
//
//    @Override
//    public void onConnectionSuspended(int i) {
//
//    }
//
//    @Override
//    public void onConnectionFailed(ConnectionResult connectionResult) {
//        Log.d("OnConnectionFailed", "Connection failed: " + connectionResult.toString());
//    }
//
//    protected void startLocationUpdatesTest() throws SecurityException{
//        PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
//                mGoogleApiClient, mLocationRequest, this);
//        Log.d("StartingUpdates", "Location update started ..............: ");
//    }
//
//    private void stopLocationUpdatesTest() {
//        if (!requestingLocationUpdates) {
//            return;
//        }
//        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
//                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task) {
//                        requestingLocationUpdates = false;
//                    }
//                });
//    }
}
