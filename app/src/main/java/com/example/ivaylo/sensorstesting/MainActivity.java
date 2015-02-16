package com.example.ivaylo.sensorstesting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class MainActivity extends ActionBarActivity implements SensorEventListener, View.OnClickListener {

    private TextView xAxis, yAxis, zAxis;
    private Sensor accelerometerSensor;
    private SensorManager sensorManager;
    private Button startBtn, stopBtn, sendReportBtn;
    private TextView sensorType;

    private TextView latLoc, lonLoc, speed;

    private boolean externalStorageWriteable = false;
    private String outFile = "accelerometer_data.csv";
    private File externalStorageDirectory;
    private FileWriter fileWriter;

    private BroadcastReceiver receiver;

    private float linear_acceleration[] = new float[] {0f, 0f, 0f};
    private float gravity[] = new float[] {0f, 0f, 0f};

    private Intent gpsIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //get external storage state & directory
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            externalStorageWriteable = true;
        }

        externalStorageDirectory = Environment.getExternalStorageDirectory();

        //init buttons
        initViews();

        //configure accelerometer
        initAccelerometer();
    }

    private void startGPSservice() {

        gpsIntent = new Intent(this, LocationService.class);
        gpsIntent.setAction(LocationService.ACTION_ASYNC);
        startService(gpsIntent);

    }

    private void initViews() {

        //text fields
        xAxis = (TextView) findViewById(R.id.xAxis);
        yAxis = (TextView) findViewById(R.id.yAxis);
        zAxis = (TextView) findViewById(R.id.zAxis);
        sensorType = (TextView) findViewById(R.id.sensorType);

        //text fields
        latLoc = (TextView) findViewById(R.id.locLat);
        lonLoc = (TextView) findViewById(R.id.locLon);
        speed = (TextView) findViewById(R.id.speed);

        //buttons
        startBtn = (Button) findViewById(R.id.startBtn);
        startBtn.setOnClickListener(this);
        stopBtn = (Button) findViewById(R.id.stopBtn);
        stopBtn.setOnClickListener(this);
        sendReportBtn = (Button) findViewById(R.id.sendReportBtn);
        sendReportBtn.setOnClickListener(this);
    }

    private void initAccelerometer() {

        sensorManager = (SensorManager) getSystemService(this.SENSOR_SERVICE);

        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if(accelerometerSensor != null) {
            //ok we have an accelerometer

            Log.d("SENSORS_TESTING", accelerometerSensor.toString());

            sensorType.setText("" + accelerometerSensor.toString());

        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(externalStorageWriteable)
        {
            //create writer
            try {
                fileWriter = new FileWriter(externalStorageDirectory + "/" + outFile, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        receiveGPSBroadcast();


    }

    private void receiveGPSBroadcast() {

        //UNCOMMENT below lines to receive GPS data that is sent from the GPS background service

        IntentFilter intentFilter = new IntentFilter(LocationService.BROADCAST_ACTION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(LocationService.BROADCAST_ACTION) &&
                        intent.hasExtra("Latitude") && intent.hasExtra("Longitude") && intent.hasExtra("Speed") ) {
                    double lat = intent.getDoubleExtra("Latitude", 0);
                    double lon = intent.getDoubleExtra("Longitude", 0);
                    int speed_kmh = intent.getIntExtra("Speed", 0);

                    latLoc.setText("" + lat);
                    lonLoc.setText("" + lon);
                    speed.setText("" + speed_kmh);

                    /*Toast.makeText(context,
                            "Lat: " + lat + " Lon: " + lon + " Provider: " + provider,
                            Toast.LENGTH_SHORT).show();*/
                }
            }
        };

        registerReceiver(receiver, intentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);

        if(fileWriter != null)
        {
            try {
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(receiver != null)
        {
            unregisterReceiver(receiver);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            //Log.d(Constants.TAG, "Event values: " + event.values.toString());

            xAxis.setText("" + x);
            yAxis.setText("" + y);
            zAxis.setText("" + z);

            Date now = new Date();
            //Log.d( Constants.TAG, "now: " + new Timestamp(now.getTime()) );

            /*try {
                fileWriter.append( x + "," + y + "," + z + "," + new Timestamp(now.getTime()) + "\n" ) ;
            } catch (IOException e) {
                e.printStackTrace();
            }*/

            final float alpha = 0.8f;

            // Isolate the force of gravity with the low-pass filter.
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            // Remove the gravity contribution with the high-pass filter.
            linear_acceleration[0] = event.values[0] - gravity[0];
            linear_acceleration[1] = event.values[1] - gravity[1];
            linear_acceleration[2] = event.values[2] - gravity[2];


            /*Log.d(Constants.TAG, "\n" +
                            "Acceleration vector: " + sqrt(x * x + y * y + z * z) + "\n" +
                            "Linear acceleration: " + sqrt(
                                linear_acceleration[0] * linear_acceleration[0] +
                                linear_acceleration[1] * linear_acceleration[1] +
                                linear_acceleration[2] * linear_acceleration[2]
                            )
                );*/
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.startBtn:
                //register listener
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
                //start GPS process
                startGPSservice();

                Toast.makeText(this, getString(R.string.recording_started), Toast.LENGTH_LONG).show();

                break;

            case R.id.stopBtn:

                if(accelerometerSensor != null) {
                    //unregister accelerometer listener
                    sensorManager.unregisterListener(this);
                }

                if(gpsIntent != null) {
                    stopGpsService();
                }

                Toast.makeText(this, getString(R.string.recording_stopped), Toast.LENGTH_LONG).show();

                break;

            case R.id.sendReportBtn:

                //stop GPS
                stopGpsService();

                //TODO stop accelerometer recording when saving to file is implemented

                //check network state -> allow upload only through WIFI
                ConnectivityManager cm =
                        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null &&
                        activeNetwork.isConnectedOrConnecting();

                boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;

                if ( isConnected && isWiFi ){
                    //upload GPS data
                    try {
                        String result = new UploadFile().execute(Constants.uploadReportURL).get();

                        if(result.equals(Constants.responseSuccessString)) {
                            Toast.makeText(this, getString(R.string.send_report_success), Toast.LENGTH_LONG).show();

                            //truncate gps data file

                        }
                        else {
                            Toast.makeText(this, getString(R.string.send_report_fail), Toast.LENGTH_LONG).show();
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, getString(R.string.send_report_requirements), Toast.LENGTH_LONG).show();
                }



                break;
        }
    }

    private void stopGpsService() {
        if (gpsIntent != null) {
            stopService(gpsIntent);
        }
    }
}
