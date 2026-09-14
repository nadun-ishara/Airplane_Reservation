package com.airline.controller;

import com.airline.util.BoardingPassGenerator;
import com.airline.util.DatabaseConnection;
import com.airline.util.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.*;

/**
 * Controller for the "My Bookings" / "All Bookings" view.
 */
public class MyBookingsController {

    @FXML private Label lblHeaderTitle;
    @FXML private Label lblHeaderSubtitle;

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> filterCombo;

    @FXML private TableView<BookingRow>             bookingsTable;
    @FXML private TableColumn<BookingRow, Integer>  colBookingId;
    @FXML private TableColumn<BookingRow, String>   colPnr;
    @FXML private TableColumn<BookingRow, Integer>  colFlight;
    @FXML private TableColumn<BookingRow, String>   colAirline;
    @FXML private TableColumn<BookingRow, String>   colRoute;
    @FXML private TableColumn<BookingRow, String>   colPassenger;
    @FXML private TableColumn<BookingRow, String>   colSeat;
    @FXML private TableColumn<BookingRow, String>   colStatus;
    @FXML private TableColumn<BookingRow, Double>   colPrice;

    @FXML private Label lblTotalCount;
    @FXML private Label lblConfirmedCount;
    @FXML private Label lblTotalSpent;

    private final ObservableList<BookingRow> masterBookings = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();
        if (session != null && session.isAdmin()) {
            if (lblHeaderTitle != null) lblHeaderTitle.setText("Reservation Management");
            if (lblHeaderSubtitle != null) lblHeaderSubtitle.setText("Master overview of all passenger bookings across all flights.");
        }

        colBookingId.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        colPnr      .setCellValueFactory(new PropertyValueFactory<>("pnr"));
        colFlight   .setCellValueFactory(new PropertyValueFactory<>("flightId"));
        colAirline  .setCellValueFactory(new PropertyValueFactory<>("airline"));
        colRoute    .setCellValueFactory(new PropertyValueFactory<>("route"));
        colPassenger.setCellValueFactory(new PropertyValueFactory<>("passengerName"));
        colSeat     .setCellValueFactory(new PropertyValueFactory<>("seatNumber"));
        colStatus   .setCellValueFactory(new PropertyValueFactory<>("status"));
        colPrice    .setCellValueFactory(new PropertyValueFactory<>("price"));

