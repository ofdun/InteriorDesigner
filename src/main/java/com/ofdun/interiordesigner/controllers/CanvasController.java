package com.ofdun.interiordesigner.controllers;

import com.ofdun.interiordesigner.managers.SceneManager;
import com.ofdun.interiordesigner.models.Camera;
import com.ofdun.interiordesigner.models.ZBuffer;
import jakarta.inject.Singleton;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Singleton
public class CanvasController {
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

    public void drawTriangle(Point3D p1, Point3D p2, Point3D p3, Boolean insideView) {
        if (p1.getY() > p2.getY()) { Point3D temp = p1; p1 = p2; p2 = temp; }
        if (p2.getY() > p3.getY()) { Point3D temp = p2; p2 = p3; p3 = temp; }
        if (p1.getY() > p2.getY()) { Point3D temp = p1; p1 = p2; p2 = temp; }

        int y1 = (int) Math.round(p1.getY());
        int y2 = (int) Math.round(p2.getY());
        int y3 = (int) Math.round(p3.getY());

        double denom = (p2.getY() - p3.getY()) * (p1.getX() - p3.getX()) +
                (p3.getX() - p2.getX()) * (p1.getY() - p3.getY());

        if (Math.abs(denom) < 1e-6)
            return;

        for (int y = y1; y <= y2; y++) {
            fillTriangleLine(p1, p2, p1, p3, y, p1, p2, p3, denom);
        }

        for (int y = y2 + 1; y <= y3; y++) {
            fillTriangleLine(p2, p3, p1, p3, y, p1, p2, p3, denom);
        }
    }

    public void drawTriangle(Point3D p1, Point3D p2, Point3D p3) {
        drawTriangle(p1, p2, p3, false);
    }

    private void fillTriangleLine(Point3D p1, Point3D p2, Point3D p3, Point3D p4, int y,
                                  Point3D tp1, Point3D tp2, Point3D tp3, double denom) {
        double x1 = (p2.getY() == p1.getY()) ? p1.getX() :
                p1.getX() + (p2.getX() - p1.getX()) * (y - p1.getY()) / (p2.getY() - p1.getY());
        double x2 = (p4.getY() == p3.getY()) ? p3.getX() :
                p3.getX() + (p4.getX() - p3.getX()) * (y - p3.getY()) / (p4.getY() - p3.getY());

        if (x1 > x2) {
            double temp = x1; x1 = x2; x2 = temp;
        }

        int startX = (int) Math.round(x1);
        int endX = (int) Math.round(x2);

        for (int x = startX; x <= endX; x++) {
            double w1 = ((tp2.getY() - tp3.getY()) * (x - tp3.getX()) +
                    (tp3.getX() - tp2.getX()) * (y - tp3.getY())) / denom;
            double w2 = ((tp3.getY() - tp1.getY()) * (x - tp3.getX()) +
                    (tp1.getX() - tp3.getX()) * (y - tp3.getY())) / denom;
            double w3 = 1 - w1 - w2;

            if (w1 >= 0 && w2 >= 0 && w3 >= 0) {
                double z = w1 * tp1.getZ() + w2 * tp2.getZ() + w3 * tp3.getZ();

                if (_zBuffer.testAndSet(x, y, z)) {
                    drawPoint(new Point2D(x, y));
                }
            }
        }
    }

    private void drawPoint(Point2D point) {
        _invisibleGraphicsContext.strokeLine(point.getX(), point.getY(), point.getX(),  point.getY());
    }
}