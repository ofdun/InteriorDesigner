package com.ofdun.interiordesigner.models;

import com.ofdun.interiordesigner.managers.Transformer;
import javafx.geometry.Point3D;
import org.ejml.simple.SimpleMatrix;

import java.util.List;

public class Mesh {
    public static final int MOVE_STEP = 5;
    public static final double ROTATION_STEP = Math.toRadians(1);
    private final List<Point3D> _vertices;
    private final List<List<Integer>> _faces;
    private final String _id;
    private Point3D _center;
    private SimpleMatrix _transformState;

    public Mesh(List<Point3D> vertices, List<List<Integer>> faces, String id) {
        _vertices = vertices;
        _faces = faces;
        _id = id;
        _transformState = SimpleMatrix.identity(4);

        calcCenter();
    }

    public List<Point3D> getVertices() {
        return transformedVertices();
    }

    public List<List<Integer>> getFaces() {
        return _faces;
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
            p = Transformer.transformPoint(p, _transformState);
            x += p.getX();
            y += p.getY();
            z += p.getZ();
        }

        _center = new Point3D(x / _vertices.size(),y / _vertices.size(),z / _vertices.size());
    }

    private Point3D getCenter() {
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
