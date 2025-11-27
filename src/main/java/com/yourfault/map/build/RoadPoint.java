package com.yourfault.map.build;

public final class RoadPoint {
    private final double x;
    private final double z;

    public RoadPoint(double x, double z) {
        this.x = x;
        this.z = z;
    }

    public double x() {
        return x;
    }

    public double z() {
        return z;
    }
}
