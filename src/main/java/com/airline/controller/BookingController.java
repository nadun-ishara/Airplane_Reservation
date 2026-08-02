package com.airline.controller;

import com.airline.model.Flight;
import com.airline.util.DatabaseConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

public class BookingController {

    @FXML private Text flightDetailsText;
    @FXML private TextField nameField;
    @FXML private TextField passportField;
    @FXML private TextField emailField;

    private Flight selectedFlight;

    // This method is called from DashboardController to pass the flight data
    public void setFlightData(Flight flight) {
        this.selectedFlight = flight;
        flightDetailsText.setText("Booking Flight: " + flight.getAirline() + " from " + flight.getDepartureCity() + " to " + flight.getArrivalCity());
    }

    @FXML
    void handleConfirmBooking(ActionEvent event) {
        String name = nameField.getText().trim();
        String passport = passportField.getText().trim();
        String email = emailField.getText().trim();

        if (name.isEmpty() || passport.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "All passenger fields are required!");
            return;
        }

        // Generate Random 6-Character PNR
        String pnr = generatePNR();

        // Save to Database
        try (Connection conn = DatabaseConnection.getConnection()) {
            // We use a transaction because we need to insert a reservation AND update flight seats
            conn.setAutoCommit(false);

            String insertReservation = "INSERT INTO reservations (user_id, flight_id, passenger_name, passport_number, contact_email, pnr, status) VALUES (?, ?, ?, ?, ?, ?, 'Paid')";
            try (PreparedStatement pstmt = conn.prepareStatement(insertReservation)) {
                pstmt.setInt(1, 1); // Hardcoded user_id=1 (Admin) for now
                pstmt.setInt(2, selectedFlight.getFlightId());
                pstmt.setString(3, name);
                pstmt.setString(4, passport);
                pstmt.setString(5, email);
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

            showAlert(Alert.AlertType.INFORMATION, "Booking Confirmed!", "Booking successful!\n\nYour PNR is: " + pnr + "\n\nPlease save this code for Check-in.");
            closeWindow();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to confirm booking.");
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
        Stage stage = (Stage) nameField.getScene().getWindow();
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
