package com.example.outdoordirections;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.widget.TextView;
import android.widget.Toast;

import com.example.outdoordirections.model.Point;
import com.example.outdoordirections.model.Route;
import com.example.outdoordirections.model.Vertex;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.PlaneRenderer;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.Objects;

public class ARActivity extends AppCompatActivity implements SensorEventListener {

    // UI
    TextView test;

    // Location
    public LocationManager locationManager;
    public LocationListener locationListener = new ARActivity.MyLocationListener();

    // Current location
    GeoPoint currentLocation;
    Point utmCurrent = new Point(0,0, 0, 0);
    ArrayList<Point> currents = new ArrayList<>();
    Vertex vertexCurrent = new Vertex(0,0, 0);
    Vertex viewPoint = new Vertex(0,0, 0);
    Point utmDestination = new Point(0,0,0,0);

    // Map
    private MapView osm;
    private IMapController mc;
    Marker currentMarker, destinationMarker;

    // Route
    Route route = new Route(new ArrayList<>());
    Polyline pathLine;
    int ni;

    // AR variables
    private ArFragment arCam;
    private Node oldNode = null;
    TransformableNode model;
    private int deviceHeight, deviceWidth;
    private int count = 0;
    private Scene.OnUpdateListener sceneUpdate;
    private ArrayList<Double> angleBetweenTwoVectorList = new ArrayList<>();

    // Sensor
    private float Rot[] = null; //for gravity rotational data
    private float I[] = null; //for magnetic rotational data
    private float accels[] = new float[3];
    private float mags[] = new float[3];
    private float[] values = new float[3];
    private float yaw;
    private ArrayList<Float> yaws = new ArrayList<>();
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
        setContentView(R.layout.activity_aractivity);

        //sensor manager & sensor required to calculate yaw
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);



        // Get route
        ArrayList<Point> points = (ArrayList<Point>)getIntent().getSerializableExtra("route");
        route.setPoints(points);

