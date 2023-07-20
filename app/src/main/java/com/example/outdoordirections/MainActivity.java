package com.example.outdoordirections;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {


    // UI variables
    ImageButton getLocation;
    Button directionBtn, openCamera;

    // Location
    public LocationManager locationManager;
    public LocationListener locationListener = new MyLocationListener();

    // Lat, Lon, distance, Speed, time
    double dLat1 = 0.0, dLon1 = 0.0;
    double dLat2 = 0.0, dLon2 = 0.0;
    double deltaLat = 0.0, deltaLon = 0.0;
    double dist = 0.0, saveDist = 0.0;
    long time = (long) 0.0;
    double dTime = 0.0, dDist = 0.0;
    double totalTime = 0.0, saveTotalTime = 0.0, totalDistance = 0.0;

    // Need this variables to calculate distance
    double a = 0.0, c = 0.0;

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
    Marker marker, locationMarker;

    // current location
    GeoPoint currentLocation;
    GeoPoint destination;
    boolean isCurrent = false;


    // to draw polyline
    Polyline line;
    Polyline pathLine;
    ArrayList<GeoPoint> pathPoints = new ArrayList<>();
    ArrayList<GeoPoint> path = new ArrayList<>();

    GeoPoint currentPnt, nextPnt;
    double ttff = 5.0;

    // AR variables
    private ArFragment arCam;
    private Node oldNode = null;
    TransformableNode model;
    private int deviceHeight, deviceWidth;
    private Node node = new Node();
    private int count = 0;

    public static boolean checkSystemSupport(Activity activity) {

        // checking whether the API version of the running Android >= 24
        // that means Android Nougat 7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String openGlVersion = ((ActivityManager) Objects.requireNonNull(activity.getSystemService(Context.ACTIVITY_SERVICE))).getDeviceConfigurationInfo().getGlEsVersion();

            // checking whether the OpenGL version >= 3.0
            if (Double.parseDouble(openGlVersion) >= 3.0) {
                return true;
            } else {
                Toast.makeText(activity, "App needs OpenGl Version 3.0 or later", Toast.LENGTH_SHORT).show();
                activity.finish();
                return false;
            }
        } else {
            Toast.makeText(activity, "App does not support required Build Version", Toast.LENGTH_SHORT).show();
            activity.finish();
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        setContentView(R.layout.activity_main);
        // Direction API need this
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        // Device size
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        deviceHeight = displayMetrics.heightPixels;
        deviceWidth = displayMetrics.widthPixels;


        // initialize UI variables
        getLocation = (ImageButton) findViewById(R.id.btnLocation);
        directionBtn = findViewById(R.id.btnDirection);
        openCamera = findViewById(R.id.btnCamera);
        osm = findViewById(R.id.mapview);
        // Change map view size
//        RelativeLayout.LayoutParams mapViewParams = new RelativeLayout.LayoutParams(deviceWidth, deviceHeight/2);
//        osm.setLayoutParams(mapViewParams);


        // AR Camera Button
        openCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (checkSystemSupport(MainActivity.this)) {

                    if (oldNode==null){
                        Texture.Sampler sampler = Texture.Sampler.builder()
                                .setMinFilter(Texture.Sampler.MinFilter.LINEAR_MIPMAP_LINEAR)
                                .setMagFilter(Texture.Sampler.MagFilter.LINEAR)
                                .build();

                        Texture.builder()
                                .setSource(() -> getApplicationContext().getAssets().open("arrow_texture.png"))
                                .setSampler(sampler)
                                .build().thenAccept(texture -> {
                            MaterialFactory.makeTransparentWithTexture(getApplicationContext(), texture) //new Color(0, 255, 244))
                                    .thenAccept(
                                            material -> {

                                                // TODO:
                                                ModelRenderable model = ShapeFactory.makeCube(
                                                        new Vector3(.3f, .006f, 1.0f),
                                                        Vector3.zero(), material);


                                                addNode(model);

                                            }
                                    );
                        }).exceptionally(throwable -> {
                            Toast.makeText(MainActivity.this, "error:"+throwable.getCause(), Toast.LENGTH_SHORT).show();
                            return null;
                        });

//                        ModelRenderable.builder()
//                                .setSource(MainActivity.this, Uri.parse("gfg_gold_text_stand_2.glb"))
//                                .setIsFilamentGltf(true)
//                                .build()
//                                .thenAccept(modelRenderable -> addNode(modelRenderable))
//                                .exceptionally(throwable -> {
//                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                                    builder.setMessage("Something is not right" + throwable.getMessage()).show();
//                                    return null;
//                                });
                    }


                    // ArFragment is linked up with its respective id used in the activity_main.xml
                    arCam = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arCameraArea);
                    arCam.getArSceneView().getScene().addOnUpdateListener(new Scene.OnUpdateListener() {
                        @Override
                        public void onUpdate(FrameTime frameTime) {
                            // TODO: ?
//                            arCam.onUpdate(frameTime);
                            if(oldNode!=null){
                                if (count%10==0){
                                    updateNode();
                                }
                                count++;
                            }
                        }
                    });
                } else {
                    return;
                }

            }
        });

        // Request Direction
        directionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OkHttpClient client = new OkHttpClient().newBuilder().build();
                String url = "https://api.openrouteservice.org/" +
                        "v2/directions/driving-car?api_key=" +
                        getString(R.string.api_key)+
                        "&start=" +
                        currentLocation.getLongitude()+","+ currentLocation.getLatitude()+
                        "&end=" +
                        destination.getLongitude()+"," +destination.getLatitude();
                Request request = new Request.Builder().url(url)
                        .method("GET", null)
                        .build();

                // Parse json response
                try {
                    Response response = client.newCall(request).execute();
                    String jsonData = response.body().string();
                    JSONObject jsonObject = new JSONObject(jsonData);
                    JSONArray features = jsonObject.getJSONArray("features");
                    JSONObject geometry = features.getJSONObject(0).getJSONObject("geometry");
                    JSONArray pathCoordinates = geometry.getJSONArray("coordinates");
                    for (int i = 0; i < pathCoordinates.length(); i++) {
                        JSONArray pathPointJson = pathCoordinates.getJSONArray(i);
                        GeoPoint pathPoint = new GeoPoint(
                                pathPointJson.getDouble(1),
                                pathPointJson.getDouble(0)
                        );
                        pathPoints.add(pathPoint);

                    }
                    drawCustomPolyline(pathPoints);
                   // Log.d("response", "============== "+ pathPoints);


                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }


            }
        });

        // Tap destination on map
        osm.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                destination = p;

                addMarkerLocation(p);
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));

        marker = new Marker(osm);
        locationMarker = new Marker(osm);
        line = new Polyline(osm);
        pathLine = new Polyline(osm);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // check needed permissions
        checkLocationPermissions();
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

    private void updateNode() {

        Camera camera = arCam.getArSceneView().getScene().getCamera();
        // TODO: Cast ray to center of screen
        Ray ray = camera.screenPointToRay(deviceWidth/2, 500);

        model.setLocalPosition(ray.getPoint(1f));
        // model.setLocalRotation(arCam.getArSceneView().getScene().getCamera().getLocalRotation());

    }

    private void addNode(ModelRenderable modelRenderable) {
        // Remove old object
        if(oldNode!=null){
            arCam.getArSceneView().getScene().removeChild(oldNode);
        }
        node.setParent(arCam.getArSceneView().getScene());
        Camera camera = arCam.getArSceneView().getScene().getCamera();
        // TODO: Cast ray to center of screen
        Ray ray = camera.screenPointToRay(deviceWidth/2, 500);

        model = new TransformableNode(arCam.getTransformationSystem());
        model.setParent(node);
        model.setRenderable(modelRenderable);
        model.setLocalPosition(ray.getPoint(1f));
        // model.setLocalRotation(arCam.getArSceneView().getScene().getCamera().getLocalRotation());

        oldNode = node;
        arCam.getArSceneView().getScene().addChild(node);

    }

    @Override
    public void onPause() {
        super.onPause();
        // TODO: Should be tested this line
        // locationManager.removeUpdates(locationListener);
        osm.onPause();
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

                // first time running this method
                // just set dLat1, dLon1 and first time
                if (dLat1 == 0.0 && dLon1 == 0.0) {
                    dLat1 = location.getLatitude();
                    dLon1 = location.getLongitude();
                    time = location.getTime();
                } else {
                    // calculate distance
                    //  https://www.tabnine.com/code/java/methods/java.lang.Math/toRadians?snippet=59212f4b4758780004fb373b
                    dLat2 = location.getLatitude();
                    dLon2 = location.getLongitude();
                    deltaLat = Math.toRadians(dLat2 - dLat1);
                    deltaLon = Math.toRadians(dLon2 - dLon1);
                    a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                            Math.cos(Math.toRadians(dLat1)) * Math.cos(Math.toRadians(dLat2)) *
                                    Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
                    c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                    dist = (6371000 * c);
                    dDist = dist - saveDist;
                    saveDist = dist;
                    // keep total distance
                    totalDistance = totalDistance + dist;
//                    Log.d("dist", "============== "+ dist);

                    // get time difference
                    dTime = (location.getTime() - time) / 1000;
//                    Log.d("dTime", "*****> "+dTime);
                    // keep total time
                    totalTime = totalTime + dTime;
//                    Log.d("Difference", "*****> "+totalTime+ "-"+ saveTotalTime);
                    // set first point to draw path polyline
                    if (totalTime - saveTotalTime == dTime) {
                        currentPnt = new GeoPoint(dLat1, dLon1);

                        // after seconds, we set second point of polyline ...
                    } else if (totalTime - saveTotalTime > 3 * dTime) {
                        nextPnt = new GeoPoint(dLat2, dLon2);
                        // some check to have a smooth and good path
                        if ((currentPnt.getLatitude() - nextPnt.getLatitude() != 0.0 ||
                                currentPnt.getLongitude() - nextPnt.getLongitude() != 0.0) &&
                                dDist < 2.0 && totalTime > ttff) {
                            // avoid from
                            if (path.isEmpty()){
                                currentPnt = nextPnt;
                            }
                            // draw polyline
                            // drawPolyline();
                            saveTotalTime = totalTime;
                        }
                    }
                    // update location and time for next run
                    dLat1 = dLat2;
                    dLon1 = dLon2;
                    time = location.getTime();
//                    Log.d("dTime", "************** "+ dTime);

                    // check navigation mode
//                    if (navigateCheck) {
//                        angle = compassOverlay.getOrientationProvider().getLastKnownOrientation();
////                    Log.d("Angle", ""+angle);
//                        angle = (360 - angle);
//                        if (angle < 0)
//                            angle += 360;
//                        if (angle > 360)
//                            angle -= 360;
//
//                        dAngle = angle - saveAngle;
//                        saveAngle = angle;
//                        // ignore small change in angle
//                        if (dAngle > 4) {
//                            osm.setMapOrientation((float) angle);
//                        }
//                    }

                }

                // reduce speed decimals
                DecimalFormat df = new DecimalFormat("#.###");
                df.setRoundingMode(RoundingMode.CEILING);

//                txtLocation.setText("Location(lat, lon): " + df.format(dLat1) + ", " + df.format(dLon1));
//                txtLat.setText("Lat: " + dLat1);
//                txtLon.setText("Lon: " + dLon1);


                // get current location
                currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

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
            Toast.makeText(MainActivity.this, "Please Enable GPS", Toast.LENGTH_SHORT).show();
        }
    }

    // draw polyline function
    public void drawPolyline() {

        try {
            osm.getOverlays().remove(line);
        } catch (Exception ex) {

        }

//        Log.d("CurrentPnt", "***** > "+ currentPnt);
//        Log.d("CurrentPnt", "***** > "+ nextPnt);
        path.add(currentPnt);
        path.add(nextPnt);
        line.setPoints(path);
        line.setInfoWindow(null);
        line.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
        line.getOutlinePaint().setColor(Color.rgb(0, 191, 255));
        // line.getPaint().setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
//        line.getOutlinePaint().setStrokeWidth(20.0f);
        line.getOutlinePaint().setStyle(Paint.Style.FILL);
        line.getOutlinePaint().setAntiAlias(true);

        osm.getOverlays().add(line);
        osm.invalidate();


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
        osm.getOverlays().remove(locationMarker);
        locationMarker = new Marker(osm);
        locationMarker.setPosition(destination);
        locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        Drawable markerIcon = getResources().getDrawable(R.drawable.location);
        locationMarker.setIcon(markerIcon);


        osm.getOverlays().add(locationMarker);
        osm.invalidate();
        locationMarker.setTitle("Destination");
    }

    // update marker - current location
    public void addMarker(GeoPoint current) {
        osm.getOverlays().remove(marker);
        marker = new Marker(osm);
        marker.setPosition(current);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        Drawable markerIcon = getResources().getDrawable(R.drawable.mylocation32blue);
        marker.setIcon(markerIcon);

//        for (int i = 0; i < osm.getOverlays().size(); i++) {
//            Overlay overlay = osm.getOverlays().get(i);
//            if (overlay instanceof Marker && ((Marker) overlay).getId().equals("String")) {
//                osm.getOverlays().remove(overlay);
//            }
//        }

//        osm.getOverlays().clear();
        osm.getOverlays().add(marker);
        osm.invalidate();
        marker.setTitle("Current Location");
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


    // get permissions
    private boolean checkLocationPermissions() {
        int fineLocPrms = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseLocPrms = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION);
        int writeExtrnalStrg = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int internetPrms = ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET);
        int networkStat = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_NETWORK_STATE);
        int wifiStat = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_WIFI_STATE);


        List<String> listPermission = new ArrayList<>();
        if (fineLocPrms != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (coarseLocPrms != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (writeExtrnalStrg != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (networkStat != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(android.Manifest.permission.ACCESS_NETWORK_STATE);
        }
        if (internetPrms != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(android.Manifest.permission.INTERNET);
        }
        if (wifiStat != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(Manifest.permission.ACCESS_WIFI_STATE);
        }
        if (!listPermission.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermission.toArray(new String[listPermission.size()]), 1);
        }

        try {
            gps_enable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {

        }

        return true;
    }
}