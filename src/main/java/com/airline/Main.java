package com.airline;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        URL resource = getClass().getResource("/fxml/Login.fxml");
        if (resource == null) {
            throw new RuntimeException("Cannot find Login.fxml");
        }
        
        Parent root = FXMLLoader.load(resource);
        
        Scene scene = new Scene(root, 800, 600);
        // Load CSS styling
        URL cssResource = getClass().getResource("/css/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }
        
        primaryStage.setTitle("Airline Reservation System - Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
