package com.ofdun.interiordesigner.strategies;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * Интерфейс для различных стратегий рендеринга треугольника.
 * Позволяет переключаться между версией с профилированием и без него.
 */
public interface RenderingStrategy {

    /**
     * Рендерит один пиксель треугольника
     */
    void renderPixel(int x, int y, PixelRenderContext context);

    /**
     * Рендерит строку пикселей треугольника (для параллельного рендеринга)
     */
    void renderPixelParallel(int x, int y, PixelRenderContext context);

    /**
     * Вызывается перед началом рендеринга треугольника
     */
    default void beforeTriangleRender(String meshId) {}

    /**
     * Вызывается после завершения рендеринга треугольника
     */
    default void afterTriangleRender(String meshId) {}

    /**
     * Вызывается перед началом рендеринга всей сцены
     */
    default void beforeSceneRender() {}

    /**
     * Вызывается после завершения рендеринга всей сцены
     */
    default void afterSceneRender() {}

    /**
     * Выводит статистику (если применимо)
     */
    default void printStatistics() {}

    /**
     * Сбрасывает статистику
     */
    default void resetStatistics() {}

    /**
     * Контекст для рендеринга одного пикселя
     */
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

    /**
     * Данные для рендеринга (освещение, текстуры и т.д.)
     */
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

    /**
     * Интерфейс для отрисовки пикселя
     */
    @FunctionalInterface
    interface PixelConsumer {
        void drawPixel(int x, int y, Color color);
    }

    /**
     * Интерфейс для доступа к Z-буферу
     */
    @FunctionalInterface
    interface ZBufferAccess {
        boolean testAndSet(int x, int y, double z);
    }
}

