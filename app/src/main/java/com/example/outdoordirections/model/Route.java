package com.example.outdoordirections.model;

import java.util.ArrayList;

public class Route {
    ArrayList<Point> points;

    public Route(ArrayList<Point> points){
        this.points = points;
    }

    public void setPoints(ArrayList<Point> points) {
        this.points = points;
    }

    public ArrayList<Point> getPoints() {
        return this.points;
    }

    public int size(){
        return this.points.size();
    }
}
