package com.airline.controller;

import com.airline.util.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;

/**
 * Controller for the "My Bookings" sidebar view.
 *
 * Reads from the actual schema:
 *   reservations (reservation_id, user_id, flight_id, booking_date, status ENUM('Paid','Pending'))
 *   flights      (flight_id, airline, departure_city, arrival_city, price ...)
 *   users        (user_id, full_name, email ...)
 *   check_in     (checkin_id, reservation_id, seat_number, boarding_time)  -- optional join
 */
public class MyBookingsController {

    @FXML private TableView<BookingRow>             bookingsTable;
    @FXML private TableColumn<BookingRow, Integer>  colBookingId;
    @FXML private TableColumn<BookingRow, Integer>  colFlight;
    @FXML private TableColumn<BookingRow, String>   colAirline;
    @FXML private TableColumn<BookingRow, String>   colRoute;
    @FXML private TableColumn<BookingRow, String>   colPassenger;
    @FXML private TableColumn<BookingRow, String>   colSeat;
    @FXML private TableColumn<BookingRow, String>   colStatus;
    @FXML private TableColumn<BookingRow, Double>   colPrice;

    @FXML private ComboBox<String> filterCombo;

    @FXML private Label lblTotalCount;
    @FXML private Label lblConfirmedCount;
    @FXML private Label lblTotalSpent;

    private final ObservableList<BookingRow> allBookings = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colBookingId.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        colFlight   .setCellValueFactory(new PropertyValueFactory<>("flightId"));
        colAirline  .setCellValueFactory(new PropertyValueFactory<>("airline"));
        colRoute    .setCellValueFactory(new PropertyValueFactory<>("route"));
        colPassenger.setCellValueFactory(new PropertyValueFactory<>("passengerName"));
        colSeat     .setCellValueFactory(new PropertyValueFactory<>("seatNumber"));
        colStatus   .setCellValueFactory(new PropertyValueFactory<>("status"));
        colPrice    .setCellValueFactory(new PropertyValueFactory<>("price"));

        // Colour-code the Status cell to match ENUM('Paid', 'Pending')
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

        // Filter options must match the DB ENUM values
        filterCombo.setItems(FXCollections.observableArrayList("All", "Paid", "Pending"));
        filterCombo.setValue("All");

        loadBookings();
    }

    // ------------------------------------------------------------------
    // SQL matches the REAL schema:
    //   reservations.user_id  → users.user_id  (for passenger name)
    //   reservations.flight_id → flights       (for route, airline, price)
    //   LEFT JOIN check_in so rows still show even if not checked in yet
    // ------------------------------------------------------------------
    private void loadBookings() {
        allBookings.clear();

        String sql = """
                SELECT r.reservation_id,
                       r.flight_id,
                       f.airline,
                       CONCAT(f.departure_city, ' → ', f.arrival_city) AS route,
                       u.full_name,
                       COALESCE(ci.seat_number, 'Not Assigned') AS seat_number,
                       r.status,
                       f.price,
                       r.booking_date
                FROM reservations r
                JOIN    flights  f  ON r.flight_id = f.flight_id
                JOIN    users    u  ON r.user_id   = u.user_id
                LEFT JOIN check_in ci ON ci.reservation_id = r.reservation_id
                ORDER BY r.reservation_id DESC
                """;

        try (Connection  conn = DatabaseConnection.getConnection();
             Statement   stmt = conn.createStatement();
             ResultSet   rs   = stmt.executeQuery(sql)) {

            double paidTotal  = 0;
            int    paidCount  = 0;

            while (rs.next()) {
                BookingRow row = new BookingRow(
                        rs.getInt("reservation_id"),
                        rs.getInt("flight_id"),
                        rs.getString("airline"),
                        rs.getString("route"),
                        rs.getString("full_name"),
                        rs.getString("seat_number"),
                        rs.getString("status"),
                        rs.getDouble("price")
                );
                allBookings.add(row);

                if ("paid".equalsIgnoreCase(row.getStatus())) {
                    paidCount++;
                    paidTotal += row.getPrice();
                }
            }

            bookingsTable.setItems(allBookings);

            lblTotalCount    .setText(String.valueOf(allBookings.size()));
            lblConfirmedCount.setText(String.valueOf(paidCount));   // "Confirmed" stat = Paid
            lblTotalSpent    .setText(String.format("%.2f", paidTotal));

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[MyBookingsController] DB error: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        filterCombo.setValue("All");
        loadBookings();
    }

    @FXML
    private void handleFilter(ActionEvent event) {
        String selected = filterCombo.getValue();
        if (selected == null || selected.equals("All")) {
            bookingsTable.setItems(allBookings);
            return;
        }
        ObservableList<BookingRow> filtered = FXCollections.observableArrayList();
        for (BookingRow row : allBookings) {
            if (row.getStatus().equalsIgnoreCase(selected)) filtered.add(row);
        }
        bookingsTable.setItems(filtered);
    }

    @FXML
    private void handleCancelBooking(ActionEvent event) {
        BookingRow selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a reservation to cancel.");
            return;
        }
        if ("pending".equalsIgnoreCase(selected.getStatus())) {
            // Pending bookings can be deleted outright
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Cancel Reservation");
            confirm.setHeaderText("Cancel Reservation #" + selected.getBookingId() + "?");
            confirm.setContentText("This pending booking will be permanently removed.");
            confirm.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.OK) {
                    deleteReservation(selected.getBookingId());
                    loadBookings();
                }
            });
        } else {
            showAlert(Alert.AlertType.INFORMATION, "Cannot Cancel",
                    "Only Pending reservations can be cancelled.\n" +
                    "Reservation #" + selected.getBookingId() + " is already " + selected.getStatus() + ".");
        }
    }

    private void deleteReservation(int reservationId) {
        // Remove check_in record first (FK constraint), then reservation
        String deleteCheckin     = "DELETE FROM check_in     WHERE reservation_id = ?";
        String deleteReservation = "DELETE FROM reservations WHERE reservation_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement p1 = conn.prepareStatement(deleteCheckin);
                 PreparedStatement p2 = conn.prepareStatement(deleteReservation)) {
                p1.setInt(1, reservationId);
                p1.executeUpdate();
                p2.setInt(1, reservationId);
                p2.executeUpdate();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not cancel reservation: " + e.getMessage());
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
        private final int    flightId;
        private final String airline;
        private final String route;
        private final String passengerName;
        private final String seatNumber;
        private final String status;
        private final double price;

        public BookingRow(int bookingId, int flightId, String airline, String route,
                          String passengerName, String seatNumber, String status, double price) {
            this.bookingId     = bookingId;
            this.flightId      = flightId;
            this.airline       = airline;
            this.route         = route;
            this.passengerName = passengerName;
            this.seatNumber    = seatNumber;
            this.status        = status;
            this.price         = price;
        }

        public int    getBookingId()     { return bookingId; }
        public int    getFlightId()      { return flightId; }
        public String getAirline()       { return airline; }
        public String getRoute()         { return route; }
        public String getPassengerName() { return passengerName; }
        public String getSeatNumber()    { return seatNumber; }
        public String getStatus()        { return status; }
        public double getPrice()         { return price; }
    }
}
