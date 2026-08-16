package com.airline.controller;

import com.airline.model.Flight;
import com.airline.util.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardController {

    @FXML private TextField departureSearch;
    @FXML private TextField arrivalSearch;

    @FXML private TableView<Flight>             flightTable;
    @FXML private TableColumn<Flight, Integer>  colId;
    @FXML private TableColumn<Flight, String>   colAirline;
    @FXML private TableColumn<Flight, String>   colDeparture;
    @FXML private TableColumn<Flight, String>   colArrival;
    @FXML private TableColumn<Flight, String>   colTime;
    @FXML private TableColumn<Flight, Double>   colPrice;
    @FXML private TableColumn<Flight, Integer>  colSeats;

    // UX elements
    @FXML private Button  btnBook;
    @FXML private Label   lblSelectHint;
    @FXML private HBox    selectedFlightPanel;
    @FXML private Label   lblSelectedFlight;
    @FXML private Label   lblSelectedPrice;

    private final ObservableList<Flight> flightList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Wire table columns
        colId       .setCellValueFactory(new PropertyValueFactory<>("flightId"));
        colAirline  .setCellValueFactory(new PropertyValueFactory<>("airline"));
        colDeparture.setCellValueFactory(new PropertyValueFactory<>("departureCity"));
        colArrival  .setCellValueFactory(new PropertyValueFactory<>("arrivalCity"));
        colTime     .setCellValueFactory(new PropertyValueFactory<>("departureDatetime"));
        colPrice    .setCellValueFactory(new PropertyValueFactory<>("price"));
        colSeats    .setCellValueFactory(new PropertyValueFactory<>("availableSeats"));

        // ── Selection listener ──────────────────────────────────────────────
        // Reacts every time the user clicks a different row.
        // Enables the Book button and shows a green selection panel.
        flightTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldFlight, newFlight) -> onFlightSelected(newFlight));

        // Load all flights on startup
        loadFlightsFromDatabase("", "");
    }

    // -----------------------------------------------------------------------
    // Called whenever a row is selected or deselected
    // -----------------------------------------------------------------------
    private void onFlightSelected(Flight flight) {
        if (flight == null) {
            // Nothing selected — reset to default state
            btnBook.setDisable(true);
            btnBook.setStyle("-fx-background-color: #94A3B8; -fx-opacity: 0.75;");
            lblSelectHint.setVisible(true);
            lblSelectHint.setManaged(true);
            selectedFlightPanel.setVisible(false);
            selectedFlightPanel.setManaged(false);
        } else {
            // A row is selected — activate the Book button
            btnBook.setDisable(false);
            btnBook.setStyle(""); // reverts to .success-button CSS (green)

            // Hide hint, show selection panel
            lblSelectHint.setVisible(false);
            lblSelectHint.setManaged(false);
            selectedFlightPanel.setVisible(true);
            selectedFlightPanel.setManaged(true);

            // Populate selection info
            lblSelectedFlight.setText(
                flight.getAirline() + "  •  " +
                flight.getDepartureCity() + " → " + flight.getArrivalCity()
            );
            lblSelectedPrice.setText(String.format("$%.2f", flight.getPrice()));
        }
    }

    // -----------------------------------------------------------------------
    // Search / Clear
    // -----------------------------------------------------------------------
    @FXML
    void handleSearch(ActionEvent event) {
        loadFlightsFromDatabase(
            departureSearch.getText().trim(),
            arrivalSearch.getText().trim()
        );
    }

    @FXML
    void handleClear(ActionEvent event) {
        departureSearch.clear();
        arrivalSearch.clear();
        loadFlightsFromDatabase("", "");
    }

    private void loadFlightsFromDatabase(String departure, String arrival) {
        flightList.clear();

        String query = "SELECT * FROM flights WHERE 1=1";
        if (!departure.isEmpty()) query += " AND departure_city LIKE ?";
        if (!arrival.isEmpty())   query += " AND arrival_city LIKE ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            int idx = 1;
            if (!departure.isEmpty()) pstmt.setString(idx++, "%" + departure + "%");
            if (!arrival.isEmpty())   pstmt.setString(idx,   "%" + arrival   + "%");

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                flightList.add(new Flight(
                    rs.getInt("flight_id"),
                    rs.getString("airline"),
                    rs.getString("departure_city"),
                    rs.getString("arrival_city"),
                    rs.getString("departure_datetime"),
                    rs.getDouble("price"),
                    rs.getInt("available_seats")
                ));
            }
            flightTable.setItems(flightList);

            // Reset selection state after a new search
            onFlightSelected(null);
            flightTable.getSelectionModel().clearSelection();

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[DashboardController] Error loading flights: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Book button handler
    // -----------------------------------------------------------------------
    @FXML
    void handleBookFlight(ActionEvent event) {
        Flight selectedFlight = flightTable.getSelectionModel().getSelectedItem();

        // Defensive check (should never be null since button is disabled when nothing selected)
        if (selectedFlight == null) return;

        if (selectedFlight.getAvailableSeats() <= 0) {
            showAlert(Alert.AlertType.WARNING, "Flight Full",
                "This flight is fully booked. Please choose another flight.");
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/Bookings.fxml"));
            javafx.scene.Node view = loader.load();

            BookingController bookingCtrl = loader.getController();
            bookingCtrl.setFlight(selectedFlight);

            MainController.getInstance().setView(view);
            MainController.getInstance().setActiveTabExternal("bookings");

        } catch (java.io.IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open booking screen:\n" + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}