package com.ofdun.interiordesigner.controllers;

import jakarta.inject.Singleton;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Singleton
public class ControlsController {
    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    private final Map<String, Runnable> buttonEvents = new HashMap<>();
    private static final Logger _log = LoggerFactory.getLogger(ControlsController.class);

    private Consumer<String> choiceBoxSelectionCallback = null;
    private Consumer<Double> lightIntensityChangeCallback = null;
    private Consumer<Color> wallsColorChangeCallback = null;
    private Consumer<Color> floorColorChangeCallback = null;
    private Consumer<Color> ceilingColorChangeCallback = null;
    private TriConsumer<Double, Double, Double> roomSizeChangeCallback = null;

    @FXML
    private ListView<String> objectsListView;

    @FXML
    private ChoiceBox<String> objectChoiceBox;

    @FXML
    private ChoiceBox<String> wallChoiceBox;

    @FXML
    private Slider lightIntensitySlider;

    @FXML
    private ColorPicker wallsColorPicker;

    @FXML
    private ColorPicker floorColorPicker;

    @FXML
    private ColorPicker ceilingColorPicker;

    @FXML
    private TextField roomWidthField;

    @FXML
    private TextField roomDepthField;

    @FXML
    private TextField roomHeightField;

    @FXML
    private Button applyRoomSizeButton;

    @FXML
    private Button objectAddButton;

    @FXML
    public void initialize() {
        objectsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        if (objectChoiceBox != null) {
            objectChoiceBox.getItems().clear();
            objectChoiceBox.getItems().add("Не выбрано");
            objectChoiceBox.setValue("Не выбрано");

            objectChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                if (choiceBoxSelectionCallback != null) {
                    String id = (newV == null || "Не выбрано".equals(newV)) ? null : newV;
                    choiceBoxSelectionCallback.accept(id);
                } else {
                    handleButtonPress("render");
                }
            });
        }

        if (wallChoiceBox != null) {
            wallChoiceBox.getItems().clear();
            wallChoiceBox.getItems().addAll("Задняя стена", "Передняя стена", "Левая стена", "Правая стена");
            wallChoiceBox.setValue("Задняя стена");
        }

        if (lightIntensitySlider != null) {
            lightIntensitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (lightIntensityChangeCallback != null) {
                    lightIntensityChangeCallback.accept(newVal.doubleValue());
                }
            });
        }

        if (wallsColorPicker != null) {
            wallsColorPicker.setValue(Color.rgb(0xC1, 0x9A, 0x6B));
            wallsColorPicker.setOnAction(event -> {
                if (wallsColorChangeCallback != null) {
                    wallsColorChangeCallback.accept(wallsColorPicker.getValue());
                }
            });
        }

        if (floorColorPicker != null) {
            floorColorPicker.setValue(Color.rgb(0xC1, 0x9A, 0x6B));
            floorColorPicker.setOnAction(event -> {
                if (floorColorChangeCallback != null) {
                    floorColorChangeCallback.accept(floorColorPicker.getValue());
                }
            });
        }

        if (ceilingColorPicker != null) {
            ceilingColorPicker.setValue(Color.rgb(0xC1, 0x9A, 0x6B));
            ceilingColorPicker.setOnAction(event -> {
                if (ceilingColorChangeCallback != null) {
                    ceilingColorChangeCallback.accept(ceilingColorPicker.getValue());
                }
            });
        }
    }

    public void bindChoiceBoxSelection(Consumer<String> callback) {
        this.choiceBoxSelectionCallback = callback;
    }

    public String getChoiceBoxSelection() {
        if (objectChoiceBox == null) {
            return null;
        }

        String value = objectChoiceBox.getValue();
        return "None".equals(value) ? null : value;
    }

    public String getSelectedWall() {
        if (wallChoiceBox == null) {
            return null;
        }
        return wallChoiceBox.getValue();
    }

    public Double getLightIntensity() {
        if (lightIntensitySlider == null) {
            return 1.0;
        }
        return lightIntensitySlider.getValue();
    }

    public void bindLightIntensityChange(Consumer<Double> callback) {
        this.lightIntensityChangeCallback = callback;
    }

    public void bindWallsColorChange(Consumer<Color> callback) {
        this.wallsColorChangeCallback = callback;
    }

    public void bindFloorColorChange(Consumer<Color> callback) {
        this.floorColorChangeCallback = callback;
    }

    public void bindCeilingColorChange(Consumer<Color> callback) {
        this.ceilingColorChangeCallback = callback;
    }

    public void bindRoomSizeChange(TriConsumer<Double, Double, Double> callback) {
        this.roomSizeChangeCallback = callback;
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
        return objectsListView.getSelectionModel().getSelectedItems();
    }

    public void addObjectToObjectListView(String object) {
        Platform.runLater(() -> objectsListView.getItems().add(object));
    }

    public void removeObjectFromObjectListView(String object) {
        Platform.runLater(() -> objectsListView.getItems().remove(object));
    }

    public void removeMultipleObjectsFromObjectListView(List<String> objects) {
        Platform.runLater(() -> objectsListView.getItems().removeAll(objects));
    }

    public void addLightningSourceChoiceBoxItem(String id) {
        if (objectChoiceBox == null) return;
        Platform.runLater(() -> {
            if (!objectChoiceBox.getItems().contains(id)) {
                objectChoiceBox.getItems().add(id);
            }
        });
    }

    public void removeLightningSourceChoiceBoxItem(String id) {
        if (objectChoiceBox == null) return;
        Platform.runLater(() -> {
            objectChoiceBox.getItems().remove(id);
            if (id.equals(objectChoiceBox.getValue())) {
                objectChoiceBox.setValue("None");
            }
        });
    }

    public Stage getStage() {
        return (Stage) objectAddButton.getScene().getWindow();
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
    private void onObjectScalePlusButtonPressed(ActionEvent ignored) {
        handleButtonPress("objectScalePlus");
    }

    @FXML
    private void onObjectScaleMinusButtonPressed(ActionEvent ignored) {
        handleButtonPress("objectScaleMinus");
    }

    @FXML
    public void onApplyRoomSizeButtonPressed(ActionEvent ignored) {
        if (roomSizeChangeCallback != null) {
            try {
                double width = Double.parseDouble(roomWidthField.getText());
                double height = Double.parseDouble(roomHeightField.getText());
                double depth = Double.parseDouble(roomDepthField.getText());

                if (width > 0 && height > 0 && depth > 0) {
                    roomSizeChangeCallback.accept(width, height, depth);
                }
            } catch (NumberFormatException _) {
            }
        }
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

    @FXML
    public void onCameraZoomInButtonPressed(ActionEvent ignored) {
        handleButtonPress("cameraZoomIn");
    }

    @FXML
    public void onCameraZoomOutButtonPressed(ActionEvent ignored) {
        handleButtonPress("cameraZoomOut");
    }

    @FXML
    public void onAddWindowButtonPressed(ActionEvent ignored) {
        handleButtonPress("addWindow");
    }
}
