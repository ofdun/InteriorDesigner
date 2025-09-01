package com.ofdun.interiordesigner.controllers;

import jakarta.inject.Singleton;
import javafx.fxml.FXML;
import javafx.geometry.Point3D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CanvasController {
    Logger logger = LoggerFactory.getLogger(CanvasController.class);

    @FXML
    private Canvas _canvas;
    private Canvas _invisibleCanvas;

    private GraphicsContext _graphicsContext;
    private GraphicsContext _invisibleGraphicsContext;

    @FXML
    public void initialize() {
        _graphicsContext = _canvas.getGraphicsContext2D();

        _invisibleCanvas = new Canvas(_canvas.getWidth(), _canvas.getHeight());
        _invisibleGraphicsContext = _invisibleCanvas.getGraphicsContext2D();
        setupCanvas();
    }

    private void setupCanvas() {
        fillDefaultColor();
        _invisibleGraphicsContext.setStroke(Color.BLACK);
        _invisibleGraphicsContext.setLineWidth(2);
        render();
    }

    public void render() {
        _graphicsContext.drawImage(_invisibleCanvas.snapshot(null, null), 0, 0);
        clearInvisibleCanvas();
    }

    private void fillDefaultColor() {
        _invisibleGraphicsContext.setFill(Color.GRAY);
        _invisibleGraphicsContext.fillRect(0, 0, _invisibleCanvas.getWidth(), _invisibleCanvas.getHeight());
    }

    private void clearInvisibleCanvas() {
        _invisibleGraphicsContext.clearRect(0, 0, _invisibleCanvas.getWidth(), _invisibleCanvas.getHeight());
        fillDefaultColor();
    }

    public void onMousePressed(MouseEvent mouseEvent) {
        logger.info("{}, {}", mouseEvent.getX(), mouseEvent.getY());
    }

    public void drawLine(Point3D startPoint, Point3D endPoint) {
        _invisibleGraphicsContext.strokeLine(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
    }
}