        // Colour-code the Status cell
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item.toLowerCase()) {
                    case "paid"    -> "-fx-text-fill: #28a745; -fx-font-weight: bold;";
                    case "pending" -> "-fx-text-fill: #e67e22; -fx-font-weight: bold;";
                    default        -> "-fx-text-fill: #6c757d; -fx-font-weight: bold;";
                });
            }
        });

        // Filter options
        filterCombo.setItems(FXCollections.observableArrayList("All", "Paid", "Pending"));
        filterCombo.setValue("All");

        // Real-time search listener
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }

        loadBookings();
    }

    private void loadBookings() {
        masterBookings.clear();
        UserSession session = UserSession.getInstance();

        StringBuilder sql = new StringBuilder("""
                SELECT r.reservation_id,
                       r.pnr,
                       r.flight_id,
                       f.airline,
                       CONCAT(f.departure_city, ' → ', f.arrival_city) AS route,
                       f.departure_datetime,
                       r.passenger_name AS full_name,
                       COALESCE(r.seat_number, 'Unassigned') AS seat_number,
                       r.status,
                       f.price,
                       r.booking_date
                FROM reservations r
                JOIN flights f ON r.flight_id = f.flight_id
                """);

        // If not admin, only show this user's bookings
        if (session != null && !session.isAdmin()) {
            sql.append(" WHERE r.user_id = ").append(session.getUserId());
        }

        sql.append(" ORDER BY r.reservation_id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {

            while (rs.next()) {
                BookingRow row = new BookingRow(
                        rs.getInt("reservation_id"),
                        rs.getString("pnr"),
                        rs.getInt("flight_id"),
                        rs.getString("airline"),
                        rs.getString("route"),
                        rs.getString("departure_datetime"),
                        rs.getString("full_name"),
                        rs.getString("seat_number"),
                        rs.getString("status"),
                        rs.getDouble("price")
                );
                masterBookings.add(row);
            }

            applyFilters();

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[MyBookingsController] DB error: " + e.getMessage());
        }
    }

    private void applyFilters() {
        String statusFilter = filterCombo.getValue();
        String query = txtSearch != null ? txtSearch.getText().trim().toLowerCase() : "";

        ObservableList<BookingRow> filtered = FXCollections.observableArrayList();
        double paidTotal = 0;
        int paidCount = 0;

        for (BookingRow row : masterBookings) {
            boolean matchesStatus = statusFilter == null || statusFilter.equalsIgnoreCase("All")
                    || row.getStatus().equalsIgnoreCase(statusFilter);

            boolean matchesSearch = query.isEmpty()
                    || row.getPnr().toLowerCase().contains(query)
                    || row.getPassengerName().toLowerCase().contains(query)
                    || row.getAirline().toLowerCase().contains(query)
                    || row.getRoute().toLowerCase().contains(query)
                    || String.valueOf(row.getFlightId()).contains(query);

            if (matchesStatus && matchesSearch) {
                filtered.add(row);
                if ("paid".equalsIgnoreCase(row.getStatus())) {
                    paidCount++;
                    paidTotal += row.getPrice();
                }
            }
        }

        bookingsTable.setItems(filtered);

        if (lblTotalCount != null) lblTotalCount.setText(String.valueOf(filtered.size()));
        if (lblConfirmedCount != null) lblConfirmedCount.setText(String.valueOf(paidCount));
        if (lblTotalSpent != null) lblTotalSpent.setText(String.format("%.2f", paidTotal));
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        if (txtSearch != null) txtSearch.clear();
        filterCombo.setValue("All");
        loadBookings();
    }

    @FXML
    private void handleFilter(ActionEvent event) {
        applyFilters();
    }

    @FXML
    private void handleDownloadBoardingPass(ActionEvent event) {
        BookingRow selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a booking from the table to download its boarding pass.");
            return;
        }

        Stage stage = (Stage) bookingsTable.getScene().getWindow();
        boolean success = BoardingPassGenerator.generate(
                stage,
                selected.getPnr(),
                selected.getPassengerName(),
                selected.getAirline(),
                selected.getRoute(),
                selected.getDepartureTime(),
                selected.getSeatNumber(),
                selected.getPrice(),
                selected.getStatus()
        );

        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Boarding Pass Downloaded", "Boarding pass for PNR " + selected.getPnr() + " was saved successfully.");
        }
    }

    @FXML
    private void handleCancelBooking(ActionEvent event) {
        BookingRow selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a reservation to cancel.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Reservation");
        confirm.setHeaderText("Cancel Booking #" + selected.getBookingId() + " (PNR: " + selected.getPnr() + ")");
        confirm.setContentText("Are you sure you want to cancel this booking for " + selected.getPassengerName() + "?\n\nThe seat (" + selected.getSeatNumber() + ") will be returned to the available flight pool.");

        ButtonType btnYes = new ButtonType("Yes, Cancel Booking", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Keep Booking", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnYes, btnNo);

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == btnYes) {
                deleteReservationAndRestoreSeat(selected.getBookingId(), selected.getFlightId());
                loadBookings();
            }
        });
    }

    /**
     * Deletes reservation, associated checkin, and increments available_seats in flights.
     */
    private void deleteReservationAndRestoreSeat(int reservationId, int flightId) {
        String deleteCheckin     = "DELETE FROM check_in WHERE reservation_id = ?";
        String deleteReservation = "DELETE FROM reservations WHERE reservation_id = ?";
        String restoreSeat       = "UPDATE flights SET available_seats = available_seats + 1 WHERE flight_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement p1 = conn.prepareStatement(deleteCheckin);
                 PreparedStatement p2 = conn.prepareStatement(deleteReservation);
                 PreparedStatement p3 = conn.prepareStatement(restoreSeat)) {

                p1.setInt(1, reservationId);
                p1.executeUpdate();

                p2.setInt(1, reservationId);
                p2.executeUpdate();

                p3.setInt(1, flightId);
                p3.executeUpdate();

                conn.commit();
                showAlert(Alert.AlertType.INFORMATION, "Reservation Cancelled", "Booking #" + reservationId + " has been cancelled. The seat has been restored to flight #" + flightId + ".");
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not cancel reservation: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    // -----------------------------------------------------------------------
    // Row model for the TableView
    // -----------------------------------------------------------------------
    public static class BookingRow {
        private final int    bookingId;
        private final String pnr;
        private final int    flightId;
        private final String airline;
        private final String route;
        private final String departureTime;
        private final String passengerName;
        private final String seatNumber;
        private final String status;
        private final double price;

        public BookingRow(int bookingId, String pnr, int flightId, String airline, String route,
                          String departureTime, String passengerName, String seatNumber, String status, double price) {
            this.bookingId     = bookingId;
            this.pnr           = pnr;
            this.flightId      = flightId;
            this.airline       = airline;
            this.route         = route;
            this.departureTime = departureTime;
            this.passengerName = passengerName;
            this.seatNumber    = seatNumber;
            this.status        = status;
            this.price         = price;
        }

        public int    getBookingId()     { return bookingId; }
        public String getPnr()           { return pnr; }
        public int    getFlightId()      { return flightId; }
        public String getAirline()       { return airline; }
        public String getRoute()         { return route; }
        public String getDepartureTime() { return departureTime; }
        public String getPassengerName() { return passengerName; }
        public String getSeatNumber()    { return seatNumber; }
        public String getStatus()        { return status; }
        public double getPrice()         { return price; }
    }
}
