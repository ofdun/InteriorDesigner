package com.ofdun.interiordesigner;

import com.ofdun.interiordesigner.objectreader.ObjStreamReader;
import io.micronaut.context.ApplicationContext;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MyApplication extends Application {
    private static final Logger log = LoggerFactory.getLogger(MyApplication.class);
    ApplicationContext context;

    @Override
    public void init() throws Exception {
        // Инициализируем Micronaut ДО запуска JavaFX
        context = ApplicationContext.run();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MyApplication.class.getResource("hello-view.fxml"));
        fxmlLoader.setControllerFactory(context::getBean);
        Scene scene = new Scene(fxmlLoader.load(), 600, 600);
//        stage.setTitle("Hello!");
        stage.setScene(scene);

        stage.show();
    }

    @Override
    public void stop() throws Exception {
        // Закрываем Micronaut контекст при выходе
        if (context != null) {
            context.close();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}