package com.example.stringsearcher;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Application extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("application-view.fxml"));
        fxmlLoader.setController(new ApplicationController());
        Scene scene = new Scene(fxmlLoader.load(), 800, 500);
        stage.setTitle("Text Scanner");
        stage.setScene(scene);
        stage.setMinHeight(500);
        stage.setMinWidth(800);
        stage.setResizable(false);
        stage.show();
        stage.setOnCloseRequest(evt -> Platform.exit());
    }

    public static void main(String[] args) {
        launch();
    }
}