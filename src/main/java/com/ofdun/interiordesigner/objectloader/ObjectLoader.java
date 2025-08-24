package com.ofdun.interiordesigner.objectloader;

import com.ofdun.interiordesigner.objectreader.IObjectReader;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

import java.io.BufferedReader;
import java.util.List;

@Singleton
public class ObjectLoader {
    private final IObjectReader objectReader;

    @Inject
    public ObjectLoader(IObjectReader objectReader) {
        this.objectReader = objectReader;
    }

    public MeshView load(BufferedReader buffer) {
        readObjectData(buffer);

        TriangleMesh mesh = buildMesh();

        return new MeshView(mesh);
    }

    private void readObjectData(BufferedReader buffer) {
        objectReader.setReader(buffer);
        objectReader.readAllVertices();
        objectReader.readAllFaces();
    }

    private TriangleMesh buildMesh() {
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);

        fillMeshVertices(mesh);
        fillMeshFaces(mesh);

        addTexCoords(mesh);

        return mesh;
    }

    private void fillMeshVertices(TriangleMesh mesh) {
        List<Float> vertices = objectReader.readAllVertices();
        float[] pointsArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            pointsArray[i] = vertices.get(i);
        }
        mesh.getPoints().setAll(pointsArray);
    }

    private void fillMeshFaces(TriangleMesh mesh) {
        List<Integer> faces = objectReader.readAllFaces();
        int[] facesArray = new int[faces.size() * 2];
        for (int i = 0; i < faces.size(); i++) {
            facesArray[i * 2] = faces.get(i);
            facesArray[i * 2 + 1] = 0; // texture
        }
        mesh.getFaces().setAll(facesArray);
    }

    private void addTexCoords(TriangleMesh mesh) {
        mesh.getTexCoords().addAll(0.5f, 0.5f);
    }
}