package com.ofdun.interiordesigner.generators;

import com.ofdun.interiordesigner.models.Mesh;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RoomGenerator {

    public static List<Mesh> generateRoom(double width, double height, double depth, String baseId) {
        List<Mesh> roomParts = new ArrayList<>();

        double halfWidth = width / 2;
        double halfHeight = height / 2;
        double halfDepth = depth / 2;

        Color roomColor = Color.rgb(0xC1, 0x9A, 0x6B);

        roomParts.add(createFloor(halfWidth, halfHeight, halfDepth, baseId + "_floor", roomColor));
        roomParts.add(createLeftWall(halfWidth, halfHeight, halfDepth, baseId + "_left", roomColor));
        roomParts.add(createRightWall(halfWidth, halfHeight, halfDepth, baseId + "_right", roomColor));
        roomParts.add(createBackWall(halfWidth, halfHeight, halfDepth, baseId + "_back", roomColor));
        roomParts.add(createFrontWall(halfWidth, halfHeight, halfDepth, baseId + "_front", roomColor));
        roomParts.add(createCeiling(halfWidth, halfHeight, halfDepth, baseId + "_ceiling", roomColor));

        return roomParts;
    }

    private static Mesh createFloor(double hw, double hh, double hd, String id, Color color) {
        List<Point3D> vertices = new ArrayList<>();
        List<Point3D> normals = new ArrayList<>();
        List<List<Integer>> faces = new ArrayList<>();
        List<List<Integer>> normalIndices = new ArrayList<>();

        vertices.add(new Point3D(-hw, -hh, -hd));
        vertices.add(new Point3D(hw, -hh, -hd));
        vertices.add(new Point3D(hw, hh, -hd));
        vertices.add(new Point3D(-hw, hh, -hd));

        normals.add(new Point3D(0, 0, 1));

        faces.add(Arrays.asList(0, 1, 2, 3));

        normalIndices.add(Arrays.asList(0, 0, 0));
        normalIndices.add(Arrays.asList(0, 0, 0));

        return new Mesh(vertices, normals, Collections.emptyList(), faces, normalIndices, Collections.emptyList(), id, id, color, null, false);
    }

    private static Mesh createLeftWall(double hw, double hh, double hd, String id, Color color) {
        List<Point3D> vertices = new ArrayList<>();
        List<Point3D> normals = new ArrayList<>();
        List<List<Integer>> faces = new ArrayList<>();
        List<List<Integer>> normalIndices = new ArrayList<>();

        vertices.add(new Point3D(-hw, -hh, -hd));
        vertices.add(new Point3D(-hw, -hh, hd));
        vertices.add(new Point3D(-hw, hh, hd));
        vertices.add(new Point3D(-hw, hh, -hd));

        normals.add(new Point3D(1, 0, 0));

        faces.add(Arrays.asList(0, 1, 2, 3));

        normalIndices.add(Arrays.asList(0, 0, 0));
        normalIndices.add(Arrays.asList(0, 0, 0));

        return new Mesh(vertices, normals, Collections.emptyList(), faces, normalIndices, Collections.emptyList(), id, id, color, null, false);
    }

    private static Mesh createRightWall(double hw, double hh, double hd, String id, Color color) {
        List<Point3D> vertices = new ArrayList<>();
        List<Point3D> normals = new ArrayList<>();
        List<List<Integer>> faces = new ArrayList<>();
        List<List<Integer>> normalIndices = new ArrayList<>();

        vertices.add(new Point3D(hw, -hh, hd));
        vertices.add(new Point3D(hw, -hh, -hd));
        vertices.add(new Point3D(hw, hh, -hd));
        vertices.add(new Point3D(hw, hh, hd));

        normals.add(new Point3D(-1, 0, 0));

        faces.add(Arrays.asList(0, 1, 2, 3));

        normalIndices.add(Arrays.asList(0, 0, 0));
        normalIndices.add(Arrays.asList(0, 0, 0));

        return new Mesh(vertices, normals, Collections.emptyList(), faces, normalIndices, Collections.emptyList(), id, id, color, null, false);
    }

    private static Mesh createBackWall(double hw, double hh, double hd, String id, Color color) {
        List<Point3D> vertices = new ArrayList<>();
        List<Point3D> normals = new ArrayList<>();
        List<List<Integer>> faces = new ArrayList<>();
        List<List<Integer>> normalIndices = new ArrayList<>();

        vertices.add(new Point3D(-hw, -hh, -hd));
        vertices.add(new Point3D(hw, -hh, -hd));
        vertices.add(new Point3D(hw, -hh, hd));
        vertices.add(new Point3D(-hw, -hh, hd));

        normals.add(new Point3D(0, 1, 0));

        faces.add(Arrays.asList(0, 1, 2, 3));

        normalIndices.add(Arrays.asList(0, 0, 0));
        normalIndices.add(Arrays.asList(0, 0, 0));

        return new Mesh(vertices, normals, Collections.emptyList(), faces, normalIndices, Collections.emptyList(), id, id, color, null, false);
    }

    private static Mesh createFrontWall(double hw, double hh, double hd, String id, Color color) {
        List<Point3D> vertices = new ArrayList<>();
        List<Point3D> normals = new ArrayList<>();
        List<List<Integer>> faces = new ArrayList<>();
        List<List<Integer>> normalIndices = new ArrayList<>();

        vertices.add(new Point3D(-hw, hh, hd));
        vertices.add(new Point3D(hw, hh, hd));
        vertices.add(new Point3D(hw, hh, -hd));
        vertices.add(new Point3D(-hw, hh, -hd));

        normals.add(new Point3D(0, -1, 0));

        faces.add(Arrays.asList(0, 1, 2, 3));

        normalIndices.add(Arrays.asList(0, 0, 0));
        normalIndices.add(Arrays.asList(0, 0, 0));

        return new Mesh(vertices, normals, Collections.emptyList(), faces, normalIndices, Collections.emptyList(), id, id, color, null, false);
    }

    private static Mesh createCeiling(double hw, double hh, double hd, String id, Color color) {
        List<Point3D> vertices = new ArrayList<>();
        List<Point3D> normals = new ArrayList<>();
        List<List<Integer>> faces = new ArrayList<>();
        List<List<Integer>> normalIndices = new ArrayList<>();

        vertices.add(new Point3D(-hw, -hh, hd));
        vertices.add(new Point3D(hw, -hh, hd));
        vertices.add(new Point3D(hw, hh, hd));
        vertices.add(new Point3D(-hw, hh, hd));

        normals.add(new Point3D(0, 0, -1));

        faces.add(Arrays.asList(0, 1, 2, 3));

        normalIndices.add(Arrays.asList(0, 0, 0));
        normalIndices.add(Arrays.asList(0, 0, 0));

        return new Mesh(vertices, normals, Collections.emptyList(), faces, normalIndices, Collections.emptyList(), id, id, color, null, false);
    }
}
