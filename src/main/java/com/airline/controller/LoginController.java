package com.airline.controller;

import com.airline.util.DatabaseConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    public void initialize() {
        // Initialization logic if needed
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Form Error!", "Please enter your email and password");
            return;
        }

        // Validate against the `users` table in MySQL database
        String query = "SELECT full_name, role FROM users WHERE email = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, email);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String fullName = rs.getString("full_name");
                String role = rs.getString("role");

                // 1. Close the Login Window using the loginButton's scene reference
                loginButton.getScene().getWindow().hide();

                // 2. Load the Dashboard and open it
                loadDashboard(fullName, role);

            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Incorrect email or password");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not connect to the database. Is MySQL running?");
        }
    }

    // New method to handle the scene switch cleanly
    private void loadDashboard(String fullName, String role) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("SkyLink Pro - Elite Voyager (" + role + ")");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "System Error", "Could not load the Dashboard UI.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}