package com.ofdun.interiordesigner.managers;

import com.ofdun.interiordesigner.controllers.CanvasController;
import com.ofdun.interiordesigner.controllers.ControlsController;
import com.ofdun.interiordesigner.models.Mesh;
import com.ofdun.interiordesigner.objectloaders.ObjectLoader;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.geometry.Point3D;
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
    private final Camera _camera = new Camera();
    private final Projecter _projecter = new Projecter(_camera, 1000, 800);
    private final CanvasController _canvasController;
    private final ControlsController _controlsController;
    private final ObjectLoader _objectLoader;
    private static final Logger log = LoggerFactory.getLogger(SceneManager.class);

    @Inject
    SceneManager(CanvasController canvasController, ControlsController controlsController, ObjectLoader objectLoader) {
        _canvasController = canvasController;
        _controlsController = controlsController;
        _objectLoader = objectLoader;

        bindEvents();
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

    private void bindEvents() {
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

        _controlsController.bindButtonEvent("cameraXPlus", () ->
                _camera.transform(new SimpleMatrix(new double[][] {
                        {1, 0, 0, 0},
                        {0, 1, 0, 0},
                        {0, 0, 1, 0},
                        {Camera.MOVE_STEP, 0, 0, 1}
                }))
        );

        _controlsController.bindButtonEvent("cameraXMinus", () ->
                _camera.transform(new SimpleMatrix(new double[][] {
                        {1, 0, 0, 0},
                        {0, 1, 0, 0},
                        {0, 0, 1, 0},
                        {-Camera.MOVE_STEP, 0, 0, 1}
                }))
        );

        _controlsController.bindButtonEvent("cameraYPlus", () ->
                _camera.transform(new SimpleMatrix(new double[][] {
                        {1, 0, 0, 0},
                        {0, 1, 0, 0},
                        {0, 0, 1, 0},
                        {0, Camera.MOVE_STEP, 0, 1}
                }))
        );

        _controlsController.bindButtonEvent("cameraYMinus", () ->
                _camera.transform(new SimpleMatrix(new double[][] {
                        {1, 0, 0, 0},
                        {0, 1, 0, 0},
                        {0, 0, 1, 0},
                        {0, -Camera.MOVE_STEP, 0, 1}
                }))
        );

        _controlsController.bindButtonEvent("cameraZPlus", () ->
                _camera.transform(new SimpleMatrix(new double[][] {
                        {1, 0, 0, 0},
                        {0, 1, 0, 0},
                        {0, 0, 1, 0},
                        {0, 0, Camera.MOVE_STEP, 1}
                }))
        );

        _controlsController.bindButtonEvent("cameraZMinus", () ->
                _camera.transform(new SimpleMatrix(new double[][] {
                        {1, 0, 0, 0},
                        {0, 1, 0, 0},
                        {0, 0, 1, 0},
                        {0, 0, -Camera.MOVE_STEP, 1}
                }))
        );

        _controlsController.bindButtonEvent("cameraXAnglePlus", () -> {
            double cos = Math.cos(Camera.ROTATION_STEP);
            double sin = Math.sin(Camera.ROTATION_STEP);

            _camera.transform(new SimpleMatrix(new double[][]{
                    {1,   0,    0, 0},
                    {0, cos, -sin, 0},
                    {0, sin,  cos, 0},
                    {0,   0,    0, 1}
            }));
        });

        _controlsController.bindButtonEvent("cameraXAngleMinus", () -> {
            double cos = Math.cos(-Camera.ROTATION_STEP);
            double sin = Math.sin(-Camera.ROTATION_STEP);

            _camera.transform(new SimpleMatrix(new double[][]{
                    {1,   0,    0, 0},
                    {0, cos, -sin, 0},
                    {0, sin,  cos, 0},
                    {0,   0,    0, 1}
            }));
        });

        _controlsController.bindButtonEvent("cameraYAnglePlus", () -> {
            double cos = Math.cos(Camera.ROTATION_STEP);
            double sin = Math.sin(Camera.ROTATION_STEP);

            _camera.transform(new SimpleMatrix(new double[][]{
                    { cos, 0, sin, 0},
                    {   0, 1,   0, 0},
                    {-sin, 0, cos, 0},
                    {   0, 0,   0, 1}
            }));
        });

        _controlsController.bindButtonEvent("cameraYAngleMinus", () -> {
            double cos = Math.cos(-Camera.ROTATION_STEP);
            double sin = Math.sin(-Camera.ROTATION_STEP);

            _camera.transform(new SimpleMatrix(new double[][]{
                    { cos, 0, sin, 0},
                    {   0, 1,   0, 0},
                    {-sin, 0, cos, 0},
                    {   0, 0,   0, 1}
            }));
        });

        _controlsController.bindButtonEvent("cameraZAnglePlus", () -> {
            double cos = Math.cos(Camera.ROTATION_STEP);
            double sin = Math.sin(Camera.ROTATION_STEP);

            _camera.transform(new SimpleMatrix(new double[][]{
                    {cos, -sin, 0, 0},
                    {sin,  cos, 0, 0},
                    {  0,    0, 1, 0},
                    {  0,    0, 0, 1}
            }));
        });

        _controlsController.bindButtonEvent("cameraZAngleMinus", () -> {
            double cos = Math.cos(-Camera.ROTATION_STEP);
            double sin = Math.sin(-Camera.ROTATION_STEP);

            _camera.transform(new SimpleMatrix(new double[][]{
                    {cos, -sin, 0, 0},
                    {sin,  cos, 0, 0},
                    {  0,    0, 1, 0},
                    {  0,    0, 0, 1}
            }));
        });

        _controlsController.bindButtonEvent("objectXPlus", () ->
                transformSelectedMeshes(new SimpleMatrix(new double[][] {
                        {1, 0, 0, 0},
                        {0, 1, 0, 0},
                        {0, 0, 1, 0},
                        {Mesh.MOVE_STEP, 0, 0, 1}
                }))
        );

        _controlsController.bindButtonEvent("objectXMinus", () ->
                transformSelectedMeshes(new SimpleMatrix(new double[][] {
                        {1, 0, 0, 0},
                        {0, 1, 0, 0},
                        {0, 0, 1, 0},
                        {-Mesh.MOVE_STEP, 0, 0, 1}
                }))
        );

        _controlsController.bindButtonEvent("objectYPlus", () ->
                transformSelectedMeshes(new SimpleMatrix(new double[][] {
                        {1, 0, 0, 0},
                        {0, 1, 0, 0},
                        {0, 0, 1, 0},
                        {0, Mesh.MOVE_STEP, 0, 1}
                }))
        );

        _controlsController.bindButtonEvent("objectYMinus", () ->
                transformSelectedMeshes(new SimpleMatrix(new double[][] {
                        {1, 0, 0, 0},
                        {0, 1, 0, 0},
                        {0, 0, 1, 0},
                        {0, -Mesh.MOVE_STEP, 0, 1}
                }))
        );

        _controlsController.bindButtonEvent("objectZPlus", () ->
                transformSelectedMeshes(new SimpleMatrix(new double[][] {
                        {1, 0, 0, 0},
                        {0, 1, 0, 0},
                        {0, 0, 1, 0},
                        {0, 0, Mesh.MOVE_STEP, 1}
                }))
        );

        _controlsController.bindButtonEvent("objectZMinus", () ->
                transformSelectedMeshes(new SimpleMatrix(new double[][] {
                        {1, 0, 0, 0},
                        {0, 1, 0, 0},
                        {0, 0, 1, 0},
                        {0, 0, -Mesh.MOVE_STEP, 1}
                }))
        );

        _controlsController.bindButtonEvent("objectXAnglePlus", () -> {
            double cos = Math.cos(Mesh.ROTATION_STEP);
            double sin = Math.sin(Mesh.ROTATION_STEP);

            transformSelectedMeshes(new SimpleMatrix(new double[][] {
                    {1,   0,    0, 0},
                    {0, cos, -sin, 0},
                    {0, sin,  cos, 0},
                    {0,   0,    0, 1}
            }));
        });

        _controlsController.bindButtonEvent("objectXAngleMinus", () -> {
            double cos = Math.cos(-Mesh.ROTATION_STEP);
            double sin = Math.sin(-Mesh.ROTATION_STEP);

            transformSelectedMeshes(new SimpleMatrix(new double[][] {
                    {1,   0,    0, 0},
                    {0, cos, -sin, 0},
                    {0, sin,  cos, 0},
                    {0,   0,    0, 1}
            }));
        });

        _controlsController.bindButtonEvent("objectYAnglePlus", () -> {
            double cos = Math.cos(Mesh.ROTATION_STEP);
            double sin = Math.sin(Mesh.ROTATION_STEP);

            transformSelectedMeshes(new SimpleMatrix(new double[][] {
                    { cos, 0, sin, 0},
                    {   0, 1,   0, 0},
                    {-sin, 0, cos, 0},
                    {   0, 0,   0, 1}
            }));
        });

        _controlsController.bindButtonEvent("objectYAngleMinus", () -> {
            double cos = Math.cos(-Mesh.ROTATION_STEP);
            double sin = Math.sin(-Mesh.ROTATION_STEP);

            transformSelectedMeshes(new SimpleMatrix(new double[][] {
                    { cos, 0, sin, 0},
                    {   0, 1,   0, 0},
                    {-sin, 0, cos, 0},
                    {   0, 0,   0, 1}
            }));
        });

        _controlsController.bindButtonEvent("objectZAnglePlus", () -> {
            double cos = Math.cos(Mesh.ROTATION_STEP);
            double sin = Math.sin(Mesh.ROTATION_STEP);

            transformSelectedMeshes(new SimpleMatrix(new double[][] {
                    {cos, -sin, 0, 0},
                    {sin,  cos, 0, 0},
                    {  0,    0, 1, 0},
                    {  0,    0, 0, 1}
            }));
        });

        _controlsController.bindButtonEvent("objectZAngleMinus", () -> {
            double cos = Math.cos(-Mesh.ROTATION_STEP);
            double sin = Math.sin(-Mesh.ROTATION_STEP);

            transformSelectedMeshes(new SimpleMatrix(new double[][] {
                    {cos, -sin, 0, 0},
                    {sin,  cos, 0, 0},
                    {  0,    0, 1, 0},
                    {  0,    0, 0, 1}
            }));
        });

    }

    public void addMeshView(Mesh meshView) {
        _objects.put(meshView.getId(), meshView);
        _controlsController.addObjectToObjectListView(meshView.getId());
    }

    public Boolean removeMashById(String id) {
        _controlsController.removeObjectFromObjectListView(id);
        var res = _objects.remove(id);

        renderAllMeshes();

        return res != null;
    }

    public void renderAllMeshes() {
        for (Mesh mesh : _objects.values()) {
//            log.info("rendering {}", mesh.id());
            var vertices = mesh.getVertices();
            var faces =  mesh.getFaces();
            var projectedPoints = projectAllPoints(vertices);

            renderFaces(faces, projectedPoints);
        }

        _canvasController.render();
    }

    private void renderFaces(List<List<Integer>> faces, List<Point3D> points) {
        for (List<Integer> face : faces) {
            for (int i = 0; i < face.size(); i++) {
                _canvasController.drawLine(
                        points.get(face.get(i)), points.get(face.get((i + 1) % face.size()))
                );
            }
        }
    }

    private List<Point3D> projectAllPoints(List<Point3D> points) {
        List<Point3D> projectedPoints = new ArrayList<>();
        for (Point3D point : points) {
            var projectedPoint = _projecter.project(point);
//            log.info("projectedPoint {} {}", projectedPoint.getX(), projectedPoint.getY());
            projectedPoints.add(projectedPoint);
        }
        return projectedPoints;
    }
}

