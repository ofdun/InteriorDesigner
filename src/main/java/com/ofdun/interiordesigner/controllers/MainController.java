package com.ofdun.interiordesigner.controllers;

import jakarta.inject.Singleton;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.VBox;

@Singleton
public class MainController {
    @FXML
    private Canvas canvas;
    @FXML
    private VBox controls;

    public void initialize() {
    }
}
