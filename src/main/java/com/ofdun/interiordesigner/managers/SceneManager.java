package com.ofdun.interiordesigner.managers;

import com.ofdun.interiordesigner.controllers.CanvasController;
import com.ofdun.interiordesigner.controllers.ControlsController;
import com.ofdun.interiordesigner.models.Mesh;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.geometry.Point3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class SceneManager {
    private final List<Mesh> _objects = new ArrayList<>();
    private final Camera _camera = Camera.DefaultCamera();
    private final Projecter _projecter = new Projecter(_camera, 500, 500);
    private final CanvasController _canvasController;
    private final ControlsController _controlsController;
    private static final Logger log = LoggerFactory.getLogger(SceneManager.class);

    @Inject
    SceneManager(CanvasController canvasController, ControlsController controlsController) {
        _canvasController = canvasController;
        _controlsController = controlsController;

        _controlsController.bindDrawCubeButtonPressedEvent(this::renderAllMeshes);
    }

    public void addMeshView(Mesh meshView) {
        _objects.add(meshView);
    }

    public Boolean removeMashById(String id) {
        return _objects.removeIf(
                mesh -> mesh.id().equals(id)
        );
    }

    public void renderAllMeshes() {
        for (Mesh mesh : _objects) {
            log.info("rendering {}", mesh.id());
            var points = mesh.vertices();
            var faces =  mesh.faces();
            var projectedPoints = projectAllPoints(points);

            renderFaces(faces, projectedPoints);
        }
    }

    private void renderFaces(List<List<Integer>> faces, List<Point3D> points) {
        for (List<Integer> face : faces) {
            for (int i = 0; i < face.size(); i++) {
                _canvasController.drawLine(
                        points.get(face.get(i)), points.get(face.get((i + 1) % face.size()))
                );
            }
        }

        _canvasController.render();
    }

    private List<Point3D> projectAllPoints(List<Point3D> points) {
        List<Point3D> projectedPoints = new ArrayList<>();
        for (Point3D point : points) {
            var projectedPoint = _projecter.project(point);
            log.info("projectedPoint {} {}", projectedPoint.getX(), projectedPoint.getY());
            projectedPoints.add(projectedPoint);
        }
        return projectedPoints;
    }
}

record Camera(Point3D position,
              Point3D up,
              Point3D dir,
              Point3D right) {
    public static Camera DefaultCamera() {
        return new Camera(
                new Point3D(30,-100, 50),
                new Point3D(0,0, 1),
                new Point3D(0,1, 0),
                new Point3D(1,0, 0)
        );
    }
}

class Projecter {
    Camera _camera = Camera.DefaultCamera();
    Integer _width;
    Integer _height;

    Projecter(Camera camera, Integer width, Integer height) {
        _camera = camera;
        _width = width;
        _height = height;
    }

    public Point3D project(Point3D point) {
        var relative = new Point3D(
                point.getX() - _camera.position().getX(),
                point.getY() - _camera.position().getY(),
                point.getZ() - _camera.position().getZ()
        );

        var xCam = relative.dotProduct(_camera.right());
        var yCam = relative.dotProduct(_camera.up());
        var zCam = relative.dotProduct(_camera.dir());

        zCam = Math.max(zCam, 1e-6);

        double x = xCam / zCam;
        double y = yCam / zCam;

        double screenX = (x + 1) * _width / 2;
        double screenY = (1 - y) * _height / 2;

        return new Point3D(screenX, screenY, zCam);
    }
}
