package com.airline.controller;

import com.airline.util.DatabaseConnection;
import com.airline.util.PasswordUtil;
import com.airline.util.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
        // Clear any previous session
        UserSession.cleanUserSession();
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter your email and password.");
            return;
        }

        // Validate against users table in MySQL database using BCrypt
        String query = "SELECT user_id, full_name, email, password, role, nic_passport FROM users WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, email.trim());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");

                if (PasswordUtil.checkPassword(password, storedPassword)) {
                    int userId = rs.getInt("user_id");
                    String fullName = rs.getString("full_name");
                    String userEmail = rs.getString("email");
                    String role = rs.getString("role");
                    String nicPassport = rs.getString("nic_passport");

                    // Progressive Migration: If password was plain-text, transparently upgrade to BCrypt
                    if (PasswordUtil.needsRehash(storedPassword)) {
                        try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE users SET password = ? WHERE user_id = ?")) {
                            updateStmt.setString(1, PasswordUtil.hashPassword(password));
                            updateStmt.setInt(2, userId);
                            updateStmt.executeUpdate();
                            System.out.println("[Security] Upgraded user " + userEmail + " password to BCrypt hash.");
                        } catch (SQLException e) {
                            System.err.println("[Security] Could not rehash password: " + e.getMessage());
                        }
                    }

                    // Initialize thread-safe session
                    UserSession.initSession(userId, fullName, userEmail, role, nicPassport);

                    // Close the Login Window
                    Stage loginStage = (Stage) loginButton.getScene().getWindow();
                    loginStage.close();

                    // Load the Dashboard
                    loadDashboard(fullName, role);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Login Failed", "Incorrect email or password.");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Incorrect email or password.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not connect to the database. Please verify MySQL is running.\n\nError: " + e.getMessage());
        }
    }

    @FXML
    void handleShowRegister(ActionEvent event) {
        Stage regStage = new Stage();
        regStage.initModality(Modality.APPLICATION_MODAL);
        regStage.setTitle("SkyLink Pro - Create Account");

        VBox root = new VBox(15);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 30;");
        root.setPrefWidth(420);

        Label title = new Label("Register New Account");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #003366;");

        Label subtitle = new Label("Create your passenger account to book flights seamlessly.");
        subtitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        TextField txtName = new TextField();
        txtName.setPromptText("e.g. John Doe");
        txtName.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        TextField txtRegEmail = new TextField();
        txtRegEmail.setPromptText("e.g. john@example.com");
        txtRegEmail.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Minimum 6 characters");
        txtPass.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        TextField txtNic = new TextField();
        txtNic.setPromptText("e.g. N1234567 or NIC");
        txtNic.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");

        Button btnSubmit = new Button("Register");
        btnSubmit.setStyle("-fx-background-color: #003366; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        btnSubmit.setPrefWidth(120);

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> regStage.close());

        btnSubmit.setOnAction(e -> {
            String nameVal = txtName.getText().trim();
            String emailVal = txtRegEmail.getText().trim();
            String passVal = txtPass.getText();
            String nicVal = txtNic.getText().trim();

            if (nameVal.isEmpty() || emailVal.isEmpty() || passVal.isEmpty() || nicVal.isEmpty()) {
                lblError.setText("All fields are required.");
                return;
            }
            if (!emailVal.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                lblError.setText("Please enter a valid email address.");
                return;
            }
            if (passVal.length() < 6) {
                lblError.setText("Password must be at least 6 characters.");
                return;
            }

            // Insert into users
            String checkSql = "SELECT user_id FROM users WHERE email = ?";
            String insertSql = "INSERT INTO users (full_name, email, password, nic_passport, role) VALUES (?, ?, ?, ?, 'Passenger')";

            try (Connection conn = DatabaseConnection.getConnection()) {
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, emailVal);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next()) {
                        lblError.setText("An account with this email already exists.");
                        return;
                    }
                }

                try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                    insStmt.setString(1, nameVal);
                    insStmt.setString(2, emailVal);
                    insStmt.setString(3, PasswordUtil.hashPassword(passVal));
                    insStmt.setString(4, nicVal.toUpperCase());
                    insStmt.executeUpdate();
                }

                showAlert(Alert.AlertType.INFORMATION, "Registration Successful", "Your account has been created successfully! You can now log in.");
                emailField.setText(emailVal);
                passwordField.setText(passVal);
                regStage.close();

            } catch (SQLException ex) {
                ex.printStackTrace();
                lblError.setText("Database error: " + ex.getMessage());
            }
        });

        HBox btnBox = new HBox(12, btnSubmit, btnCancel);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(
                title, subtitle,
                new Label("Full Name:"), txtName,
                new Label("Email Address:"), txtRegEmail,
                new Label("Password:"), txtPass,
                new Label("NIC / Passport Number:"), txtNic,
                lblError,
                btnBox
        );

        Scene scene = new Scene(root);
        regStage.setScene(scene);
        regStage.showAndWait();
    }

    private void loadDashboard(String fullName, String role) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
            Parent root = loader.load();

            MainController mainCtrl = loader.getController();
            mainCtrl.updateUserProfileDisplay();

            Stage stage = new Stage();
            stage.setTitle("SkyLink Pro - Airline Operations (" + role + ")");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            showAlert(Alert.AlertType.ERROR, "System Error", "Could not load Dashboard UI.\n\nCause: " + cause.getMessage());
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