package com.ofdun.interiordesigner.models;

import javafx.geometry.Point3D;
import java.util.List;

public class ShadowRayTracer {
    private static final double SHADOW_BIAS = 0.0001;
    private static final double EPSILON = 1e-9;
    private static final double MIN_SHADOW_DISTANCE = 0.05;
    private static final double SURFACE_EPSILON = 0.001;

    private static final java.util.Map<String, Long> shadowRayTimePerMesh = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, Integer> shadowRayCallsPerMesh = new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean isPointInShadow(Point3D point, Point3D lightPosition, List<Mesh> allMeshes, String ignoreMeshId) {
        return isPointInShadow(point, lightPosition, allMeshes, ignoreMeshId, null);
    }

    public static boolean isPointInShadow(Point3D point, Point3D lightPosition, List<Mesh> allMeshes, String ignoreMeshId, String currentMeshId) {
        long startTime = System.nanoTime();

        Point3D lightDir = lightPosition.subtract(point);
        double lightDistance = lightDir.magnitude();

        if (lightDistance < MIN_SHADOW_DISTANCE) {
            recordShadowRayTime(currentMeshId, System.nanoTime() - startTime);
            return false;
        }

        Point3D shadowRayDirection = lightDir.normalize();

        double adaptiveBias = Math.min(SHADOW_BIAS * lightDistance, SURFACE_EPSILON);
        Point3D rayOrigin = point.add(shadowRayDirection.multiply(adaptiveBias));

        double maxDistance = lightDistance - (adaptiveBias * 2);

        if (maxDistance <= SURFACE_EPSILON) {
            recordShadowRayTime(currentMeshId, System.nanoTime() - startTime);
            return false;
        }

        for (Mesh mesh : allMeshes) {
            if (mesh.isRoom()) {
                continue;
            }

            if (ignoreMeshId != null && ignoreMeshId.equals(mesh.getId())) {
                continue;
            }

            if (rayIntersectsMesh(rayOrigin, shadowRayDirection, maxDistance, mesh)) {
                recordShadowRayTime(currentMeshId, System.nanoTime() - startTime);
                return true;
            }
        }

        recordShadowRayTime(currentMeshId, System.nanoTime() - startTime);
        return false;
    }

    private static void recordShadowRayTime(String meshId, long timeNanos) {
        if (meshId != null) {
            shadowRayTimePerMesh.merge(meshId, timeNanos, Long::sum);
            shadowRayCallsPerMesh.merge(meshId, 1, Integer::sum);
        }
    }

    private static boolean rayIntersectsMesh(Point3D rayOrigin, Point3D rayDirection,
                                           double maxDistance, Mesh mesh) {
        List<Point3D> vertices = mesh.getVertices();
        List<List<Integer>> faces = mesh.getFaces();

        for (List<Integer> face : faces) {
            if (face.size() >= 3) {
                if (face.size() == 3) {
                    if (rayIntersectsTriangle(rayOrigin, rayDirection, maxDistance,
                            vertices.get(face.get(0)), vertices.get(face.get(1)), vertices.get(face.get(2)))) {
                        return true;
                    }
                } else {
                    Point3D firstVertex = vertices.get(face.get(0));
                    for (int i = 1; i < face.size() - 1; i++) {
                        Point3D secondVertex = vertices.get(face.get(i));
                        Point3D thirdVertex = vertices.get(face.get(i + 1));

                        if (rayIntersectsTriangle(rayOrigin, rayDirection, maxDistance,
                                firstVertex, secondVertex, thirdVertex)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean rayIntersectsTriangle(Point3D rayOrigin, Point3D rayDirection, double maxDistance,
                                               Point3D vertex0, Point3D vertex1, Point3D vertex2) {
        Point3D edge1 = vertex1.subtract(vertex0);
        Point3D edge2 = vertex2.subtract(vertex0);

        if (edge1.magnitude() < EPSILON || edge2.magnitude() < EPSILON) {
            return false;
        }

        Point3D h = rayDirection.crossProduct(edge2);
        double a = edge1.dotProduct(h);

        if (Math.abs(a) < EPSILON) {
            return false;
        }

        double f = 1.0 / a;
        Point3D s = rayOrigin.subtract(vertex0);
        double u = f * s.dotProduct(h);

        if (u < -EPSILON * 10 || u > 1.0 + EPSILON * 10) {
            return false;
        }

        Point3D q = s.crossProduct(edge1);
        double v = f * rayDirection.dotProduct(q);

        if (v < -EPSILON * 10 || u + v > 1.0 + EPSILON * 10) {
            return false;
        }

        double t = f * edge2.dotProduct(q);

        return t > SURFACE_EPSILON && t < maxDistance - SURFACE_EPSILON;
    }

    public static java.util.Map<String, Long> getShadowRayTimePerMesh() {
        return new java.util.HashMap<>(shadowRayTimePerMesh);
    }

    public static java.util.Map<String, Integer> getShadowRayCallsPerMesh() {
        return new java.util.HashMap<>(shadowRayCallsPerMesh);
    }

    public static void resetStatistics() {
        shadowRayTimePerMesh.clear();
        shadowRayCallsPerMesh.clear();
    }

}
