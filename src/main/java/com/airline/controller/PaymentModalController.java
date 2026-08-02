package com.airline.controller;

import com.airline.model.Flight;
import com.airline.util.DatabaseConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

public class PaymentModalController {

    @FXML private Label lblAmount;
    @FXML private Label lblPassengerInfo;
    @FXML private ComboBox<String> comboPaymentMethod;
    @FXML private VBox cardDetailsBox;
    @FXML private VBox paypalDetailsBox;
    @FXML private TextField txtNameOnCard;
    @FXML private TextField txtCardNumber;
    @FXML private TextField txtExpiry;
    @FXML private TextField txtCvv;
    @FXML private TextField txtPaypalEmail;

    private Flight flight;
    private String passengerName;
    private String passport;
    private String email;
    private String seatNumber;

    @FXML
    public void initialize() {
        comboPaymentMethod.getItems().addAll("Credit / Debit Card", "PayPal");
        comboPaymentMethod.getSelectionModel().selectFirst();

        comboPaymentMethod.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            boolean isCard = "Credit / Debit Card".equals(val);
            cardDetailsBox.setVisible(isCard);
            cardDetailsBox.setManaged(isCard);
            paypalDetailsBox.setVisible(!isCard);
            paypalDetailsBox.setManaged(!isCard);
        });
    }

    public void setBookingDetails(Flight flight, String name, String passport, String email, String seat) {
        this.flight = flight;
        this.passengerName = name;
        this.passport = passport;
        this.email = email;
        this.seatNumber = seat;

        double price = flight != null ? flight.getPrice() : 450.0;
        lblAmount.setText(String.format("Total Amount Due: $%.2f", price));
        lblPassengerInfo.setText(name + " | Seat: " + seat);
    }

    @FXML
    void handlePayNow(ActionEvent event) {
        String method = comboPaymentMethod.getValue();

        if ("Credit / Debit Card".equals(method)) {
            if (txtNameOnCard.getText().trim().isEmpty() || txtCardNumber.getText().trim().length() < 15
                    || txtCvv.getText().trim().length() < 3) {
                showAlert(Alert.AlertType.ERROR, "Invalid Card", "Please enter valid card details.");
                return;
            }
        } else {
            if (!txtPaypalEmail.getText().contains("@")) {
                showAlert(Alert.AlertType.ERROR, "Invalid Email", "Please enter a valid PayPal email.");
                return;
            }
        }

        String pnr = generatePNR();
        saveReservation(pnr, method);
    }

    private void saveReservation(String pnr, String paymentMethod) {
        String insertSql = "INSERT INTO reservations (user_id, flight_id, passenger_name, passport_number, contact_email, pnr, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String updateSql = "UPDATE flights SET available_seats = available_seats - 1 WHERE flight_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(insertSql)) {
                ps1.setInt(1, 1);
                ps1.setInt(2, flight != null ? flight.getFlightId() : 0);
                ps1.setString(3, passengerName);
                ps1.setString(4, passport);
                ps1.setString(5, email);
                ps1.setString(6, pnr);
                ps1.setString(7, "Paid");
                ps1.executeUpdate();
            }

            if (flight != null) {
                try (PreparedStatement ps2 = conn.prepareStatement(updateSql)) {
                    ps2.setInt(1, flight.getFlightId());
                    ps2.executeUpdate();
                }
            }

            conn.commit();
            conn.setAutoCommit(true);

            // Show success and navigate to Dashboard
            showAlert(Alert.AlertType.INFORMATION, "Payment Successful! ✈",
                    "Booking confirmed via " + paymentMethod + "!\n\n" +
                    "Passenger: " + passengerName + "\n" +
                    "Seat: " + seatNumber + "\n" +
                    "PNR: " + pnr + "\n\n" +
                    "Please save your PNR for Check-in.");

            MainController.getInstance().showDashboard();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Payment processed but failed to save booking.");
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        MainController.getInstance().showBookings();
    }

    private String generatePNR() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder pnr = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) {
            pnr.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return pnr.toString();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
