package com.yourfault.utils;

import java.util.Random;

public class PerlinNoise {
    private final int[] permutation = new int[512];

    public PerlinNoise(long seed) {
        Random random = new Random(seed);
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }
        for (int i = 255; i >= 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }
        for (int i = 0; i < 512; i++) {
            permutation[i] = p[i & 255];
        }
    }

    public double sample(double x, double z) {
        return noise(x, 0.0, z);
    }

    public double sample3D(double x, double y, double z) {
        return noise(x, y, z);
    }

    private double noise(double x, double y, double z) {
        int xi = fastFloor(x) & 255;
        int yi = fastFloor(y) & 255;
        int zi = fastFloor(z) & 255;

        double xf = x - fastFloor(x);
        double yf = y - fastFloor(y);
        double zf = z - fastFloor(z);

        double u = fade(xf);
        double v = fade(yf);
        double w = fade(zf);

        int aaa = permutation[permutation[permutation[xi] + yi] + zi];
        int aba = permutation[permutation[permutation[xi] + yi + 1] + zi];
        int aab = permutation[permutation[permutation[xi] + yi] + zi + 1];
        int abb = permutation[permutation[permutation[xi] + yi + 1] + zi + 1];
        int baa = permutation[permutation[permutation[xi + 1] + yi] + zi];
        int bba = permutation[permutation[permutation[xi + 1] + yi + 1] + zi];
        int bab = permutation[permutation[permutation[xi + 1] + yi] + zi + 1];
        int bbb = permutation[permutation[permutation[xi + 1] + yi + 1] + zi + 1];

        double x1 = lerp(u, grad(aaa, xf, yf, zf), grad(baa, xf - 1, yf, zf));
        double x2 = lerp(u, grad(aba, xf, yf - 1, zf), grad(bba, xf - 1, yf - 1, zf));
        double y1 = lerp(v, x1, x2);

        double x3 = lerp(u, grad(aab, xf, yf, zf - 1), grad(bab, xf - 1, yf, zf - 1));
        double x4 = lerp(u, grad(abb, xf, yf - 1, zf - 1), grad(bbb, xf - 1, yf - 1, zf - 1));
        double y2 = lerp(v, x3, x4);

        return lerp(w, y1, y2);
    }

    private static int fastFloor(double value) {
        return value >= 0 ? (int) value : (int) value - 1;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
