package com.example.outdoordirections.model;

import org.json.JSONException;
import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.io.Serializable;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.OSRef;

public class Point implements Serializable {
    double x;
    double y;
    double z;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public void convert2utm(GeoPoint pnt) {
        LatLng gp = new LatLng(pnt.getLatitude(), pnt.getLongitude());
        OSRef osRef = gp.toOSRef();
        double easting = osRef.getEasting();
        double northing = osRef.getNorthing();

        this.x = easting;
        this.y = northing;
    }
}
