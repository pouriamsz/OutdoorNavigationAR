package com.example.outdoordirections;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.StrictMode;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.outdoordirections.model.API;
import com.example.outdoordirections.model.Point;
import com.example.outdoordirections.model.Route;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;


public class MapActivity extends AppCompatActivity {


    // UI variables
    ImageButton getLocation;
    Button directionBtn, openCamera;

    // Location
    public LocationManager locationManager;
    public LocationListener locationListener = new MyLocationListener();

    // Check
    private boolean gps_enable = false;
    private boolean network_enable = false;
    boolean gpsRunning = false;
    boolean showGPSDialog = false;

    // Map
    private MapView osm;
    private IMapController mc;
    private CompassOverlay compassOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;
    Marker currentMarker, destinationMarker;

    // current location
    GeoPoint currentLocation;
    Point utmCurrent = new Point(0, 0);
    GeoPoint destination;
    Point utmDestination = new Point(0, 0);
    boolean isCurrent = false;

    // Route
    Route route;
    Polyline pathLine;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        setContentView(R.layout.activity_main);
        // Direction API need this
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // initialize UI variables
        getLocation = (ImageButton) findViewById(R.id.btnLocation);
        directionBtn = findViewById(R.id.btnDirection);
        openCamera = findViewById(R.id.btnCamera);
        osm = findViewById(R.id.mapview);


