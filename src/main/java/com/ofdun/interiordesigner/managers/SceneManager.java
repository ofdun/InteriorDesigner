package com.ofdun.interiordesigner.managers;

import com.ofdun.interiordesigner.controllers.CanvasController;
import com.ofdun.interiordesigner.controllers.ControlsController;
import com.ofdun.interiordesigner.models.Camera;
import com.ofdun.interiordesigner.models.Mesh;
import com.ofdun.interiordesigner.models.Projecter;
import com.ofdun.interiordesigner.objectloaders.ObjectLoader;
import com.ofdun.interiordesigner.models.LightSource;
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
    private final LightingManager _lightingManager;
    private final CanvasController _canvasController;
    private final ControlsController _controlsController;
    private final ObjectLoader _objectLoader;
    private static final Logger log = LoggerFactory.getLogger(SceneManager.class);

    @Inject
    SceneManager(CanvasController canvasController, ControlsController controlsController,
                 ObjectLoader objectLoader, LightingManager lightingManager) {
        _canvasController = canvasController;
        _controlsController = controlsController;
        _objectLoader = objectLoader;
        _lightingManager = lightingManager;

        bindCanvasEvents();
        bindControlsEvents();
        _controlsController.bindChoiceBoxSelection(this::onLightChoiceSelected);
        _controlsController.bindLightIntensityChange(this::onLightIntensityChanged);
    }

    private void onLightChoiceSelected(String id) {
        var intensity = _controlsController.getLightIntensity();
        onLightChanged(id, intensity);
    }

    private void onLightChanged(String id, Double intensity) {
        if (id == null) {
            _lightingManager.clearLights();
        } else {
            Mesh mesh = _objects.get(id);
            if (mesh != null) {
                Point3D pos = mesh.getCenter();
                Point3D lightPos = new Point3D(pos.getX(), pos.getY(), pos.getZ());
                LightSource ls = new LightSource(lightPos, Color.WHITE, intensity, mesh.getId());
                log.info("Updated light source for mesh id {} at {} with intensity {}", mesh.getId(), lightPos, intensity);
                _lightingManager.setSingleLightSource(ls);
            } else {
                _lightingManager.clearLights();
            }
        }

        renderAllMeshes();
    }

    private void onLightIntensityChanged(Double intensity) {
        var id = _controlsController.getChoiceBoxSelection();

        onLightChanged(id, intensity);
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

                Point3D newCenter = mesh.getCenter();
                Point3D newLightPos = new Point3D(newCenter.getX(), newCenter.getY(), newCenter.getZ());
                _lightingManager.updateLightSourcePosition(selectedMeshId, newLightPos);
            }
        }
    }

    public void addMeshView(Mesh meshView) {
        _objects.put(meshView.getId(), meshView);

        if (meshView.isRoom()) {
            _camera.resetToRoom();
        } else {
            _controlsController.addObjectToObjectListView(meshView.getId());
            _controlsController.addChoiceBoxItem(meshView.getId());
        }
    }

    public Boolean removeMashById(String id) {
        _controlsController.removeObjectFromObjectListView(id);
        _controlsController.removeChoiceBoxItem(id);
        var res = _objects.remove(id);

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

            renderFaces(faces, normalIndices, projectedPoints, vertices, normals, mesh.isRoom(), color, mesh.getId());
        }

        _canvasController.render();
    }

    private void renderFaces(List<List<Integer>> faces, List<List<Integer>> normalIndices,
                           List<Point3D> projectedPoints, List<Point3D> worldVertices,
                           List<Point3D> normals, Boolean insideView, Paint paint, String meshId) {
        for (int faceIndex = 0; faceIndex < faces.size(); faceIndex++) {
            List<Integer> face = faces.get(faceIndex);

            if (face.size() >= 3) {
                boolean shouldRender = shouldRenderTriangleWithNormals(
                        faceIndex, normalIndices, normals, insideView);

                if (shouldRender) {
                    if (face.size() == 3) {
                        renderTriangleWithLighting(face, faceIndex, projectedPoints, worldVertices,
                                normalIndices, normals, paint, meshId);
                    } else {
                        var first = face.get(0);
                        for (int i = 1; i < face.size() - 1; i++) {
                            List<Integer> triangleFace = List.of(first, face.get(i), face.get(i + 1));
                            renderTriangleWithLighting(triangleFace, faceIndex, projectedPoints, worldVertices,
                                    normalIndices, normals, paint, meshId);
                        }
                    }
                }
            }
        }
    }

    private void renderTriangleWithLighting(List<Integer> face, int faceIndex, List<Point3D> projectedPoints,
                                          List<Point3D> worldVertices, List<List<Integer>> normalIndices,
                                          List<Point3D> normals, Paint basePaint, String meshId) {
        Point3D p1 = projectedPoints.get(face.get(0));
        Point3D p2 = projectedPoints.get(face.get(1));
        Point3D p3 = projectedPoints.get(face.get(2));

        Point3D n1 = null, n2 = null, n3 = null;
        if (!normals.isEmpty() && !normalIndices.isEmpty() && faceIndex < normalIndices.size()) {
            List<Integer> faceNormalIndices = normalIndices.get(faceIndex);
            if (faceNormalIndices.size() >= 3) {
                if (faceNormalIndices.get(0) < normals.size()) n1 = normals.get(faceNormalIndices.get(0));
                if (faceNormalIndices.get(1) < normals.size()) n2 = normals.get(faceNormalIndices.get(1));
                if (faceNormalIndices.get(2) < normals.size()) n3 = normals.get(faceNormalIndices.get(2));
            } else if (!faceNormalIndices.isEmpty() && faceNormalIndices.get(0) < normals.size()) {
                Point3D sharedNormal = normals.get(faceNormalIndices.get(0));
                n1 = n2 = n3 = sharedNormal;
            }
        }

        Point3D w1 = worldVertices.get(face.get(0));
        Point3D w2 = worldVertices.get(face.get(1));
        Point3D w3 = worldVertices.get(face.get(2));

        if (n1 == null || n2 == null || n3 == null) {
            Point3D edge1 = w2.subtract(w1);
            Point3D edge2 = w3.subtract(w1);
            Point3D faceNormal = edge1.crossProduct(edge2).normalize();
            n1 = n2 = n3 = faceNormal;
        }

        List<Mesh> allMeshes = new ArrayList<>(_objects.values());

        var triangleData = new CanvasController.TriangleLightingData(
             p1, p2, p3,
            w1, w2, w3,
            n1, n2, n3,
            _camera.getDir().multiply(-1),
            (basePaint instanceof Color) ? (Color) basePaint : Color.GRAY,
            _lightingManager,
            allMeshes,
            meshId
        );

        _canvasController.drawTriangleWithLighting(triangleData);
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
