package com.ofdun.interiordesigner.strategies;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;

import java.util.List;

public interface RenderingStrategy {
    void renderPixel(int x, int y, PixelRenderContext context);

    void renderPixelParallel(int x, int y, PixelRenderContext context);

    default void beforeTriangleRender(String meshId) {}

    default void afterTriangleRender(String meshId) {}

    default void beforeSceneRender() {}

    default void afterSceneRender() {}

    default void printStatistics() {}

    default void resetStatistics() {}

    record PixelRenderContext(
        Point3D[] screenPoints,
        Point3D[] worldPoints,
        Point3D[] normals,
        String meshId,
        double barycentricDenominator,
        RenderingData renderingData,
        PixelConsumer pixelConsumer,
        ZBufferAccess zBuffer
    ) {}

    record RenderingData(
        Point3D viewDirection,
        Color materialColor,
        Object lightingManager, // LightingManager
        List<?> allMeshes, // List<Mesh>
        javafx.scene.image.Image texture,
        Point2D uv1,
        Point2D uv2,
        Point2D uv3
    ) {}

    @FunctionalInterface
    interface PixelConsumer {
        void drawPixel(int x, int y, Color color);
    }

    @FunctionalInterface
    interface ZBufferAccess {
        boolean testAndSet(int x, int y, double z);
    }
}

