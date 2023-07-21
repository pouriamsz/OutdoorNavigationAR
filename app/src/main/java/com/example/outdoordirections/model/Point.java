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
        this.z = 0.0;
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

    public Point add(Point p){
        Point pc = new Point(this.x, this.y);
        pc.x = pc.x+p.x;
        pc.y = pc.y+p.y;

        return pc;
    }

    public Point mulScalar(double s){
        Point pc = new Point(this.x, this.y);
        pc.x = pc.x*s;
        pc.y = pc.y*s;

        return pc;
    }

    public double distance(Point p){
        return Math.sqrt((this.x-p.x)*(this.x-p.x)+(this.y-p.y)*(this.y-p.y));
    }

    public Point sub(Point p){
        Point pc = new Point(this.x, this.y);
        pc.x = p.x-pc.x;
        pc.y = p.y-pc.y;

        return new Point(pc.x, pc.y);
    }

    public Point normalize(){
        Point pc = new Point(this.x, this.y);
        double k = pc.norm();
        pc.x /= k;
        pc.y /= k;

        return pc;
    }

    public double norm(){
        return Math.sqrt(this.norm2());
    }

    public double norm2(){
        return this.x*this.x +
                this.y*this.y +
                this.z*this.z;

    }

    public double dot(Point p1){
        return (this.x*p1.x)+(this.y*p1.y);
    }
}
