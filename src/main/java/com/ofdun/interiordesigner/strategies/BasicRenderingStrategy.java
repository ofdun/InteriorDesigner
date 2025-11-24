package com.ofdun.interiordesigner.strategies;

import com.ofdun.interiordesigner.managers.LightingManager;
import com.ofdun.interiordesigner.models.Mesh;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.List;

public class BasicRenderingStrategy implements RenderingStrategy {

    private static final double EPSILON = 1e-6;

    @Override
    public void renderPixel(int x, int y, PixelRenderContext context) {
        renderPixelInternal(x, y, context, context.pixelConsumer());
    }

    @Override
    public void renderPixelParallel(int x, int y, PixelRenderContext context) {
        renderPixelInternal(x, y, context, context.pixelConsumer());
    }

    private void renderPixelInternal(int x, int y, PixelRenderContext context, PixelConsumer consumer) {
        Point3D[] screenPoints = context.screenPoints();
        BarycentricCoordinates coords = calculateBarycentricCoordinates(
            x, y, screenPoints[0], screenPoints[1], screenPoints[2], context.barycentricDenominator()
        );

        if (coords.isInsideTriangle()) {
            double z1 = screenPoints[0].getZ();
            double z2 = screenPoints[1].getZ();
            double z3 = screenPoints[2].getZ();

            double oneOverZ = coords.w1 / z1 + coords.w2 / z2 + coords.w3 / z3;
            double z = 1.0 / oneOverZ;

            if (context.zBuffer().testAndSet(x, y, z)) {
                Point3D interpolatedNormal = interpolateNormals(
                    x, y,
                    screenPoints[0], screenPoints[1], screenPoints[2],
                    context.normals()[0], context.normals()[1], context.normals()[2]
                );

                Point3D interpolatedWorldPos = interpolateWorldPositionPerspectiveCorrect(
                    context.worldPoints()[0], context.worldPoints()[1], context.worldPoints()[2],
                    coords, z1, z2, z3
                );

                Color finalColor = calculateColor(interpolatedWorldPos, interpolatedNormal, coords, z1, z2, z3, context);
                consumer.drawPixel(x, y, finalColor);
            }
        }
    }

    private Color calculateColor(Point3D worldPos, Point3D normal, BarycentricCoordinates coords,
                                 double z1, double z2, double z3, PixelRenderContext context) {
        RenderingData data = context.renderingData();
        LightingManager lightingManager = (LightingManager) data.lightingManager();
        @SuppressWarnings("unchecked")
        List<Mesh> allMeshes = (List<Mesh>) data.allMeshes();

        if (data.texture() != null && data.uv1() != null && data.uv2() != null && data.uv3() != null) {
            Point2D interpolatedUV = interpolateUVPerspectiveCorrect(
                data.uv1(), data.uv2(), data.uv3(), coords, z1, z2, z3
            );
            Color textureColor = sampleTexture(data.texture(), interpolatedUV);

            return lightingManager.calculateLightingWithShadows(
                worldPos, normal, data.viewDirection(), textureColor, allMeshes
            );
        } else {
            return lightingManager.calculateLightingWithShadows(
                worldPos, normal, data.viewDirection(), data.materialColor(), allMeshes
            );
        }
    }

    private Point2D interpolateUVPerspectiveCorrect(Point2D uv1, Point2D uv2, Point2D uv3,
                                                    BarycentricCoordinates coords,
                                                    double z1, double z2, double z3) {
        double oneOverZ = coords.w1 / z1 + coords.w2 / z2 + coords.w3 / z3;

        double uOverZ = coords.w1 * uv1.getX() / z1 + coords.w2 * uv2.getX() / z2 + coords.w3 * uv3.getX() / z3;
        double vOverZ = coords.w1 * uv1.getY() / z1 + coords.w2 * uv2.getY() / z2 + coords.w3 * uv3.getY() / z3;

        return new Point2D(uOverZ / oneOverZ, vOverZ / oneOverZ);
    }

