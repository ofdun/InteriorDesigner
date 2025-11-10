package com.ofdun.interiordesigner.managers;

import com.ofdun.interiordesigner.controllers.CanvasController;
import com.ofdun.interiordesigner.controllers.ControlsController;
import com.ofdun.interiordesigner.generators.DoorGenerator;
import com.ofdun.interiordesigner.generators.WindowGenerator;
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
import javafx.stage.DirectoryChooser;
import javafx.geometry.Point2D;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

@Singleton
public class SceneManager {
    private final Map<String, Mesh> objects = new HashMap<>();
    private final Map<String, String> displayNameToId = new HashMap<>();
    private final Map<String, Integer> displayNameCounter = new HashMap<>();
    private final Camera camera = new Camera(this);
    private final Projecter projecter = new Projecter(camera, 1000, 800);
    private final LightingManager lightingManager;
    private final CanvasController canvasController;
    private final ControlsController controlsController;
    private final ObjectLoader objectLoader;
    private static final Logger log = LoggerFactory.getLogger(SceneManager.class);

    private double roomWidth = 100;
    private double roomHeight = 100;
    private double roomDepth = 100;
    private Color wallsColor = Color.rgb(0xC1, 0x9A, 0x6B);
    private Color floorColor = Color.rgb(0xC1, 0x9A, 0x6B);
    private Color ceilingColor = Color.rgb(0xC1, 0x9A, 0x6B);
    private int windowCounter = 0;
    private int doorCounter = 0;

    @Inject
    SceneManager(CanvasController canvasController, ControlsController controlsController,
                 ObjectLoader objectLoader, LightingManager lightingManager) {
        this.canvasController = canvasController;
        this.controlsController = controlsController;
        this.objectLoader = objectLoader;
        this.lightingManager = lightingManager;

        bindCanvasEvents();
        bindControlsEvents();
        this.controlsController.bindChoiceBoxSelection(this::onLightChoiceSelected);
        this.controlsController.bindLightIntensityChange(this::onLightIntensityChanged);
        this.controlsController.bindWallsColorChange(this::onWallsColorChanged);
        this.controlsController.bindFloorColorChange(this::onFloorColorChanged);
        this.controlsController.bindCeilingColorChange(this::onCeilingColorChanged);
        this.controlsController.bindRoomSizeChange(this::onRoomSizeChanged);
    }

    private void onLightChoiceSelected(String displayName) {
        var intensity = controlsController.getLightIntensity();
        onLightChanged(displayName, intensity);
    }

    private void onLightChanged(String displayName, Double intensity) {
        if (displayName == null) {
            lightingManager.clearLights();
        } else {
            String id = displayNameToId.get(displayName);
            if (id == null) {
                id = displayName;
            }

            Mesh mesh = objects.get(id);
            if (mesh != null) {
                Point3D pos = mesh.getCenter();
                Point3D lightPos = new Point3D(pos.getX(), pos.getY(), pos.getZ());
                LightSource ls = new LightSource(lightPos, Color.WHITE, intensity, mesh.getId());
                log.info("Updated light source for mesh id {} at {} with intensity {}", mesh.getId(), lightPos, intensity);
                lightingManager.setSingleLightSource(ls);
            } else {
                lightingManager.clearLights();
            }
        }

        renderAllMeshes();
    }

    private void onLightIntensityChanged(Double intensity) {
        var id = controlsController.getChoiceBoxSelection();

        onLightChanged(id, intensity);
    }

    private void onWallsColorChanged(Color color) {
        this.wallsColor = color;
        for (Mesh mesh : objects.values()) {
            if (mesh.isWall()) {
                mesh.setColor(color);
            }
        }
        renderAllMeshes();
    }

    private void onFloorColorChanged(Color color) {
        this.floorColor = color;
        for (Mesh mesh : objects.values()) {
            if (mesh.isFloor()) {
                mesh.setColor(color);
            }
        }
        renderAllMeshes();
    }

    private void onCeilingColorChanged(Color color) {
        this.ceilingColor = color;
        for (Mesh mesh : objects.values()) {
            if (mesh.isCeiling()) {
                mesh.setColor(color);
            }
        }
        renderAllMeshes();
    }

    private void onRoomSizeChanged(Double width, Double height, Double depth) {
        this.roomWidth = width;
        this.roomHeight = height;
        this.roomDepth = depth;
        recreateRoom();
    }

