package com.ofdun.interiordesigner.models;

import com.ofdun.interiordesigner.managers.Transformer;
import javafx.geometry.Point3D;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.image.Image;
import lombok.Getter;
import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.List;

public class Mesh {
    public static final int MOVE_STEP = 5;
    public static final double ROTATION_STEP = Math.toRadians(10);
    public static final double SCALE_STEP = 1.2;
    private final List<Point3D> vertices;
    private final List<Point3D> normals;

    @Getter
    private final List<Point2D> textureCoords;

    @Getter
    private final List<List<Integer>> faces;

    @Getter
    private final List<List<Integer>> normalIndices;

    @Getter
    private final List<List<Integer>> textureIndices;

    @Getter
    private final String id;

    @Getter
    private final Paint color;

    @Getter
    private final Image texture;

    private Point3D center;
    private SimpleMatrix transformState;
    private final String ROOM_ID = "0";

    public Mesh(List<Point3D> vertices, List<Point3D> normals, List<Point2D> textureCoords,
                List<List<Integer>> faces, List<List<Integer>> normalIndices,
                List<List<Integer>> textureIndices, String id, Paint color, Image texture) {
        this.vertices = vertices;
        this.normals = normals;
        this.textureCoords = textureCoords;
        this.faces = faces;
        this.normalIndices = normalIndices;
        this.textureIndices = textureIndices;
        this.id = id;
        this.texture = texture;
        transformState = SimpleMatrix.identity(4);

        if (isRoom()) {
            this.color = Color.rgb(0xC1, 0x9A, 0x6B);
        } else {
            this.color = color;
        }

        calcCenter();
    }

    public Boolean isRoom() {
        return id.equals(ROOM_ID) || id.startsWith(ROOM_ID + "_");
    }

    public List<Point3D> getVertices() {
        return transformedVertices();
    }

    public List<Point3D> getNormals() {
        return transformedNormals();
    }

    private void calcCenter() {
        if (vertices.isEmpty()) {
            center = new Point3D(0, 0, 0);
            return;
        }

        double x = 0, y = 0, z = 0;
        for (Point3D p : vertices) {
            x += p.getX();
            y += p.getY();
            z += p.getZ();
        }

        center = new Point3D(x / vertices.size(), y / vertices.size(), z / vertices.size());
    }

    public Point3D getCenter() {
        return Transformer.transformPoint(center, transformState);
    }

    public void applyTransform(SimpleMatrix t) {
        var center = getCenter();
        var toCenter = new SimpleMatrix(new double[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {-center.getX(), -center.getY(), -center.getZ(), 1},
        });
        var fromCenter = new SimpleMatrix(new double[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {center.getX(), center.getY(), center.getZ(), 1},
        });
        this.transformState = transformState.mult(toCenter).mult(t).mult(fromCenter);
    }

    private List<Point3D> transformedVertices() {
        return vertices.stream()
                .map(v ->  Transformer.transformPoint(v, transformState))
                .toList();
    }

    private List<Point3D> transformedNormals() {
        if (normals.isEmpty()) {
            return new ArrayList<>();
        }

        SimpleMatrix rotationMatrix = Transformer.extractRotation(transformState);

        return normals.stream()
                .map(n -> Transformer.transformPoint(n, rotationMatrix))
                .toList();
    }

    public Point3D[] getBounds() {
        if (vertices.isEmpty()) {
            return new Point3D[]{new Point3D(0, 0, 0), new Point3D(0, 0, 0)};
        }

        List<Point3D> transformedVertices = getVertices();
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

        for (Point3D vertex : transformedVertices) {
            minX = Math.min(minX, vertex.getX());
            minY = Math.min(minY, vertex.getY());
            minZ = Math.min(minZ, vertex.getZ());
            maxX = Math.max(maxX, vertex.getX());
            maxY = Math.max(maxY, vertex.getY());
            maxZ = Math.max(maxZ, vertex.getZ());
        }

        return new Point3D[]{new Point3D(minX, minY, minZ), new Point3D(maxX, maxY, maxZ)};
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Mesh mesh))
            return false;

        return id.equals(mesh.id);
    }

    @Override
    public int hashCode() {
        return Integer.parseInt(id);
    }
}
