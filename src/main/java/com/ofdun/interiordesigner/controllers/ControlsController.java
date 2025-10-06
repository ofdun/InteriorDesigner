package com.ofdun.interiordesigner.controllers;

import jakarta.inject.Singleton;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Singleton
public class ControlsController {
    private final Map<String, Runnable> buttonEvents = new HashMap<>();
    private static final Logger _log = LoggerFactory.getLogger(ControlsController.class);

    private Consumer<String> choiceBoxSelectionCallback = null;
    private Consumer<Double> lightIntensityChangeCallback = null;

    @FXML
    private ListView<String> _objectsListView;

    @FXML
    private ChoiceBox<String> _objectChoiceBox;

    @FXML
    private Slider _lightIntensitySlider;

    @FXML
    private Button _objectAddButton;

    @FXML
    public void initialize() {
        _objectsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        if (_objectChoiceBox != null) {
            _objectChoiceBox.getItems().clear();
            _objectChoiceBox.getItems().add("None");
            _objectChoiceBox.setValue("None");

            _objectChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                if (choiceBoxSelectionCallback != null) {
                    String id = (newV == null || "None".equals(newV)) ? null : newV;
                    choiceBoxSelectionCallback.accept(id);
                } else {
                    handleButtonPress("render");
                }
            });
        }

        if (_lightIntensitySlider != null) {
            _lightIntensitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (lightIntensityChangeCallback != null) {
                    lightIntensityChangeCallback.accept(newVal.doubleValue());
                }
            });
        }
    }

    public void bindChoiceBoxSelection(Consumer<String> callback) {
        this.choiceBoxSelectionCallback = callback;
    }

    public String getChoiceBoxSelection() {
        if (_objectChoiceBox == null) {
            return null;
        }

        String value = _objectChoiceBox.getValue();
        return "None".equals(value) ? null : value;
    }


    public Double getLightIntensity() {
        if (_lightIntensitySlider == null) {
            return 1.0;
        }
        return _lightIntensitySlider.getValue();
    }

    public void bindLightIntensityChange(Consumer<Double> callback) {
        this.lightIntensityChangeCallback = callback;
    }

    public void bindButtonEvent(String buttonName, Runnable event) {
        buttonEvents.put(buttonName, event);
    }

    private void handleButtonPress(String buttonName) {
        Runnable event = buttonEvents.get(buttonName);
        if (event != null) {
            event.run();
            if (!buttonName.equals("render")) {
                handleButtonPress("render");
            }
        }
    }

    public List<String> getHighlightedListView() {
        return _objectsListView.getSelectionModel().getSelectedItems();
    }

    public void addObjectToObjectListView(String object) {
        Platform.runLater(() -> _objectsListView.getItems().add(object));
    }

    public void removeObjectFromObjectListView(String object) {
        Platform.runLater(() -> _objectsListView.getItems().remove(object));
    }

    public void addChoiceBoxItem(String id) {
        if (_objectChoiceBox == null) return;
        Platform.runLater(() -> {
            if (!_objectChoiceBox.getItems().contains(id)) {
                _objectChoiceBox.getItems().add(id);
            }
        });
    }

    public void removeChoiceBoxItem(String id) {
        if (_objectChoiceBox == null) return;
        Platform.runLater(() -> {
            _objectChoiceBox.getItems().remove(id);
            if (id.equals(_objectChoiceBox.getValue())) {
                _objectChoiceBox.setValue("None");
            }
        });
    }

    public Stage getStage() {
        return (Stage) _objectAddButton.getScene().getWindow();
    }

    @FXML
    private void onObjectAddButton(ActionEvent ignored) {
        handleButtonPress("objectAdd");
    }

    @FXML
    private void onObjectRemoveButton(ActionEvent ignored) {
        handleButtonPress("objectRemove");
    }

    @FXML
    public void onCameraXPlusButtonPressed(ActionEvent ignored) {
        handleButtonPress("cameraXPlus");
    }

    @FXML
    public void onCameraXMinusButtonPressed(ActionEvent ignored) {
        handleButtonPress("cameraXMinus");
    }

    @FXML
    public void onCameraYPlusButtonPressed(ActionEvent ignored) {
        handleButtonPress("cameraYPlus");
    }

    @FXML
    public void onCameraYMinusButtonPressed(ActionEvent ignored) {
        handleButtonPress("cameraYMinus");
    }

    @FXML
    public void onCameraZPlusButtonPressed(ActionEvent ignored) {
        handleButtonPress("cameraZPlus");
    }

    @FXML
    public void onCameraZMinusButtonPressed(ActionEvent ignored) {
        handleButtonPress("cameraZMinus");
    }

    @FXML
    public void onCameraXAnglePlusButtonPressed(ActionEvent ignored) {
        handleButtonPress("cameraXAnglePlus");
    }

    @FXML
    public void onCameraXAngleMinusButtonPressed(ActionEvent ignored) {
        handleButtonPress("cameraXAngleMinus");
    }

    @FXML
    public void onCameraYAnglePlusButtonPressed(ActionEvent ignored) {
        handleButtonPress("cameraYAnglePlus");
    }

    @FXML
    public void onCameraYAngleMinusButtonPressed(ActionEvent ignored) {
        handleButtonPress("cameraYAngleMinus");
    }

    @FXML
    public void onCameraZAnglePlusButtonPressed(ActionEvent ignored) {
        handleButtonPress("cameraZAnglePlus");
    }

    @FXML
    public void onCameraZAngleMinusButtonPressed(ActionEvent ignored) {
        handleButtonPress("cameraZAngleMinus");
    }

    @FXML
    public void onObjectXPlusButtonPressed(ActionEvent ignored) {
        handleButtonPress("objectXPlus");
    }

    @FXML
    public void onObjectXMinusButtonPressed(ActionEvent ignored) {
        handleButtonPress("objectXMinus");
    }

    @FXML
    public void onObjectYPlusButtonPressed(ActionEvent ignored) {
        handleButtonPress("objectYPlus");
    }

    @FXML
    public void onObjectYMinusButtonPressed(ActionEvent ignored) {
        handleButtonPress("objectYMinus");
    }

    @FXML
    public void onObjectZPlusButtonPressed(ActionEvent ignored) {
        handleButtonPress("objectZPlus");
    }

    @FXML
    public void onObjectZMinusButtonPressed(ActionEvent ignored) {
        handleButtonPress("objectZMinus");
    }

    @FXML
    public void onObjectXAnglePlusButtonPressed(ActionEvent ignored) {
        handleButtonPress("objectXAnglePlus");
    }

    @FXML
    public void onObjectXAngleMinusButtonPressed(ActionEvent ignored) {
        handleButtonPress("objectXAngleMinus");
    }

    @FXML
    public void onObjectYAnglePlusButtonPressed(ActionEvent ignored) {
        handleButtonPress("objectYAnglePlus");
    }

    @FXML
    public void onObjectYAngleMinusButtonPressed(ActionEvent ignored) {
        handleButtonPress("objectYAngleMinus");
    }

    @FXML
    public void onObjectZAnglePlusButtonPressed(ActionEvent ignored) {
        handleButtonPress("objectZAnglePlus");
    }

    @FXML
    public void onObjectZAngleMinusButtonPressed(ActionEvent ignored) {
        handleButtonPress("objectZAngleMinus");
    }
}
