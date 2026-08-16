package com.airline.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;
    
    @FXML private Button btnDashboard;
    @FXML private Button btnBookings;
    @FXML private Button btnAnalytics;
    @FXML private Button btnSettings;
    
    // We can define a static reference to allow other controllers to change views
    private static MainController instance;

    public MainController() {
        instance = this;
    }

    public static MainController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        // Load the Dashboard view by default into the center area
        showDashboard();
    }

    @FXML
    public void showDashboard() {
        setActiveTab(btnDashboard);
        loadView("/fxml/Dashboard.fxml");
    }

    @FXML
    public void showBookings() {
        setActiveTab(btnBookings);
        loadView("/fxml/MyBookings.fxml");
    }

    @FXML
    public void showAnalytics() {
        setActiveTab(btnAnalytics);
        loadView("/fxml/Analytics.fxml");
    }

    @FXML
    public void showSettings() {
        setActiveTab(btnSettings);
        loadView("/fxml/Settings.fxml");
    }

    private void setActiveTab(Button activeButton) {
        if (btnDashboard == null) return; // Guard clause if initialize hasn't completed
        btnDashboard.getStyleClass().remove("sidebar-btn-active");
        btnBookings.getStyleClass().remove("sidebar-btn-active");
        btnAnalytics.getStyleClass().remove("sidebar-btn-active");
        btnSettings.getStyleClass().remove("sidebar-btn-active");
        
        if (activeButton != null) {
            activeButton.getStyleClass().add("sidebar-btn-active");
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Airline Reservation - Login");
            stage.setScene(new Scene(root, 600, 400));
            stage.show();

            Stage currentStage = (Stage) contentArea.getScene().getWindow();
            currentStage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            setView(view);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to load view: " + fxmlPath);
        }
    }
    
    public void setView(Node view) {
        contentArea.getChildren().setAll(view);
    }
    
    /** Called externally (e.g., from DashboardController) to sync the active sidebar tab */
    public void setActiveTabExternal(String tab) {
        switch (tab) {
            case "bookings" -> setActiveTab(btnBookings);
            case "analytics" -> setActiveTab(btnAnalytics);
            case "settings"  -> setActiveTab(btnSettings);
            default          -> setActiveTab(btnDashboard);
        }
    }
}
