package com.airline.controller;

import com.airline.util.UserSession;
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
import java.net.URL;

public class MainController {

    @FXML private StackPane contentArea;

    @FXML private Button btnDashboard;
    @FXML private Button btnBookings;
    @FXML private Button btnAnalytics;
    @FXML private Button btnSettings;

    @FXML private Label lblPageTitle;
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;
    @FXML private Label lblAvatar;
    @FXML private Label lblPortalSubtitle;

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
        updateUserProfileDisplay();
        showDashboard();
    }

    public void updateUserProfileDisplay() {
        UserSession session = UserSession.getInstance();
        if (session != null) {
            String name = session.getFullName() != null ? session.getFullName() : "User";
            String role = session.getRole() != null ? session.getRole() : "Passenger";

            if (lblUserName != null) lblUserName.setText(name);
            if (lblUserRole != null) lblUserRole.setText(role.toUpperCase());
            if (lblAvatar != null) {
                lblAvatar.setText(name.isEmpty() ? "U" : name.substring(0, 1).toUpperCase());
            }
            if (lblPortalSubtitle != null) {
                lblPortalSubtitle.setText("✈  " + role + " Portal");
            }
        }
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
            UserSession.cleanUserSession();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Airline Reservation System - Login");

            Scene scene = new Scene(root, 800, 600);
            URL cssResource = getClass().getResource("/css/styles.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }

            stage.setScene(scene);
            stage.setResizable(false);
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