    private void recreateRoom() {
        List<String> roomPartIds = new ArrayList<>();
        for (Mesh mesh : objects.values()) {
            if (mesh.isRoom()) {
                roomPartIds.add(mesh.getId());
            }
        }

        for (String id : roomPartIds) {
            objects.remove(id);
        }

        List<String> windowAndDoorDisplayNames = new ArrayList<>();
        for (Map.Entry<String, String> entry : displayNameToId.entrySet()) {
            String displayName = entry.getKey();
            String id = entry.getValue();
            Mesh mesh = objects.get(id);
            if (mesh != null && (mesh.isWindow() || mesh.isDoor())) {
                windowAndDoorDisplayNames.add(displayName);
            }
        }

        for (String displayName : windowAndDoorDisplayNames) {
            String id = displayNameToId.get(displayName);
            objects.remove(id);
            displayNameToId.remove(displayName);
        }

        if (!windowAndDoorDisplayNames.isEmpty()) {
            controlsController.removeMultipleObjectsFromObjectListView(windowAndDoorDisplayNames);
        }

        windowCounter = 0;
        doorCounter = 0;
        displayNameCounter.put("Window", 1);
        displayNameCounter.put("Door", 1);

        var roomParts = com.ofdun.interiordesigner.generators.RoomGenerator.generateRoom(
            roomWidth, roomHeight, roomDepth, "0");

        for (var roomPart : roomParts) {
            if (roomPart.isWall()) {
                roomPart.setColor(wallsColor);
            } else if (roomPart.isFloor()) {
                roomPart.setColor(floorColor);
            } else if (roomPart.isCeiling()) {
                roomPart.setColor(ceilingColor);
            }
            objects.put(roomPart.getId(), roomPart);
        }

        camera.resetToRoom();

        renderAllMeshes();
    }

    public List<Mesh> getAllMeshes() {
        return List.copyOf(objects.values());
    }

    private void bindCanvasEvents() {
        canvasController.bindMouseEventCallback("cameraOrbit", camera::orbit);
        canvasController.bindEventCallback("render", this::renderAllMeshes);
    }

