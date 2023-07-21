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

    public int findNear(Point p){
        int minIndex = 0;
        double minValue = points.get(0).distance(p);
        for (int i = 1; i < points.size() ; i++) {
            double d = points.get(i).distance(p);
            if (d<minValue){
                minValue = d;
                minIndex = i;
            }
        }

        return minIndex;
    }

    public int next(int i){
        return i+1;
    }

    public boolean finish(int i){
        if (i+1 >= points.size()){
            return true;
        }

        return false;
    }
}
