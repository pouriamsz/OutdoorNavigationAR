package com.example.outdoordirections;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        this.getSupportActionBar().hide();

        checkLocationPermissions();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashScreen.this, MainActivity.class));
                finish();
                overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_left);
            }
        },5000);
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


        return true;
    }
}