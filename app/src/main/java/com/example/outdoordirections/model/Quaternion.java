package com.example.outdoordirections.model;

import com.google.ar.sceneform.math.Vector3;

public class Quaternion {
    com.google.ar.sceneform.math.Quaternion quaternion;

    public Quaternion(com.google.ar.sceneform.math.Quaternion q){
        this.quaternion = q;
    }

    public com.google.ar.sceneform.math.Quaternion getQuaternion() {
        return quaternion;
    }

    public Vector3 normal(){
        Vector3 eu = this.quaternion2Euler();
        double x = Math.cos(eu.x)*Math.cos(eu.y);
        double y = Math.sin(eu.x)*Math.sin(eu.y);
        double z = Math.sin(eu.y);

        return new Vector3((float)x, (float)y, (float)z);
    }

    public Vector3 quaternion2Euler(){
        Quaternion r = this.unit();
        double phi = Math.atan2(2*(r.quaternion.w*r.quaternion.x+r.quaternion.y*r.quaternion.z),
                1-2*(r.quaternion.x*r.quaternion.x+r.quaternion.y*r.quaternion.y));
        double theta = Math.asin(2 * (r.quaternion.w*r.quaternion.y - r.quaternion.z*r.quaternion.x));
        double psi = Math.atan2(2*(r.quaternion.x*r.quaternion.y+r.quaternion.w*r.quaternion.z),
                1-2*(r.quaternion.y*r.quaternion.y+r.quaternion.z*r.quaternion.z));

        return new Vector3((float)phi, (float)theta, (float)psi);
    }

    public Quaternion unit(){
        double k = this.norm();
        Quaternion qc = new Quaternion(this.quaternion);
        qc.quaternion.w /= k;
        qc.quaternion.x /= k;
        qc.quaternion.y /= k;
        qc.quaternion.z /= k;

        return qc;
    }

    public double norm(){
        return Math.sqrt(this.norm2());
    }

    public double norm2(){
        return this.quaternion.w*this.quaternion.w +
                this.quaternion.x*this.quaternion.x +
                this.quaternion.y*this.quaternion.y +
                this.quaternion.z*this.quaternion.z;

    }

    public Quaternion setFrom2Vec(Point p1, Point p2){
        p1 = p1.normalize();
        p2 = p2.normalize();

        double r = p1.dot(p2)+1;

        if (r < 0.00001) {
            r = 0;
            if (Math.abs(p1.x) > Math.abs(p1.z)) {
                this.quaternion.x = (float)(-p1.y);
                this.quaternion.y = (float)(p1.x);
                this.quaternion.z = 0.0f;
                this.quaternion.w = (float)r;
            }else{
                this.quaternion.x = 0.0f;
                this.quaternion.y = (float)(-p1.z);
                this.quaternion.z = (float)p1.y;
                this.quaternion.w = (float)r;
            }
        }else{
            this.quaternion.x = (float)(p1.y*p2.z - p1.z*p2.y);
            this.quaternion.y = (float)(p1.z*p2.x - p1.x*p2.z);
            this.quaternion.z = (float)(p1.x*p2.y - p1.y*p2.x);
            this.quaternion.w = (float)r;
        }

        return this.unit();
    }

}
