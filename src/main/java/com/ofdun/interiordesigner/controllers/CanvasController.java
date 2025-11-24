package com.ofdun.interiordesigner.controllers;

import com.ofdun.interiordesigner.managers.LightingManager;
import com.ofdun.interiordesigner.models.Mesh;
import com.ofdun.interiordesigner.models.ZBuffer;
import com.ofdun.interiordesigner.strategies.BasicRenderingStrategy;
import com.ofdun.interiordesigner.strategies.ProfilingRenderingStrategy;
import com.ofdun.interiordesigner.strategies.RenderingStrategy;
import jakarta.inject.Singleton;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

@Singleton
public class CanvasController {
    private static final double EPSILON = 1e-6;

    @Setter
    @Getter
    private static int renderThreadCount = 12;
    private static ExecutorService renderExecutor = null;
    private static final int MIN_SCANLINES_FOR_PARALLEL = 20;

    private record PixelData(int x, int y, Color color) {}
    private final ConcurrentHashMap<Long, PixelData> pixelBuffer = new ConcurrentHashMap<>();

    @Setter
    @Getter
    private RenderingStrategy renderingStrategy = new BasicRenderingStrategy();

    private final Logger log = LoggerFactory.getLogger(CanvasController.class);
    private final Map<String, Runnable> eventCallbacks = new HashMap<>();
    private final Map<String, BiConsumer<Double, Double>> mouseEventCallbacks = new HashMap<>();

    @FXML
    private Canvas canvas;
    private Canvas invisibleCanvas;
    private final ZBuffer zBuffer = new ZBuffer(1250, 800);

    private GraphicsContext graphicsContext;
    private GraphicsContext invisibleGraphicsContext;

    private boolean isDragging = false;
    private double lastMouseX;
    private double lastMouseY;

