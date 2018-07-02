package com.example.vinay.camlogger;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

/**
 * Created by vinay on 7/1/18.
 */

public class PermissionsChecker {

    // Storage Permissions
    public static final int REQUEST_EXTERNAL_STORAGE = 0;
    public static final int REQUEST_LOCATION = 1;
    public static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    public static String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    // Check Storage Permissions:
    public static boolean checkStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    // Check Location Permissions:
    public static boolean checkLocationPermissions(Activity activity) {
        int permissionCoarse = PackageManager.PERMISSION_GRANTED;
//        int permissionCoarse = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION);
        int permissionFine = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);
        return (permissionCoarse == PackageManager.PERMISSION_GRANTED) && (permissionFine == PackageManager.PERMISSION_GRANTED);
    }

    // Request Storage Permissions:
    public static void requestStoragePermissions(Activity activity) {
        // We don't have permission so prompt the user
        ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
    }

    // Request Location Permissions:
    public static void requestLocationPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, PERMISSIONS_LOCATION, REQUEST_LOCATION);
    }

    //    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // permission was granted, yay! Do the
                        // contacts-related task you need to do.
                        Log.d("PermissionsStorage", "External Storage Write Permissions Granted");
                    } else {
                        // permission denied, boo! Disable the
                        // functionality that depends on this permission.
                        Log.d("PermissionsStorage", "External Storage Write Permissions NOT Granted");
                    }

                    if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        Log.d("PermissionsStorage", "External Storage Read Permissions Granted");
                    } else {
                        Log.d("PermissionsStorage", "External Storage Read Permissions NOT Granted");
                    }
                }
                return;
            }

            case REQUEST_LOCATION: {
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d("PermissionsLocation", "Fine Location Permission Granted!");
                    } else {
                        Log.d("PermissionsLocation", "Fine Location Permission NOT Granted!");
                    }

                    if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        Log.d("Permissions Location", "Coarse Location Permission Granted");
                    } else {
                        Log.d("Permissions Location", "Coarse Location Permission NOT Granted");
                    }
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    /* Checks if external storage is available for read and write */
    static public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }




}
