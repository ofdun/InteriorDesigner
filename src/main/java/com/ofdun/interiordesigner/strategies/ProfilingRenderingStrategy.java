package com.ofdun.interiordesigner.strategies;

import com.ofdun.interiordesigner.managers.LightingManager;
import com.ofdun.interiordesigner.models.Mesh;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProfilingRenderingStrategy implements RenderingStrategy {

    private static final double EPSILON = 1e-6;

    private final Map<String, Long> zBufferTimePerMesh = new ConcurrentHashMap<>();
    private final Map<String, Integer> zBufferCallsPerMesh = new ConcurrentHashMap<>();

    private final Map<String, Long> interpolateNormalsTimePerMesh = new ConcurrentHashMap<>();
    private final Map<String, Integer> interpolateNormalsCallsPerMesh = new ConcurrentHashMap<>();

    private final Map<String, Long> interpolateWorldPosTimePerMesh = new ConcurrentHashMap<>();
    private final Map<String, Integer> interpolateWorldPosCallsPerMesh = new ConcurrentHashMap<>();

    private final Map<String, Long> textureSamplingTimePerMesh = new ConcurrentHashMap<>();
    private final Map<String, Integer> textureSamplingCallsPerMesh = new ConcurrentHashMap<>();

    private final Map<String, Long> lightingTimePerMesh = new ConcurrentHashMap<>();
    private final Map<String, Integer> lightingCallsPerMesh = new ConcurrentHashMap<>();

    private final Map<String, Long> renderTriangleTimePerMesh = new ConcurrentHashMap<>();
    private final Map<String, Integer> renderTriangleCallsPerMesh = new ConcurrentHashMap<>();

    @Override
    public void renderPixel(int x, int y, PixelRenderContext context) {
        renderPixelInternal(x, y, context, context.pixelConsumer());
    }

    @Override
    public void renderPixelParallel(int x, int y, PixelRenderContext context) {
        renderPixelInternal(x, y, context, context.pixelConsumer());
    }

    private void renderPixelInternal(int x, int y, PixelRenderContext context, PixelConsumer consumer) {
        Point3D[] screenPoints = context.screenPoints();
        BarycentricCoordinates coords = calculateBarycentricCoordinates(
            x, y, screenPoints[0], screenPoints[1], screenPoints[2], context.barycentricDenominator()
        );

        if (coords.isInsideTriangle()) {
            double z1 = screenPoints[0].getZ();
            double z2 = screenPoints[1].getZ();
            double z3 = screenPoints[2].getZ();

            double oneOverZ = coords.w1 / z1 + coords.w2 / z2 + coords.w3 / z3;
            double z = 1.0 / oneOverZ;

            long zbufferStart = System.nanoTime();
            boolean passedZTest = context.zBuffer().testAndSet(x, y, z);
            long zbufferEnd = System.nanoTime();
            recordTime(zBufferTimePerMesh, zBufferCallsPerMesh, context.meshId(), zbufferEnd - zbufferStart);

            if (passedZTest) {
                long normalsStart = System.nanoTime();
                Point3D interpolatedNormal = interpolateNormals(
                    x, y,
                    screenPoints[0], screenPoints[1], screenPoints[2],
                    context.normals()[0], context.normals()[1], context.normals()[2]
                );
                long normalsEnd = System.nanoTime();
                recordTime(interpolateNormalsTimePerMesh, interpolateNormalsCallsPerMesh,
                          context.meshId(), normalsEnd - normalsStart);

                long worldPosStart = System.nanoTime();
                Point3D interpolatedWorldPos = interpolateWorldPositionPerspectiveCorrect(
                    context.worldPoints()[0], context.worldPoints()[1], context.worldPoints()[2],
                    coords, z1, z2, z3
                );
                long worldPosEnd = System.nanoTime();
                recordTime(interpolateWorldPosTimePerMesh, interpolateWorldPosCallsPerMesh,
                          context.meshId(), worldPosEnd - worldPosStart);

                Color finalColor = calculateColorWithProfiling(interpolatedWorldPos, interpolatedNormal,
                                                               coords, z1, z2, z3, context);
                consumer.drawPixel(x, y, finalColor);
            }
        }
    }

    private Color calculateColorWithProfiling(Point3D worldPos, Point3D normal, BarycentricCoordinates coords,
                                              double z1, double z2, double z3, PixelRenderContext context) {
        RenderingData data = context.renderingData();
        LightingManager lightingManager = (LightingManager) data.lightingManager();
        @SuppressWarnings("unchecked")
        List<Mesh> allMeshes = (List<Mesh>) data.allMeshes();

        if (data.texture() != null && data.uv1() != null && data.uv2() != null && data.uv3() != null) {
            Point2D interpolatedUV = interpolateUVPerspectiveCorrect(
                data.uv1(), data.uv2(), data.uv3(), coords, z1, z2, z3
            );

            long texStart = System.nanoTime();
            Color textureColor = sampleTexture(data.texture(), interpolatedUV);
            long texEnd = System.nanoTime();
            recordTime(textureSamplingTimePerMesh, textureSamplingCallsPerMesh,
                      context.meshId(), texEnd - texStart);

            long lightStart = System.nanoTime();
            Color result = lightingManager.calculateLightingWithShadows(
                worldPos, normal, data.viewDirection(), textureColor, allMeshes
            );
            long lightEnd = System.nanoTime();
            recordTime(lightingTimePerMesh, lightingCallsPerMesh,
                      context.meshId(), lightEnd - lightStart);

            return result;
        } else {
            long lightStart = System.nanoTime();
            Color result = lightingManager.calculateLightingWithShadows(
                worldPos, normal, data.viewDirection(), data.materialColor(), allMeshes
            );
            long lightEnd = System.nanoTime();
            recordTime(lightingTimePerMesh, lightingCallsPerMesh,
                      context.meshId(), lightEnd - lightStart);

            return result;
        }
    }

    private void recordTime(Map<String, Long> timeMap, Map<String, Integer> callsMap,
                           String meshId, long timeNanos) {
        timeMap.merge(meshId, timeNanos, Long::sum);
        callsMap.merge(meshId, 1, Integer::sum);
    }

    @Override
    public void printStatistics() {
        System.out.println("\n" + "=".repeat(130));
        System.out.println("                              СТАТИСТИКА ПРОИЗВОДИТЕЛЬНОСТИ РЕНДЕРИНГА ПО МОДЕЛЯМ");
        System.out.println("=".repeat(130));

        java.util.Set<String> allMeshIds = new java.util.HashSet<>();
        allMeshIds.addAll(renderTriangleTimePerMesh.keySet());
        allMeshIds.addAll(zBufferTimePerMesh.keySet());
        allMeshIds.addAll(lightingTimePerMesh.keySet());

        if (allMeshIds.isEmpty()) {
            System.out.println("Нет данных для отображения");
            System.out.println("=".repeat(130) + "\n");
            return;
        }

        for (String meshId : allMeshIds) {
            System.out.println("\n╔═ МОДЕЛЬ: " + meshId + " " + "═".repeat(Math.max(0, 115 - meshId.length())));

            printMeshOperationStat("  Рендер треугольников",
                renderTriangleTimePerMesh.getOrDefault(meshId, 0L),
                renderTriangleCallsPerMesh.getOrDefault(meshId, 0));

            printMeshOperationStat("  Z-Buffer тест",
                zBufferTimePerMesh.getOrDefault(meshId, 0L),
                zBufferCallsPerMesh.getOrDefault(meshId, 0));

            printMeshOperationStat("  Интерполяция нормалей",
                interpolateNormalsTimePerMesh.getOrDefault(meshId, 0L),
                interpolateNormalsCallsPerMesh.getOrDefault(meshId, 0));

            printMeshOperationStat("  Интерполяция мировых координат",
                interpolateWorldPosTimePerMesh.getOrDefault(meshId, 0L),
                interpolateWorldPosCallsPerMesh.getOrDefault(meshId, 0));

            printMeshOperationStat("  Сэмплинг текстур",
                textureSamplingTimePerMesh.getOrDefault(meshId, 0L),
                textureSamplingCallsPerMesh.getOrDefault(meshId, 0));

            printMeshOperationStat("  Вычисление освещения",
                lightingTimePerMesh.getOrDefault(meshId, 0L),
                lightingCallsPerMesh.getOrDefault(meshId, 0));

            long totalForMesh = renderTriangleTimePerMesh.getOrDefault(meshId, 0L) +
                               zBufferTimePerMesh.getOrDefault(meshId, 0L) +
                               interpolateNormalsTimePerMesh.getOrDefault(meshId, 0L) +
                               interpolateWorldPosTimePerMesh.getOrDefault(meshId, 0L) +
                               textureSamplingTimePerMesh.getOrDefault(meshId, 0L) +
                               lightingTimePerMesh.getOrDefault(meshId, 0L);

            double totalMeshMs = totalForMesh / 1_000_000.0;
            System.out.println("  " + "─".repeat(126));
            System.out.printf("  %-40s | Всего: %10.2f мс%n", "ИТОГО для модели:", totalMeshMs);
            System.out.println("╚" + "═".repeat(128));
        }

        System.out.println("\n>>> ИТОГОВАЯ СТАТИСТИКА ПО ВСЕМ МОДЕЛЯМ:");
        System.out.println("─".repeat(130));

        long grandTotal = 0;
        for (Long time : renderTriangleTimePerMesh.values()) grandTotal += time;
        for (Long time : zBufferTimePerMesh.values()) grandTotal += time;
        for (Long time : interpolateNormalsTimePerMesh.values()) grandTotal += time;
        for (Long time : interpolateWorldPosTimePerMesh.values()) grandTotal += time;
        for (Long time : textureSamplingTimePerMesh.values()) grandTotal += time;
        for (Long time : lightingTimePerMesh.values()) grandTotal += time;

        double grandTotalMs = grandTotal / 1_000_000.0;
        System.out.printf("  Общее время всех операций: %.2f мс (%.3f сек)%n", grandTotalMs, grandTotalMs / 1000.0);

        System.out.println("=".repeat(130) + "\n");
    }

    private void printMeshOperationStat(String operationName, long timeNanos, int calls) {
        if (calls == 0) {
            System.out.printf("  %-40s | Не использовалось%n", operationName);
            return;
        }

        double timeMs = timeNanos / 1_000_000.0;
        double avgTimeUs = (timeNanos / 1000.0) / calls;

        System.out.printf("  %-40s | Всего: %10.2f мс | Вызовов: %8d | Среднее: %7.2f мкс%n",
                operationName, timeMs, calls, avgTimeUs);
    }

    @Override
    public void resetStatistics() {
        zBufferTimePerMesh.clear();
        zBufferCallsPerMesh.clear();
        interpolateNormalsTimePerMesh.clear();
        interpolateNormalsCallsPerMesh.clear();
        interpolateWorldPosTimePerMesh.clear();
        interpolateWorldPosCallsPerMesh.clear();
        textureSamplingTimePerMesh.clear();
        textureSamplingCallsPerMesh.clear();
        lightingTimePerMesh.clear();
        lightingCallsPerMesh.clear();
        renderTriangleTimePerMesh.clear();
        renderTriangleCallsPerMesh.clear();
    }

    private Point2D interpolateUVPerspectiveCorrect(Point2D uv1, Point2D uv2, Point2D uv3,
                                                    BarycentricCoordinates coords,
                                                    double z1, double z2, double z3) {
        double oneOverZ = coords.w1 / z1 + coords.w2 / z2 + coords.w3 / z3;

        double uOverZ = coords.w1 * uv1.getX() / z1 + coords.w2 * uv2.getX() / z2 + coords.w3 * uv3.getX() / z3;
        double vOverZ = coords.w1 * uv1.getY() / z1 + coords.w2 * uv2.getY() / z2 + coords.w3 * uv3.getY() / z3;

        return new Point2D(uOverZ / oneOverZ, vOverZ / oneOverZ);
    }

    private Color sampleTexture(Image texture, Point2D uv) {
        if (texture == null) {
            return Color.GRAY;
        }

        int width = (int) texture.getWidth();
        int height = (int) texture.getHeight();

        double u = uv.getX() - Math.floor(uv.getX());
        double v = 1.0 - (uv.getY() - Math.floor(uv.getY()));

        int texX = (int) (u * (width - 1));
        int texY = (int) (v * (height - 1));

        texX = Math.max(0, Math.min(width - 1, texX));
        texY = Math.max(0, Math.min(height - 1, texY));

        try {
            return texture.getPixelReader().getColor(texX, texY);
        } catch (Exception e) {
            return Color.GRAY;
        }
    }

    private Point3D interpolateWorldPositionPerspectiveCorrect(Point3D p1, Point3D p2, Point3D p3,
                                                              BarycentricCoordinates coords,
                                                              double z1, double z2, double z3) {
        double oneOverZ = coords.w1 / z1 + coords.w2 / z2 + coords.w3 / z3;

        double xOverZ = coords.w1 * p1.getX() / z1 + coords.w2 * p2.getX() / z2 + coords.w3 * p3.getX() / z3;
        double yOverZ = coords.w1 * p1.getY() / z1 + coords.w2 * p2.getY() / z2 + coords.w3 * p3.getY() / z3;
        double zOverZ = coords.w1 * p1.getZ() / z1 + coords.w2 * p2.getZ() / z2 + coords.w3 * p3.getZ() / z3;

        return new Point3D(
            xOverZ / oneOverZ,
            yOverZ / oneOverZ,
            zOverZ / oneOverZ
        );
    }

    private BarycentricCoordinates calculateBarycentricCoordinates(int x, int y,
                                                                  Point3D p1, Point3D p2, Point3D p3,
                                                                  double barycentricDenominator) {
        double w1 = ((p2.getY() - p3.getY()) * (x - p3.getX()) +
                    (p3.getX() - p2.getX()) * (y - p3.getY())) / barycentricDenominator;
        double w2 = ((p3.getY() - p1.getY()) * (x - p3.getX()) +
                    (p1.getX() - p3.getX()) * (y - p3.getY())) / barycentricDenominator;
        double w3 = 1 - w1 - w2;

        return new BarycentricCoordinates(w1, w2, w3);
    }

    private Point3D interpolateNormals(int x, int y, Point3D pA, Point3D pB, Point3D pC,
                                       Point3D nA, Point3D nB, Point3D nC) {
        double totalHeight = pC.getY() - pA.getY();
        if (Math.abs(totalHeight) < EPSILON) {
            return nA.normalize();
        }

        Point3D p1, p2, n1, n2;
        double u;
        double height;

        if (y <= pB.getY()) {
            p1 = pA;
            p2 = pB;
            n1 = nA;
            n2 = nB;
        } else {
            p1 = pB;
            p2 = pC;
            n1 = nB;
            n2 = nC;
        }
        height = p2.getY() - p1.getY();
        u = Math.abs(height) < EPSILON ? 0 : (y - p1.getY()) / height;
        u = Math.max(0, Math.min(1, u));

        Point3D nQ = new Point3D(
            u * n2.getX() + (1 - u) * n1.getX(),
            u * n2.getY() + (1 - u) * n1.getY(),
            u * n2.getZ() + (1 - u) * n1.getZ()
        );

        double w = (y - pA.getY()) / totalHeight;
        w = Math.max(0, Math.min(1, w));

        Point3D nR = new Point3D(
            w * nC.getX() + (1 - w) * nA.getX(),
            w * nC.getY() + (1 - w) * nA.getY(),
            w * nC.getZ() + (1 - w) * nA.getZ()
        );

        double qX = u * p2.getX() + (1 - u) * p1.getX();
        double rX = w * pC.getX() + (1 - w) * pA.getX();
        double xDiff = rX - qX;
        double t = Math.abs(xDiff) < EPSILON ? 0.5 : (x - qX) / xDiff;
        t = Math.max(0, Math.min(1, t));

        Point3D nP = new Point3D(
            t * nQ.getX() + (1 - t) * nR.getX(),
            t * nQ.getY() + (1 - t) * nR.getY(),
            t * nQ.getZ() + (1 - t) * nR.getZ()
        );

        return nP.normalize();
    }

    private record BarycentricCoordinates(double w1, double w2, double w3) {
        boolean isInsideTriangle() {
            return w1 >= 0 && w2 >= 0 && w3 >= 0;
        }
    }
}

