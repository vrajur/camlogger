package com.example.vinay.camlogger;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created by vinay on 7/1/18.
 */

public class FileWriter {

    public FileWriter(String fileName, Activity activity) throws IOException {
        this.file = getPublicPicturesDirectory(fileName, activity);
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


    /* Gets File */
    public File getPublicPicturesDirectory(String fileName, Activity activity) {

        // Get Permissions:
        Log.d("createFile", "Checking File Permissions");
        if (!checkStoragePermissions(activity)) {
            requestStoragePermissions(activity);
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
    public static final int REQUEST_EXTERNAL_STORAGE = 1;
    public static String[] PERMISSIONS_STORAGE = {
//            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
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

//    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case FileWriter.REQUEST_EXTERNAL_STORAGE: {
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

}