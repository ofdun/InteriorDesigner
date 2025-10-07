package com.ofdun.interiordesigner.models;

import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import lombok.Getter;

public class LightSource {
    @Getter
    private final Point3D position;
    private final Color color;
    @Getter
    private final double intensity;
    @Getter
    private final String sourceMeshId;
    private final double ambientStrength = 0.8;
    private final double diffuseStrength = 1.0;
    private final double specularStrength = 0.8;
    private final int shininess = 32;
    private final double ATTENUATION_COEFFICIENT = 1.0;
    private final double ATTENUATION_LINEAR_COEFFICIENT = 0.01;
    private final double ATTENUATION_QUADRO_COEFFICIENT = 0.001;

    public LightSource(Point3D position, Color color, double intensity, String sourceMeshId) {
        this.position = position;
        this.color = color;
        this.intensity = intensity;
        this.sourceMeshId = sourceMeshId;
    }

    public Color calculateLighting(Point3D surfacePoint, Point3D surfaceNormal,
                                  Point3D viewDirection, Color materialColor) {
        Point3D normal = surfaceNormal.normalize();

        Point3D lightDirection = position.subtract(surfacePoint).normalize();

        double distance = position.distance(surfacePoint);
        double attenuation = intensity / (ATTENUATION_COEFFICIENT + ATTENUATION_LINEAR_COEFFICIENT * distance + ATTENUATION_QUADRO_COEFFICIENT * distance * distance);

        Color ambient = multiplyColor(materialColor, color, ambientStrength * intensity);

        double diffuseFactor = Math.max(0.0, normal.dotProduct(lightDirection));
        Color diffuse = multiplyColor(materialColor, color, diffuseStrength * diffuseFactor * attenuation * intensity);

        Point3D reflectDirection = reflect(lightDirection.multiply(-1), normal);
        double specularFactor = Math.pow(Math.max(0.0, viewDirection.dotProduct(reflectDirection)), shininess);
        Color specular = multiplyColor(Color.WHITE, color, specularStrength * specularFactor * attenuation * intensity);

        return addColors(addColors(ambient, diffuse), specular);
    }

    public Color calculateShadowLighting(Point3D surfacePoint, Point3D surfaceNormal,
                                         Color materialColor) {
        Point3D normal = surfaceNormal.normalize();
        Point3D lightDirection = position.subtract(surfacePoint).normalize();

        double distance = position.distance(surfacePoint);
        double attenuation = intensity / (ATTENUATION_COEFFICIENT + ATTENUATION_LINEAR_COEFFICIENT * distance + ATTENUATION_QUADRO_COEFFICIENT * distance * distance);

        Color ambient = multiplyColor(materialColor, color, ambientStrength * intensity * 0.5);

        double diffuseFactor = Math.max(0.0, normal.dotProduct(lightDirection));
        Color softDiffuse = multiplyColor(materialColor, color, 0.2 * diffuseFactor * attenuation * intensity);

        return addColors(ambient, softDiffuse);
    }

    private Point3D reflect(Point3D incident, Point3D normal) {
        return incident.subtract(normal.multiply(2.0 * incident.dotProduct(normal)));
    }

    private Color multiplyColor(Color c1, Color c2, double factor) {
        return Color.color(
                Math.min(1.0, c1.getRed() * c2.getRed() * factor),
                Math.min(1.0, c1.getGreen() * c2.getGreen() * factor),
                Math.min(1.0, c1.getBlue() * c2.getBlue() * factor)
        );
    }

    private Color addColors(Color c1, Color c2) {
        return Color.color(
                Math.min(1.0, c1.getRed() + c2.getRed()),
                Math.min(1.0, c1.getGreen() + c2.getGreen()),
                Math.min(1.0, c1.getBlue() + c2.getBlue())
        );
    }
}
