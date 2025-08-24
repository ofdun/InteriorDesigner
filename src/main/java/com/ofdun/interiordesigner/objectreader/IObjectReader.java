package com.ofdun.interiordesigner.objectreader;

import java.io.BufferedReader;
import java.util.List;

public interface IObjectReader {
    void setReader(BufferedReader reader);
    List<Float> readAllVertices();
    List<Integer> readAllFaces();
}
