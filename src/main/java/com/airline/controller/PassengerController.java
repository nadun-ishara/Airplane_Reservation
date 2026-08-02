package com.airline.controller;

import com.airline.model.Flight;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.io.IOException;

import java.util.regex.Pattern;

public class PassengerController {

    @FXML private Label lblFlightDetails;
    @FXML private ComboBox<String> comboTitle;
    @FXML private TextField txtFullName;
    @FXML private DatePicker dateDOB;
    @FXML private TextField txtPassport;
    @FXML private TextField txtNationality;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;

    private Flight selectedFlight;

    @FXML
    public void initialize() {
        // Populate combo box
        comboTitle.getItems().addAll("Mr.", "Mrs.", "Ms.", "Dr.");
        comboTitle.getSelectionModel().selectFirst();
        
        // 1. UI/UX: Real-time Validation & Visual Feedback
        txtEmail.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isValidEmail(newValue)) {
                txtEmail.setStyle("-fx-border-color: #28a745; -fx-border-width: 2px; -fx-border-radius: 4px; -fx-background-color: #f8f9fa;");
            } else {
                txtEmail.setStyle("-fx-border-color: #dc3545; -fx-border-width: 2px; -fx-border-radius: 4px; -fx-background-color: #fff3f3;");
            }
        });
        
        txtPassport.textProperty().addListener((observable, oldValue, newValue) -> {
             if (newValue.trim().length() >= 5) {
                txtPassport.setStyle("-fx-border-color: #28a745; -fx-border-width: 2px; -fx-border-radius: 4px; -fx-background-color: #f8f9fa;");
            } else {
                txtPassport.setStyle("-fx-border-color: #dc3545; -fx-border-width: 2px; -fx-border-radius: 4px; -fx-background-color: #fff3f3;");
            }
        });
    }

    public void setFlightData(Flight flight) {
        this.selectedFlight = flight;
        lblFlightDetails.setText("Booking Flight: " + flight.getAirline() + " from " + flight.getDepartureCity() + " to " + flight.getArrivalCity());
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pat = Pattern.compile(emailRegex);
        if (email == null)
            return false;
        return pat.matcher(email).matches();
    }

    @FXML
    void handleConfirm(ActionEvent event) {
        // Validate before proceeding to Mock Payment Gateway
        if (txtFullName.getText().trim().isEmpty() || txtPassport.getText().trim().isEmpty() || !isValidEmail(txtEmail.getText())) {
             showAlert(Alert.AlertType.ERROR, "Validation Error", "Please ensure all required fields (Name, Passport, valid Email) are filled correctly.");
             return;
        }
        
        // Advanced Workflow: Proceed to Mock Payment Gateway
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Payment.fxml"));
            Parent root = loader.load();

            // Pass the Passenger details and Flight to Payment Controller
            PaymentController paymentController = loader.getController();
            paymentController.setBookingDetails(selectedFlight, txtFullName.getText(), txtPassport.getText(), txtEmail.getText());

            Stage stage = new Stage();
            stage.setTitle("Secure Payment Gateway");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Close this passenger details window after payment
            closeWindow();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "System Error", "Could not load Payment Gateway UI.");
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtFullName.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