        // AR Camera Button
        openCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // TODO
                if (route != null && route.size()>0){
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ArrayList<Point> points = new ArrayList<>();
                            points = route.getPoints();
                            Intent intent = new Intent(MapActivity.this, ARActivity.class);
                            intent.putExtra("route", points);
                            startActivity(intent);
                            finish();
                            overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_left);
                        }
                    },600);
                }

            }
        });

        // Request Direction
        directionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (destination != null){
                    String url = "https://api.openrouteservice.org/" +
                            "v2/directions/driving-car?api_key=" +
                            getString(R.string.api_key)+
                            "&start=" +
                            currentLocation.getLongitude()+","+ currentLocation.getLatitude()+
                            "&end=" +
                            destination.getLongitude()+"," +destination.getLatitude();
                    API directionApi = new API(url, "GET");

                    // Parse json response
                    try {
                        directionApi.sendRequest();
                        Response response = directionApi.getResponse();
                        String jsonData = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonData);
                        JSONArray features = jsonObject.getJSONArray("features");
                        JSONObject geometry = features.getJSONObject(0).getJSONObject("geometry");
                        JSONArray pathCoordinates = geometry.getJSONArray("coordinates");
                        ArrayList<Point> pathUtm = new ArrayList<>();
                        ArrayList<GeoPoint> pathPoints = new ArrayList<>();
                        for (int i = 0; i < pathCoordinates.length(); i++) {
                            JSONArray pathPointJson = pathCoordinates.getJSONArray(i);
                            GeoPoint pathPoint = new GeoPoint(
                                    pathPointJson.getDouble(1),
                                    pathPointJson.getDouble(0)
                            );


                            Point utmPoint = new Point(0.0, 0.0);
                            utmPoint.convert2utm(pathPoint);

                            pathUtm.add(utmPoint);

                            pathPoints.add(pathPoint);

                        }

                        route = new Route(pathUtm);
                        drawCustomPolyline(pathPoints);
                        // Log.d("response", "============== "+ pathPoints);


                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

        // Tap destination on map
        osm.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                destination = p;
                utmDestination.convert2utm(destination);

                addMarkerLocation(p);
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));

        currentMarker = new Marker(osm);
        destinationMarker = new Marker(osm);
        pathLine = new Polyline(osm);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // check needed permissions
        isEnableGPS();
        // Get Location first time
        getMyLocation();

        // Map
        // render
        osm.setTileSource(TileSourceFactory.MAPNIK);
        // zoomable
        osm.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        // multi touch
        osm.setMultiTouchControls(true);

        mc = (MapController) osm.getController();
        mc.setZoom(18.0);

        // compass
        compassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), osm);
        compassOverlay.enableCompass();
        osm.getOverlays().add(compassOverlay);

        // Rotate map
        mRotationGestureOverlay = new RotationGestureOverlay(osm);
        mRotationGestureOverlay.setEnabled(true);
        osm.setMultiTouchControls(true);
        osm.getOverlays().add(mRotationGestureOverlay);

        // Animate to current location manually
        // rotate map to north
        getLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getMyLocation();
            }
        });

    }


    @Override
    public void onPause() {
        super.onPause();
        // TODO: Should be tested this line
        // locationManager.removeUpdates(locationListener);
        osm.onPause();

    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        getMyLocation();
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        osm.onResume();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            // stop request gps location
            locationManager.removeUpdates(locationListener);
        }
    }

    // LocationListener class
    //
    class MyLocationListener implements LocationListener {

        //
        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (location != null) {
                // locationManager.removeUpdates(locationListener);

                // get current location
                currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

                utmCurrent.convert2utm(currentLocation);

                // animate and update marker
                try {
                    if (!isCurrent) {
                        mc.animateTo(currentLocation);
                        isCurrent = true;
                    }

                    addMarker(currentLocation);
                } catch (Exception ex) {

                }

            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            showGPSDialog = true;
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            showGPSDialog = false;
            Toast.makeText(MapActivity.this, "Please Enable GPS", Toast.LENGTH_SHORT).show();
        }
    }

    // calculate distance
    private double calcDistance(GeoPoint p1, GeoPoint p2) {
        //  https://www.tabnine.com/code/java/methods/java.lang.Math/toRadians?snippet=59212f4b4758780004fb373b
        double dLat1 = p1.getLatitude();
        double dLon1 = p1.getLongitude();
        double dLat2 = p2.getLatitude();
        double dLon2 = p2.getLongitude();
        double deltaLat = Math.toRadians(dLat2 - dLat1);
        double deltaLon = Math.toRadians(dLon2 - dLon1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(Math.toRadians(dLat1)) * Math.cos(Math.toRadians(dLat2)) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = (6371000 * c);

        return dist;
    }


    // draw polyline function
    public void drawCustomPolyline(ArrayList<GeoPoint> points) {

        try {
            osm.getOverlays().remove(pathLine);
        } catch (Exception ex) {

        }

//        Log.d("CurrentPnt", "***** > "+ currentPnt);
//        Log.d("CurrentPnt", "***** > "+ nextPnt);


        pathLine.setPoints(points);
        pathLine.setInfoWindow(null);
        pathLine.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
        pathLine.getOutlinePaint().setColor(Color.rgb(0, 191, 255));
        // line.getPaint().setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
//        line.getOutlinePaint().setStrokeWidth(20.0f);
        pathLine.getOutlinePaint().setStyle(Paint.Style.FILL);
        pathLine.getOutlinePaint().setAntiAlias(true);

        osm.getOverlays().add(pathLine);
        osm.invalidate();


    }


    // update marker
    public void addMarkerLocation(GeoPoint destination) {
        osm.getOverlays().remove(destinationMarker);
        destinationMarker = new Marker(osm);
        destinationMarker.setPosition(destination);
        destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        Drawable markerIcon = getResources().getDrawable(R.drawable.location);
        destinationMarker.setIcon(markerIcon);


        osm.getOverlays().add(destinationMarker);
        osm.invalidate();
        destinationMarker.setTitle("Destination");
    }

    // update marker - current location
    public void addMarker(GeoPoint current) {
        osm.getOverlays().remove(currentMarker);
        currentMarker = new Marker(osm);
        currentMarker.setPosition(current);
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        Drawable markerIcon = getResources().getDrawable(R.drawable.mylocation32blue);
        currentMarker.setIcon(markerIcon);

//        for (int i = 0; i < osm.getOverlays().size(); i++) {
//            Overlay overlay = osm.getOverlays().get(i);
//            if (overlay instanceof Marker && ((Marker) overlay).getId().equals("String")) {
//                osm.getOverlays().remove(overlay);
//            }
//        }

//        osm.getOverlays().clear();
        osm.getOverlays().add(currentMarker);
        osm.invalidate();
        currentMarker.setTitle("Current Location");
    }

    // get current location
    public void getMyLocation() {
        osm.setMapOrientation((float) 0.0);
        try {
            gps_enable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {

        }

        try {
            network_enable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {

        }

        if (!gps_enable && !showGPSDialog) {
            onGPS();
        }

//        if (!gps_enable && showGPSDialog) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//            builder.setTitle("Attention");
//            builder.setMessage("Please Enable GPS");
//            builder.create().show();
//
//        }

        if (gps_enable) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            gpsRunning = true;
//            Log.d("GPS", "********** GPS position **********");
        }
        if (network_enable) {
//            Log.d("Net", " =========== network position ===========");
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }


        try {
            mc.animateTo(currentLocation);
            addMarker(currentLocation);

        } catch (Exception ex) {

        }

    }

    // turn on  GPS
    private void onGPS() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Enable GPS").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));

            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        showGPSDialog = true;

    }

    private void isEnableGPS(){
        try {
            gps_enable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {

        }
    }


}