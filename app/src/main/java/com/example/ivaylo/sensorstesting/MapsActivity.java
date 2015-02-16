package com.example.ivaylo.sensorstesting;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;


public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private Cursor cursor = null;

    private String version;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maps);

        SharedPreferences sharedPreferences = getSharedPreferences("SETTINGS", Context.MODE_PRIVATE);

        if(sharedPreferences.contains("version")) {
            version = sharedPreferences.getString("version", "1.0");
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("version", version);
        editor.commit();
        Log.d(Constants.TAG, "SharedPref version:" + version);

        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(android.os.Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));



        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(42.6571483,23.2857894), 13.0f));

        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(42.6571483, 23.2857894))
                .title(getString(R.string.map_marker_title_1))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .alpha(0.75f));

        /*Uri streetViewURI = Uri.parse("google.streetview:cbll="+42.6571483+","+23.2857894);
        Intent streetView = new Intent(Intent.ACTION_VIEW, streetViewURI);
        streetView.setPackage("com.google.android.apps.maps");
        startActivity(streetView);*/

        PolygonOptions polygonOptions = new PolygonOptions();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(Constants.externalStorageDirectory + "/" + Constants.testFile)));

            String result = "";
            String line = null;
            while ((line = reader.readLine()) != null){
                result += line + "\n";
                String[] RowData = line.split(",");
                Double lat = Double.parseDouble(RowData[0]);
                Double lon = Double.parseDouble(RowData[1]);

                Log.d(Constants.TAG, "Lat: " + lat + " Lon: " + lon);

                polygonOptions.add(
                        new LatLng(lat, lon)
                );
            }

            Polygon polygon = mMap.addPolygon(polygonOptions);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        //load markers
        /*if(cursor.moveToFirst())
        {
            //db is not empty, load markers
            do {


            } while(cursor.moveToNext());
        }*/


        /*

        //CODE FROM LECTURE

        mMap.addMarker(new MarkerOptions()
                .position(MMSofia)
                .title("MM Academy Sofia")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .alpha(0.75f)
        );
        mMap.addMarker(new MarkerOptions()
                .position(MMPlovdiv)
                .title("MM Academy Plovdiv")

        );

        //mMap.moveCamera();
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(42.0, 23.0), 8.0f));

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(MMSofia);
        circleOptions.radius(1000f);

        Circle circle = mMap.addCircle(circleOptions);

        PolygonOptions polygonOptions = new PolygonOptions();
        polygonOptions.add(
            new LatLng(MMSofia.latitude - 0.02f, MMSofia.longitude - 0.02f),
            new LatLng(MMSofia.latitude - 0.02f, MMSofia.longitude),
            new LatLng(MMSofia.latitude, MMSofia.longitude),
            new LatLng(MMSofia.latitude, MMSofia.longitude - 0.02f),
            new LatLng(MMSofia.latitude - 0.02f, MMSofia.longitude - 0.02f)
        );

        Polygon polygon = mMap.addPolygon(polygonOptions);

        Uri streetViewURI = Uri.parse("google.streetview:cbll="+MMSofia.latitude+","+MMSofia.longitude);
        Intent streetView = new Intent(Intent.ACTION_VIEW, streetViewURI);
        streetView.setPackage("com.google.android.apps.maps");
        startActivity(streetView);*/
    }
}
