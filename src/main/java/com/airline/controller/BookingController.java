package com.airline.controller;

import com.airline.model.Flight;
import com.airline.util.DatabaseConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class BookingController {

    @FXML private VBox seatMapContainer;
    @FXML private Label lblFlightInfo;
    @FXML private ComboBox<String> comboTitle;
    @FXML private TextField txtFullName;
    @FXML private TextField txtPassport;
    @FXML private TextField txtEmail;
    @FXML private TextField txtNationality;
    @FXML private Label lblSelectedSeat;
    @FXML private Label lblTotalPrice;
    @FXML private Label lblPassportError;
    @FXML private Label lblEmailError;
    @FXML private Button btnProceed;

    private ToggleGroup seatGroup = new ToggleGroup();
    private String selectedSeat = null;
    private Flight currentFlight = null;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("^[A-Z0-9]{5,12}$");

    @FXML
    public void initialize() {
        comboTitle.getItems().addAll("Mr.", "Mrs.", "Ms.", "Dr.", "Prof.");
        comboTitle.getSelectionModel().selectFirst();

        // Real-time validation listeners
        txtPassport.textProperty().addListener((obs, old, val) -> {
            if (!val.isEmpty() && !PASSPORT_PATTERN.matcher(val.toUpperCase()).matches()) {
                lblPassportError.setText("Must be 5-12 uppercase alphanumeric chars (e.g. N1234567)");
            } else {
                lblPassportError.setText("");
            }
        });

        txtEmail.textProperty().addListener((obs, old, val) -> {
            if (!val.isEmpty() && !EMAIL_PATTERN.matcher(val).matches()) {
                lblEmailError.setText("Enter a valid email address");
            } else {
                lblEmailError.setText("");
            }
        });

        // Generate seat map with default flight
        generateSeatMap(null);
    }

    /**
     * Called from DashboardController to pass the selected flight data
     */
    public void setFlight(Flight flight) {
        this.currentFlight = flight;
        lblFlightInfo.setText("Flight " + flight.getAirline() + " • " +
                flight.getDepartureCity() + " → " + flight.getArrivalCity());
        lblTotalPrice.setText(String.format("$%.2f", flight.getPrice()));

        // Reload seat map with real data for this flight
        seatMapContainer.getChildren().clear();
        seatGroup = new ToggleGroup();
        generateSeatMap(flight.getFlightId());
    }

    private Set<String> getOccupiedSeats(int flightId) {
        Set<String> occupied = new HashSet<>();
        String query = "SELECT seat_number FROM reservations WHERE flight_id = ? AND seat_number IS NOT NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, flightId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                occupied.add(rs.getString("seat_number"));
            }
        } catch (Exception e) {
            // If seat_number column doesn't exist yet, use default simulated occupancy
        }
        return occupied;
    }

    private void generateSeatMap(Integer flightId) {
        // Simulated occupied seats (used if DB data unavailable)
        Set<String> occupied = new HashSet<>();
        if (flightId != null) {
            occupied = getOccupiedSeats(flightId);
        }
        if (occupied.isEmpty()) {
            // Default demo occupied seats
            occupied.add("2A"); occupied.add("3C"); occupied.add("5A");
            occupied.add("6B"); occupied.add("7E"); occupied.add("7F");
            occupied.add("8B"); occupied.add("8F"); occupied.add("9D");
            occupied.add("10C"); occupied.add("11C"); occupied.add("11E");
        }

        String[] cols = {"A", "B", "C", "D", "E", "F"};

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        // -- Column header row --
        // Row number spacer
        Label rNumHdr = new Label("");
        rNumHdr.setPrefWidth(25);
        grid.add(rNumHdr, 0, 0);

        for (int c = 0; c < cols.length; c++) {
            Label hdr = new Label(cols[c]);
            hdr.setPrefWidth(46);
            hdr.setAlignment(Pos.CENTER);
            hdr.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold; -fx-font-size: 13px;");
            int colIndex = c <= 2 ? c + 1 : c + 2; // +2 to skip aisle col (4)
            grid.add(hdr, colIndex, 0);
            GridPane.setHalignment(hdr, HPos.CENTER);
        }

        // Aisle header
        Label aisleHdr = new Label("AISLE");
        aisleHdr.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 9px; -fx-font-weight: bold;");
        aisleHdr.setAlignment(Pos.CENTER);
        aisleHdr.setPrefWidth(20);
        grid.add(aisleHdr, 4, 0);
        GridPane.setHalignment(aisleHdr, HPos.CENTER);

        // -- Seat rows --
        for (int r = 1; r <= 12; r++) {
            // Left row number
            Label rowLabel = new Label(String.valueOf(r));
            rowLabel.setPrefWidth(25);
            rowLabel.setAlignment(Pos.CENTER);
            rowLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
            grid.add(rowLabel, 0, r);

            for (int c = 0; c < cols.length; c++) {
                String seatId = r + cols[c];
                boolean isOccupied = occupied.contains(seatId);

                ToggleButton btn = createSeatButton(seatId, isOccupied);
                int colIndex = c <= 2 ? c + 1 : c + 2;
                grid.add(btn, colIndex, r);
                GridPane.setHalignment(btn, HPos.CENTER);
            }

            // Right row number
            Label rowLabelR = new Label(String.valueOf(r));
            rowLabelR.setPrefWidth(25);
            rowLabelR.setAlignment(Pos.CENTER);
            rowLabelR.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
            grid.add(rowLabelR, 8, r);
        }

        seatMapContainer.getChildren().add(grid);
    }

    private ToggleButton createSeatButton(String seatId, boolean occupied) {
        ToggleButton btn = new ToggleButton(seatId);
        btn.setPrefWidth(46);
        btn.setPrefHeight(40);

        if (occupied) {
            btn.setDisable(true);
            btn.setStyle(
                "-fx-background-color: #E2E8F0; -fx-background-radius: 8;" +
                "-fx-text-fill: #94A3B8; -fx-font-size: 10px; -fx-border-color: #CBD5E1;" +
                "-fx-border-radius: 8;"
            );
        } else {
            btn.setToggleGroup(seatGroup);
            applyAvailableStyle(btn);
            btn.setOnAction(e -> handleSeatSelection(btn));
        }
        return btn;
    }

    private void applyAvailableStyle(ToggleButton btn) {
        btn.setStyle(
            "-fx-background-color: #FFFFFF; -fx-background-radius: 8;" +
            "-fx-text-fill: #64748B; -fx-font-size: 10px; -fx-border-color: #CBD5E1;" +
            "-fx-border-radius: 8; -fx-cursor: hand;"
        );
    }

    private void applySelectedStyle(ToggleButton btn) {
        btn.setStyle(
            "-fx-background-color: #1CA1F2; -fx-background-radius: 8;" +
            "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;" +
            "-fx-border-color: #0E87D4; -fx-border-radius: 8; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(28,161,242,0.4), 6, 0, 0, 2);"
        );
    }

    private void handleSeatSelection(ToggleButton clicked) {
        // Reset all visible seat buttons
        for (Toggle t : seatGroup.getToggles()) {
            ToggleButton tb = (ToggleButton) t;
            if (!tb.isDisabled()) applyAvailableStyle(tb);
        }

        if (clicked.isSelected()) {
            applySelectedStyle(clicked);
            selectedSeat = clicked.getText();

            String seatType = selectedSeat.endsWith("A") || selectedSeat.endsWith("F")
                    ? "Window" : selectedSeat.endsWith("C") || selectedSeat.endsWith("D") ? "Aisle" : "Middle";
            lblSelectedSeat.setText(selectedSeat + " (" + seatType + ")");

            double price = currentFlight != null ? currentFlight.getPrice() : 450.00;
            lblTotalPrice.setText(String.format("$%.2f", price));
            lblTotalPrice.setStyle("-fx-font-size: 26px; -fx-text-fill: #0A192F; -fx-font-weight: bold;");

            // Activate proceed button
            btnProceed.setStyle(
                "-fx-background-color: linear-gradient(to right, #1CA1F2, #0E87D4);" +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;" +
                "-fx-font-size: 13px; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(28,161,242,0.35), 10, 0, 0, 4);"
            );
        } else {
            selectedSeat = null;
            lblSelectedSeat.setText("None Selected");
            lblTotalPrice.setText("$0.00");
            btnProceed.setStyle(
                "-fx-background-color: #94A3B8; -fx-text-fill: white;" +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand;"
            );
        }
    }

    @FXML
    void handleProceed(ActionEvent event) {
        if (selectedSeat == null) {
            showError("No Seat Selected", "Please select a seat from the map.");
            return;
        }

        String name = txtFullName.getText().trim();
        String passport = txtPassport.getText().trim();
        String email = txtEmail.getText().trim();

        if (name.isEmpty()) { showError("Missing Field", "Full name is required."); return; }
        if (passport.isEmpty() || !PASSPORT_PATTERN.matcher(passport.toUpperCase()).matches()) {
            showError("Invalid Passport", "Please enter a valid passport/NIC number."); return;
        }
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            showError("Invalid Email", "Please enter a valid email address."); return;
        }

        // Load Payment Modal view
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PaymentModal.fxml"));
            Node paymentView = loader.load();

            PaymentModalController payCtrl = loader.getController();
            payCtrl.setBookingDetails(
                currentFlight,
                comboTitle.getValue() + " " + name,
                passport.toUpperCase(),
                email,
                selectedSeat
            );

            MainController.getInstance().setView(paymentView);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error", "Could not load payment screen.");
        }
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }
}
