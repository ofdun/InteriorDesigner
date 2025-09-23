package com.ofdun.interiordesigner.objectloaders;

import com.ofdun.interiordesigner.models.Mesh;
import com.ofdun.interiordesigner.objectreaders.IObjectReader;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.ejml.simple.SimpleMatrix;

import java.io.BufferedReader;

@Singleton
public class ObjectLoader {
    private final IObjectReader _objectReader;
    private static Integer _id = 0;

    @Inject
    public ObjectLoader(IObjectReader objectReader) {
        _objectReader = objectReader;
    }

    public Mesh load(BufferedReader buffer) {
        readObjectData(buffer);

        var mesh = buildMesh();
        _id++;

        return mesh;
    }

    private void readObjectData(BufferedReader buffer) {
        _objectReader.setReader(buffer);
        _objectReader.readAllVertices();
        _objectReader.readAllFaces();
        _objectReader.readAllNormals();
        _objectReader.readAllNormalIndices();
    }

    private Mesh buildMesh() {
        var vertices = _objectReader.readAllVertices();
        var faces = _objectReader.readAllFaces();
        var normals = _objectReader.readAllNormals();
        var normalIndecies = _objectReader.readAllNormalIndices();

        return new Mesh(
                vertices, normals, faces, normalIndecies, Integer.toString(_id)
        );
    }
}