package com.example.outdoordirections.model;

public class Vertex {
    double x;
    double y;
    double z;

    public Vertex(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
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

    public Vertex add(Vertex p) {
        Vertex pc = new Vertex(this.x, this.y, this.z);
        pc.x = pc.x + p.x;
        pc.y = pc.y + p.y;
        pc.z = pc.z + p.z;

        return pc;
    }

    public Vertex mulScalar(double s) {
        Vertex pc = new Vertex(this.x, this.y, this.z);
        pc.x = pc.x * s;
        pc.y = pc.y * s;
        pc.z = pc.z * s;

        return pc;
    }

    public double distance(Vertex p) {
        return Math.sqrt((this.x - p.x) * (this.x - p.x) +
                (this.y - p.y) * (this.y - p.y) +
                (this.z - p.z) * (this.z - p.z)
        );
    }

    public Vertex sub(Vertex p) {
        Vertex pc = new Vertex(this.x, this.y, this.z);
        pc.x = p.x - pc.x;
        pc.y = p.y - pc.y;
        pc.z = p.z - pc.z;

        return pc;
    }

    public Vertex normalize() {
        Vertex pc = new Vertex(this.x, this.y, this.z);
        double k = pc.norm();
        pc.x /= k;
        pc.y /= k;
        pc.z /= k;

        return pc;
    }

    public double norm() {
        return Math.sqrt(this.norm2());
    }

    public double norm2() {
        return this.x * this.x +
                this.y * this.y +
                this.z * this.z;

    }

    public double length(){
        return this.norm();
    }

    public double dot(Vertex p1) {
        return (this.x * p1.x) +
                (this.y * p1.y) +
                (this.z * p1.z);
    }
}
