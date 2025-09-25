package com.ofdun.interiordesigner.controllers;

import com.ofdun.interiordesigner.models.LightingManager;
import com.ofdun.interiordesigner.models.ZBuffer;
import jakarta.inject.Singleton;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Singleton
public class CanvasController {
    private static final double EPSILON = 1e-6;

    private final Logger log = LoggerFactory.getLogger(CanvasController.class);
    private final Map<String, Runnable> eventCallbacks = new HashMap<>();
    private final Map<String, BiConsumer<Double, Double>> mouseEventCallbacks = new HashMap<>();

    @FXML
    private Canvas _canvas;
    private Canvas _invisibleCanvas;
    private final ZBuffer _zBuffer = new ZBuffer(1000, 800);

    private GraphicsContext _graphicsContext;
    private GraphicsContext _invisibleGraphicsContext;

    private boolean isDragging = false;
    private double lastMouseX;
    private double lastMouseY;

    @FXML
    public void initialize() {
        _graphicsContext = _canvas.getGraphicsContext2D();

        _invisibleCanvas = new Canvas(_canvas.getWidth(), _canvas.getHeight());
        _invisibleGraphicsContext = _invisibleCanvas.getGraphicsContext2D();
        setupCanvas();
    }

    private void setupCanvas() {
        fillDefaultColor();
        _invisibleGraphicsContext.setStroke(Color.GRAY);
        _invisibleGraphicsContext.setLineWidth(2);
        render();
    }

    public void render() {
        _graphicsContext.drawImage(_invisibleCanvas.snapshot(null, null), 0, 0);
        clearInvisibleCanvas();
    }

    private void fillDefaultColor() {
        _invisibleGraphicsContext.setFill(Color.WHITE);
        _invisibleGraphicsContext.fillRect(0, 0, _invisibleCanvas.getWidth(), _invisibleCanvas.getHeight());
    }

    private void clearInvisibleCanvas() {
        _zBuffer.clear();
        _invisibleGraphicsContext.clearRect(0, 0, _invisibleCanvas.getWidth(), _invisibleCanvas.getHeight());
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

//            log.info("Mouse dragged: deltaX = {}, deltaY = {}, yaw = {}, pitch = {}",
//                    deltaX, deltaY, yawAngle, pitchAngle);
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
        _invisibleGraphicsContext.setStroke(paint);
        _invisibleGraphicsContext.strokeLine(point.getX(), point.getY(), point.getX(),  point.getY());
    }

    public void drawTriangleWithLighting(TriangleLightingData triangleData) {
        TriangleData triangle = createTriangleData(triangleData);
        RenderingContext context = new RenderingContext(
            triangleData.viewDirection(),
            triangleData.materialColor(),
            triangleData.lightingManager()
        );

        renderTriangle(triangle, context);
    }

    private TriangleData createTriangleData(TriangleLightingData data) {
        return new TriangleData(
            sortPointsByY(data.screenP1(), data.screenP2(), data.screenP3()),
            new Point3D[]{data.worldP1(), data.worldP2(), data.worldP3()},
            new Point3D[]{data.normal1(), data.normal2(), data.normal3()}
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
            double z = interpolateZ(screenPoints[0], screenPoints[1], screenPoints[2], coords);

            if (_zBuffer.testAndSet(x, y, z)) {
                Color litColor = context.lightingManager.calculateTriangleLighting(
                        triangle.worldPoints[0], triangle.worldPoints[1], triangle.worldPoints[2],
                        triangle.normals[0], triangle.normals[1], triangle.normals[2],
                        context.viewDirection, context.materialColor, coords.w1, coords.w2, coords.w3);

                drawPoint(new Point2D(x, y), litColor);
            }
        }
    }

    private Point3D[] sortPointsByY(Point3D p1, Point3D p2, Point3D p3) {
        Point3D[] points = new Point3D[]{p1, p2, p3};
        Arrays.sort(points, java.util.Comparator.comparingDouble(Point3D::getY));
        return points;
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

    private double interpolateZ(Point3D p1, Point3D p2, Point3D p3, BarycentricCoordinates coords) {
        return coords.w1 * p1.getZ() + coords.w2 * p2.getZ() + coords.w3 * p3.getZ();
    }

    private record BarycentricCoordinates(double w1, double w2, double w3) {
            boolean isInsideTriangle() {
                return w1 >= 0 && w2 >= 0 && w3 >= 0;
            }
        }

    private record TriangleData(
        Point3D[] screenPoints,
        Point3D[] worldPoints,
        Point3D[] normals
    ) {}

    private record RenderingContext(
        Point3D viewDirection,
        Color materialColor,
        LightingManager lightingManager
    ) {}


    public record TriangleLightingData(
        Point3D screenP1, Point3D screenP2, Point3D screenP3,
        Point3D worldP1, Point3D worldP2, Point3D worldP3,
        Point3D normal1, Point3D normal2, Point3D normal3,
        Point3D viewDirection,
        Color materialColor,
        LightingManager lightingManager
    ) {}
}

