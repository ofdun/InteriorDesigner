package com.ofdun.interiordesigner.objectloaders;

import com.ofdun.interiordesigner.models.Mesh;
import com.ofdun.interiordesigner.objectreaders.IObjectReader;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;

@Singleton
public class ObjectLoader {
    private static final Logger log = LoggerFactory.getLogger(ObjectLoader.class);
    private final IObjectReader objectReader;
    private static Integer id = 1;
    private File currentDirectory;

    @Inject
    public ObjectLoader(IObjectReader objectReader) {
        this.objectReader = objectReader;
    }

    public Mesh load(BufferedReader buffer, File directory) {
        currentDirectory = directory;
        readObjectData(buffer);

        var mesh = buildMesh();
        id++;

        return mesh;
    }

    private void readObjectData(BufferedReader buffer) {
        objectReader.setReader(buffer);
        objectReader.readAllVertices();
        objectReader.readAllFaces();
        objectReader.readAllNormals();
        objectReader.readAllNormalIndices();
        objectReader.readAllTextureCoords();
        objectReader.readAllTextureIndices();
    }

    private Mesh buildMesh() {
        var vertices = objectReader.readAllVertices();
        var faces = objectReader.readAllFaces();
        var normals = objectReader.readAllNormals();
        var normalIndices = objectReader.readAllNormalIndices();
        var textureCoords = objectReader.readAllTextureCoords();
        var textureIndices = objectReader.readAllTextureIndices();

        Image texture = loadTexture();

        String displayName = currentDirectory != null ? currentDirectory.getName() : Integer.toString(id);

        return new Mesh(
                vertices, normals, textureCoords, faces, normalIndices, textureIndices,
                Integer.toString(id), displayName, Color.GRAY, texture, true
        );
    }

    private Image loadTexture() {
        if (currentDirectory == null) {
            return null;
        }

        File[] imageFiles = currentDirectory.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".png") || lowerName.endsWith(".jpg") ||
                   lowerName.endsWith(".jpeg") || lowerName.endsWith(".bmp");
        });

        if (imageFiles != null && imageFiles.length > 0) {
            try {
                log.info("Loading texture: {}", imageFiles[0].getAbsolutePath());
                return new Image(new FileInputStream(imageFiles[0]));
            } catch (Exception e) {
                log.error("Failed to load texture: {}", e.getMessage());
            }
        }

        File texturesDir = new File(currentDirectory, "textures");
        if (texturesDir.exists() && texturesDir.isDirectory()) {
            imageFiles = texturesDir.listFiles((dir, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.endsWith(".png") || lowerName.endsWith(".jpg") ||
                       lowerName.endsWith(".jpeg") || lowerName.endsWith(".bmp");
            });

            if (imageFiles != null && imageFiles.length > 0) {
                try {
                    log.info("Loading texture from textures folder: {}", imageFiles[0].getAbsolutePath());
                    return new Image(new FileInputStream(imageFiles[0]));
                } catch (Exception e) {
                    log.error("Failed to load texture: {}", e.getMessage());
                }
            }
        }

        return null;
    }
}