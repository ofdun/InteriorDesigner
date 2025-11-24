package com.ofdun.interiordesigner.benchmark;

import com.ofdun.interiordesigner.controllers.CanvasController;
import com.ofdun.interiordesigner.managers.SceneManager;
import com.ofdun.interiordesigner.strategies.BenchmarkRenderingStrategy;
import io.micronaut.context.ApplicationContext;
import javafx.application.Platform;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class RenderingBenchmark {

    private static volatile boolean javaFXInitialized = false;

    private static final int[] THREAD_COUNTS = {1, 2, 4, 8, 12, 24, 48, 96};
    private static final int[] POLYGON_COUNTS = {500};
    private static final int WARMUP_RUNS = 0;
    private static final int BENCHMARK_RUNS = 1;
    private static final String RESULTS_DIR = "benchmark_results";

    private static void initializeJavaFX() {
        if (javaFXInitialized) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);

        new Thread(() -> {
            try {
                Platform.startup(() -> {
                    javaFXInitialized = true;
                    latch.countDown();
                });
            } catch (IllegalStateException e) {
                javaFXInitialized = true;
                latch.countDown();
            }
        }).start();

        try {
            latch.await();
            System.out.println("✓ JavaFX Platform инициализирован");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Не удалось инициализировать JavaFX", e);
        }
    }

    private static void saveResultsToFile(int threadCount, int polygonCount, long[] times) {
        File dir = new File(RESULTS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = String.format("%s/threads_%d_polygons_%d.txt", RESULTS_DIR, threadCount, polygonCount);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (long time : times) {
                writer.write(String.valueOf(time));
                writer.newLine();
            }
            System.out.printf("  ✓ Результаты сохранены в файл: %s\n", fileName);
        } catch (IOException e) {
            System.err.printf("  ⚠ Не удалось сохранить результаты в файл %s: %s\n",
                            fileName, e.getMessage());
        }
    }

    private static void generateBenchmarkSceneWithPolygons(SceneManager sceneManager, ApplicationContext context, int targetPolygons) {
        sceneManager.initializeRoom(150, 100, 150);

        var objectLoader = context.getBean(com.ofdun.interiordesigner.objectloaders.ObjectLoader.class);
        var lightingManager = context.getBean(com.ofdun.interiordesigner.managers.LightingManager.class);

        try {
            int chairCount = targetPolygons / 500;

            double radius = 30.0;
            for (int i = 0; i < chairCount; i++) {
                double angle = (2.0 * Math.PI * i) / chairCount;
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                loadAndAddObject(objectLoader, sceneManager, "models/500polygons-chair", "chair.obj", x, 0, z);
            }

            loadAndAddObject(objectLoader, sceneManager, "models/Lamp", "lamp.obj", 0, 40, 0);

            var lampMeshes = sceneManager.getAllMeshes().stream()
                .filter(mesh -> {
                    boolean canBeLight = mesh.getCanBeLightningSource();
                    String name = mesh.getDisplayName();
                    var center = mesh.getCenter();
                    int triangles = mesh.getFaces().size();
                    System.out.printf("    Меш: %s, canBeLightSource=%b, center=(%.1f,%.1f,%.1f), треугольников=%d\n",
                                     name, canBeLight, center.getX(), center.getY(), center.getZ(), triangles);
                    return canBeLight;
                })
                .findFirst();

            if (lampMeshes.isPresent()) {
                var lampMesh = lampMeshes.get();
                var lampPosition = lampMesh.getCenter();
                var lightSource = new com.ofdun.interiordesigner.models.LightSource(
                    lampPosition,
                    javafx.scene.paint.Color.rgb(255, 244, 229),
                    2.0,
                    lampMesh.getId()
                );

                lightingManager.setSingleLightSource(lightSource);

                int totalTriangles = 0;
                for (var mesh : sceneManager.getAllMeshes()) {
                    totalTriangles += mesh.getFaces().size();
                }
                System.out.printf("Всего треугольников в сцене для трассировки: %d\n", totalTriangles);
            }

            System.out.println("Комната: 150x100x150");
            System.out.printf("Объекты: %d стульев (~%d полигонов), 1 лампа\n", chairCount, targetPolygons);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadAndAddObject(com.ofdun.interiordesigner.objectloaders.ObjectLoader objectLoader,
                                        SceneManager sceneManager,
                                        String directory, String fileName,
                                        double x, double y, double z) throws Exception {
        loadAndAddObject(objectLoader, sceneManager, directory, fileName, x, y, z, 1.0);
    }

    private static void loadAndAddObject(com.ofdun.interiordesigner.objectloaders.ObjectLoader objectLoader,
                                        SceneManager sceneManager,
                                        String directory, String fileName,
                                        double x, double y, double z, double scale) throws Exception {
        java.io.File dir = new java.io.File(directory);
        java.io.File objFile = new java.io.File(dir, fileName);

        try (var stream = new java.io.BufferedReader(new java.io.FileReader(objFile))) {
            var mesh = objectLoader.load(stream, dir);

            if (scale != 1.0) {
                mesh.applyTransform(com.ofdun.interiordesigner.managers.TransformMatrices.scale(scale));
            }

            if (x != 0) {
                mesh.applyTransform(com.ofdun.interiordesigner.managers.TransformMatrices.translateX(x));
            }
            if (y != 0) {
                mesh.applyTransform(com.ofdun.interiordesigner.managers.TransformMatrices.translateY(y));
            }
            if (z != 0) {
                mesh.applyTransform(com.ofdun.interiordesigner.managers.TransformMatrices.translateZ(z));
            }

            sceneManager.addMeshView(mesh);
        }
    }

    private static long performSingleRender(ApplicationContext context) {
        SceneManager sceneManager = context.getBean(SceneManager.class);
        CanvasController canvasController = context.getBean(CanvasController.class);

        if (!(canvasController.getRenderingStrategy() instanceof BenchmarkRenderingStrategy benchStrategy)) {
            System.err.println("Ошибка: не установлена BenchmarkRenderingStrategy");
            return 0;
        }

        final long[] renderTime = new long[1];
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                canvasController.prepareForRender();

                benchStrategy.startRenderTimer();

                sceneManager.renderAllMeshes();

                renderTime[0] = benchStrategy.stopRenderTimer();
            } catch (Exception e) {
                System.err.println("Ошибка при выполнении рендеринга: " + e.getMessage());
                e.printStackTrace();
                renderTime[0] = 0;
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }

        return renderTime[0];
    }

    private static BenchmarkResult benchmarkWithThreadsAndPolygons(int threadCount, int polygonCount) {
        System.out.printf("\nТестирование с %d потоками и %d полигонами\n", threadCount, polygonCount);

        CanvasController.configureRenderThreads(threadCount);

        ApplicationContext context = ApplicationContext.run();
        SceneManager sceneManager = context.getBean(SceneManager.class);
        CanvasController canvasController = context.getBean(CanvasController.class);

        var benchmarkStrategy = new com.ofdun.interiordesigner.strategies.BenchmarkRenderingStrategy();
        canvasController.setRenderingStrategy(benchmarkStrategy);

        CountDownLatch sceneLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                generateBenchmarkSceneWithPolygons(sceneManager, context, polygonCount);
            } finally {
                sceneLatch.countDown();
            }
        });

        try {
            sceneLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            context.close();
            CanvasController.shutdownRenderExecutor();
            return new BenchmarkResult(threadCount, polygonCount, 0, 0, 0, 0);
        }

        for (int i = 0; i < WARMUP_RUNS; i++) {
            benchmarkStrategy.resetStatistics();
            performSingleRender(context);
        }

        var lightingManager = context.getBean(com.ofdun.interiordesigner.managers.LightingManager.class);
        var lightSources = lightingManager.getLightSources();

        System.out.printf("Объектов в сцене перед замерами: %d\n", sceneManager.getAllMeshes().size());
        int totalTrianglesInScene = 0;
        for (var mesh : sceneManager.getAllMeshes()) {
            int meshTriangles = mesh.getFaces().size();
            totalTrianglesInScene += meshTriangles;
        }
        System.out.printf("Всего треугольников в сцене: %d\n", totalTrianglesInScene);

        System.out.println("Замеры:");
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        long[] times = new long[BENCHMARK_RUNS];

        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            benchmarkStrategy.resetStatistics();

            long renderTime = performSingleRender(context);
            times[i] = renderTime;
            totalTime += renderTime;
            minTime = Math.min(minTime, renderTime);
            maxTime = Math.max(maxTime, renderTime);

            long pixelsRendered = benchmarkStrategy.getPixelsRendered();
            System.out.printf("    Прогон %d: %d мс (пикселей: %d, треугольников обработано: ~%d)\n",
                i + 1, renderTime, pixelsRendered, pixelsRendered / 100);

            if (i < BENCHMARK_RUNS - 1) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        System.out.printf("  Среднее время: %.2f мс\n", (double) totalTime / BENCHMARK_RUNS);

        saveResultsToFile(threadCount, polygonCount, times);

        double avgTime = (double) totalTime / BENCHMARK_RUNS;

        double variance = 0;
        for (long time : times) {
            variance += Math.pow(time - avgTime, 2);
        }
        double stdDev = Math.sqrt(variance / BENCHMARK_RUNS);

        context.close();
        CanvasController.shutdownRenderExecutor();

        return new BenchmarkResult(threadCount, polygonCount, avgTime, minTime, maxTime, stdDev);
    }

    private static void printResults(BenchmarkResult[] results) {
        System.out.println("\n" + "═".repeat(120));
        System.out.println("                                    РЕЗУЛЬТАТЫ БЕНЧМАРКА");
        System.out.println("═".repeat(120));

        System.out.println("\n┌──────────┬──────────────┐");
        System.out.println("│ Потоки   │ Среднее (мс) │");

        for (BenchmarkResult result : results) {
            System.out.printf("│ %8d │ %12.2f │\n",
                result.threadCount,
                result.avgTime
            );
        }

        System.out.println("└──────────┴──────────────┘");

        System.out.println("═".repeat(120));
    }

    public static void runBenchmark() {
        initializeJavaFX();

        System.out.println("\nПараметры теста:");
        System.out.println("  • Количество прогонов для прогрева: " + WARMUP_RUNS);
        System.out.println("  • Количество измерений: " + BENCHMARK_RUNS);
        System.out.print("  • Количества потоков: ");
        for (int i = 0; i < THREAD_COUNTS.length; i++) {
            System.out.print(THREAD_COUNTS[i]);
            if (i < THREAD_COUNTS.length - 1) System.out.print(", ");
        }
        System.out.print("\n  • Количества полигонов: ");
        for (int i = 0; i < POLYGON_COUNTS.length; i++) {
            System.out.print(POLYGON_COUNTS[i]);
            if (i < POLYGON_COUNTS.length - 1) System.out.print(", ");
        }
        System.out.println("\n");

        long benchmarkStartTime = System.currentTimeMillis();

        // Выполняем бенчмарки для каждой комбинации потоков и полигонов
        for (int polygonCount : POLYGON_COUNTS) {
            System.out.println("\n" + "═".repeat(120));
            System.out.printf("ТЕСТИРОВАНИЕ С %d ПОЛИГОНАМИ (%d стульев)\n", polygonCount, polygonCount / 500);
            System.out.println("═".repeat(120));

            BenchmarkResult[] results = new BenchmarkResult[THREAD_COUNTS.length];

            for (int i = 0; i < THREAD_COUNTS.length; i++) {
                results[i] = benchmarkWithThreadsAndPolygons(THREAD_COUNTS[i], polygonCount);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            printResults(results);
        }

        long benchmarkEndTime = System.currentTimeMillis();
        long totalBenchmarkTime = (benchmarkEndTime - benchmarkStartTime) / 1000;

        System.out.println("\n" + "═".repeat(120));
        System.out.printf("Общее время выполнения: %d сек\n",
                         totalBenchmarkTime);

        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        try {
            runBenchmark();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static class BenchmarkResult {
        final int threadCount;
        final int polygonCount;
        final double avgTime;
        final long minTime;
        final long maxTime;
        final double stdDev;

        BenchmarkResult(int threadCount, int polygonCount, double avgTime, long minTime, long maxTime, double stdDev) {
            this.threadCount = threadCount;
            this.polygonCount = polygonCount;
            this.avgTime = avgTime;
            this.minTime = minTime;
            this.maxTime = maxTime;
            this.stdDev = stdDev;
        }
    }
}

