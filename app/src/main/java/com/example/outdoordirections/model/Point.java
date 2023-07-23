package com.example.outdoordirections.model;

import org.osmdroid.util.GeoPoint;

import java.io.Serializable;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.OSRef;

public class Point implements Serializable {
    double x;
    double y;
    double z;
    double lat;
    double lon;

    public Point(double x, double y, double lat, double lon) {
        this.x = x;
        this.y = y;
        this.lat = lat;
        this.lon = lon;
        this.z = 0.0;
    }

    // Set
    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setX(double x){this.x=x;}
    public void setY(double y){this.y=y;}
    public void setZ(double z) {
        this.z = z;
    }

    // Get
    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
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

    // this should be current point
    public Point toCartesianCoordinateNoRotation(Point point) {
        // TODO: test?
//        double x = (point.lon - this.lon) * this.metersPerDegreeLongitude;
//        double y = (point.lat - this.lon) * this.metersPerDegreeLatitude;

        double x = point.x - this.x;
        double y = point.y - this.y;

        return new Point(x, y, point.lat, point.lon);
    }

    // this should be current point
    public Point toCoordinateNoRotation(Point point) {
        double latitude = this.lat + point.lat;
        double longitude = this.lon + point.lon;
        return new Point(point.x, point.y, latitude, longitude);
    }

    public void convert2utm() {

        LatLng gp = new LatLng(this.lat, this.lon);
        OSRef osRef = gp.toOSRef();
        double easting = osRef.getEasting();
        double northing = osRef.getNorthing();

        this.x = easting;
        this.y = northing;
    }

    public double distance(Point p) {
        return Math.sqrt((this.x - p.x) * (this.x - p.x) +
                (this.y - p.y) * (this.y - p.y) +
                (this.z - p.z) * (this.z - p.z)
        );
    }
}


