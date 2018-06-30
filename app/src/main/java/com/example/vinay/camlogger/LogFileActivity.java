package com.example.vinay.camlogger;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogFileActivity extends AppCompatActivity implements SensorEventListener {

    class FileWriter {

        public FileWriter(String fileName) throws IOException {
            this.file = getPublicPicturesDirectory(fileName);
            if (!this.file.createNewFile()) {
                Log.e("createFile", "Failed to create file: " + this.file.getPath());
            }
            this.stream = new FileOutputStream(this.file);
            this.writer = new OutputStreamWriter(this.stream);
            valid = true;
        }

        public FileWriter() {
            valid = false;
        }

        public void write(String string) throws IOException {
            if (valid) {
                writer.write(string);
            }
        }

        public void close() throws IOException {
            if (valid) {
                writer.close();
                stream.flush();
                stream.close();
            }
        }

        boolean valid = false;
        public File file;
        public FileOutputStream stream;
        public OutputStreamWriter writer;
    }

    public String[] sensors = {"Accelerometer", "Magnetometer", "Gyroscope", "Proximity",
            "Barometer", "Gravity", "Linear Acceleration", "Rotation Vector", "Step Detector",
            "Step Counter", "Significant Motion Detector", "Game Rotation Vector",
            "Geomagnetic Rotation Vector", "Orientation" , "Tilt Detector",
            "AMD", "RMD", "Gestures", "Tap", "Facing", "Tilt", "Pedometer"};

    SensorManager sensorManager;
    Sensor accelSensor;

    CameraManager cameraManager;
    CameraDevice camera;
    FileWriter accelWriter;
    int activeSensors = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_file);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);


        ToggleButton toggleAccelButton = (ToggleButton) findViewById(R.id.toggleButton2); // initiate a toggle button
        toggleAccelButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    // Toggle checked
                    String filename = "AccelerometerLog_" + getTimestamp() + ".txt";
                    try {
                        accelWriter = new FileWriter(filename);
                        activeSensors += 1;

                    }
                    catch (Exception e) {
                        Log.e("creatingWriter", "Failed to create writer: " + e.getMessage());
                    }
                    Log.d("CreatedWriter", "Created File: " + filename);
                } else {
                    // Toggle not checked
                    try {
                        accelWriter.close();
                        activeSensors -= 1;
                    }
                    catch (Exception e) {
                        Log.e("closingWriter", "Failed to close writer: " + e.getMessage());
                    }
                }
            }
        });

        ToggleButton toggleCameraButton = (ToggleButton) findViewById(R.id.toggleButton6);
        toggleCameraButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                if (isChecked) {
                    try {
                        String[] cameraDevices = cameraManager.getCameraIdList();
                        for (String camera : cameraDevices) {
                            Log.d("CamDbg", "Camera Id: " + camera);
                        }

//                        // CamCapSess:
//                        TextureView textureView = (TextureView) findViewById(R.id.textureView);
//                        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
//                            @Override
//                            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//                                surface;
//                            }
//
//                        });

                    } catch (Exception e) {
                        Log.e("CameraError", "Failed to enumerate cameras");
                    }
                }

            }
        });

        // Display Camera Feed



        Intent intent = getIntent();
        TextView textView = findViewById(R.id.textView);
        String msg;
        if (isExternalStorageWritable()) {
            msg = "Log File is Writable!";
        } else {
            msg = "Log File is Not Writable :(";
        }
        msg += "\n" + Environment.getExternalStorageState();
        textView.setText(msg);

        // Create Log File:
        try {
            FileWriter sensorWriter = new FileWriter("AvailableSensors.txt");

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


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }


    /* Gets File */
    public File getPublicPicturesDirectory(String fileName) {

        // Get Permissions:
        Log.d("createFile", "Checking File Permissions");
        if (!checkStoragePermissions(this)) {
            requestStoragePermissions(this);
            Log.d("createFile", "Permissions Requested");
        }

        // Get the directory for pictures
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        File[] files = path.listFiles();
        path = new File(path, "/CamLogger/");
        Log.d("dbg", "Files in: " + path.getPath());
        for(File file : files) {
            Log.d("dbg", "\t" + file.getPath());
        }
        if (!path.mkdirs()) {
            Log.e("ERR", "File not created: " + path);
        }
        File file = new File(path, fileName);
        return file;
    }



    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
//            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // Camera Permissions
    private static final int REQUEST_CAMERA = 1;
    private static String[] PERMISSIONS_CAMERA = {
            Manifest.permission.CAMERA
    };

    // Check Storage Permissions:
    public static boolean checkStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    // Request Storage Permissions:
    public static void requestStoragePermissions(Activity activity) {
        // We don't have permission so prompt the user
        ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
    }

    // Check Camera Permissions:
    public static boolean checkCameraPermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    // Request Camera Permissions:
    public static void requestCameraPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, PERMISSIONS_CAMERA, REQUEST_CAMERA);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d("MSG", "Write Permission has been granted!");

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("MSG", "Write Permission has not been granted");
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (activeSensors > 0) {
            try {
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        String logData = String.format("%d; ACC; %f; %f; %f; %f; %f; %f\n", event.timestamp, event.values[0], event.values[1], event.values[2], 0.f, 0.f, 0.f);
                        Log.d("Accelerometer", logData);
                        accelWriter.write(logData);
                        break;
                    case Sensor.TYPE_ROTATION_VECTOR:
                        // DO SOMETHING
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
        super.onResume();
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

}
