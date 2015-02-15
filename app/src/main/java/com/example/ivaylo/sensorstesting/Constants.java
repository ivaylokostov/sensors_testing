package com.example.ivaylo.sensorstesting;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;

/**
 * Created by Ivaylo on 2/10/2015.
 */
public class Constants {

    public final static String TAG = "SENSORS_TESTING";

    public static File externalStorageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    public final static String outFile = "gps_data.csv";
    public static final String uploadReportURL = "http://apps.noveporter.com/android_testing_app/save_file.php";
    public static final String responseSuccessString = "Success";
}
