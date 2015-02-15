package com.example.ivaylo.sensorstesting;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

public class LocationService extends Service
{
    public static final String ACTION_ASYNC = "LOCATION_LISTENER_SERVICE";
    public static final String BROADCAST_ACTION = "LOCATION_LISTENER_RESULT";

    private Looper serviceLooper;

    private ServiceHandler serviceHandler;
    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            long endTime = System.currentTimeMillis() + 5*1000;
            while (System.currentTimeMillis() < endTime) {
                synchronized (this) {
                    try {
                        wait(endTime - System.currentTimeMillis());
                    } catch (Exception e) {
                    }
                }
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }

    private static final int TWO_MINUTES = 1000 * 60 * 2;
    public LocationManager locationManager;
    public MyLocationListener listener;
    public Location previousBestLocation = null;

    Intent intent;

    public LocationService() {}

    @Override
    public void onCreate()
    {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

        intent = new Intent(BROADCAST_ACTION);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        /*Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);*/


        if ( intent != null
             && intent.getAction() != null
             && ACTION_ASYNC.equals(intent.getAction()) ) {

                Log.d(Constants.TAG, "ACTION_ASYNC: " + ACTION_ASYNC +  " intent.getAction(): " + intent.getAction());
                runListener();
        }

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    public void runListener() {

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10 * 1000L, 0L, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5 * 1000L, 0L, listener);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    /*@Override
    protected void onHandleIntent(Intent intent) {

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10 * 1000L, 0L, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10 * 1000L, 0L, listener);

    }*/

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }



    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        Log.v(Constants.TAG, "Stopping GPS service...");
        if(locationManager != null)
        {
            locationManager.removeUpdates(listener);
            this.stopSelf();
        }
    }

    public class MyLocationListener implements LocationListener
    {

        private FileWriter fileWriter;
        private File externalStorageDirectory = Constants.externalStorageDirectory;
        private String outFile = Constants.outFile;
        private boolean externalStorageWritable = false;

        private float speed = 0f;
        private Location lastLocation = null;

        public void onLocationChanged(final Location loc)
        {

            Log.i(Constants.TAG, "Location changed");
            if(isBetterLocation(loc, previousBestLocation)) {

                if (lastLocation == null)
                {
                    //just save data
                    lastLocation = loc;
                } else {

                    if(loc.getSpeed() == 0.0)
                    {
                        speed = (float) ( distance(loc, lastLocation) / ( (loc.getTime() - lastLocation.getTime()) / 1000 ) );
                        Log.d(Constants.TAG, "distance: " + distance(loc, lastLocation) + " speed (km/h): " + (int) ((speed*3600)/1000));
                        loc.setSpeed(speed);
                    }

                    lastLocation = loc;
                }


                //write to file
                try {
                    writeToFile(loc);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Uncomment to send broadcast to activity
                intent.putExtra("Latitude", loc.getLatitude());
                intent.putExtra("Longitude", loc.getLongitude());
                intent.putExtra("Speed", (int) ((loc.getSpeed()*3600)/1000));
                sendBroadcast(intent);
            }

        }

        private void writeToFile(Location loc) throws IOException {
            //get directory
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                externalStorageWritable = true;
            }

            if(externalStorageWritable) {

                //write to file
                fileWriter = new FileWriter(externalStorageDirectory + "/" + outFile, true);
                Date now = new Date();
                try {
                    fileWriter.append(loc.getLatitude() + "," + loc.getLongitude() + "," + loc.getSpeed() + "," + new Timestamp(now.getTime()) + "\n");

                    Log.d(Constants.TAG, "GPS data: " + loc.getLatitude() + "," + loc.getLongitude() + "," + loc.getSpeed() + "," + new Timestamp(now.getTime()) + "\n");

                } catch (IOException e) {
                    e.printStackTrace();
                }
                fileWriter.close();
            }
        }

        private Double distance(Location one, Location two) {
            int R = 6371000;
            Double dLat = toRad(two.getLatitude() - one.getLatitude());
            Double dLon = toRad(two.getLongitude() - one.getLongitude());
            Double lat1 = toRad(one.getLatitude());
            Double lat2 = toRad(two.getLatitude());
            Double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
            Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            Double d = R * c;
            return d;
        }
        private double toRad(Double d) {
            return d * Math.PI / 180;
        }

        public void onProviderDisabled(String provider)
        {
            Toast.makeText(getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT).show();
        }


        public void onProviderEnabled(String provider)
        {
            Toast.makeText( getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }


        public void onStatusChanged(String provider, int status, Bundle extras)
        {

        }

    }
}