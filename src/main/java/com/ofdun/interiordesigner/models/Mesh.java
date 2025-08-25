package com.ofdun.interiordesigner.models;

import javafx.geometry.Point3D;

import java.util.List;

public record Mesh(List<Point3D> vertices, List<List<Integer>> faces, String id) {
}
