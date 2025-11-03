package com.ofdun.interiordesigner.controllers;

import com.ofdun.interiordesigner.managers.LightingManager;
import com.ofdun.interiordesigner.models.Mesh;
import com.ofdun.interiordesigner.models.ZBuffer;
import jakarta.inject.Singleton;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Singleton
public class CanvasController {
    private static final double EPSILON = 1e-6;

    private final Logger log = LoggerFactory.getLogger(CanvasController.class);
    private final Map<String, Runnable> eventCallbacks = new HashMap<>();
    private final Map<String, BiConsumer<Double, Double>> mouseEventCallbacks = new HashMap<>();

    @FXML
    private Canvas canvas;
    private Canvas invisibleCanvas;
    private final ZBuffer zBuffer = new ZBuffer(1250, 800);

    private GraphicsContext graphicsContext;
    private GraphicsContext invisibleGraphicsContext;

    private long fpsLastTime = System.nanoTime();
    private int fpsFrameCount = 0;
    private double currentFps = 0.0;

    private boolean isDragging = false;
    private double lastMouseX;
    private double lastMouseY;

    @FXML
    public void initialize() {
        graphicsContext = canvas.getGraphicsContext2D();

        invisibleCanvas = new Canvas(canvas.getWidth(), canvas.getHeight());
        invisibleGraphicsContext = invisibleCanvas.getGraphicsContext2D();
        setupCanvas();
    }

    private void setupCanvas() {
        fillDefaultColor();
        invisibleGraphicsContext.setStroke(Color.GRAY);
        invisibleGraphicsContext.setLineWidth(2);
        render();
    }

    public void render() {
        long now = System.nanoTime();
        fpsFrameCount++;
        double elapsedSec = (now - fpsLastTime) / 1_000_000_000.0;
        if (elapsedSec >= 1.0) {
            currentFps = fpsFrameCount / elapsedSec;
            log.info("FPS: {}", String.format("%.2f", currentFps));
            fpsFrameCount = 0;
            fpsLastTime = now;
        }

        String fpsText = String.format("FPS: %.2f", currentFps);
        invisibleGraphicsContext.setFill(Color.BLACK);
        invisibleGraphicsContext.setFont(Font.font("Arial", 14));
        invisibleGraphicsContext.fillText(fpsText, 8, 18);

        graphicsContext.drawImage(invisibleCanvas.snapshot(null, null), 0, 0);
        clearInvisibleCanvas();
    }

    private void fillDefaultColor() {
        invisibleGraphicsContext.setFill(Color.WHITE);
        invisibleGraphicsContext.fillRect(0, 0, invisibleCanvas.getWidth(), invisibleCanvas.getHeight());
    }

    private void clearInvisibleCanvas() {
        zBuffer.clear();
        invisibleGraphicsContext.clearRect(0, 0, invisibleCanvas.getWidth(), invisibleCanvas.getHeight());
        fillDefaultColor();
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
        invisibleGraphicsContext.setStroke(paint);
        invisibleGraphicsContext.strokeLine(point.getX(), point.getY(), point.getX(),  point.getY());
    }

    public void drawLine(Point3D p1, Point3D p2, Paint paint, double lineWidth) {
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

        for (int y = y1; y <= y2; y++) {
            renderTriangleLine(p1, p2, p1, p3, y, triangle, barycentricDenominator, context);
        }

        for (int y = y2 + 1; y <= y3; y++) {
            renderTriangleLine(p2, p3, p1, p3, y, triangle, barycentricDenominator, context);
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

    private void renderPixel(int x, int y, TriangleData triangle, double barycentricDenominator, RenderingContext context) {
        Point3D[] screenPoints = triangle.screenPoints;
        BarycentricCoordinates coords = calculateBarycentricCoordinates(x, y, screenPoints[0], screenPoints[1], screenPoints[2], barycentricDenominator);

        if (coords.isInsideTriangle()) {
            double z1 = screenPoints[0].getZ();
            double z2 = screenPoints[1].getZ();
            double z3 = screenPoints[2].getZ();

            double oneOverZ = coords.w1 / z1 + coords.w2 / z2 + coords.w3 / z3;
            double z = 1.0 / oneOverZ;

            if (zBuffer.testAndSet(x, y, z)) {
                Point3D interpolatedNormal = interpolateNormals(
                    x, y,
                    screenPoints[0], screenPoints[1], screenPoints[2],
                    triangle.normals[0], triangle.normals[1], triangle.normals[2]
                );

                Point3D interpolatedWorldPos = interpolateWorldPositionPerspectiveCorrect(
                    triangle.worldPoints[0], triangle.worldPoints[1], triangle.worldPoints[2],
                    coords, z1, z2, z3
                );

                Color finalColor;

                if (context.texture != null && context.uv1 != null && context.uv2 != null && context.uv3 != null) {
                    Point2D interpolatedUV = interpolateUVPerspectiveCorrect(
                        context.uv1, context.uv2, context.uv3, coords, z1, z2, z3);

                    Color textureColor = sampleTexture(context.texture, interpolatedUV);

                    finalColor = context.lightingManager.calculateLightingWithShadows(
                        interpolatedWorldPos, interpolatedNormal,
                        context.viewDirection, textureColor, context.allMeshes
                    );
                } else {
                    finalColor = context.lightingManager.calculateLightingWithShadows(
                        interpolatedWorldPos, interpolatedNormal,
                        context.viewDirection, context.materialColor, context.allMeshes
                    );
                }

                drawPoint(new Point2D(x, y), finalColor);
            }
        }
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

    private record PerspectiveWeights(double w1, double w2, double w3) {}

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
}

