package com.airline.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the Account Settings view.
 * Handles profile updates (username, email) and password changes with basic validation.
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
        // Pre-populate with current session values (placeholders for now)
        txtUsername.setText("Admin User");
        txtEmail.setText("admin@skylink.com");
    }

    @FXML
    private void handleSaveChanges(ActionEvent event) {
        // Reset errors
        lblEmailError.setText("");
        lblPasswordError.setText("");
        lblStatus.setText("");
        lblStatus.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        boolean valid = true;

        // --- Email validation ---
        String email = txtEmail.getText().trim();
        if (!email.isEmpty() && !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            lblEmailError.setText("Please enter a valid email address.");
            valid = false;
        }

        // --- Password validation (only if user filled in the password section) ---
        String currentPwd  = txtCurrentPassword.getText();
        String newPwd      = txtNewPassword.getText();
        String confirmPwd  = txtConfirmPassword.getText();

        boolean anyPasswordFieldFilled = !currentPwd.isEmpty() || !newPwd.isEmpty() || !confirmPwd.isEmpty();

        if (anyPasswordFieldFilled) {
            if (currentPwd.isEmpty()) {
                lblPasswordError.setText("Please enter your current password.");
                valid = false;
            } else if (newPwd.length() < 8) {
                lblPasswordError.setText("New password must be at least 8 characters.");
                valid = false;
            } else if (!newPwd.equals(confirmPwd)) {
                lblPasswordError.setText("New passwords do not match.");
                valid = false;
            }
        }

        if (!valid) return;

        // All good — show success message (real persistence logic would go here)
        lblStatus.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #28a745;");
        lblStatus.setText("✓ Settings saved successfully!");

        // Clear password fields after a successful save
        txtCurrentPassword.clear();
        txtNewPassword.clear();
        txtConfirmPassword.clear();
    }

    @FXML
    private void handleDiscard(ActionEvent event) {
        // Reset fields to initialized values
        initialize();
        txtCurrentPassword.clear();
        txtNewPassword.clear();
        txtConfirmPassword.clear();
        lblEmailError.setText("");
        lblPasswordError.setText("");
        lblStatus.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #6c757d;");
        lblStatus.setText("Changes discarded.");
    }
}
