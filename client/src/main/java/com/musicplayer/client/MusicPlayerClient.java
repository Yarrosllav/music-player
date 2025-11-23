package com.musicplayer.client;

import atlantafx.base.theme.PrimerDark;
import com.musicplayer.client.controller.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MusicPlayerClient extends Application {

    @Override
    public void start(Stage primaryStage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
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
