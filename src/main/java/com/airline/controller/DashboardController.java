package com.airline.controller;

import com.airline.model.Flight;
import com.airline.util.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardController {

    @FXML private TextField departureSearch;
    @FXML private TextField arrivalSearch;

    // Notice we replaced the <?> with <Flight> and the actual data types
    @FXML private TableView<Flight> flightTable;
    @FXML private TableColumn<Flight, Integer> colId;
    @FXML private TableColumn<Flight, String> colAirline;
    @FXML private TableColumn<Flight, String> colDeparture;
    @FXML private TableColumn<Flight, String> colArrival;
    @FXML private TableColumn<Flight, String> colTime;
    @FXML private TableColumn<Flight, Double> colPrice;
    @FXML private TableColumn<Flight, Integer> colSeats;
    

    // This list holds the data that gets passed to the table
    private ObservableList<Flight> flightList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. Link the table columns to the getter methods in our Flight model
        colId.setCellValueFactory(new PropertyValueFactory<>("flightId"));
        colAirline.setCellValueFactory(new PropertyValueFactory<>("airline"));
        colDeparture.setCellValueFactory(new PropertyValueFactory<>("departureCity"));
        colArrival.setCellValueFactory(new PropertyValueFactory<>("arrivalCity"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("departureDatetime"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colSeats.setCellValueFactory(new PropertyValueFactory<>("availableSeats"));

        // 2. Fetch data from the database immediately when the window opens
        loadFlightsFromDatabase("", "");
    }
    

    @FXML
    void handleSearch(ActionEvent event) {
        String dep = departureSearch.getText().trim();
        String arr = arrivalSearch.getText().trim();
        loadFlightsFromDatabase(dep, arr);
    }

    @FXML
    void handleClear(ActionEvent event) {
        departureSearch.clear();
        arrivalSearch.clear();
        loadFlightsFromDatabase("", ""); // Refresh table with all data
    }

    private void loadFlightsFromDatabase(String departure, String arrival) {
        flightList.clear(); // Clear old results

        // Build our SQL query dynamically based on search fields
        String query = "SELECT * FROM flights WHERE 1=1";
        if (!departure.isEmpty()) {
            query += " AND departure_city LIKE ?";
        }
        if (!arrival.isEmpty()) {
            query += " AND arrival_city LIKE ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            int paramIndex = 1;
            if (!departure.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + departure + "%");
            }
            if (!arrival.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + arrival + "%");
            }

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                // Read from database row and create a Flight object
                Flight flight = new Flight(
                        rs.getInt("flight_id"),
                        rs.getString("airline"),
                        rs.getString("departure_city"),
                        rs.getString("arrival_city"),
                        rs.getString("departure_datetime"), 
                        rs.getDouble("price"),
                        rs.getInt("available_seats")
                );
                flightList.add(flight); // Add to our list
            }

            // Put the populated list into the table view
            flightTable.setItems(flightList);

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error loading flights from database.");
        }
    }

    @FXML
    void handleBookFlight(ActionEvent event) {
        Flight selectedFlight = flightTable.getSelectionModel().getSelectedItem();

        if (selectedFlight == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Flight Selected");
            alert.setHeaderText(null);
            alert.setContentText("Please select a flight from the table first!");
            alert.show();
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/Bookings.fxml"));
            javafx.scene.Node view = loader.load();

            // Pass the selected flight into the BookingController
            BookingController bookingCtrl = loader.getController();
            bookingCtrl.setFlight(selectedFlight);

            // Swap the center view in the SPA shell and highlight the sidebar tab
            MainController.getInstance().setView(view);
            MainController.getInstance().setActiveTabExternal("bookings");

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}