    private Color sampleTexture(Image texture, Point2D uv) {
        if (texture == null) {
            return Color.GRAY;
        }

        int width = (int) texture.getWidth();
        int height = (int) texture.getHeight();

        double u = uv.getX() - Math.floor(uv.getX());
        double v = 1.0 - (uv.getY() - Math.floor(uv.getY()));

        int texX = (int) (u * (width - 1));
        int texY = (int) (v * (height - 1));

        texX = Math.max(0, Math.min(width - 1, texX));
        texY = Math.max(0, Math.min(height - 1, texY));

        try {
            return texture.getPixelReader().getColor(texX, texY);
        } catch (Exception e) {
            return Color.GRAY;
        }
    }

    private Point3D interpolateWorldPositionPerspectiveCorrect(Point3D p1, Point3D p2, Point3D p3,
                                                              BarycentricCoordinates coords,
                                                              double z1, double z2, double z3) {
        double oneOverZ = coords.w1 / z1 + coords.w2 / z2 + coords.w3 / z3;

        double xOverZ = coords.w1 * p1.getX() / z1 + coords.w2 * p2.getX() / z2 + coords.w3 * p3.getX() / z3;
        double yOverZ = coords.w1 * p1.getY() / z1 + coords.w2 * p2.getY() / z2 + coords.w3 * p3.getY() / z3;
        double zOverZ = coords.w1 * p1.getZ() / z1 + coords.w2 * p2.getZ() / z2 + coords.w3 * p3.getZ() / z3;

        return new Point3D(
            xOverZ / oneOverZ,
            yOverZ / oneOverZ,
            zOverZ / oneOverZ
        );
    }

    private BarycentricCoordinates calculateBarycentricCoordinates(int x, int y,
                                                                  Point3D p1, Point3D p2, Point3D p3,
                                                                  double barycentricDenominator) {
        double w1 = ((p2.getY() - p3.getY()) * (x - p3.getX()) +
                    (p3.getX() - p2.getX()) * (y - p3.getY())) / barycentricDenominator;
        double w2 = ((p3.getY() - p1.getY()) * (x - p3.getX()) +
                    (p1.getX() - p3.getX()) * (y - p3.getY())) / barycentricDenominator;
        double w3 = 1 - w1 - w2;

        return new BarycentricCoordinates(w1, w2, w3);
    }

    private Point3D interpolateNormals(int x, int y, Point3D pA, Point3D pB, Point3D pC,
                                       Point3D nA, Point3D nB, Point3D nC) {
        double totalHeight = pC.getY() - pA.getY();
        if (Math.abs(totalHeight) < EPSILON) {
            return nA.normalize();
        }

        Point3D p1, p2, n1, n2;
        double u;
        double height;

        if (y <= pB.getY()) {
            p1 = pA;
            p2 = pB;
            n1 = nA;
            n2 = nB;
        } else {
            p1 = pB;
            p2 = pC;
            n1 = nB;
            n2 = nC;
        }
        height = p2.getY() - p1.getY();
        u = Math.abs(height) < EPSILON ? 0 : (y - p1.getY()) / height;
        u = Math.max(0, Math.min(1, u));

        Point3D nQ = new Point3D(
            u * n2.getX() + (1 - u) * n1.getX(),
            u * n2.getY() + (1 - u) * n1.getY(),
            u * n2.getZ() + (1 - u) * n1.getZ()
        );

        double w = (y - pA.getY()) / totalHeight;
        w = Math.max(0, Math.min(1, w));

        Point3D nR = new Point3D(
            w * nC.getX() + (1 - w) * nA.getX(),
            w * nC.getY() + (1 - w) * nA.getY(),
            w * nC.getZ() + (1 - w) * nA.getZ()
        );

        double qX = u * p2.getX() + (1 - u) * p1.getX();
        double rX = w * pC.getX() + (1 - w) * pA.getX();
        double xDiff = rX - qX;
        double t = Math.abs(xDiff) < EPSILON ? 0.5 : (x - qX) / xDiff;
        t = Math.max(0, Math.min(1, t));

        Point3D nP = new Point3D(
            t * nQ.getX() + (1 - t) * nR.getX(),
            t * nQ.getY() + (1 - t) * nR.getY(),
            t * nQ.getZ() + (1 - t) * nR.getZ()
        );

        return nP.normalize();
    }

    private record BarycentricCoordinates(double w1, double w2, double w3) {
        boolean isInsideTriangle() {
            return w1 >= 0 && w2 >= 0 && w3 >= 0;
        }
    }
}

