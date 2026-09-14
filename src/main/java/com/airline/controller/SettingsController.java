package com.airline.controller;

import com.airline.util.DatabaseConnection;
import com.airline.util.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Controller for the Account Settings view.
 * Handles profile updates (full name, email) and password changes with live MySQL database persistence.
 */
public class SettingsController {

    // Profile fields
    @FXML private TextField     txtUsername;
    @FXML private TextField     txtEmail;

    // Password fields
    @FXML private PasswordField txtCurrentPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;

    // Error / status labels
    @FXML private Label lblEmailError;
    @FXML private Label lblPasswordError;
    @FXML private Label lblStatus;

    @FXML
    public void initialize() {
        populateFields();
    }

    private void populateFields() {
        UserSession session = UserSession.getInstance();
        if (session != null) {
            txtUsername.setText(session.getFullName() != null ? session.getFullName() : "");
            txtEmail.setText(session.getEmail() != null ? session.getEmail() : "");
        }
    }

    @FXML
    private void handleSaveChanges(ActionEvent event) {
        // Reset errors
        lblEmailError.setText("");
        lblPasswordError.setText("");
        lblStatus.setText("");
        lblStatus.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        UserSession session = UserSession.getInstance();
        if (session == null) {
            lblStatus.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
            lblStatus.setText("Error: No active user session.");
            return;
        }

        String fullName = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();

        if (fullName.isEmpty()) {
            lblStatus.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
            lblStatus.setText("Full Name cannot be blank.");
            return;
        }

        // --- Email validation ---
        if (email.isEmpty() || !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            lblEmailError.setText("Please enter a valid email address.");
            return;
        }

        // --- Password validation ---
        String currentPwd  = txtCurrentPassword.getText();
        String newPwd      = txtNewPassword.getText();
        String confirmPwd  = txtConfirmPassword.getText();

        boolean changingPassword = !currentPwd.isEmpty() || !newPwd.isEmpty() || !confirmPwd.isEmpty();

        if (changingPassword) {
            if (currentPwd.isEmpty()) {
                lblPasswordError.setText("Please enter your current password.");
                return;
            } else if (newPwd.length() < 6) {
                lblPasswordError.setText("New password must be at least 6 characters.");
                return;
            } else if (!newPwd.equals(confirmPwd)) {
                lblPasswordError.setText("New passwords do not match.");
                return;
            }
        }

        // --- MySQL Database Persistence ---
        try (Connection conn = DatabaseConnection.getConnection()) {
            // If changing password or email, verify current credentials / uniqueness
            if (changingPassword) {
                String verifySql = "SELECT password FROM users WHERE user_id = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(verifySql)) {
                    checkStmt.setInt(1, session.getUserId());
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next()) {
                        String existingPass = rs.getString("password");
                        if (!existingPass.equals(currentPwd)) {
                            lblPasswordError.setText("Current password is incorrect.");
                            return;
                        }
                    } else {
                        lblPasswordError.setText("User record not found.");
                        return;
                    }
                }
            }

            // Check email uniqueness if email changed
            if (!email.equalsIgnoreCase(session.getEmail())) {
                String checkEmailSql = "SELECT user_id FROM users WHERE email = ? AND user_id != ?";
                try (PreparedStatement checkEmailStmt = conn.prepareStatement(checkEmailSql)) {
                    checkEmailStmt.setString(1, email);
                    checkEmailStmt.setInt(2, session.getUserId());
                    ResultSet rs = checkEmailStmt.executeQuery();
                    if (rs.next()) {
                        lblEmailError.setText("This email is already in use by another account.");
                        return;
                    }
                }
            }

            // Update user in DB
            if (changingPassword) {
                String updateSql = "UPDATE users SET full_name = ?, email = ?, password = ? WHERE user_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, fullName);
                    updateStmt.setString(2, email);
                    updateStmt.setString(3, newPwd);
                    updateStmt.setInt(4, session.getUserId());
                    updateStmt.executeUpdate();
                }
            } else {
                String updateSql = "UPDATE users SET full_name = ?, email = ? WHERE user_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, fullName);
                    updateStmt.setString(2, email);
                    updateStmt.setInt(3, session.getUserId());
                    updateStmt.executeUpdate();
                }
            }

            // Update active session and UI
            session.setFullName(fullName);
            session.setEmail(email);

            if (MainController.getInstance() != null) {
                MainController.getInstance().updateUserProfileDisplay();
            }

            lblStatus.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #28a745;");
            lblStatus.setText("✓ Settings and profile updated successfully!");

            // Clear password fields
            txtCurrentPassword.clear();
            txtNewPassword.clear();
            txtConfirmPassword.clear();

        } catch (SQLException e) {
            e.printStackTrace();
            lblStatus.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
            lblStatus.setText("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleDiscard(ActionEvent event) {
        populateFields();
        txtCurrentPassword.clear();
        txtNewPassword.clear();
        txtConfirmPassword.clear();
        lblEmailError.setText("");
        lblPasswordError.setText("");
        lblStatus.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #6c757d;");
        lblStatus.setText("Changes discarded.");
    }
}
