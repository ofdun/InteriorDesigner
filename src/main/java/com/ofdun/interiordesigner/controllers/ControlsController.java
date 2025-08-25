package com.ofdun.interiordesigner.controllers;

import jakarta.inject.Singleton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

@Singleton
public class ControlsController {
    private Runnable _drawCubeButtonPressedEvent;

    public void bindDrawCubeButtonPressedEvent(Runnable drawCubeButtonPressedEvent) {
        _drawCubeButtonPressedEvent = drawCubeButtonPressedEvent;
    }

    @FXML
    public void onDrawCubeButtonPressed(ActionEvent ignored) {
        if (_drawCubeButtonPressedEvent != null) {
            _drawCubeButtonPressedEvent.run();
        }
    }
}
