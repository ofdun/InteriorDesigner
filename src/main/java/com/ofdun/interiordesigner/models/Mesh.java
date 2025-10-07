package com.ofdun.interiordesigner.models;

import com.ofdun.interiordesigner.managers.Transformer;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.List;

public class Mesh {
    public static final int MOVE_STEP = 5;
    public static final double ROTATION_STEP = Math.toRadians(10);
    public static final double SCALE_STEP = 1.2;
    private final List<Point3D> _vertices;
    private final List<Point3D> _normals;
    private final List<List<Integer>> _faces;
    private final List<List<Integer>> _normalIndices;
    private final String _id;
    private final Paint _color;
    private Point3D _center;
    private SimpleMatrix _transformState;
    private final String ROOM_ID = "0";

    public Mesh(List<Point3D> vertices, List<Point3D> normals, List<List<Integer>> faces,
                List<List<Integer>> normalIndices, String id, Paint color) {
        _vertices = vertices;
        _normals = normals;
        _faces = faces;
        _normalIndices = normalIndices;
        _id = id;
        _transformState = SimpleMatrix.identity(4);

        if (_id.equals(ROOM_ID)) {
            _color = Color.rgb(0xC1, 0x9A, 0x6B);
        } else {
            _color = color;
        }

        calcCenter();
    }

    public Boolean isRoom() {
        return _id.equals(ROOM_ID);
    }

    public List<Point3D> getVertices() {
        return transformedVertices();
    }

    public List<Point3D> getNormals() {
        return transformedNormals();
    }

    public List<List<Integer>> getFaces() {
        return _faces;
    }

    public List<List<Integer>> getNormalIndices() {
        return _normalIndices;
    }

    public Paint getColor() {
        return _color;
    }

    public String getId() {
        return _id;
    }

    private void calcCenter() {
        if (_vertices.isEmpty()) {
            _center = new Point3D(0, 0, 0);
            return;
        }

        double x = 0, y = 0, z = 0;
        for (Point3D p : _vertices) {
            x += p.getX();
            y += p.getY();
            z += p.getZ();
        }

        _center = new Point3D(x / _vertices.size(), y / _vertices.size(), z / _vertices.size());
    }

    public Point3D getCenter() {
        return Transformer.transformPoint(_center, _transformState);
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
        this._transformState = _transformState.mult(toCenter).mult(t).mult(fromCenter);
    }

    private List<Point3D> transformedVertices() {
        return _vertices.stream()
                .map(v ->  Transformer.transformPoint(v, _transformState))
                .toList();
    }

    private List<Point3D> transformedNormals() {
        if (_normals.isEmpty()) {
            return new ArrayList<>();
        }

        SimpleMatrix rotationMatrix = Transformer.extractRotation(_transformState);

        return _normals.stream()
                .map(n -> Transformer.transformPoint(n, rotationMatrix))
                .toList();
    }

    public Point3D[] getBounds() {
        if (_vertices.isEmpty()) {
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

        return _id.equals(mesh._id);
    }

    @Override
    public int hashCode() {
        return Integer.parseInt(_id);
    }
}
