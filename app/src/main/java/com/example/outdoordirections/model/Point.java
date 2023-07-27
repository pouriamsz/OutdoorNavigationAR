package com.example.outdoordirections.model;

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
        double x = point.x - this.x;
        double y = point.y - this.y;

        // TODO: lat & lon is wrong, x and y should convert to lat & lon
        return new Point(x, y, point.lat, point.lon);
    }

    // this should be current point
    public Point toCoordinateNoRotation(Point point) {
        double latitude = this.lat + point.lat;
        double longitude = this.lon + point.lon;

        // TODO: x & y is wrong, lat & lon should convert to x & y
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

    // Distance by x & y
    public double distance(Point p) {
        return Math.sqrt((this.x - p.x) * (this.x - p.x) +
                (this.y - p.y) * (this.y - p.y) +
                (this.z - p.z) * (this.z - p.z)
        );
    }

    // Distance by lat & lon
    public double geoDistance( Point p2) {
        //  https://www.tabnine.com/code/java/methods/java.lang.Math/toRadians?snippet=59212f4b4758780004fb373b
        double dLat1 = this.getLat();
        double dLon1 = this.getLon();
        double dLat2 = p2.getLat();
        double dLon2 = p2.getLon();
        double deltaLat = Math.toRadians(dLat2 - dLat1);
        double deltaLon = Math.toRadians(dLon2 - dLon1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(Math.toRadians(dLat1)) * Math.cos(Math.toRadians(dLat2)) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = (6371000 * c);

        return dist;
    }
}


