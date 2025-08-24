package com.ofdun.interiordesigner;

import com.ofdun.interiordesigner.objectloader.ObjectLoader;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.event.ActionEvent;
import javafx.scene.shape.TriangleMesh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@Singleton
public class CanvasController {
    Logger logger = LoggerFactory.getLogger(CanvasController.class);

    @FXML
    private Canvas _canvas;
    private Canvas _invisibleCanvas;

    @FXML
    private Button _drawCubeButton;

    private ObjectLoader _objectLoader;

    private GraphicsContext _graphicsContext;
    private GraphicsContext _invisibleGraphicsContext;
    private double _startX, _startY;

    @Inject
    CanvasController(ObjectLoader objectLoader) {
        this._objectLoader = objectLoader;
    }

    @FXML
    public void initialize() {
        _graphicsContext = _canvas.getGraphicsContext2D();

        _invisibleCanvas = new Canvas(_canvas.getWidth(), _canvas.getHeight());
        _invisibleGraphicsContext = _invisibleCanvas.getGraphicsContext2D();
        setupCanvas();
    }

    private void setupCanvas() {
        _invisibleGraphicsContext.setFill(Color.WHITE);
        _invisibleGraphicsContext.fillRect(0, 0, _canvas.getWidth(), _canvas.getHeight());
        _invisibleGraphicsContext.setStroke(Color.BLACK);
        _invisibleGraphicsContext.setLineWidth(2);
    }

    private void render() {
        _graphicsContext.drawImage(_invisibleCanvas.snapshot(null, null), 0, 0);
    }

    @FXML
    private void onMousePressed(MouseEvent event) {
        _startX = event.getX();
        _startY = event.getY();
    }

    @FXML
    private void onMouseReleased(MouseEvent event) {
        drawLine(_startX, _startY, event.getX(), event.getY());
    }

    @FXML
    private void onDrawCubeButtonPressed(ActionEvent event) {
        Path path = Paths.get("cube.obj");
        try (var reader = Files.newBufferedReader(path)) {
            var object  = _objectLoader.load(reader);

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void drawLine(double x1, double y1, double x2, double y2) {
        _invisibleGraphicsContext.strokeLine(x1, y1, x2, y2);
        render();
    }
}