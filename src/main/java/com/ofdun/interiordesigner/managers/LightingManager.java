package com.ofdun.interiordesigner.managers;

import com.ofdun.interiordesigner.models.LightSource;
import com.ofdun.interiordesigner.models.Mesh;
import com.ofdun.interiordesigner.models.ShadowRayTracer;
import jakarta.inject.Singleton;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Singleton
@NoArgsConstructor
public class LightingManager {
    private final List<LightSource> lightSources = new ArrayList<>();

    public void setSingleLightSource(LightSource lightSource) {
        lightSources.clear();
        if (lightSource != null) {
            lightSources.add(lightSource);
        }
    }

    public void clearLights() {
        lightSources.clear();
    }

    public void updateLightSourcePosition(String meshId, Point3D newPosition) {
        for (LightSource light : lightSources) {
            if (meshId != null && meshId.equals(light.getSourceMeshId())) {
                LightSource updatedLight = new LightSource(newPosition, Color.WHITE, light.getIntensity(), meshId);
                lightSources.remove(light);
                lightSources.add(updatedLight);
                break;
            }
        }
    }

    public Color calculateLightingWithShadows(Point3D surfacePoint, Point3D surfaceNormal,
                                            Point3D viewDirection, Color materialColor,
                                            List<Mesh> allMeshes) {
        if (lightSources.isEmpty()) {
            return materialColor;
        }

        Color finalColor = Color.BLACK;

        for (LightSource light : lightSources) {
            double lightDistance = light.getPosition().distance(surfacePoint);

            boolean isInShadow = false;

            if (lightDistance > 0.1) {
                isInShadow = ShadowRayTracer.isPointInShadow(
                    surfacePoint, light.getPosition(), allMeshes, light.getSourceMeshId());
            }

            Color lightContribution;
            if (isInShadow) {
                lightContribution = light.calculateShadowLighting(surfacePoint, surfaceNormal,
                        materialColor);
            } else {
                lightContribution = light.calculateLighting(surfacePoint, surfaceNormal,
                                                          viewDirection, materialColor);
            }

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

    public List<LightSource> getLightSources() {
        return List.copyOf(lightSources);
    }
}