    private void bindControlsEvents() {
        controlsController.bindButtonEvent("render", this::renderAllMeshes);
        controlsController.bindButtonEvent("objectAdd", () -> {
            var directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose Object Folder");

            var stage = controlsController.getStage();

            File dir = directoryChooser.showDialog(stage);
            if (dir != null) {
                log.info("Selected folder: {}", dir.getAbsolutePath());
                File[] files = dir.listFiles((ignored, name) -> name.toLowerCase().endsWith(".obj"));

                if (files == null || files.length == 0) {
                    log.info("No .obj files found in the selected folder.");
                    return;
                }

                log.info("Selected .obj file: {}", files[0].getAbsolutePath());
                try (var stream = new BufferedReader(new FileReader(files[0]))) {
                    var meshView = objectLoader.load(stream, dir);
                    addMeshView(meshView);
                    renderAllMeshes();
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }
        });

        controlsController.bindButtonEvent("objectRemove", () -> {
            for (var selectedMeshId : controlsController.getHighlightedListView()) {
                removeMashById(selectedMeshId);
            }
        });

        controlsController.bindButtonEvent("objectXPlus", () ->
                transformSelectedMeshes(TransformMatrices.translateX(Mesh.MOVE_STEP)));
        controlsController.bindButtonEvent("objectXMinus", () ->
                transformSelectedMeshes(TransformMatrices.translateX(-Mesh.MOVE_STEP)));
        controlsController.bindButtonEvent("objectYPlus", () ->
                transformSelectedMeshes(TransformMatrices.translateY(Mesh.MOVE_STEP)));
        controlsController.bindButtonEvent("objectYMinus", () ->
                transformSelectedMeshes(TransformMatrices.translateY(-Mesh.MOVE_STEP)));
        controlsController.bindButtonEvent("objectZPlus", () ->
                transformSelectedMeshes(TransformMatrices.translateZ(Mesh.MOVE_STEP)));
        controlsController.bindButtonEvent("objectZMinus", () ->
                transformSelectedMeshes(TransformMatrices.translateZ(-Mesh.MOVE_STEP)));

        controlsController.bindButtonEvent("objectXAnglePlus", () ->
                transformSelectedMeshes(TransformMatrices.rotateX(Mesh.ROTATION_STEP)));
        controlsController.bindButtonEvent("objectXAngleMinus", () ->
                transformSelectedMeshes(TransformMatrices.rotateX(-Mesh.ROTATION_STEP)));
        controlsController.bindButtonEvent("objectYAnglePlus", () ->
                transformSelectedMeshes(TransformMatrices.rotateY(Mesh.ROTATION_STEP)));
        controlsController.bindButtonEvent("objectYAngleMinus", () ->
                transformSelectedMeshes(TransformMatrices.rotateY(-Mesh.ROTATION_STEP)));
        controlsController.bindButtonEvent("objectZAnglePlus", () ->
                transformSelectedMeshes(TransformMatrices.rotateZ(Mesh.ROTATION_STEP)));
        controlsController.bindButtonEvent("objectZAngleMinus", () ->
                transformSelectedMeshes(TransformMatrices.rotateZ(-Mesh.ROTATION_STEP)));

        controlsController.bindButtonEvent("objectScalePlus", () ->
                transformSelectedMeshes(TransformMatrices.scale(Mesh.SCALE_STEP)));
        controlsController.bindButtonEvent("objectScaleMinus", () ->
                transformSelectedMeshes(TransformMatrices.scale(1.0 / Mesh.SCALE_STEP)));

        controlsController.bindButtonEvent("cameraZoomIn", () -> {
            camera.zoom(0.95);
            renderAllMeshes();
        });
        controlsController.bindButtonEvent("cameraZoomOut", () -> {
            camera.zoom(1.05);
            renderAllMeshes();
        });

        controlsController.bindButtonEvent("addWindow", this::onAddWindow);
        controlsController.bindButtonEvent("addDoor", this::onAddDoor);
    }

    private void onAddWindow() {
        String selectedWall = controlsController.getSelectedWall();
        if (selectedWall == null) {
            return;
        }

        String wallId = switch (selectedWall) {
            case "Задняя стена" -> "0_back";
            case "Передняя стена" -> "0_front";
            case "Левая стена" -> "0_left";
            case "Правая стена" -> "0_right";
            default -> null;
        };

        if (wallId == null) {
            return;
        }

        double windowWidth = Math.min(roomWidth, roomDepth) * 0.2;
        double windowHeight = roomHeight * 0.2;

        double offsetX = 0;
        double offsetZ = 0;

        windowCounter++;
        String windowId = "window_" + windowCounter;

        Mesh window = WindowGenerator.generateWindow(
            wallId, roomWidth, roomDepth, windowWidth, windowHeight, offsetX, offsetZ, windowId
        );

        addMeshView(window);
        renderAllMeshes();
    }

    private void onAddDoor() {
        String selectedWall = controlsController.getSelectedDoorWall();
        if (selectedWall == null) {
            return;
        }

        String wallId = switch (selectedWall) {
            case "Задняя стена" -> "0_back";
            case "Передняя стена" -> "0_front";
            case "Левая стена" -> "0_left";
            case "Правая стена" -> "0_right";
            default -> null;
        };

        if (wallId == null) {
            return;
        }

        double doorWidth = Math.min(roomWidth, roomDepth) * 0.15;
        double doorHeight = roomHeight * 0.7;

        double offsetX = 0;
        double offsetZ = -roomHeight / 2 + doorHeight / 2;

        doorCounter++;
        String doorId = "door_" + doorCounter;

        Mesh door = DoorGenerator.generateDoor(
            wallId, roomWidth, roomDepth, doorWidth, doorHeight, offsetX, offsetZ, doorId
        );

        addMeshView(door);
        renderAllMeshes();
    }

    private void transformSelectedMeshes(SimpleMatrix transform) {
        for (String selectedDisplayName : controlsController.getHighlightedListView()) {
            String id = displayNameToId.get(selectedDisplayName);
            if (id == null) {
                id = selectedDisplayName;
            }

            var mesh = objects.get(id);
//            log.info(mesh.getCenter().toString());
            if (mesh != null) {
                mesh.applyTransform(transform);

                Point3D newCenter = mesh.getCenter();
                Point3D newLightPos = new Point3D(newCenter.getX(), newCenter.getY(), newCenter.getZ());
                lightingManager.updateLightSourcePosition(id, newLightPos);
            }
        }
    }

    public void initializeRoom(double width, double height, double depth) {
        this.roomWidth = width;
        this.roomHeight = height;
        this.roomDepth = depth;

        var roomParts = com.ofdun.interiordesigner.generators.RoomGenerator.generateRoom(
            width, height, depth, "0");

        for (var roomPart : roomParts) {
            addMeshView(roomPart);
        }
    }

    public void addMeshView(Mesh meshView) {
        objects.put(meshView.getId(), meshView);

        if (meshView.isRoom()) {
            camera.resetToRoom();
        } else {
            String baseName = meshView.getDisplayName();
            String uniqueDisplayName = baseName;

            if (displayNameToId.containsKey(uniqueDisplayName)) {
                int counter = displayNameCounter.getOrDefault(baseName, 1);
                counter++;
                displayNameCounter.put(baseName, counter);
                uniqueDisplayName = baseName + " (" + counter + ")";
            } else {
                displayNameCounter.putIfAbsent(baseName, 1);
            }

            displayNameToId.put(uniqueDisplayName, meshView.getId());
            controlsController.addObjectToObjectListView(uniqueDisplayName);

            if (meshView.getCanBeLightningSource()) {
                controlsController.addLightningSourceChoiceBoxItem(uniqueDisplayName);
            }
        }
    }

    public Boolean removeMashById(String displayName) {
        String id = displayNameToId.get(displayName);
        if (id == null) {
            id = displayName;
        }

        controlsController.removeObjectFromObjectListView(displayName);
        controlsController.removeLightningSourceChoiceBoxItem(displayName);
        displayNameToId.remove(displayName);
        var res = objects.remove(id);

        return res != null;
    }

    public Mesh getMeshById(String id) {
        return objects.get(id);
    }

    private void renderRoom() {
        var roomMeshes = new ArrayList<Mesh>();
        var windowMeshes = new ArrayList<Mesh>();
        var doorMeshes = new ArrayList<Mesh>();

        for (Mesh mesh : objects.values()) {
            if (mesh.isWindow()) {
                windowMeshes.add(mesh);
            } else if (mesh.isDoor()) {
                doorMeshes.add(mesh);
            } else if (mesh.isRoom()) {
                roomMeshes.add(mesh);
            }
        }

        for (Mesh mesh : roomMeshes) {
            renderMesh(mesh);
            var vertices = mesh.getVertices();
            renderEdges(mesh.getFaces(), mesh.getNormalIndices(), mesh.getNormals(), projectAllPoints(vertices));
        }

        for (Mesh mesh : windowMeshes) {
            renderMesh(mesh);
            var vertices = mesh.getVertices();
            renderEdges(mesh.getFaces(), mesh.getNormalIndices(), mesh.getNormals(), projectAllPoints(vertices));
        }

        for (Mesh mesh : doorMeshes) {
            renderMesh(mesh);
            var vertices = mesh.getVertices();
            renderEdges(mesh.getFaces(), mesh.getNormalIndices(), mesh.getNormals(), projectAllPoints(vertices));
        }
    }

    public void renderAllMeshes() {
        renderRoom();

        for (Mesh mesh : objects.values()) {
            if (mesh.isRoom() || mesh.isWindow() || mesh.isDoor()) {
                continue;
            }
            renderMesh(mesh);
        }

        canvasController.render();
    }

    private void renderMesh(Mesh mesh) {
        var vertices = mesh.getVertices();
        var normals = mesh.getNormals();
        var faces = mesh.getFaces();
        var normalIndices = mesh.getNormalIndices();
        var projectedPoints = projectAllPoints(vertices);
        var color = mesh.getColor();

        renderFaces(faces, normalIndices, projectedPoints, vertices, normals, color, mesh.getId());
    }

    private void renderFaces(List<List<Integer>> faces, List<List<Integer>> normalIndices,
                           List<Point3D> projectedPoints, List<Point3D> worldVertices,
                           List<Point3D> normals, Paint paint, String meshId) {
        Mesh mesh = objects.get(meshId);
        var textureCoords = mesh != null ? mesh.getTextureCoords() : new ArrayList<Point2D>();
        var textureIndices = mesh != null ? mesh.getTextureIndices() : new ArrayList<List<Integer>>();
        var texture = mesh != null ? mesh.getTexture() : null;

        for (int faceIndex = 0; faceIndex < faces.size(); faceIndex++) {
            List<Integer> face = faces.get(faceIndex);

            if (face.size() >= 3) {
                boolean shouldRender = shouldRenderTriangleWithNormals(
                        faceIndex, normalIndices, normals);

                if (shouldRender) {
                    if (face.size() == 3) {
                        renderTriangleWithLighting(face, faceIndex, projectedPoints, worldVertices,
                                normalIndices, normals, paint, meshId, textureCoords, textureIndices, texture);
                    } else {
                        var first = face.get(0);
                        for (int i = 1; i < face.size() - 1; i++) {
                            List<Integer> triangleFace = List.of(first, face.get(i), face.get(i + 1));
                            renderTriangleWithLighting(triangleFace, faceIndex, projectedPoints, worldVertices,
                                    normalIndices, normals, paint, meshId, textureCoords, textureIndices, texture);
                        }
                    }
                }
            }
        }
    }

    private void renderEdges(List<List<Integer>> faces, List<List<Integer>> normalIndices,
                            List<Point3D> normals, List<Point3D> projectedPoints) {
        for (int faceIndex = 0; faceIndex < faces.size(); faceIndex++) {
            List<Integer> face = faces.get(faceIndex);

            if (face.size() >= 3) {
                boolean shouldRender = shouldRenderTriangleWithNormals(faceIndex, normalIndices, normals);

                if (shouldRender) {
                    for (int i = 0; i < face.size(); i++) {
                        int currentIdx = face.get(i);
                        int nextIdx = face.get((i + 1) % face.size());

                        Point3D p1 = projectedPoints.get(currentIdx);
                        Point3D p2 = projectedPoints.get(nextIdx);

                        canvasController.drawLine(p1, p2, Color.rgb(60, 60, 60), 1.5);
                    }
                }
            }
        }
    }

    private void renderTriangleWithLighting(List<Integer> face, int faceIndex, List<Point3D> projectedPoints,
                                          List<Point3D> worldVertices, List<List<Integer>> normalIndices,
                                          List<Point3D> normals, Paint basePaint, String meshId,
                                          List<Point2D> textureCoords, List<List<Integer>> textureIndices,
                                          javafx.scene.image.Image texture) {
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

        Point2D uv1 = null, uv2 = null, uv3 = null;
        if (!textureCoords.isEmpty() && !textureIndices.isEmpty() && faceIndex < textureIndices.size()) {
            List<Integer> faceTexIndices = textureIndices.get(faceIndex);
            if (faceTexIndices.size() >= 3) {
                int idx0 = faceTexIndices.get(0);
                int idx1 = faceTexIndices.get(1);
                int idx2 = faceTexIndices.get(2);

                if (idx0 >= 0 && idx0 < textureCoords.size()) uv1 = textureCoords.get(idx0);
                if (idx1 >= 0 && idx1 < textureCoords.size()) uv2 = textureCoords.get(idx1);
                if (idx2 >= 0 && idx2 < textureCoords.size()) uv3 = textureCoords.get(idx2);
            }
        }

        List<Mesh> allMeshes = new ArrayList<>(objects.values());

        var triangleData = new CanvasController.TriangleLightingData(
             p1, p2, p3,
            w1, w2, w3,
            n1, n2, n3,
            camera.getDir().multiply(-1),
            (basePaint instanceof Color) ? (Color) basePaint : Color.GRAY,
                lightingManager,
            allMeshes,
            meshId,
            uv1, uv2, uv3,
            texture
        );

        canvasController.drawTriangleWithLighting(triangleData);
    }

    private boolean shouldRenderTriangleWithNormals(int faceIndex,
                                                   List<List<Integer>> normalIndices,
                                                   List<Point3D> normals) {
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

        Point3D cameraDir = camera.getDir();

        double BIAS = 1e-2;
        double dotProduct = faceNormal.dotProduct(cameraDir) - BIAS;

        return dotProduct < 0;
    }

    private List<Point3D> projectAllPoints(List<Point3D> points) {
        List<Point3D> projectedPoints = new ArrayList<>();
        for (Point3D point : points) {
            var projectedPoint = projecter.project(point);
            projectedPoints.add(projectedPoint);
        }
        return projectedPoints;
    }
}
