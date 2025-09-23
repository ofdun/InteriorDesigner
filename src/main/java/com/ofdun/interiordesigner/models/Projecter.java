package com.ofdun.interiordesigner.models;

import javafx.geometry.Point3D;

public class Projecter {
    private final Camera camera;
    private final Integer width;
    private final Integer height;

    public Projecter(Camera camera, Integer width, Integer height) {
        this.camera = camera;
        this.width = width;
        this.height = height;
    }

    public Point3D project(Point3D point) {
        var relative = new Point3D(
                point.getX() - camera.getPosition().getX(),
                point.getY() - camera.getPosition().getY(),
                point.getZ() - camera.getPosition().getZ()
        );

        var xCam = relative.dotProduct(camera.getRight());
        var yCam = relative.dotProduct(camera.getUp());
        var zCam = relative.dotProduct(camera.getDir());

        zCam = Math.max(zCam, 1e-6);

        double x = xCam / zCam;
        double y = yCam / zCam;

        double screenX = (x + 1) * width / 2;
        double screenY = (1 - y) * height / 2;

        return new Point3D(screenX, screenY, zCam);
    }
}
