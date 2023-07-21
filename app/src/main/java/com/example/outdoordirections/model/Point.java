package com.example.outdoordirections.model;

public class Point {
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
}
