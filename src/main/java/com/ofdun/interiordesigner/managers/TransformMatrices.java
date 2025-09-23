package com.ofdun.interiordesigner.managers;

import org.ejml.simple.SimpleMatrix;

public class TransformMatrices {
    public static SimpleMatrix translateX(double step) {
        return new SimpleMatrix(new double[][] {
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {step, 0, 0, 1}
        });
    }

    public static SimpleMatrix translateY(double step) {
        return new SimpleMatrix(new double[][] {
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, step, 0, 1}
        });
    }

    public static SimpleMatrix translateZ(double step) {
        return new SimpleMatrix(new double[][] {
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, step, 1}
        });
    }

    public static SimpleMatrix rotateX(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        return new SimpleMatrix(new double[][] {
                {1,   0,    0, 0},
                {0, cos, -sin, 0},
                {0, sin,  cos, 0},
                {0,   0,    0, 1}
        });
    }

    public static SimpleMatrix rotateY(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        return new SimpleMatrix(new double[][] {
                { cos, 0, sin, 0},
                {   0, 1,   0, 0},
                {-sin, 0, cos, 0},
                {   0, 0,   0, 1}
        });
    }

    public static SimpleMatrix rotateZ(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        return new SimpleMatrix(new double[][] {
                {cos, -sin, 0, 0},
                {sin,  cos, 0, 0},
                {  0,    0, 1, 0},
                {  0,    0, 0, 1}
        });
    }
}
