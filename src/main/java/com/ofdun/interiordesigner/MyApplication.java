package com.ofdun.interiordesigner;

import com.ofdun.interiordesigner.managers.SceneManager;
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
    public void init() {
        context = ApplicationContext.run();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MyApplication.class.getResource("hello-view.fxml"));
        fxmlLoader.setControllerFactory(context::getBean);
        Scene scene = new Scene(fxmlLoader.load(), 1250, 1000);
//        stage.setTitle("Hello!");
        stage.setScene(scene);

        var sm = context.getBean(SceneManager.class);

        sm.initializeRoom(100, 100, 100);
        sm.renderAllMeshes();

        stage.show();
    }

    @Override
    public void stop() throws Exception {
        if (context != null) {
            context.close();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}