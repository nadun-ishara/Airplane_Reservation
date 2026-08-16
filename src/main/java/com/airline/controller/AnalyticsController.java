package com.airline.controller;

import com.airline.util.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.sql.*;

/**
 * Controller for the Flight Analytics view.
 *
 * All data is pulled live from the database using the actual schema:
 *   flights      (flight_id, airline, departure_city, arrival_city, price, total_seats, available_seats)
 *   reservations (reservation_id, user_id, flight_id, booking_date, status ENUM('Paid','Pending'))
 */
public class AnalyticsController {

    // --- KPI Labels ---
    @FXML private Label lblTotalFlights;
    @FXML private Label lblTotalReservations;
    @FXML private Label lblPaidCount;
    @FXML private Label lblPaidPct;
    @FXML private Label lblRevenue;

    // --- Charts ---
    @FXML private PieChart              destinationPieChart;   // booking count by arrival city
    @FXML private BarChart<String, Number> airlineBarChart;    // bookings per airline
    @FXML private BarChart<String, Number> seatBarChart;       // available vs occupied seats
    @FXML private PieChart              statusPieChart;        // Paid vs Pending

    // --- Top Route ---
    @FXML private Label lblTopRoute;
    @FXML private Label lblTopRouteCount;

    // -----------------------------------------------------------------------

    @FXML
    public void initialize() {
        loadAll();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadAll();
    }

    private void loadAll() {
        loadKpis();
        loadDestinationPieChart();
        loadAirlineBarChart();
        loadSeatBarChart();
        loadStatusPieChart();
        loadTopRoute();
    }

    // -----------------------------------------------------------------------
    // KPI Cards
    // -----------------------------------------------------------------------
    private void loadKpis() {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM flights)      AS total_flights,
                    (SELECT COUNT(*) FROM reservations) AS total_res,
                    (SELECT COUNT(*) FROM reservations WHERE status = 'Paid') AS paid_count,
                    (SELECT COALESCE(SUM(f.price), 0)
                       FROM reservations r
                       JOIN flights f ON r.flight_id = f.flight_id
                      WHERE r.status = 'Paid')          AS revenue
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int    totalFlights = rs.getInt("total_flights");
                int    totalRes     = rs.getInt("total_res");
                int    paidCount    = rs.getInt("paid_count");
                double revenue      = rs.getDouble("revenue");

                lblTotalFlights     .setText(String.valueOf(totalFlights));
                lblTotalReservations.setText(String.valueOf(totalRes));
                lblPaidCount        .setText(String.valueOf(paidCount));
                lblRevenue          .setText(String.format("$%.2f", revenue));

