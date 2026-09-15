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
import java.io.FileOutputStream;
import java.sql.*;
import java.util.Random;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import java.io.File;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;

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
    private double  finalFare = 0.0;
    private String  seatClass = "Economy Class";

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
        setBookingDetails(flight, name, passport, email, seat, flight != null ? flight.getPrice() : 0.0, "Economy Class");
    }

    public void setBookingDetails(Flight flight, String name, String passport, String email, String seat, double price, String seatClass) {
        this.flight        = flight;
        this.passengerName = name;
        this.passport      = passport;
        this.email         = email;
        this.seatNumber    = seat;
        this.finalFare     = price > 0 ? price : (flight != null ? flight.getPrice() : 0.0);
        this.seatClass     = seatClass != null ? seatClass : "Economy Class";

        lblAmount.setText(String.format("Total Amount Due: $%.2f", this.finalFare));
        lblPassengerInfo.setText(name + "  |  Seat: " + seat + " (" + this.seatClass + ")");
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
    //   reservations (user_id, flight_id, passenger_name, passport_number, contact_email, pnr, seat_number, status)
    // -----------------------------------------------------------------------
    private void saveReservation(String pnr, String paymentMethod) {
        String insertReservation = "INSERT INTO reservations (user_id, flight_id, passenger_name, passport_number, contact_email, pnr, seat_number, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String insertCheckin     = "INSERT INTO check_in (reservation_id, seat_number, boarding_time) VALUES (?, ?, NOW())";
        String updateSeats       = "UPDATE flights SET available_seats = available_seats - 1 WHERE flight_id = ? AND available_seats > 0";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Insert reservation and get the generated ID
            int reservationId = -1;
            try (PreparedStatement ps = conn.prepareStatement(insertReservation)) {
                int currentUserId = 1;
                if (com.airline.util.UserSession.getInstance() != null) {
                    currentUserId = com.airline.util.UserSession.getInstance().getUserId();
                }
                ps.setInt(1, currentUserId);
                ps.setInt(2, flight != null ? flight.getFlightId() : 0);
                ps.setString(3, passengerName);
                ps.setString(4, passport);
                ps.setString(5, email);
                ps.setString(6, pnr);
                ps.setString(7, seatNumber);
                ps.setString(8, "Paid");
                ps.executeUpdate();
                
                // Bulletproof way to get the last auto-incremented ID in MySQL
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()")) {
                    if (rs.next()) reservationId = rs.getInt(1);
                }
            }

            // 2. Insert check-in record (optional depending on your DB)
            if (reservationId > 0 && seatNumber != null) {
                try (PreparedStatement ps2 = conn.prepareStatement(insertCheckin)) {
                    ps2.setInt(1, reservationId);
                    ps2.setString(2, seatNumber);
                    ps2.executeUpdate();
                } catch (Exception e) {
                    System.out.println("Check-in insert skipped/failed: " + e.getMessage());
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

            // ----------------------------------------------------
            // PREMIUM CUSTOM SUCCESS DIALOG (CUSTOM STAGE)
            // ----------------------------------------------------
            Stage successStage = new Stage();
            successStage.initModality(Modality.APPLICATION_MODAL);
            successStage.initStyle(StageStyle.UNDECORATED);

            VBox root = new VBox(20);
            root.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 30; -fx-border-color: #E2E8F0; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 20, 0, 0, 10);");
            root.setAlignment(Pos.CENTER);

            Label icon = new Label("✔");
            icon.setStyle("-fx-font-size: 60px; -fx-text-fill: #10B981;");

            Label title = new Label("Payment Successful!");
            title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-font-family: 'Segoe UI', sans-serif;");

            Label subTitle = new Label("Your booking has been confirmed via " + paymentMethod);
            subTitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B; -fx-font-family: 'Segoe UI', sans-serif;");

            // Receipt Box
            VBox receiptBox = new VBox(10);
            receiptBox.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 20; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-min-width: 300;");
            
            Label lblPass = new Label("Passenger: " + passengerName);
            lblPass.setStyle("-fx-font-size: 15px; -fx-text-fill: #334155; -fx-font-family: 'Segoe UI', sans-serif;");
            
            Label lblSeatDetails = new Label("Seat: " + seatNumber + "  (" + seatClass + ")");
            lblSeatDetails.setStyle("-fx-font-size: 15px; -fx-text-fill: #334155; -fx-font-family: 'Segoe UI', sans-serif;");
            
            Label lblPnr = new Label("PNR: " + pnr);
            lblPnr.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-padding: 10 0 0 0; -fx-font-family: 'Segoe UI', sans-serif;");
            
            receiptBox.getChildren().addAll(lblPass, lblSeatDetails, new Separator(), lblPnr);

            // Action Buttons
            HBox buttonBox = new HBox(15);
            buttonBox.setAlignment(Pos.CENTER);
            
            Button btnPdf = new Button("Download Boarding Pass (PDF)");
            btnPdf.setStyle("-fx-background-color: #1CA1F2; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
            btnPdf.setOnAction(e -> {
                String airline = flight != null ? flight.getAirline() : "SkyLink Pro";
                String route = flight != null ? (flight.getDepartureCity() + " → " + flight.getArrivalCity()) : "Scheduled Route";
                String time = flight != null ? flight.getDepartureDatetime() : "Scheduled";
                double price = finalFare > 0 ? finalFare : (flight != null ? flight.getPrice() : 0.0);
                boolean ok = com.airline.util.BoardingPassGenerator.generate(
                    successStage, pnr, passengerName, airline, route, time, seatNumber + " (" + seatClass + ")", price, "Paid"
                );
                if (ok) {
                    showAlert(Alert.AlertType.INFORMATION, "Boarding Pass Generated", "Boarding pass PDF has been generated and saved successfully!");
                }
            });

            Button btnClose = new Button("Return to Dashboard");
            btnClose.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
            btnClose.setOnAction(e -> {
                successStage.close();
                MainController.getInstance().showDashboard();
            });

            buttonBox.getChildren().addAll(btnPdf, btnClose);

            root.getChildren().addAll(icon, title, subTitle, receiptBox, buttonBox);
            
            Scene scene = new Scene(root);
            scene.setFill(null);
            successStage.setScene(scene);
            successStage.showAndWait();

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
