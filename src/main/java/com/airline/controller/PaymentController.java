package com.airline.controller;

import com.airline.model.Flight;
import com.airline.util.DatabaseConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

public class PaymentController {

    @FXML private Text txtAmount;
    
    @FXML private ComboBox<String> comboPaymentMethod;
    
    // Card UI Elements
    @FXML private VBox cardDetailsBox;
    @FXML private TextField txtCardName;
    @FXML private TextField txtCardNumber;
    @FXML private TextField txtExpiry;
    @FXML private TextField txtCVV;

    // PayPal UI Elements
    @FXML private VBox paypalDetailsBox;
    @FXML private TextField txtPaypalEmail;

    private Flight selectedFlight;
    private String paxName;
    private String paxPassport;
    private String paxEmail;

    @FXML
    public void initialize() {
        // Setup Payment Methods
        comboPaymentMethod.getItems().addAll("Credit / Debit Card", "PayPal");
        comboPaymentMethod.getSelectionModel().selectFirst();
        
        // Listen for changes and swap UI
        comboPaymentMethod.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if ("PayPal".equals(newVal)) {
                cardDetailsBox.setVisible(false);
                cardDetailsBox.setManaged(false);
                paypalDetailsBox.setVisible(true);
                paypalDetailsBox.setManaged(true);
            } else {
                cardDetailsBox.setVisible(true);
                cardDetailsBox.setManaged(true);
                paypalDetailsBox.setVisible(false);
                paypalDetailsBox.setManaged(false);
            }
        });
    }

    public void setBookingDetails(Flight flight, String name, String passport, String email) {
        this.selectedFlight = flight;
        this.paxName = name;
        this.paxPassport = passport;
        this.paxEmail = email;

        txtAmount.setText(String.format("Total Amount Due: $%.2f", flight.getPrice()));
    }

    @FXML
    void handlePayNow(ActionEvent event) {
        String paymentMethod = comboPaymentMethod.getValue();
        
        // Dynamic Validation based on method
        if ("Credit / Debit Card".equals(paymentMethod)) {
            if (txtCardName.getText().trim().isEmpty() || txtCardNumber.getText().trim().length() < 15 || txtCVV.getText().trim().length() < 3) {
                showAlert(Alert.AlertType.ERROR, "Payment Failed", "Please enter valid credit card details.");
                return;
            }
            System.out.println("Processing CARD payment for $" + selectedFlight.getPrice());
        } else if ("PayPal".equals(paymentMethod)) {
            if (!txtPaypalEmail.getText().contains("@")) {
                showAlert(Alert.AlertType.ERROR, "Payment Failed", "Please enter a valid PayPal email address.");
                return;
            }
            System.out.println("Processing PAYPAL payment for $" + selectedFlight.getPrice());
        }

        // Generate PNR
        String pnr = generatePNR();

        // Save to Database
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Could not connect to database.");
                return;
            }
            conn.setAutoCommit(false);

            String insertReservation = "INSERT INTO reservations (user_id, flight_id, passenger_name, passport_number, contact_email, pnr, status) VALUES (?, ?, ?, ?, ?, ?, 'Paid')";
            try (PreparedStatement pstmt = conn.prepareStatement(insertReservation)) {
                pstmt.setInt(1, 1); // Admin user
                pstmt.setInt(2, selectedFlight.getFlightId());
                pstmt.setString(3, paxName);
                pstmt.setString(4, paxPassport);
                pstmt.setString(5, paxEmail);
                pstmt.setString(6, pnr);
                pstmt.executeUpdate();
            }

            String updateSeats = "UPDATE flights SET available_seats = available_seats - 1 WHERE flight_id = ?";
            try (PreparedStatement pstmt2 = conn.prepareStatement(updateSeats)) {
                pstmt2.setInt(1, selectedFlight.getFlightId());
                pstmt2.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(true);

            showAlert(Alert.AlertType.INFORMATION, "Payment Successful!", "Ticket Booked via " + paymentMethod + "!\n\nPassenger: " + paxName + "\nYour PNR is: " + pnr + "\n\nPlease save this code for Check-in.");
            closeWindow();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "System Error", "Payment succeeded, but failed to save booking to database.");
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private String generatePNR() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder pnr = new StringBuilder();
        Random rnd = new Random();
        while (pnr.length() < 6) { 
            int index = (int) (rnd.nextFloat() * chars.length());
            pnr.append(chars.charAt(index));
        }
        return pnr.toString();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtAmount.getScene().getWindow();
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