                if (totalRes > 0) {
                    int pct = (int) Math.round((paidCount * 100.0) / totalRes);
                    lblPaidPct.setText(pct + "% of total");
                } else {
                    lblPaidPct.setText("No reservations yet");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            setKpiError();
        }
    }

    private void setKpiError() {
        lblTotalFlights     .setText("N/A");
        lblTotalReservations.setText("N/A");
        lblPaidCount        .setText("N/A");
        lblRevenue          .setText("N/A");
    }

    // -----------------------------------------------------------------------
    // Pie Chart 1: Booking count per arrival city (top destinations)
    // -----------------------------------------------------------------------
    private void loadDestinationPieChart() {
        destinationPieChart.getData().clear();

        String sql = """
                SELECT f.arrival_city, COUNT(r.reservation_id) AS booking_count
                FROM reservations r
                JOIN flights f ON r.flight_id = f.flight_id
                GROUP BY f.arrival_city
                ORDER BY booking_count DESC
                LIMIT 6
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            while (rs.next()) {
                String city  = rs.getString("arrival_city");
                int    count = rs.getInt("booking_count");
                pieData.add(new PieChart.Data(city + " (" + count + ")", count));
            }

            if (pieData.isEmpty()) {
                // Fallback: show flight distribution from flights table
                pieData.addAll(loadDestinationFallback());
            }

            destinationPieChart.setData(pieData);
            destinationPieChart.setTitle("");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** If no reservations exist yet, show arrival city distribution from the flights table. */
    private ObservableList<PieChart.Data> loadDestinationFallback() {
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        String sql = "SELECT arrival_city, COUNT(*) AS cnt FROM flights GROUP BY arrival_city";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                data.add(new PieChart.Data(rs.getString("arrival_city"), rs.getInt("cnt")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    // -----------------------------------------------------------------------
    // Bar Chart 1: Reservations per Airline
    // -----------------------------------------------------------------------
    private void loadAirlineBarChart() {
        airlineBarChart.getData().clear();

        String sql = """
                SELECT f.airline, COUNT(r.reservation_id) AS booking_count
                FROM reservations r
                JOIN flights f ON r.flight_id = f.flight_id
                GROUP BY f.airline
                ORDER BY booking_count DESC
                """;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Reservations");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            boolean hasData = false;
            while (rs.next()) {
                series.getData().add(new XYChart.Data<>(rs.getString("airline"), rs.getInt("booking_count")));
                hasData = true;
            }

            if (!hasData) {
                // Fallback: show flight count per airline
                series = loadAirlineFlightFallback();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        airlineBarChart.getData().add(series);
    }

    private XYChart.Series<String, Number> loadAirlineFlightFallback() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Flights");
        String sql = "SELECT airline, COUNT(*) AS cnt FROM flights GROUP BY airline";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                series.getData().add(new XYChart.Data<>(rs.getString("airline"), rs.getInt("cnt")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return series;
    }

    // -----------------------------------------------------------------------
    // Bar Chart 2: Seat Availability per Flight (Available vs Occupied)
    // -----------------------------------------------------------------------
    private void loadSeatBarChart() {
        seatBarChart.getData().clear();

        String sql = """
                SELECT flight_id,
                       available_seats,
                       (total_seats - available_seats) AS occupied_seats
                FROM flights
                ORDER BY flight_id
                """;

        XYChart.Series<String, Number> availSeries = new XYChart.Series<>();
        availSeries.setName("Available");

        XYChart.Series<String, Number> occupSeries = new XYChart.Series<>();
        occupSeries.setName("Occupied");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String flightId = "FL-" + rs.getInt("flight_id");
                availSeries.getData().add(new XYChart.Data<>(flightId, rs.getInt("available_seats")));
                occupSeries.getData().add(new XYChart.Data<>(flightId, rs.getInt("occupied_seats")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        seatBarChart.getData().addAll(availSeries, occupSeries);
    }

    // -----------------------------------------------------------------------
    // Pie Chart 2: Booking Status (Paid vs Pending)
    // -----------------------------------------------------------------------
    private void loadStatusPieChart() {
        statusPieChart.getData().clear();

        String sql = """
                SELECT status, COUNT(*) AS cnt
                FROM reservations
                GROUP BY status
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            int total = 0;
            while (rs.next()) {
                int cnt = rs.getInt("cnt");
                total += cnt;
                pieData.add(new PieChart.Data(rs.getString("status") + " (" + cnt + ")", cnt));
            }

            if (pieData.isEmpty()) {
                pieData.add(new PieChart.Data("No Reservations", 1));
            }

            statusPieChart.setData(pieData);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------------------------
    // Top Route label (most booked departure→arrival pair)
    // -----------------------------------------------------------------------
    private void loadTopRoute() {
        String sql = """
                SELECT CONCAT(f.departure_city, ' \u2192 ', f.arrival_city) AS route,
                       COUNT(r.reservation_id) AS cnt
                FROM reservations r
                JOIN flights f ON r.flight_id = f.flight_id
                GROUP BY route
                ORDER BY cnt DESC
                LIMIT 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            if (rs.next()) {
                lblTopRoute     .setText(rs.getString("route"));
                lblTopRouteCount.setText(rs.getInt("cnt") + " reservations");
            } else {
                // Fallback: just show first flight route
                lblTopRoute     .setText("No bookings yet");
                lblTopRouteCount.setText("");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblTopRoute.setText("N/A");
        }
    }
}
