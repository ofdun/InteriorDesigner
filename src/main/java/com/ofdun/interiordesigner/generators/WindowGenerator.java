package com.ofdun.interiordesigner.generators;

import com.ofdun.interiordesigner.models.Mesh;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WindowGenerator {

    public static Mesh generateWindow(String wallId, double roomWidth, double roomDepth,
                                      double windowWidth, double windowHeight,
                                      double offsetX, double offsetZ, String id) {
        List<Point3D> vertices = new ArrayList<>();
        List<Point3D> normals = new ArrayList<>();
        List<List<Integer>> faces = new ArrayList<>();
        List<List<Integer>> normalIndices = new ArrayList<>();

        Color windowColor = Color.valueOf("FFFFFF");

        double halfWidth = windowWidth / 2;
        double halfHeight = windowHeight / 2;

        double halfRoomWidth = roomWidth / 2;
        double halfRoomDepth = roomDepth / 2;

        if (wallId.endsWith("_back")) {
            createBackWallWindow(vertices, normals, faces, normalIndices,
                                offsetX, offsetZ, halfWidth, halfHeight, halfRoomDepth);
        } else if (wallId.endsWith("_front")) {
            createFrontWallWindow(vertices, normals, faces, normalIndices,
                                 offsetX, offsetZ, halfWidth, halfHeight, halfRoomDepth);
        } else if (wallId.endsWith("_left")) {
            createLeftWallWindow(vertices, normals, faces, normalIndices,
                                offsetX, offsetZ, halfWidth, halfHeight, halfRoomWidth);
        } else if (wallId.endsWith("_right")) {
            createRightWallWindow(vertices, normals, faces, normalIndices,
                                 offsetX, offsetZ, halfWidth, halfHeight, halfRoomWidth);
        }

        return new Mesh(vertices, normals, Collections.emptyList(), faces,
                       normalIndices, Collections.emptyList(), id, "Window", windowColor, null, false);
    }

    private static void createBackWallWindow(List<Point3D> vertices, List<Point3D> normals,
                                            List<List<Integer>> faces, List<List<Integer>> normalIndices,
                                            double offsetX, double offsetZ, double hw, double hh, double halfRoomDepth) {
        double wallY = -halfRoomDepth + 0.1;

        normals.add(new Point3D(0, 1, 0));

        int glassStart = vertices.size();
        vertices.add(new Point3D(offsetX - hw, wallY, offsetZ - hh));
        vertices.add(new Point3D(offsetX + hw, wallY, offsetZ - hh));
        vertices.add(new Point3D(offsetX + hw, wallY, offsetZ + hh));
        vertices.add(new Point3D(offsetX - hw, wallY, offsetZ + hh));
        faces.add(Arrays.asList(glassStart, glassStart + 1, glassStart + 2, glassStart + 3));
        normalIndices.add(Arrays.asList(0, 0, 0, 0));
    }

    private static void createFrontWallWindow(List<Point3D> vertices, List<Point3D> normals,
                                             List<List<Integer>> faces, List<List<Integer>> normalIndices,
                                             double offsetX, double offsetZ, double hw, double hh, double halfRoomDepth) {
        double wallY = halfRoomDepth - 0.1;

        normals.add(new Point3D(0, -1, 0));

        int glassStart = vertices.size();
        vertices.add(new Point3D(offsetX - hw, wallY, offsetZ - hh));
        vertices.add(new Point3D(offsetX + hw, wallY, offsetZ - hh));
        vertices.add(new Point3D(offsetX + hw, wallY, offsetZ + hh));
        vertices.add(new Point3D(offsetX - hw, wallY, offsetZ + hh));
        faces.add(Arrays.asList(glassStart, glassStart + 1, glassStart + 2, glassStart + 3));
        normalIndices.add(Arrays.asList(0, 0, 0, 0));
    }

    private static void createLeftWallWindow(List<Point3D> vertices, List<Point3D> normals,
                                            List<List<Integer>> faces, List<List<Integer>> normalIndices,
                                            double offsetY, double offsetZ, double hd, double hh, double halfRoomWidth) {
        double wallX = -halfRoomWidth + 0.1;

        normals.add(new Point3D(1, 0, 0));

        int glassStart = vertices.size();
        vertices.add(new Point3D(wallX, offsetY - hd, offsetZ - hh));
        vertices.add(new Point3D(wallX, offsetY + hd, offsetZ - hh));
        vertices.add(new Point3D(wallX, offsetY + hd, offsetZ + hh));
        vertices.add(new Point3D(wallX, offsetY - hd, offsetZ + hh));
        faces.add(Arrays.asList(glassStart, glassStart + 1, glassStart + 2, glassStart + 3));
        normalIndices.add(Arrays.asList(0, 0, 0, 0));
    }

    private static void createRightWallWindow(List<Point3D> vertices, List<Point3D> normals,
                                             List<List<Integer>> faces, List<List<Integer>> normalIndices,
                                             double offsetY, double offsetZ, double hd, double hh, double halfRoomWidth) {
        double wallX = halfRoomWidth - 0.1;

        normals.add(new Point3D(-1, 0, 0));

        int glassStart = vertices.size();
        vertices.add(new Point3D(wallX, offsetY - hd, offsetZ - hh));
        vertices.add(new Point3D(wallX, offsetY + hd, offsetZ - hh));
        vertices.add(new Point3D(wallX, offsetY + hd, offsetZ + hh));
        vertices.add(new Point3D(wallX, offsetY - hd, offsetZ + hh));
        faces.add(Arrays.asList(glassStart, glassStart + 1, glassStart + 2, glassStart + 3));
        normalIndices.add(Arrays.asList(0, 0, 0, 0));
    }
}

