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
import java.sql.*;
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

    private Flight  flight;
    private String  passengerName;
    private String  passport;
    private String  email;
    private String  seatNumber;

    @FXML
    public void initialize() {
        comboPaymentMethod.getItems().addAll("Credit / Debit Card", "PayPal");
        comboPaymentMethod.getSelectionModel().selectFirst();

        comboPaymentMethod.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            boolean isCard = "Credit / Debit Card".equals(val);
            cardDetailsBox .setVisible(isCard);   cardDetailsBox .setManaged(isCard);
            paypalDetailsBox.setVisible(!isCard);  paypalDetailsBox.setManaged(!isCard);
        });
    }

    public void setBookingDetails(Flight flight, String name, String passport, String email, String seat) {
        this.flight        = flight;
        this.passengerName = name;
        this.passport      = passport;
        this.email         = email;
        this.seatNumber    = seat;

        double price = flight != null ? flight.getPrice() : 0.0;
        lblAmount      .setText(String.format("Total Amount Due: $%.2f", price));
        lblPassengerInfo.setText(name + "  |  Seat: " + seat);
    }

    @FXML
    void handlePayNow(ActionEvent event) {
        String method = comboPaymentMethod.getValue();

        if ("Credit / Debit Card".equals(method)) {
            if (txtNameOnCard.getText().trim().isEmpty()
                    || txtCardNumber.getText().trim().length() < 15
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

    // -----------------------------------------------------------------------
    // Saves to the ACTUAL database schema:
    //   reservations (user_id, flight_id, passenger_name, passport_number, contact_email, pnr, status)
    //   check_in     (reservation_id, seat_number, boarding_time)
    // -----------------------------------------------------------------------
    private void saveReservation(String pnr, String paymentMethod) {
        String insertReservation = "INSERT INTO reservations (user_id, flight_id, passenger_name, passport_number, contact_email, pnr, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertCheckin     = "INSERT INTO check_in (reservation_id, seat_number, boarding_time) VALUES (?, ?, NOW())";
        String updateSeats       = "UPDATE flights SET available_seats = available_seats - 1 WHERE flight_id = ? AND available_seats > 0";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Insert reservation and get the generated ID
            int reservationId = -1;
            try (PreparedStatement ps = conn.prepareStatement(insertReservation, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, 1);          // user_id=1 (Admin / current session user)
                ps.setInt(2, flight != null ? flight.getFlightId() : 0);
                ps.setString(3, passengerName);
                ps.setString(4, passport);
                ps.setString(5, email);
                ps.setString(6, pnr);
                ps.setString(7, "Paid");
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) reservationId = keys.getInt(1);
                }
            }

            // 2. Insert check-in record (seat assignment)
            if (reservationId > 0 && seatNumber != null) {
                try (PreparedStatement ps2 = conn.prepareStatement(insertCheckin)) {
                    ps2.setInt(1, reservationId);
                    ps2.setString(2, seatNumber);
                    ps2.executeUpdate();
                }
            }

            // 3. Decrement available_seats on the flight
            if (flight != null) {
                try (PreparedStatement ps3 = conn.prepareStatement(updateSeats)) {
                    ps3.setInt(1, flight.getFlightId());
                    ps3.executeUpdate();
                }
            }

            conn.commit();
            conn.setAutoCommit(true);

            showAlert(Alert.AlertType.INFORMATION, "Payment Successful! \u2708",
                    "Booking confirmed via " + paymentMethod + "!\n\n"
                    + "Passenger : " + passengerName + "\n"
                    + "Seat      : " + seatNumber + "\n"
                    + "PNR       : " + pnr + "\n\n"
                    + "Please save your PNR for Check-in.");

            MainController.getInstance().showDashboard();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not save booking:\n" + e.getMessage());
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        MainController.getInstance().showDashboard();
    }

    private String generatePNR() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder pnr = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) pnr.append(chars.charAt(rnd.nextInt(chars.length())));
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
