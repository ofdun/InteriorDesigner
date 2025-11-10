package com.ofdun.interiordesigner.generators;

import com.ofdun.interiordesigner.models.Mesh;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DoorGenerator {

    public static Mesh generateDoor(String wallId, double roomWidth, double roomDepth,
                                    double doorWidth, double doorHeight,
                                    double offsetX, double offsetZ, String id) {
        List<Point3D> vertices = new ArrayList<>();
        List<Point3D> normals = new ArrayList<>();
        List<List<Integer>> faces = new ArrayList<>();
        List<List<Integer>> normalIndices = new ArrayList<>();

        Color doorColor = Color.rgb(139, 90, 43);

        double halfWidth = doorWidth / 2;
        double halfHeight = doorHeight / 2;

        double halfRoomWidth = roomWidth / 2;
        double halfRoomDepth = roomDepth / 2;

        if (wallId.endsWith("_back")) {
            createBackWallDoor(vertices, normals, faces, normalIndices,
                                offsetX, offsetZ, halfWidth, halfHeight, halfRoomDepth);
        } else if (wallId.endsWith("_front")) {
            createFrontWallDoor(vertices, normals, faces, normalIndices,
                                 offsetX, offsetZ, halfWidth, halfHeight, halfRoomDepth);
        } else if (wallId.endsWith("_left")) {
            createLeftWallDoor(vertices, normals, faces, normalIndices,
                                offsetX, offsetZ, halfWidth, halfHeight, halfRoomWidth);
        } else if (wallId.endsWith("_right")) {
            createRightWallDoor(vertices, normals, faces, normalIndices,
                                 offsetX, offsetZ, halfWidth, halfHeight, halfRoomWidth);
        }

        return new Mesh(vertices, normals, Collections.emptyList(), faces,
                       normalIndices, Collections.emptyList(), id, "Door", doorColor, null, false);
    }

    private static void createBackWallDoor(List<Point3D> vertices, List<Point3D> normals,
                                          List<List<Integer>> faces, List<List<Integer>> normalIndices,
                                          double offsetX, double offsetZ, double hw, double hh, double halfRoomDepth) {
        double wallY = -halfRoomDepth + 0.1;

        normals.add(new Point3D(0, 1, 0));

        int doorStart = vertices.size();
        vertices.add(new Point3D(offsetX - hw, wallY, offsetZ - hh));
        vertices.add(new Point3D(offsetX + hw, wallY, offsetZ - hh));
        vertices.add(new Point3D(offsetX + hw, wallY, offsetZ + hh));
        vertices.add(new Point3D(offsetX - hw, wallY, offsetZ + hh));
        faces.add(Arrays.asList(doorStart, doorStart + 1, doorStart + 2, doorStart + 3));
        normalIndices.add(Arrays.asList(0, 0, 0, 0));
    }

    private static void createFrontWallDoor(List<Point3D> vertices, List<Point3D> normals,
                                           List<List<Integer>> faces, List<List<Integer>> normalIndices,
                                           double offsetX, double offsetZ, double hw, double hh, double halfRoomDepth) {
        double wallY = halfRoomDepth - 0.1;

        normals.add(new Point3D(0, -1, 0));

        int doorStart = vertices.size();
        vertices.add(new Point3D(offsetX - hw, wallY, offsetZ - hh));
        vertices.add(new Point3D(offsetX + hw, wallY, offsetZ - hh));
        vertices.add(new Point3D(offsetX + hw, wallY, offsetZ + hh));
        vertices.add(new Point3D(offsetX - hw, wallY, offsetZ + hh));
        faces.add(Arrays.asList(doorStart, doorStart + 1, doorStart + 2, doorStart + 3));
        normalIndices.add(Arrays.asList(0, 0, 0, 0));
    }

    private static void createLeftWallDoor(List<Point3D> vertices, List<Point3D> normals,
                                          List<List<Integer>> faces, List<List<Integer>> normalIndices,
                                          double offsetY, double offsetZ, double hd, double hh, double halfRoomWidth) {
        double wallX = -halfRoomWidth + 0.1;

        normals.add(new Point3D(1, 0, 0));

        int doorStart = vertices.size();
        vertices.add(new Point3D(wallX, offsetY - hd, offsetZ - hh));
        vertices.add(new Point3D(wallX, offsetY + hd, offsetZ - hh));
        vertices.add(new Point3D(wallX, offsetY + hd, offsetZ + hh));
        vertices.add(new Point3D(wallX, offsetY - hd, offsetZ + hh));
        faces.add(Arrays.asList(doorStart, doorStart + 1, doorStart + 2, doorStart + 3));
        normalIndices.add(Arrays.asList(0, 0, 0, 0));
    }

    private static void createRightWallDoor(List<Point3D> vertices, List<Point3D> normals,
                                           List<List<Integer>> faces, List<List<Integer>> normalIndices,
                                           double offsetY, double offsetZ, double hd, double hh, double halfRoomWidth) {
        double wallX = halfRoomWidth - 0.1;

        normals.add(new Point3D(-1, 0, 0));

        int doorStart = vertices.size();
        vertices.add(new Point3D(wallX, offsetY - hd, offsetZ - hh));
        vertices.add(new Point3D(wallX, offsetY + hd, offsetZ - hh));
        vertices.add(new Point3D(wallX, offsetY + hd, offsetZ + hh));
        vertices.add(new Point3D(wallX, offsetY - hd, offsetZ + hh));
        faces.add(Arrays.asList(doorStart, doorStart + 1, doorStart + 2, doorStart + 3));
        normalIndices.add(Arrays.asList(0, 0, 0, 0));
    }
}

