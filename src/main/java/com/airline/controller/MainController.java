package com.airline.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;

    @FXML private Button btnDashboard;
    @FXML private Button btnBookings;
    @FXML private Button btnAnalytics;
    @FXML private Button btnSettings;

    @FXML private Label lblPageTitle;

    // Static singleton so child controllers can trigger navigation
    private static MainController instance;

    public MainController() {
        instance = this;
    }

    public static MainController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        showDashboard();
    }

    // -----------------------------------------------------------------------
    // Navigation Methods
    // -----------------------------------------------------------------------

    @FXML
    public void showDashboard() {
        setActiveTab(btnDashboard);
        setPageTitle("\u2708  Reservation Dashboard");
        loadView("/fxml/Dashboard.fxml");
    }

    @FXML
    public void showBookings() {
        setActiveTab(btnBookings);
        setPageTitle("\uD83D\uDCCB  My Bookings");
        loadView("/fxml/MyBookings.fxml");
    }

    @FXML
    public void showAnalytics() {
        setActiveTab(btnAnalytics);
        setPageTitle("\uD83D\uDCC8  Flight Analytics");
        loadView("/fxml/Analytics.fxml");
    }

    @FXML
    public void showSettings() {
        setActiveTab(btnSettings);
        setPageTitle("\u2699  Account Settings");
        loadView("/fxml/Settings.fxml");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void setActiveTab(Button activeButton) {
        if (btnDashboard == null) return;
        btnDashboard.getStyleClass().remove("sidebar-btn-active");
        btnBookings .getStyleClass().remove("sidebar-btn-active");
        btnAnalytics.getStyleClass().remove("sidebar-btn-active");
        btnSettings .getStyleClass().remove("sidebar-btn-active");
        if (activeButton != null) {
            activeButton.getStyleClass().add("sidebar-btn-active");
        }
    }

    private void setPageTitle(String title) {
        if (lblPageTitle != null) lblPageTitle.setText(title);
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("SkyLink Pro – Login");
            stage.setScene(new Scene(root, 600, 400));
            stage.show();

            Stage currentStage = (Stage) contentArea.getScene().getWindow();
            currentStage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Load any FXML into the content area */
    public void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            setView(view);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[MainController] Failed to load view: " + fxmlPath);
        }
    }

    public void setView(Node view) {
        contentArea.getChildren().setAll(view);
    }

    /** Called from child controllers (e.g. DashboardController) to sync the sidebar highlight */
    public void setActiveTabExternal(String tab) {
        switch (tab) {
            case "bookings"  -> setActiveTab(btnBookings);
            case "analytics" -> setActiveTab(btnAnalytics);
            case "settings"  -> setActiveTab(btnSettings);
            default          -> setActiveTab(btnDashboard);
        }
    }
}