class Camera {
    static final int MOVE_STEP = 10;
    static final double ROTATION_STEP = Math.toRadians(1);

    private Point3D position;
    private Point3D up;
    private Point3D dir;
    private Point3D right;

    public Camera() {
        this.position = new Point3D(30,-100, 50);
        this.up = new Point3D(0,0, 1);
        this.dir = new Point3D(0,1, 0);
        this.right = new Point3D(1,0, 0);
    }

    public Point3D getPosition() {
        return position;
    }

    public Point3D getUp() {
        return up;
    }

    public Point3D getDir() {
        return dir;
    }

    public Point3D getRight() {
        return right;
    }

    public void transform(SimpleMatrix transformationMatrix) {
        var decomposed = Transformer.decomposeMatrix(transformationMatrix);

        position = Transformer.transformPoint(position, decomposed[0]);
        right = Transformer.transformPoint(right, decomposed[1]);
        up = Transformer.transformPoint(up, decomposed[1]);
        dir = Transformer.transformPoint(dir, decomposed[1]);
    }

    @Override
    public String toString() {
        return "Camera{" +
                "position=" + position +
                ", up=" + up +
                ", dir=" + dir +
                ", right=" + right +
                '}';
    }
}

class Projecter {
    Camera _camera;
    Integer _width;
    Integer _height;

    Projecter(Camera camera, Integer width, Integer height) {
        _camera = camera;
        _width = width;
        _height = height;
    }

    public Point3D project(Point3D point) {
        var relative = new Point3D(
                point.getX() - _camera.getPosition().getX(),
                point.getY() - _camera.getPosition().getY(),
                point.getZ() - _camera.getPosition().getZ()
        );

        var xCam = relative.dotProduct(_camera.getRight());
        var yCam = relative.dotProduct(_camera.getUp());
        var zCam = relative.dotProduct(_camera.getDir());

        zCam = Math.max(zCam, 1e-6);

        double x = xCam / zCam;
        double y = yCam / zCam;

        double screenX = (x + 1) * _width / 2;
        double screenY = (1 - y) * _height / 2;

        return new Point3D(screenX, screenY, zCam);
    }
}
