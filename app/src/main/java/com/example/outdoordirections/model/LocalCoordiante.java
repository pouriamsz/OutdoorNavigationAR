package com.example.outdoordirections.model;

public class LocalCoordiante {
    Point center;

    public LocalCoordiante(Point center) {
        this.center = center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    public Point GetLocalCoordinates(Point point, double yaw) {

        // Assuming this as user's current location and also origin (0,0) point
        Point center = new Point(0, 0,
                this.center.getLat(), this.center.getLon());

        // creating a new object converter which will take our location - center as a reference point
        // to convert the given LatLng into local coordinates. Yaw here is the rotation of user phone
        // yaw is respectively calculated according to North and -z axis of phone location.
        // you can use azimuth value from sensor data of phone.
        LocalConverter converter = new LocalConverter(center, Math.toRadians(yaw));

        // converting the lat long of the location or the poi to local coordinates
        Point mainPoint = converter.toCartesian(point);


        return mainPoint;
    }
}
