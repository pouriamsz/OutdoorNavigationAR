package com.example.outdoordirections.model;

public class LocalConverter {
    Point center;
    double rotation;

    public LocalConverter(Point center, double rotation) {
        this.rotation = rotation;
        this.center = center;
    }

    public Point toCartesian(double x, double y, double lat, double lng) {
        Point geoCoordinate = new Point(x, y, lat, lng);
        Point nonRotated = this.center.toCartesianCoordinateNoRotation(geoCoordinate);
        Point rotated = this.rotate(nonRotated, this.rotation);
        return rotated;
    }

    public Point toCoordinate(double x, double y) {
        Point coordinate = new Point(x, y, 0, 0);
        Point rotatedPoints = this.rotate(coordinate, -this.rotation);
        return this.center.toCoordinateNoRotation(rotatedPoints);
    }

    public Point rotate(Point cartesianCoordinates, double radians) {
        double ox = this.center.x;
        double oy = this.center.y;
        double px = cartesianCoordinates.x;
        double py = cartesianCoordinates.y;

        double qx = ox + Math.cos(radians) * (px - ox) - Math.sin(radians) * (py - oy);
        double qy = oy + Math.sin(radians) * (px - ox) + Math.cos(radians) * (py - oy);

        return new Point(qx, qy, 0, 0);
    }

}
