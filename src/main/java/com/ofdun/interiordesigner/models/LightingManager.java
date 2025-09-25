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

    private Color interpolateColors(Color c1, Color c2, Color c3, double w1, double w2, double w3) {
        double red = w1 * c1.getRed() + w2 * c2.getRed() + w3 * c3.getRed();
        double green = w1 * c1.getGreen() + w2 * c2.getGreen() + w3 * c3.getGreen();
        double blue = w1 * c1.getBlue() + w2 * c2.getBlue() + w3 * c3.getBlue();

        return clampColor(Color.color(red, green, blue));
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
}
