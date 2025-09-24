package com.ofdun.interiordesigner.managers;

import com.ofdun.interiordesigner.controllers.CanvasController;
import com.ofdun.interiordesigner.controllers.ControlsController;
import com.ofdun.interiordesigner.models.Camera;
import com.ofdun.interiordesigner.models.Mesh;
import com.ofdun.interiordesigner.models.Projecter;
import com.ofdun.interiordesigner.objectloaders.ObjectLoader;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class SceneManager {
    private final Map<String, Mesh> _objects = new HashMap<>();
    private final Camera _camera = new Camera(this);
    private final Projecter _projecter = new Projecter(_camera, 1000, 800);
    private final CanvasController _canvasController;
    private final ControlsController _controlsController;
    private final ObjectLoader _objectLoader;
    private static final Logger log = LoggerFactory.getLogger(SceneManager.class);
    private final String ROOM_ID = "0";

    @Inject
    SceneManager(CanvasController canvasController, ControlsController controlsController,
                 ObjectLoader objectLoader) {
        _canvasController = canvasController;
        _controlsController = controlsController;
        _objectLoader = objectLoader;

        bindCanvasEvents();
        bindControlsEvents();
    }

    private void bindCanvasEvents() {
        _canvasController.bindMouseEventCallback("cameraOrbit", _camera::orbit);
        _canvasController.bindEventCallback("render", this::renderAllMeshes);
    }

    private void bindControlsEvents() {
        _controlsController.bindButtonEvent("render", this::renderAllMeshes);
        _controlsController.bindButtonEvent("objectAdd", () -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose Object File");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Object Files", "*.obj")
            );

            var stage = _controlsController.getStage();

            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                log.info("Selected file: {}", file.getAbsolutePath());
                try (var stream = new BufferedReader(new FileReader(file))) {
                    var meshView = _objectLoader.load(stream);
                    addMeshView(meshView);
                    renderAllMeshes();
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }
        });

        _controlsController.bindButtonEvent("objectRemove", () -> {
            for (var selectedMeshId : _controlsController.getHighlightedListView()) {
                removeMashById(selectedMeshId);
            }
        });

        _controlsController.bindButtonEvent("objectXPlus", () ->
                transformSelectedMeshes(TransformMatrices.translateX(Mesh.MOVE_STEP)));
        _controlsController.bindButtonEvent("objectXMinus", () ->
                transformSelectedMeshes(TransformMatrices.translateX(-Mesh.MOVE_STEP)));
        _controlsController.bindButtonEvent("objectYPlus", () ->
                transformSelectedMeshes(TransformMatrices.translateY(Mesh.MOVE_STEP)));
        _controlsController.bindButtonEvent("objectYMinus", () ->
                transformSelectedMeshes(TransformMatrices.translateY(-Mesh.MOVE_STEP)));
        _controlsController.bindButtonEvent("objectZPlus", () ->
                transformSelectedMeshes(TransformMatrices.translateZ(Mesh.MOVE_STEP)));
        _controlsController.bindButtonEvent("objectZMinus", () ->
                transformSelectedMeshes(TransformMatrices.translateZ(-Mesh.MOVE_STEP)));

        _controlsController.bindButtonEvent("objectXAnglePlus", () ->
                transformSelectedMeshes(TransformMatrices.rotateX(Mesh.ROTATION_STEP)));
        _controlsController.bindButtonEvent("objectXAngleMinus", () ->
                transformSelectedMeshes(TransformMatrices.rotateX(-Mesh.ROTATION_STEP)));
        _controlsController.bindButtonEvent("objectYAnglePlus", () ->
                transformSelectedMeshes(TransformMatrices.rotateY(Mesh.ROTATION_STEP)));
        _controlsController.bindButtonEvent("objectYAngleMinus", () ->
                transformSelectedMeshes(TransformMatrices.rotateY(-Mesh.ROTATION_STEP)));
        _controlsController.bindButtonEvent("objectZAnglePlus", () ->
                transformSelectedMeshes(TransformMatrices.rotateZ(Mesh.ROTATION_STEP)));
        _controlsController.bindButtonEvent("objectZAngleMinus", () ->
                transformSelectedMeshes(TransformMatrices.rotateZ(-Mesh.ROTATION_STEP)));
    }

    private void transformSelectedMeshes(SimpleMatrix transform) {
        for (String selectedMeshId : _controlsController.getHighlightedListView()) {
            var mesh = _objects.get(selectedMeshId);
//            log.info(mesh.getCenter().toString());
            if (mesh != null) {
                mesh.applyTransform(transform);
            }
        }
    }

    public void addMeshView(Mesh meshView) {
        var id = meshView.getId();
        _objects.put(id, meshView);

        if (id.equals(ROOM_ID)) {
            _camera.resetToRoom();
        } else {
            _controlsController.addObjectToObjectListView(id);
        }
    }

    public Boolean removeMashById(String id) {
        _controlsController.removeObjectFromObjectListView(id);
        var res = _objects.remove(id);

        renderAllMeshes();

        return res != null;
    }

    public Mesh getMeshById(String id) {
        return _objects.get(id);
    }

    public void renderAllMeshes() {
        List<Mesh> sortedMeshes = _objects.values().stream()
                .sorted((m1, m2) -> {
                    double dist1 = m1.getCenter().distance(_camera.getPosition());
                    double dist2 = m2.getCenter().distance(_camera.getPosition());
                    return Double.compare(dist2, dist1);
                })
                .toList();

        for (Mesh mesh : sortedMeshes) {
            var vertices = mesh.getVertices();
            var normals = mesh.getNormals();
            var faces = mesh.getFaces();
            var normalIndices = mesh.getNormalIndices();
            var projectedPoints = projectAllPoints(vertices);

            var color = mesh.getColor();
            if (mesh.getId().equals(ROOM_ID)) {
                color = Color.GRAY;
            }

            renderFaces(faces, normalIndices, projectedPoints, normals, mesh.getId().equals(ROOM_ID), color);
        }

        _canvasController.render();
    }

    private void renderFaces(List<List<Integer>> faces, List<List<Integer>> normalIndices,
                           List<Point3D> projectedPoints, List<Point3D> normals, Boolean insideView, Paint paint) {
        for (int faceIndex = 0; faceIndex < faces.size(); faceIndex++) {
            List<Integer> face = faces.get(faceIndex);

            if (face.size() >= 3) {
                Point3D p1 = projectedPoints.get(face.get(0));
                Point3D p2 = projectedPoints.get(face.get(1));
                Point3D p3 = projectedPoints.get(face.get(2));

                boolean shouldRender = shouldRenderTriangleWithNormals(
                        faceIndex, normalIndices, normals, insideView);

                if (shouldRender) {
                    if (face.size() == 3) {
                        _canvasController.drawTriangle(p1, p2, p3, paint);
                    } else {
                        Point3D tp1 = projectedPoints.get(face.get(0));
                        for (int i = 1; i < face.size() - 1; i++) {
                            Point3D tp2 = projectedPoints.get(face.get(i));
                            Point3D tp3 = projectedPoints.get(face.get(i + 1));
                            _canvasController.drawTriangle(tp1, tp2, tp3, paint);
                        }
                    }
                }
            }
        }
    }

    private boolean shouldRenderTriangleWithNormals(int faceIndex,
                                                   List<List<Integer>> normalIndices,
                                                   List<Point3D> normals, Boolean insideView) {
        if (normals.isEmpty() || normalIndices.isEmpty() || faceIndex >= normalIndices.size()) {
            return true;
        }

        List<Integer> faceNormalIndices = normalIndices.get(faceIndex);
        if (faceNormalIndices.isEmpty() || faceNormalIndices.get(0) == -1) {
            return true;
        }

        int normalIndex = faceNormalIndices.get(0);
        if (normalIndex >= normals.size()) {
            return true;
        }

        Point3D faceNormal = normals.get(normalIndex);

        Point3D cameraDir = _camera.getDir();

        double dotProduct = faceNormal.dotProduct(cameraDir);

        if (insideView) {
            return dotProduct > 0;
        } else {
            return dotProduct < 0;
        }
    }

    private List<Point3D> projectAllPoints(List<Point3D> points) {
        List<Point3D> projectedPoints = new ArrayList<>();
        for (Point3D point : points) {
            var projectedPoint = _projecter.project(point);
            projectedPoints.add(projectedPoint);
        }
        return projectedPoints;
    }
}
