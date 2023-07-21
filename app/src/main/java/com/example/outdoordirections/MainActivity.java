package com.example.outdoordirections;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.outdoordirections.model.API;
import com.example.outdoordirections.model.Point;
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
import java.util.ArrayList;
import java.util.Objects;

import okhttp3.Response;
import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.OSRef;


public class MainActivity extends AppCompatActivity implements SensorEventListener {


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
    Marker marker, locationMarker;

    // current location
    GeoPoint currentLocation;
    Point utmCurrent;
    GeoPoint destination;
    Point utmDestination;
    boolean isCurrent = false;

    // to draw polyline
    Polyline line;
    Polyline pathLine;
    ArrayList<GeoPoint> pathPoints = new ArrayList<>();
    ArrayList<Point> pathUtm = new ArrayList<>();

    // AR variables
    private ArFragment arCam;
    private Node oldNode = null;
    TransformableNode model;
    private int deviceHeight, deviceWidth;
    private Node node = new Node();
    private int count = 0;

    // Sensor variables
    private float Rot[] = null; //for gravity rotational data
    private float I[] = null; //for magnetic rotational data
    private float accels[] = new float[3];
    private float mags[] = new float[3];
    private float[] values = new float[3];
    private float yaw;
    private float pitch;
    private float roll;
    private SensorManager sensorManager;
    private Sensor sensor;

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

        // TODO: Remove sensor
        //sensor manager & sensor required to calculate yaw
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);

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

                                                // TODO: z is the length
                                                ModelRenderable model = ShapeFactory.makeCube(
                                                        new Vector3(.3f, .006f, 0.5f),
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
                    for (int i = 0; i < pathCoordinates.length(); i++) {
                        JSONArray pathPointJson = pathCoordinates.getJSONArray(i);
                        GeoPoint pathPoint = new GeoPoint(
                                pathPointJson.getDouble(1),
                                pathPointJson.getDouble(0)
                        );


                        Point utmPoint = convert2utm(pathPoint);

                        pathUtm.add(utmPoint);

                        pathPoints.add(pathPoint);

                    }

                    drawCustomPolyline(pathPoints);
                   // Log.d("response", "============== "+ pathPoints);


                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }


            }
        });

        // Tap destination on map
        osm.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                destination = p;
                try {
                    utmDestination = convert2utm(destination);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

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

    private Point convert2utm(GeoPoint pnt) throws IOException, JSONException {
        LatLng gp = new LatLng(pnt.getLatitude(), pnt.getLongitude());
        OSRef osRef = gp.toOSRef();
        double easting = osRef.getEasting();
        double northing = osRef.getNorthing();

        return new Point(easting, northing);
    }

    private void updateNode() {

        Camera camera = arCam.getArSceneView().getScene().getCamera();
        // TODO: Cast ray to center of screen
        Ray ray = camera.screenPointToRay(deviceWidth/2, 500);

        // TODO: This distance is so important, find a way to calculate that
        // Maybe a large number could be good at this point
        model.setLocalPosition(ray.getPoint(2f));
        // TODO: Quaternion
        //model.setLocalRotation(arCam.getArSceneView().getScene().getCamera().getLocalRotation());

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

        // Sensor
        sensorManager.unregisterListener(this);

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

        // Sensor
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            // stop request gps location
            locationManager.removeUpdates(locationListener);
        }
    }

    // Sensor
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType())
        {
            case Sensor.TYPE_MAGNETIC_FIELD:
                mags = sensorEvent.values.clone();
                break;
            case Sensor.TYPE_ACCELEROMETER:
                accels = sensorEvent.values.clone();
                break;
        }

        if (mags != null && accels != null) {
            Rot = new float[9];
            I= new float[9];
            SensorManager.getRotationMatrix(Rot, I, accels, mags);

            // Correct if screen is in Landscape
            float[] outR = new float[9];
            SensorManager.remapCoordinateSystem(Rot, SensorManager.AXIS_X,SensorManager.AXIS_Z, outR);
            SensorManager.getOrientation(outR, values);

            // here we calculated the final yaw(azimuth), roll & pitch of the device.
            // multiplied by a global standard value to get accurate results

            // this is the yaw or the azimuth we need
            yaw = values[0] * 57.2957795f;
            pitch =values[1] * 57.2957795f;
            roll = values[2] * 57.2957795f;

            //retrigger the loop when things are repopulated
            mags = null;
            accels = null;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

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

                try {
                    utmCurrent = convert2utm(currentLocation);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

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

    private void isEnableGPS(){
        try {
            gps_enable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {

        }
    }


}