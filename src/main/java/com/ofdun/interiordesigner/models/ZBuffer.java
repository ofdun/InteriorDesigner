package com.ofdun.interiordesigner.models;

public class ZBuffer {
    private final double[][] buffer;
    private final int width;
    private final int height;
    private final double initialValue;

    public ZBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        this.initialValue = Double.POSITIVE_INFINITY;
        this.buffer = new double[height][width];
        clear();
    }

    public void clear() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffer[y][x] = initialValue;
            }
        }
    }

    public boolean testAndSet(int x, int y, double z) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false;
        }

        if (z < buffer[y][x]) {
            buffer[y][x] = z;
            return true;
        }

        return false;
    }
}