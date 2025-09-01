package com.ofdun.interiordesigner.models;

import com.ofdun.interiordesigner.managers.Transformer;
import javafx.geometry.Point3D;
import org.ejml.simple.SimpleMatrix;

import java.util.List;

import javafx.geometry.Point3D;
import org.ejml.simple.SimpleMatrix;

import java.util.List;

public class Mesh {
    public static final int MOVE_STEP = 5;
    public static final double ROTATION_STEP = Math.toRadians(1);
    private final List<Point3D> vertices;
    private final List<List<Integer>> faces;
    private final String id;
    private SimpleMatrix transformState;

    public Mesh(List<Point3D> vertices, List<List<Integer>> faces, String id) {
        this.vertices = vertices;
        this.faces = faces;
        this.id = id;
        this.transformState = SimpleMatrix.identity(4);
    }

    public List<Point3D> getVertices() {
        return transformedVertices();
    }

    public List<List<Integer>> getFaces() {
        return faces;
    }

    public String getId() {
        return id;
    }

    public void applyTransform(SimpleMatrix t) {
        this.transformState = transformState.mult(t);
    }

    private List<Point3D> transformedVertices() {
        return vertices.stream()
                .map(v -> Transformer.transformPoint(v, transformState))
                .toList();
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
