package com.ofdun.interiordesigner.objectreader;

import jakarta.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class ObjStreamReader implements IObjectReader {

    private List<Float> vertices;
    private List<Integer> faces;
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
                    vertices.add(Float.parseFloat(tokens[1]));
                    vertices.add(Float.parseFloat(tokens[2]));
                    vertices.add(Float.parseFloat(tokens[3]));
                    break;
                case "f":
                    for (int i = 1; i < tokens.length; i++) {
                        String[] indices = tokens[i].split("/");
                        faces.add(Integer.parseInt(indices[0]) - 1);
                    }
                    break;
                default:
                    break;
            }
        }
        isParsed = true;
    }

    @Override
    public List<Float> readAllVertices() {
        try {
            parseFile();
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        return vertices;
    }

    @Override
    public List<Integer> readAllFaces() {
        try {
            parseFile();
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        return faces;
    }
}