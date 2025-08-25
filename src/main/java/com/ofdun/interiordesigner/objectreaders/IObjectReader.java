package com.ofdun.interiordesigner.objectreaders;

import javafx.geometry.Point3D;

import java.io.BufferedReader;
import java.util.List;

public interface IObjectReader {
    void setReader(BufferedReader reader);
    List<Point3D> readAllVertices();
    List<List<Integer>> readAllFaces();
}