//        // TO debug
//        route.addPoint(new Point(0,0, 0, 0));
//        route.addPoint(new Point(5,0, 0, 0));
//        route.addPoint(new Point(6,0, 0, 0));
//        route.addPoint(new Point(7,0, 0, 0));
//        route.addPoint(new Point(8,0, 0, 0));


        if (route.size()>1){
            ni = 1;
        }else{
            ni = 0;
        }


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
                        loadRouteModel(0.005);
                    }
                },5000);
            }

            sceneUpdate = new Scene.OnUpdateListener() {
                @Override
                public void onUpdate(FrameTime frameTime) {
                    // TODO: ?
                    // arCam.onUpdate(frameTime);
                    if(oldNode!=null){
//                        if (count%100==0){
                        updateNode();
//                        }
//                        count++;
                    }
                }
            };
            arCam.getArSceneView().getScene().addOnUpdateListener(sceneUpdate);
        } else {
            return;
        }

        // Map
        osm = findViewById(R.id.arMapview);
        osm.bringToFront();
        osm.setTranslationZ(100);
        currentMarker = new Marker(osm);
        destinationMarker = new Marker(osm);
        pathLine = new Polyline(osm);
        osm.setTileSource(TileSourceFactory.MAPNIK);
        osm.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        osm.setMultiTouchControls(true);
        mc = (MapController) osm.getController();
        mc.setZoom(18.0);
        utmDestination = (Point) getIntent().getSerializableExtra("destination");
        addMarkerLocation(new GeoPoint(utmDestination.getLat(), utmDestination.getLon()));
    }


    private void loadDestinationModel() {
        ModelRenderable.builder()
                .setSource(ARActivity.this, Uri.parse("arrow_location.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(modelRenderable ->{
                    arCam.getArSceneView().getScene().removeOnUpdateListener(sceneUpdate);
                    addDestinationNode(modelRenderable);
                })
                .exceptionally(throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ARActivity.this);
                    builder.setMessage("Something is not right" + throwable.getMessage()).show();
                    return null;
                });
    }

    private void loadRouteModel(double scale) {
        ModelRenderable.builder()
                                .setSource(ARActivity.this, Uri.parse("red_arrow_chevrons_wayfinding.glb"))
                                .setIsFilamentGltf(true)
                                .build()
                                .thenAccept(modelRenderable -> addRouteNode(modelRenderable, scale))
                                .exceptionally(throwable -> {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(ARActivity.this);
                                    builder.setMessage("Something is not right" + throwable.getMessage()).show();
                                    return null;
                                });
    }

    private void updateNode() {

        Camera camera = arCam.getArSceneView().getScene().getCamera();
        Ray ray = camera.screenPointToRay(deviceWidth/2, 2*deviceHeight/3);


        //
        Vector3 rp = ray.getPoint(5f);
        model.setWorldPosition(rp);
        // Rotate model from view point to current location
        Quaternion q = arCam.getArSceneView().getScene().getCamera().getLocalRotation();
        com.example.outdoordirections.model.Quaternion qc = new com.example.outdoordirections.model.Quaternion(q);
        Vector3 normal = qc.normal();
        Vertex normalPoint = new Vertex(normal.x, normal.z, 0).normalize();
        float initial2dRotate = (float)Math.atan2(normalPoint.getY(), normalPoint.getX());


        if (utmCurrent!=null){
            vertexCurrent.setX(utmCurrent.getX());
            vertexCurrent.setY(utmCurrent.getY());


            // TODO: 1.8 m
            // View point
            //  O    |         \
            // /|\  1.5m       1.8m
            // / \   | __ 1m __  \
            double d = 1.8;
            viewPoint = vertexCurrent.add(new Vertex(d*Math.sin(Math.toRadians(yaw)),
                    d*Math.cos(Math.toRadians(yaw)),
                    0));


            // show route on map
            ArrayList<GeoPoint> routeGeo = new ArrayList<>();
            routeGeo.add(new GeoPoint(utmCurrent.getLat(), utmCurrent.getLon()));
            for (int i = ni; i < route.size(); i++) {
                routeGeo.add(new GeoPoint(route.getPoints().get(i).getLat(),
                        route.getPoints().get(i).getLon()));
            }
            drawCustomPolyline(routeGeo);

            Vertex nextPnt = new Vertex(route.getPoints().get(ni).getX(),
                    route.getPoints().get(ni).getY(),
                    0.0);
            Vertex prevPnt;
            if (ni>0){
                prevPnt = new Vertex(route.getPoints().get(ni-1).getX(),
                        route.getPoints().get(ni-1).getY(),
                        0.0);
            }else{
                prevPnt = new Vertex(route.getPoints().get(ni).getX(),
                        route.getPoints().get(ni).getY(),
                        0.0);
            }
            // TODO: project onto line
            // check distance from line
//            Vertex projected = projectOnLine( prevPnt, nextPnt);

            // Rotate from view to destination
            // Direction from view to current == model initial direction
            final Vertex diffFromViewToCurrent = vertexCurrent.sub(viewPoint);
            final Vertex directionFromViewToCurrent = diffFromViewToCurrent.normalize();
            double alpha = Math.atan2(directionFromViewToCurrent.getY(), directionFromViewToCurrent.getX());

            // Direction from view to next point on route
            final Vertex diffFromViewToNext = nextPnt.sub(viewPoint);
            final Vertex diffFromNextToPrev = prevPnt.sub(nextPnt);

            // Distance from view to next point
            loadRouteModel(diffFromViewToNext.length()/diffFromNextToPrev.length());

            final Vertex directionFromViewToNext = diffFromViewToNext.normalize();
            double beta = Math.atan2(directionFromViewToNext.getY(), directionFromViewToNext.getX());

            double rotationDegree;
            double angleBetweenTwoVector = Math.acos(
                    directionFromViewToNext.dot(directionFromViewToCurrent)/
                            (directionFromViewToNext.length()*directionFromViewToCurrent.length())
            );
            angleBetweenTwoVector = modifyAngle(angleBetweenTwoVector);
            // TODO: 140?
            if (Math.toDegrees(angleBetweenTwoVector)>130){
                rotationDegree = Math.PI;
            }else{
                rotationDegree = beta - alpha;
            }
            final Quaternion finalQ;
            final Quaternion faceToBed;
            final Quaternion lookFromViewToNext;
            // TODO: 45 and 135?
            if (Math.toDegrees(angleBetweenTwoVector)>45 &&  Math.toDegrees(angleBetweenTwoVector)<125){
                finalQ = Quaternion.axisAngle(Vector3.up(), (float)Math.toDegrees(initial2dRotate+rotationDegree)+270f);
            }else{
                faceToBed = Quaternion.axisAngle(Vector3.right(), 90f);
                lookFromViewToNext = Quaternion.axisAngle(Vector3.up(), (float)Math.toDegrees(initial2dRotate+rotationDegree)+270f);

                finalQ = Quaternion.multiply(lookFromViewToNext, faceToBed );
            }

            model.setWorldRotation(finalQ);


            if (ni!=0){
                // to debug
//                test.setText(
//                        "len route = "+ route.size()+"\n" +
//                                "ni = "+ ni+"\n"+
//                                "current = " + utmCurrent.getX()+", "+ utmCurrent.getY()+"\n"+
//                                "yaw = "+ yaw+"\n"+
//                                "view = " +viewPoint.getX()+", "+viewPoint.getY()+"\n"+
//                                "next = "+nextPnt.getX()+", "+nextPnt.getY()+"\n"+
//                                "directionFromViewToNext = "+ directionFromViewToNext.getX()+", "+
//                                directionFromViewToNext.getY()+"\n"+
//                                " angleBetweenTwoVector = "+Math.toDegrees(angleBetweenTwoVector)+"\n"+
//                                "distance to next point = "+ diffFromViewToNext.length()
//
//                );
                if (!route.finish(ni)){
                    // TODO:1.?
                    if (diffFromViewToNext.length()/10<0.75){

                        ni = route.next(ni);
                        angleBetweenTwoVectorList = new ArrayList<>();
                    }
                }else{
                    // View point is on destination, put marker
                    if (diffFromViewToNext.length()/10<1.0){
                        loadDestinationModel();
                    }
                }
            }else{
                // Route has just one point so
                // View point is on destination, put marker
                if (diffFromViewToNext.length()/10<1.0){
                    loadDestinationModel();
                }
            }
        }

    }

    private double modifyAngle(double angleBetweenTwoVector) {
        double meanAngle = 0.;
        if (angleBetweenTwoVectorList.size()>0){
            if (angleBetweenTwoVectorList.size()>10){
                angleBetweenTwoVectorList.remove(0);
            }
            angleBetweenTwoVectorList.add(angleBetweenTwoVector);
            for (int i = 0; i < angleBetweenTwoVectorList.size() ; i++) {
                meanAngle +=  angleBetweenTwoVectorList.get(i);
            }

            meanAngle /= angleBetweenTwoVectorList.size();
        }else{
            angleBetweenTwoVectorList.add(angleBetweenTwoVector);
            meanAngle = angleBetweenTwoVector;
        }

        return meanAngle;
    }

    private Vertex projectOnLine(Vertex prevPnt, Vertex nextPnt) {
        double l2 = prevPnt.distance(nextPnt)*prevPnt.distance(nextPnt);
        if (l2==0){
            return vertexCurrent;
        }

        double t = prevPnt.sub(vertexCurrent).dot(prevPnt.sub(nextPnt))/l2;
        if (t > 1 || t < 0){
            return vertexCurrent;
        }

        double minT = Math.min(1,t);
        double maxT = Math.max(0, minT);
        Vertex projected = prevPnt.add((prevPnt.sub(nextPnt).mulScalar(maxT)));
        return projected;
    }


    private void addDestinationNode(ModelRenderable modelRenderable) {
        arCam.getArSceneView().getPlaneRenderer().setVisible(false);

        Node node = new Node();

        // Remove old object
        if(oldNode!=null){
            Node nodeToRemove = arCam.getArSceneView().getScene().getChildren().get(1);
            arCam.getArSceneView().getScene().removeChild(nodeToRemove);

        }
        node.setParent(arCam.getArSceneView().getScene());
        Camera camera = arCam.getArSceneView().getScene().getCamera();
        Ray ray = camera.screenPointToRay(deviceWidth/2, 2*deviceHeight/3);

        model = new TransformableNode(arCam.getTransformationSystem());
        model.getScaleController().setMaxScale(0.25f);
        model.getScaleController().setMinScale(0.2f);
        model.setLocalPosition(ray.getPoint(5f));
        model.setParent(node);
        model.setRenderable(modelRenderable);
        model.getTransformationSystem().selectNode(null);
        oldNode = node;
        arCam.getArSceneView().getScene().addChild(oldNode);

    }


    private void addRouteNode(ModelRenderable modelRenderable, double scale ) {
        arCam.getArSceneView().getPlaneRenderer().setVisible(false);

        Node node = new Node();

        // Remove old object
        if(oldNode!=null){
            Node nodeToRemove = arCam.getArSceneView().getScene().getChildren().get(1);
            arCam.getArSceneView().getScene().removeChild(nodeToRemove);

        }
        node.setParent(arCam.getArSceneView().getScene());
        Camera camera = arCam.getArSceneView().getScene().getCamera();
        Ray ray = camera.screenPointToRay(deviceWidth/2, 2*deviceHeight/3);

        model = new TransformableNode(arCam.getTransformationSystem());
        //TODO: scale?
        if (scale<0.5){
            scale = 0.5;
        }else if (scale > 1.0){
            scale = 1.0;
        }
        model.getScaleController().setMaxScale((float)scale*6/1000);
        model.getScaleController().setMinScale((float)scale*4/1000);
        model.setLocalPosition(ray.getPoint(5f));
        model.setParent(node);
        model.setRenderable(modelRenderable);
        model.getTransformationSystem().selectNode(null);
        oldNode = node;
        arCam.getArSceneView().getScene().addChild(oldNode);

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
                utmCurrent.setLat(currentLocation.getLatitude());
                utmCurrent.setLon(currentLocation.getLongitude());
                utmCurrent.convert2utm();
                modifyCurrent();
                addMarker(currentLocation);
                mc.animateTo(currentLocation);
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

    private void modifyCurrent(){
        double x = 0,y =0;
        if (currents.size()==0){
            currents.add(utmCurrent);
        }else{
            x = 0;
            y = 0;
            if (currents.size()>5){
                currents.remove(0);
            }
            for (int i = 0; i < currents.size() ; i++) {
                x += currents.get(i).getX();
                y += currents.get(i).getY();
            }
            x /= currents.size();
            y /= currents.size();
            utmCurrent.setX(x);
            utmCurrent.setY(y);
        }
    }


    // draw polyline function
    public void drawCustomPolyline(ArrayList<GeoPoint> points) {

        try {
            osm.getOverlays().remove(pathLine);
        } catch (Exception ex) {

        }


        pathLine.setPoints(points);
        pathLine.setInfoWindow(null);
        pathLine.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
        pathLine.getOutlinePaint().setColor(Color.rgb(0, 191, 255));
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

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

    }

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
            SensorManager.getOrientation(Rot, values);

            // here we calculated the final yaw(azimuth), roll & pitch of the device.
            // multiplied by a global standard value to get accurate results

            // this is the yaw or the azimuth we need
            modifyYaw(values[0]);

            pitch = (float)Math.toDegrees(values[1]);
            roll = (float)Math.toDegrees(values[2]);

            //retrigger the loop when things are repopulated
            mags = null;
            accels = null;
        }

    }

    private void modifyYaw(float value) {
        if (yaws.size()==0){
            yaw = (float)Math.toDegrees(value);
            yaws.add(yaw);
        }else{
            yaw = 0;
            for (int i = 0; i < yaws.size(); i++) {
                if ( yaws.get(i)<-170 || yaws.get(i)>170) {
                    if (yaws.get(0)>0){
                        yaw += Math.abs(yaws.get(i));
                    }else{
                        yaw += -Math.abs(yaws.get(i));
                    }
                }else{
                    yaw += yaws.get(i);
                }
            }
            yaw = yaw/yaws.size();
            if (Math.abs(yaw - (float)Math.toDegrees(value) )>20 &&
                    Math.abs(yaw - (float)Math.toDegrees(value) )<35){
                yaws.remove(0);
                yaws.add(yaw);
                yaw = 0;
                for (int i = 0; i < yaws.size(); i++) {
                    if ( yaws.get(i)<-170 || yaws.get(i)>170) {
                        if (yaws.get(0)>0){
                            yaw += Math.abs(yaws.get(i));
                        }else{
                            yaw += -Math.abs(yaws.get(i));
                        }
                    }else{
                        yaw += yaws.get(i);
                    }
                }
                yaw = yaw/yaws.size();
            }else{
                if (yaws.size()>5){
                    yaws.remove(0);
                }
                yaws.add((float)Math.toDegrees(value));
                yaw = 0;
                for (int i = 0; i < yaws.size(); i++) {
                    if ( yaws.get(i)<-170 || yaws.get(i)>170) {
                        if (yaws.get(0)>0){
                            yaw += Math.abs(yaws.get(i));
                        }else{
                            yaw += -Math.abs(yaws.get(i));
                        }
                    }else{
                        yaw += yaws.get(i);
                    }
                }
                yaw /= yaws.size();
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onPause() {
        super.onPause();
        osm.onPause();
        // TODO: Should be tested this line
        // locationManager.removeUpdates(locationListener);
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

}