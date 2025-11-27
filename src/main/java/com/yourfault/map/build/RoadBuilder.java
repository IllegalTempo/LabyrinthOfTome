package com.yourfault.map.build;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a primary road that spans the arena from edge to edge and exposes the
 * sampled points along the road so other systems can align with it.
 */
public class RoadBuilder {
    private static final double SAMPLE_STEP = 0.6;
    private static final double LATERAL_VARIANCE_RATIO = 0.22;
    private static final int MIN_SPINE_SAMPLES = 48;

    public RoadPath buildRoadNetwork(RoadBuildContext context) {
        if (context == null) {
            return RoadPath.empty();
        }
        double angle = context.random().nextDouble() * Math.PI * 2.0;
        List<RoadPoint> spine = createSpanPath(context, angle);
        drawPolyline(context, spine);
        maybeAddSpurs(context, spine, angle);
        return new RoadPath(spine);
    }

    private List<RoadPoint> createSpanPath(RoadBuildContext context, double angle) {
        double dirX = Math.cos(angle);
        double dirZ = Math.sin(angle);
        double perpX = -dirZ;
        double perpZ = dirX;
        int radius = context.radius();
        int sampleCount = Math.max(MIN_SPINE_SAMPLES, radius);
        List<RoadPoint> samples = new ArrayList<>(sampleCount + 1);
        RoadPoint start = new RoadPoint(
                context.centerX() - dirX * radius,
                context.centerZ() - dirZ * radius
        );
        RoadPoint end = new RoadPoint(
                context.centerX() + dirX * radius,
                context.centerZ() + dirZ * radius
        );
        samples.add(start);
        for (int i = 1; i < sampleCount; i++) {
            double t = i / (double) sampleCount;
            double axialOffset = -radius + (radius * 2.0 * t);
            double baseX = context.centerX() + dirX * axialOffset;
            double baseZ = context.centerZ() + dirZ * axialOffset;
            double wave = Math.sin(Math.PI * t);
            double lateralAmplitude = radius * LATERAL_VARIANCE_RATIO * wave;
            double lateral = (context.random().nextDouble() - 0.5) * 2.0 * lateralAmplitude;
            double candidateX = baseX + perpX * lateral;
            double candidateZ = baseZ + perpZ * lateral;
            samples.add(clampToPlayableArea(context, candidateX, candidateZ));
        }
        samples.add(end);
        return samples;
    }

    private RoadPoint clampToPlayableArea(RoadBuildContext context, double x, double z) {
        double dx = x - context.centerX();
        double dz = z - context.centerZ();
        double radiusLimit = Math.max(2.0, context.radius() - 1.5);
        double distance = Math.hypot(dx, dz);
        if (distance <= radiusLimit) {
            return new RoadPoint(x, z);
        }
        double scale = radiusLimit / Math.max(distance, 0.0001);
        double clampedX = context.centerX() + dx * scale;
        double clampedZ = context.centerZ() + dz * scale;
        return new RoadPoint(clampedX, clampedZ);
    }

    private void maybeAddSpurs(RoadBuildContext context, List<RoadPoint> spine, double mainAngle) {
        if (spine.size() < 6) {
            return;
        }
        int spurCount = context.radius() > 40 ? 2 : 1;
        double perpAngle = mainAngle + Math.PI / 2.0;
        for (int i = 0; i < spurCount; i++) {
            int index = Math.max(2, Math.min(spine.size() - 3, context.random().nextInt(spine.size())));
            RoadPoint pivot = spine.get(index);
            double angleVariation = (context.random().nextDouble() - 0.5) * (Math.PI / 6.0);
            double sign = context.random().nextBoolean() ? 1.0 : -1.0;
            double spurAngle = perpAngle * sign + angleVariation;
            double length = context.radius() * (0.35 + context.random().nextDouble() * 0.35);
            RoadPoint end = offsetFrom(pivot, spurAngle, length);
            RoadPoint clampedEnd = clampToPlayableArea(context, end.x(), end.z());
            drawSegment(context, pivot, clampedEnd);
        }
    }

    private RoadPoint offsetFrom(RoadPoint origin, double angle, double distance) {
        return new RoadPoint(
                origin.x() + Math.cos(angle) * distance,
                origin.z() + Math.sin(angle) * distance
        );
    }

    private void drawPolyline(RoadBuildContext context, List<RoadPoint> points) {
        if (points == null || points.size() < 2) {
            return;
        }
        for (int i = 0; i < points.size() - 1; i++) {
            drawSegment(context, points.get(i), points.get(i + 1));
        }
    }

    private void drawSegment(RoadBuildContext context, RoadPoint start, RoadPoint end) {
        double dx = end.x() - start.x();
        double dz = end.z() - start.z();
        double length = Math.hypot(dx, dz);
        if (length < 0.001) {
            paintSinglePoint(context, start);
            return;
        }
        double dirX = dx / length;
        double dirZ = dz / length;
        double perpX = -dirZ;
        double perpZ = dirX;
        int steps = Math.max(1, (int) Math.ceil(length / SAMPLE_STEP));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double px = start.x() + dirX * length * t;
            double pz = start.z() + dirZ * length * t;
            fillWidth(context, px, pz, perpX, perpZ);
        }
    }

    private void paintSinglePoint(RoadBuildContext context, RoadPoint point) {
        int blockX = (int) Math.round(point.x());
        int blockZ = (int) Math.round(point.z());
        if (withinRadius(context, blockX, blockZ)) {
            context.painter().paint(blockX, blockZ, context.material());
        }
    }

    private void fillWidth(RoadBuildContext context, double centerX, double centerZ, double perpX, double perpZ) {
        int halfWidth = context.halfWidth();
        Material material = context.material();
        RoadPainter painter = context.painter();
        for (int offset = -halfWidth; offset <= halfWidth; offset++) {
            int blockX = (int) Math.round(centerX + perpX * offset);
            int blockZ = (int) Math.round(centerZ + perpZ * offset);
            if (withinRadius(context, blockX, blockZ)) {
                painter.paint(blockX, blockZ, material);
            }
        }
    }

    private boolean withinRadius(RoadBuildContext context, int x, int z) {
        int dx = x - context.centerX();
        int dz = z - context.centerZ();
        return (dx * dx) + (dz * dz) <= context.radiusSquared();
    }
}