    public static void configureRenderThreads(int threadCount) {
        if (threadCount < 1) {
            throw new IllegalArgumentException("Thread count must be at least 1");
        }

        if (renderExecutor != null && !renderExecutor.isShutdown()) {
            renderExecutor.shutdown();
            try {
                if (!renderExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    renderExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                renderExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        renderThreadCount = threadCount;
        renderExecutor = Executors.newFixedThreadPool(threadCount);
        System.out.printf("Render thread pool configured: %d threads%n", threadCount);
    }

    private static void ensureExecutorInitialized() {
        if (renderExecutor == null) {
            renderExecutor = Executors.newFixedThreadPool(renderThreadCount);
        }
    }

    @FXML
    public void initialize() {
        log.debug("CanvasController.initialize() called, canvas={}", canvas != null);
        if (canvas != null) {
            graphicsContext = canvas.getGraphicsContext2D();
            invisibleCanvas = new Canvas(canvas.getWidth(), canvas.getHeight());
            invisibleGraphicsContext = invisibleCanvas.getGraphicsContext2D();
            log.debug("Canvas initialized: size={}x{}", canvas.getWidth(), canvas.getHeight());
            setupCanvas();
        } else {
            log.warn("Canvas is null in initialize(), skipping setup");
        }
    }

    private void setupCanvas() {
        fillDefaultColor();
        invisibleGraphicsContext.setStroke(Color.GRAY);
        invisibleGraphicsContext.setLineWidth(2);
        render();
    }

    public void render() {
        renderingStrategy.afterSceneRender();
        if (graphicsContext != null && invisibleCanvas != null) {
            graphicsContext.drawImage(invisibleCanvas.snapshot(null, null), 0, 0);
            clearInvisibleCanvas();
        } else {
            log.debug("render skipped: graphicsContext={}, invisibleCanvas={}",
                     graphicsContext != null, invisibleCanvas != null);
        }
    }

    public void enableProfiling() {
        this.renderingStrategy = new ProfilingRenderingStrategy();
    }

    public void disableProfiling() {
        this.renderingStrategy = new BasicRenderingStrategy();
    }

    public void printPerformanceStatistics() {
        renderingStrategy.printStatistics();
    }

    public void resetPerformanceStatistics() {
        renderingStrategy.resetStatistics();
    }

    public void prepareForRender() {
        zBuffer.clear();
    }

    private void fillDefaultColor() {
        if (invisibleGraphicsContext != null && invisibleCanvas != null) {
            invisibleGraphicsContext.setFill(Color.WHITE);
            invisibleGraphicsContext.fillRect(0, 0, invisibleCanvas.getWidth(), invisibleCanvas.getHeight());
        } else {
            log.debug("fillDefaultColor skipped: invisibleGraphicsContext={}, invisibleCanvas={}",
                     invisibleGraphicsContext != null, invisibleCanvas != null);
        }
    }

    private void clearInvisibleCanvas() {
        zBuffer.clear();
        if (invisibleGraphicsContext != null && invisibleCanvas != null) {
            invisibleGraphicsContext.clearRect(0, 0, invisibleCanvas.getWidth(), invisibleCanvas.getHeight());
            fillDefaultColor();
        } else {
            log.debug("clearInvisibleCanvas skipped: invisibleGraphicsContext={}, invisibleCanvas={}",
                     invisibleGraphicsContext != null, invisibleCanvas != null);
        }
    }

    public void bindEventCallback(String eventName, Runnable callback) {
        eventCallbacks.put(eventName, callback);
    }

    public void bindMouseEventCallback(String eventName, BiConsumer<Double, Double> callback) {
        mouseEventCallbacks.put(eventName, callback);
    }

    private void handleEvent(String eventName) {
        var callback = eventCallbacks.get(eventName);
        if (callback != null) {
            callback.run();
        }
    }

    private void handleMouseEvent(String eventName, double deltaX, double deltaY) {
        var callback = mouseEventCallbacks.get(eventName);
        if (callback != null) {
            callback.accept(deltaX, deltaY);
        }
    }

    @FXML
    public void onMousePressed(MouseEvent mouseEvent) {
        if (mouseEvent.isPrimaryButtonDown()) {
            isDragging = true;
            lastMouseX = mouseEvent.getX();
            lastMouseY = mouseEvent.getY();
        }
    }

    @FXML
    public void onMouseDragged(MouseEvent mouseEvent) {
        if (isDragging) {
            double deltaX = mouseEvent.getX() - lastMouseX;
            double deltaY = lastMouseY - mouseEvent.getY();

            double sensitivity = 0.01;
            double yawAngle = -deltaX * sensitivity;
            double pitchAngle = -deltaY * sensitivity;

            handleMouseEvent("cameraOrbit", yawAngle, pitchAngle);

            lastMouseX = mouseEvent.getX();
            lastMouseY = mouseEvent.getY();

            handleEvent("render");
        }
    }

    @FXML
    public void onMouseReleased(MouseEvent mouseEvent) {
        isDragging = false;
    }

    private void drawPoint(Point2D point, Paint paint) {
        if (invisibleGraphicsContext == null) {
            log.trace("drawPoint skipped: invisibleGraphicsContext is null");
            return;
        }
        invisibleGraphicsContext.setStroke(paint);
        invisibleGraphicsContext.strokeLine(point.getX(), point.getY(), point.getX(),  point.getY());
    }

    private void bufferPixel(int x, int y, Color color) {
        long key = ((long)y << 32) | (x & 0xFFFFFFFFL);
        pixelBuffer.put(key, new PixelData(x, y, color));
    }

    private void flushPixelBuffer() {
        if (invisibleGraphicsContext == null) {
            log.debug("flushPixelBuffer skipped: invisibleGraphicsContext is null, buffered pixels: {}", pixelBuffer.size());
            pixelBuffer.clear();
            return;
        }
        int pixelCount = pixelBuffer.size();
        for (PixelData pixel : pixelBuffer.values()) {
            invisibleGraphicsContext.setStroke(pixel.color);
            invisibleGraphicsContext.strokeLine(pixel.x, pixel.y, pixel.x, pixel.y);
        }
        pixelBuffer.clear();
        log.trace("Flushed {} pixels to canvas", pixelCount);
    }

    public void drawLine(Point3D p1, Point3D p2, Paint paint, double lineWidth) {
        if (invisibleGraphicsContext == null) {
            log.trace("drawLine skipped: invisibleGraphicsContext is null");
            return;
        }
        invisibleGraphicsContext.setStroke(paint);
        invisibleGraphicsContext.setLineWidth(lineWidth);
        invisibleGraphicsContext.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public void drawTriangleWithLighting(TriangleLightingData triangleData) {
        TriangleData triangle = createTriangleData(triangleData);

        RenderingContext context = new RenderingContext(
            triangleData.viewDirection(),
            triangleData.materialColor(),
            triangleData.lightingManager(),
            triangleData.allMeshes(),
            triangleData.texture(),
            triangleData.uv1(),
            triangleData.uv2(),
            triangleData.uv3()
        );

        renderTriangle(triangle, context);
    }

    private TriangleData createTriangleData(TriangleLightingData data) {
        record VertexData(Point3D screenPoint, Point3D worldPoint, Point3D normal) {}

        VertexData[] vertices = {
            new VertexData(data.screenP1(), data.worldP1(), data.normal1()),
            new VertexData(data.screenP2(), data.worldP2(), data.normal2()),
            new VertexData(data.screenP3(), data.worldP3(), data.normal3())
        };

        Arrays.sort(vertices, java.util.Comparator.comparingDouble(v -> v.screenPoint.getY()));

        return new TriangleData(
            new Point3D[]{vertices[0].screenPoint, vertices[1].screenPoint, vertices[2].screenPoint},
            new Point3D[]{vertices[0].worldPoint, vertices[1].worldPoint, vertices[2].worldPoint},
            new Point3D[]{vertices[0].normal, vertices[1].normal, vertices[2].normal},
            data.meshId()
        );
    }

    private void renderTriangle(TriangleData triangle, RenderingContext context) {
        Point3D[] sortedScreenPoints = triangle.screenPoints;
        Point3D p1 = sortedScreenPoints[0];
        Point3D p2 = sortedScreenPoints[1];
        Point3D p3 = sortedScreenPoints[2];

        int y1 = (int) Math.round(p1.getY());
        int y2 = (int) Math.round(p2.getY());
        int y3 = (int) Math.round(p3.getY());

        double barycentricDenominator = calculateBarycentricDenominator(p1, p2, p3);
        if (Math.abs(barycentricDenominator) < EPSILON) return;

        int totalScanlines = y3 - y1 + 1;

        if (totalScanlines < MIN_SCANLINES_FOR_PARALLEL) {
            renderTriangleSequential(p1, p2, p3, y1, y2, y3, triangle, barycentricDenominator, context);
        } else {
            renderTriangleParallel(p1, p2, p3, y1, y2, y3, triangle, barycentricDenominator, context);
        }
    }

    private void renderTriangleSequential(Point3D p1, Point3D p2, Point3D p3,
                                         int y1, int y2, int y3,
                                         TriangleData triangle, double barycentricDenominator,
                                         RenderingContext context) {
        for (int y = y1; y <= y2; y++) {
            renderTriangleLine(p1, p2, p1, p3, y, triangle, barycentricDenominator, context);
        }

        for (int y = y2 + 1; y <= y3; y++) {
            renderTriangleLine(p2, p3, p1, p3, y, triangle, barycentricDenominator, context);
        }
    }

    private void renderTriangleParallel(Point3D p1, Point3D p2, Point3D p3,
                                       int y1, int y2, int y3,
                                       TriangleData triangle, double barycentricDenominator,
                                       RenderingContext context) {
        ensureExecutorInitialized();

        int totalScanlines = y3 - y1 + 1;
        int linesPerThread = (totalScanlines + renderThreadCount - 1) / renderThreadCount;

        CountDownLatch latch = new CountDownLatch(renderThreadCount);

        for (int t = 0; t < renderThreadCount; t++) {
            final int startLine = y1 + t * linesPerThread;
            final int endLine = Math.min(y1 + (t + 1) * linesPerThread - 1, y3);

            if (startLine > y3) {
                latch.countDown();
                continue;
            }

            renderExecutor.submit(() -> {
                try {
                    for (int y = startLine; y <= endLine; y++) {
                        if (y <= y2) {
                            renderTriangleLineParallel(p1, p2, p1, p3, y, triangle, barycentricDenominator, context);
                        } else {
                            renderTriangleLineParallel(p2, p3, p1, p3, y, triangle, barycentricDenominator, context);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
            flushPixelBuffer();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void renderTriangleLine(Point3D edgeStart1, Point3D edgeEnd1,
                                   Point3D edgeStart2, Point3D edgeEnd2,
                                   int y, TriangleData triangle, double barycentricDenominator,
                                   RenderingContext context) {
        double x1 = interpolateXAtY(edgeStart1, edgeEnd1, y);
        double x2 = interpolateXAtY(edgeStart2, edgeEnd2, y);

        if (x1 > x2) {
            double temp = x1; x1 = x2; x2 = temp;
        }

        int startX = (int) Math.round(x1);
        int endX = (int) Math.round(x2);

        for (int x = startX; x <= endX; x++) {
            renderPixel(x, y, triangle, barycentricDenominator, context);
        }
    }

    private void renderTriangleLineParallel(Point3D edgeStart1, Point3D edgeEnd1,
                                           Point3D edgeStart2, Point3D edgeEnd2,
                                           int y, TriangleData triangle, double barycentricDenominator,
                                           RenderingContext context) {
        double x1 = interpolateXAtY(edgeStart1, edgeEnd1, y);
        double x2 = interpolateXAtY(edgeStart2, edgeEnd2, y);

        if (x1 > x2) {
            double temp = x1; x1 = x2; x2 = temp;
        }

        int startX = (int) Math.round(x1);
        int endX = (int) Math.round(x2);

        for (int x = startX; x <= endX; x++) {
            renderPixelParallel(x, y, triangle, barycentricDenominator, context);
        }
    }

    private void renderPixel(int x, int y, TriangleData triangle, double barycentricDenominator, RenderingContext context) {
        RenderingStrategy.PixelRenderContext pixelContext = createPixelContext(triangle, barycentricDenominator, context, this::drawPixelDirect);
        renderingStrategy.renderPixel(x, y, pixelContext);
    }

    private void renderPixelParallel(int x, int y, TriangleData triangle, double barycentricDenominator, RenderingContext context) {
        RenderingStrategy.PixelRenderContext pixelContext = createPixelContext(triangle, barycentricDenominator, context, this::bufferPixel);
        renderingStrategy.renderPixelParallel(x, y, pixelContext);
    }

    private RenderingStrategy.PixelRenderContext createPixelContext(TriangleData triangle, double barycentricDenominator,
                                                                    RenderingContext context, RenderingStrategy.PixelConsumer consumer) {
        RenderingStrategy.RenderingData renderingData = new RenderingStrategy.RenderingData(
            context.viewDirection,
            context.materialColor,
            context.lightingManager,
            context.allMeshes,
            context.texture,
            context.uv1,
            context.uv2,
            context.uv3
        );

        return new RenderingStrategy.PixelRenderContext(
            triangle.screenPoints,
            triangle.worldPoints,
            triangle.normals,
            triangle.meshId,
            barycentricDenominator,
            renderingData,
            consumer,
            zBuffer::testAndSet
        );
    }

    private void drawPixelDirect(int x, int y, Color color) {
        drawPoint(new Point2D(x, y), color);
    }

    private double calculateBarycentricDenominator(Point3D p1, Point3D p2, Point3D p3) {
        return (p2.getY() - p3.getY()) * (p1.getX() - p3.getX()) +
               (p3.getX() - p2.getX()) * (p1.getY() - p3.getY());
    }

    private double interpolateXAtY(Point3D p1, Point3D p2, double y) {
        if (Math.abs(p2.getY() - p1.getY()) < EPSILON) {
            return p1.getX();
        }
        return p1.getX() + (p2.getX() - p1.getX()) * (y - p1.getY()) / (p2.getY() - p1.getY());
    }


    private record TriangleData(
        Point3D[] screenPoints,
        Point3D[] worldPoints,
        Point3D[] normals,
        String meshId
    ) {}

    private record RenderingContext(
        Point3D viewDirection,
        Color materialColor,
        LightingManager lightingManager,
        List<Mesh> allMeshes,
        javafx.scene.image.Image texture,
        Point2D uv1,
        Point2D uv2,
        Point2D uv3
    ) {}

    public record TriangleLightingData(
        Point3D screenP1, Point3D screenP2, Point3D screenP3,
        Point3D worldP1, Point3D worldP2, Point3D worldP3,
        Point3D normal1, Point3D normal2, Point3D normal3,
        Point3D viewDirection,
        Color materialColor,
        LightingManager lightingManager,
        List<Mesh> allMeshes,
        String meshId,
        Point2D uv1, Point2D uv2, Point2D uv3,
        javafx.scene.image.Image texture
    ) {}

    public static void shutdownRenderExecutor() {
        if (renderExecutor != null && !renderExecutor.isShutdown()) {
            renderExecutor.shutdown();
            try {
                if (!renderExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    renderExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                renderExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}

