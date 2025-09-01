package com.ofdun.interiordesigner.managers;

import javafx.geometry.Point3D;
import org.ejml.simple.SimpleMatrix;

public class Transformer {
    public static SimpleMatrix[] decomposeMatrix(SimpleMatrix matrix) {
        SimpleMatrix[] matrices = new SimpleMatrix[2];

        matrices[0] = new SimpleMatrix(new double[][]{
                {1, 0, 0, matrix.get(0, 3)},
                {0, 1, 0, matrix.get(1, 3)},
                {0, 0, 1, matrix.get(2, 3)},
                {matrix.get(3, 0), matrix.get(3, 1), matrix.get(3, 2), 1}
        });

        matrices[1] = new SimpleMatrix(new double[][]{
                {matrix.get(0, 0), matrix.get(0, 1), matrix.get(0, 2), 0},
                {matrix.get(1, 0), matrix.get(1, 1), matrix.get(1, 2), 0},
                {matrix.get(2, 0), matrix.get(2, 1), matrix.get(2, 2), 0},
                {0, 0, 0, 1}
        });

        return matrices;
    }

    public static Point3D transformPoint(Point3D point, SimpleMatrix matrix) {
        var cur = new SimpleMatrix(
                new double[][]{{point.getX(), point.getY(), point.getZ(), 1}}
        );
        var transformed = cur.mult(matrix);
        return new Point3D(transformed.get(0, 0), transformed.get(0, 1), transformed.get(0, 2));
    }
}
