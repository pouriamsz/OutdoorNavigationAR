package com.example.outdoordirections;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.outdoordirections.model.Point;
import com.example.outdoordirections.model.Route;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ARActivity extends AppCompatActivity {

    // UI
    TextView test;

    // Location
    public LocationManager locationManager;
    public LocationListener locationListener = new ARActivity.MyLocationListener();

    // Current location
    GeoPoint currentLocation;
    Point utmCurrent = new Point(0,0);
    Point viewPoint = new Point(0,0);

    // Route
    Route route = new Route(new ArrayList<Point>());

    // AR variables
    private ArFragment arCam;
    private Node oldNode = null;
    TransformableNode model;
    private int deviceHeight, deviceWidth;
    private Node node = new Node();
    private int count = 0;

    int ni;


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
        setContentView(R.layout.activity_aractivity);


        ArrayList<Point> points = (ArrayList<Point>)getIntent().getSerializableExtra("route");
        route.setPoints(points);

        if (route.size()>3){
            ni = 2;
        }else{
            ni = 1;
        }

        // Log.d("Route", " =======  "+route.size());

        test = findViewById(R.id.testText);
        // current location
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        getMyLocation();

        // Device size
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        deviceHeight = displayMetrics.heightPixels;
        deviceWidth = displayMetrics.widthPixels;

        if (checkSystemSupport(this)) {

            // ArFragment is linked up with its respective id used in the activity_main.xml
            arCam = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arCameraArea);
            if (oldNode==null){
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadModel();
                    }
                },5000);
            }

            arCam.getArSceneView().getScene().addOnUpdateListener(new Scene.OnUpdateListener() {
                @Override
                public void onUpdate(FrameTime frameTime) {
                    // TODO: ?
                    // arCam.onUpdate(frameTime);
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

    private void loadModel() {
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
            Toast.makeText(this, "error:"+throwable.getCause(), Toast.LENGTH_SHORT).show();
            return null;
        });

        /* How load gltf and glb?
        ModelRenderable.builder()
                                .setSource(MainActivity.this, Uri.parse("gfg_gold_text_stand_2.glb"))
                                .setIsFilamentGltf(true)
                                .build()
                                .thenAccept(modelRenderable -> addNode(modelRenderable))
                                .exceptionally(throwable -> {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setMessage("Something is not right" + throwable.getMessage()).show();
                                    return null;
                                }); */
    }

    private void updateNode() {

        Camera camera = arCam.getArSceneView().getScene().getCamera();
        Ray ray = camera.screenPointToRay(deviceWidth/2, deviceHeight/2);


        Vector3 rp = ray.getPoint(2f);
        model.setLocalPosition(rp);
        // TODO: Quaternion
        Quaternion q = arCam.getArSceneView().getScene().getCamera().getLocalRotation();
        com.example.outdoordirections.model.Quaternion qc = new com.example.outdoordirections.model.Quaternion(q);
        Vector3 normal = qc.normal();
        Point normalPoint = new Point(normal.x, normal.z);



        if (utmCurrent!=null){
            viewPoint = utmCurrent.add(normalPoint.mulScalar(2.0));
            Point currentPnt = route.getPoints().get(ni);
            Point prevPnt = route.getPoints().get(ni-1);
            if (currentPnt.distance(viewPoint)<prevPnt.distance(viewPoint)){
                if (!route.finish(ni+1)){
                    ni = route.next(ni);
                }else{
                    // route has finished
//                    if(oldNode!=null){
//                        arCam.getArSceneView().getScene().removeChild(oldNode);
//                    }
                }
            }

            test.setText(""+ni);
            Point difNormal = viewPoint.sub(route.getPoints().get(ni)).normalize();
            Point down = new Point(0,-1);
            com.example.outdoordirections.model.Quaternion qr = new com.example.outdoordirections.model.Quaternion(new Quaternion());
            qr.setFrom2Vec(down, difNormal);
            model.setLocalRotation(qr.getQuaternion());
        }

    }

    private void addNode(ModelRenderable modelRenderable) {
        // Remove old object
        if(oldNode!=null){
            arCam.getArSceneView().getScene().removeChild(oldNode);
        }
        node.setParent(arCam.getArSceneView().getScene());
        Camera camera = arCam.getArSceneView().getScene().getCamera();
        Ray ray = camera.screenPointToRay(deviceWidth/2, deviceHeight/2);

        model = new TransformableNode(arCam.getTransformationSystem());
        model.setParent(node);
        model.setRenderable(modelRenderable);
        model.setLocalPosition(ray.getPoint(2f));
        // model.setLocalRotation(arCam.getArSceneView().getScene().getCamera().getLocalRotation());

        oldNode = node;
        arCam.getArSceneView().getScene().addChild(node);

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

            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {

        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            Toast.makeText(ARActivity.this, "Please Enable GPS", Toast.LENGTH_SHORT).show();
        }
    }


    // get current location
    public void getMyLocation() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

    }

    @Override
    public void onPause() {
        super.onPause();
        // TODO: Should be tested this line
        // locationManager.removeUpdates(locationListener);

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

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            // stop request gps location
            locationManager.removeUpdates(locationListener);
        }
    }

}