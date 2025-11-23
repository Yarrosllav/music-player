package com.musicplayer.client;

import com.musicplayer.client.controller.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MusicPlayerClient extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainController controller = new MainController();
        Scene scene = new Scene(controller.getRoot(), 1000, 700);

        primaryStage.setTitle("Music Player");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            controller.shutdown();
            System.exit(0);
        });
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
