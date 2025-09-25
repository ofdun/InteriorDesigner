package com.ofdun.interiordesigner.models;

import jakarta.inject.Singleton;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class LightingManager {
    private final List<LightSource> lightSources;

    public LightingManager() {
        this.lightSources = new ArrayList<>();
        addLightSource(new LightSource(new Point3D(100, 100, 50), Color.WHITE, 1.0));
    }

    public void addLightSource(LightSource lightSource) {
        lightSources.add(lightSource);
    }

    public void removeLightSource(LightSource lightSource) {
        lightSources.remove(lightSource);
    }

    public List<LightSource> getLightSources() {
        return List.copyOf(lightSources);
    }

    public Color calculateTriangleLighting(Point3D p1, Point3D p2, Point3D p3,
                                         Point3D n1, Point3D n2, Point3D n3,
                                         Point3D viewDirection, Color materialColor,
                                         double w1, double w2, double w3) {

        Point3D surfacePoint = interpolatePoint3D(p1, p2, p3, w1, w2, w3);
        Point3D interpolatedNormal = interpolatePoint3D(n1, n2, n3, w1, w2, w3).normalize();

        return calculateLighting(surfacePoint, interpolatedNormal, viewDirection, materialColor);
    }

    public Color calculateLighting(Point3D surfacePoint, Point3D surfaceNormal,
                                  Point3D viewDirection, Color materialColor) {
        if (lightSources.isEmpty()) {
            return materialColor;
        }

        Color finalColor = Color.BLACK;

        for (LightSource light : lightSources) {
            Color lightContribution = light.calculateLighting(surfacePoint, surfaceNormal,
                                                            viewDirection, materialColor);
            finalColor = addColors(finalColor, lightContribution);
        }

        return clampColor(finalColor);
    }

    private Color addColors(Color c1, Color c2) {
        return Color.color(
                c1.getRed() + c2.getRed(),
                c1.getGreen() + c2.getGreen(),
                c1.getBlue() + c2.getBlue()
        );
    }

    private Color clampColor(Color color) {
        return Color.color(
                Math.min(1.0, Math.max(0.0, color.getRed())),
                Math.min(1.0, Math.max(0.0, color.getGreen())),
                Math.min(1.0, Math.max(0.0, color.getBlue()))
        );
    }

    private Point3D interpolatePoint3D(Point3D p1, Point3D p2, Point3D p3,
                                      double w1, double w2, double w3) {
        return new Point3D(
                w1 * p1.getX() + w2 * p2.getX() + w3 * p3.getX(),
                w1 * p1.getY() + w2 * p2.getY() + w3 * p3.getY(),
                w1 * p1.getZ() + w2 * p2.getZ() + w3 * p3.getZ()
        );
    }

}
