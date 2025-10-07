package com.ofdun.interiordesigner.models;

import com.ofdun.interiordesigner.managers.SceneManager;
import com.ofdun.interiordesigner.managers.Transformer;
import javafx.geometry.Point3D;
import lombok.Getter;
import org.ejml.simple.SimpleMatrix;

public class Camera {
    @Getter
    private Point3D position;
    @Getter
    private Point3D up;
    @Getter
    private Point3D dir;
    @Getter
    private Point3D right;

    private final SceneManager sceneManager;

    public Camera(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        initializeCamera();
    }

    private void initializeCamera() {
        Point3D roomCenter = getRoomCenter();
        this.position = calculateInitialPosition(roomCenter);
        setupCameraVectors(roomCenter);
    }

    public void resetToRoom() {
        initializeCamera();
    }

    public void orbit(double yawAngle, double pitchAngle) {
        Point3D target = getRoomCenter();

        Point3D cameraToTarget = target.subtract(position);

        double radius = calculateDistance(cameraToTarget);
        double theta = Math.atan2(cameraToTarget.getY(), cameraToTarget.getX());
        double phi = Math.acos(cameraToTarget.getZ() / radius);

        theta += yawAngle;
        phi += pitchAngle;

        phi = Math.max(0.1, Math.min(Math.PI - 0.1, phi));

        double x = radius * Math.sin(phi) * Math.cos(theta);
        double y = radius * Math.sin(phi) * Math.sin(theta);
        double z = radius * Math.cos(phi);

        this.position = target.subtract(new Point3D(x, y, z));
        setupCameraVectors(target);
    }

    private Point3D getRoomCenter() {
        Mesh roomMesh = sceneManager.getMeshById("0");

        if (roomMesh != null) {
            Point3D[] bounds = roomMesh.getBounds();
            return new Point3D(
                    (bounds[0].getX() + bounds[1].getX()) / 2,
                    (bounds[0].getY() + bounds[1].getY()) / 2,
                    (bounds[0].getZ() + bounds[1].getZ()) / 2
            );
        } else {
            return new Point3D(100, 100, 50);
        }
    }

    private Point3D calculateInitialPosition(Point3D roomCenter) {
        Mesh roomMesh = sceneManager.getMeshById("0");

        if (roomMesh != null) {
            Point3D[] roomBounds = roomMesh.getBounds();
            double roomWidth = roomBounds[1].getX() - roomBounds[0].getX();
            double roomDepth = roomBounds[1].getY() - roomBounds[0].getY();
            double roomHeight = roomBounds[1].getZ() - roomBounds[0].getZ();

            double distance = Math.max(Math.max(roomWidth, roomDepth), roomHeight) * 0.8;

            return new Point3D(
                    roomBounds[0].getX() - distance * 0.5,
                    roomBounds[0].getY() - distance * 0.5,
                    roomCenter.getZ() + distance * 0.7
            );
        } else {
            return new Point3D(-50, -50, 150);
        }
    }

    private void setupCameraVectors(Point3D target) {
        this.up = new Point3D(0, 0, 1);

        Point3D dirVector = target.subtract(this.position);
        double length = calculateDistance(dirVector);
        this.dir = new Point3D(dirVector.getX() / length,
                dirVector.getY() / length,
                dirVector.getZ() / length);

        this.right = calculateCrossProduct(this.dir, this.up);
        this.right = normalizeVector(this.right);

        this.up = calculateCrossProduct(this.right, this.dir);
    }

    private double calculateDistance(Point3D vector) {
        return Math.sqrt(vector.getX() * vector.getX() +
                vector.getY() * vector.getY() +
                vector.getZ() * vector.getZ());
    }

    private Point3D calculateCrossProduct(Point3D a, Point3D b) {
        return new Point3D(
                a.getY() * b.getZ() - a.getZ() * b.getY(),
                a.getZ() * b.getX() - a.getX() * b.getZ(),
                a.getX() * b.getY() - a.getY() * b.getX()
        );
    }

    private Point3D normalizeVector(Point3D vector) {
        double length = calculateDistance(vector);
        if (length > 1e-6) {
            return new Point3D(vector.getX() / length,
                    vector.getY() / length,
                    vector.getZ() / length);
        }
        return vector;
    }

    public void transform(SimpleMatrix transformationMatrix) {
        var translationMatrix = Transformer.extractTranslation(transformationMatrix);
        var rotationMatrix = Transformer.extractRotation(transformationMatrix);

        position = Transformer.transformPoint(position, translationMatrix);
        right = Transformer.transformPoint(right, rotationMatrix);
        up = Transformer.transformPoint(up, rotationMatrix);
        dir = Transformer.transformPoint(dir, rotationMatrix);
    }

    @Override
    public String toString() {
        return "Camera{" +
                "position=" + position +
                ", up=" + up +
                ", dir=" + dir +
                ", right=" + right +
                '}';
    }
}
