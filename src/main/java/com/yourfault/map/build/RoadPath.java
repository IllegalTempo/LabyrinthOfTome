package com.yourfault.map.build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RoadPath {
    private static final RoadPath EMPTY = new RoadPath(Collections.emptyList());

    private final List<RoadPoint> samples;
    private final double[] cumulativeDistances;
    private final double length;

    public RoadPath(List<RoadPoint> samples) {
        if (samples == null || samples.size() < 2) {
            this.samples = Collections.emptyList();
            this.cumulativeDistances = new double[]{0.0};
            this.length = 0.0;
            return;
        }
        this.samples = Collections.unmodifiableList(new ArrayList<>(samples));
        this.cumulativeDistances = new double[this.samples.size()];
        double total = 0.0;
        cumulativeDistances[0] = 0.0;
        for (int i = 1; i < this.samples.size(); i++) {
            RoadPoint previous = this.samples.get(i - 1);
            RoadPoint current = this.samples.get(i);
            double dx = current.x() - previous.x();
            double dz = current.z() - previous.z();
            total += Math.hypot(dx, dz);
            cumulativeDistances[i] = total;
        }
        this.length = total;
    }

    public static RoadPath empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return samples.isEmpty();
    }

    public List<RoadPoint> samples() {
        return samples;
    }

    public double length() {
        return length;
    }

    public RoadPoint start() {
        return isEmpty() ? null : samples.get(0);
    }

    public RoadPoint end() {
        return isEmpty() ? null : samples.get(samples.size() - 1);
    }

    public RoadPoint sampleAtFraction(double fraction) {
        if (isEmpty()) {
            return null;
        }
        if (length <= 0.0) {
            return start();
        }
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        double distance = clamped * length;
        return sampleAtDistance(distance);
    }

    public RoadPoint sampleAtDistance(double distance) {
        if (isEmpty()) {
            return null;
        }
        if (length <= 0.0) {
            return start();
        }
        double clamped = Math.max(0.0, Math.min(length, distance));
        int upperIndex = 1;
        while (upperIndex < cumulativeDistances.length && cumulativeDistances[upperIndex] < clamped) {
            upperIndex++;
        }
        if (upperIndex >= cumulativeDistances.length) {
            return end();
        }
        int lowerIndex = upperIndex - 1;
        double lowerDistance = cumulativeDistances[lowerIndex];
        double segmentLength = Math.max(0.0001, cumulativeDistances[upperIndex] - lowerDistance);
        double t = (clamped - lowerDistance) / segmentLength;
        RoadPoint startPoint = samples.get(lowerIndex);
        RoadPoint endPoint = samples.get(upperIndex);
        double x = startPoint.x() + (endPoint.x() - startPoint.x()) * t;
        double z = startPoint.z() + (endPoint.z() - startPoint.z()) * t;
        return new RoadPoint(x, z);
    }
}
