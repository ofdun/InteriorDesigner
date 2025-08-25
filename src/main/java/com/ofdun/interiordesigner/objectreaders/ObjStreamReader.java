package com.ofdun.interiordesigner.objectreaders;

import jakarta.inject.Singleton;
import javafx.geometry.Point3D;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class ObjStreamReader implements IObjectReader {

    private List<Point3D> vertices;
    private List<List<Integer>> faces;
    private boolean isParsed = false;
    private BufferedReader reader;

    @Override
    public void setReader(BufferedReader reader) {
        if (this.reader != null) {
            try {
                this.reader.close();
            } catch (IOException ignored) {
            }
        }
        this.reader = reader;
        this.isParsed = false;
        this.vertices = new ArrayList<>();
        this.faces = new ArrayList<>();
    }

    private void parseFile() throws IOException {
        if (isParsed || reader == null) {
            return;
        }

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] tokens = line.split("\\s+");
            if (tokens.length == 0) {
                continue;
            }

            switch (tokens[0]) {
                case "v":
                    var point = new Point3D(
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3])
                    );
                    vertices.add(point);
                    break;
                case "f":
                    List<Integer> current = new ArrayList<>();
                    for (int i = 1; i < tokens.length; i++) {
                        String[] indices = tokens[i].split("/");
                        current.add(Integer.parseInt(indices[0]) - 1);
                    }
                    faces.add(current);
                    break;
                default:
                    break;
            }
        }
        isParsed = true;
    }

    @Override
    public List<Point3D> readAllVertices() {
        try {
            parseFile();
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        return vertices;
    }

    @Override
    public List<List<Integer>> readAllFaces() {
        try {
            parseFile();
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        return faces;
    }
}