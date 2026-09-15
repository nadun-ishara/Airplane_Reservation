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
import javafx.stage.Stage;

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
    @FXML private Button  btnAddFlight;
    @FXML private Button  btnEditFlight;
    @FXML private Button  btnDeleteFlight;
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

        // Admin action buttons only visible to Admins
        com.airline.util.UserSession session = com.airline.util.UserSession.getInstance();
        boolean isAdmin = (session != null && session.isAdmin());
        if (btnAddFlight != null) {
            btnAddFlight.setVisible(isAdmin);
            btnAddFlight.setManaged(isAdmin);
        }
        if (btnEditFlight != null) {
            btnEditFlight.setVisible(isAdmin);
            btnEditFlight.setManaged(isAdmin);
            btnEditFlight.setDisable(true);
        }
        if (btnDeleteFlight != null) {
            btnDeleteFlight.setVisible(isAdmin);
            btnDeleteFlight.setManaged(isAdmin);
            btnDeleteFlight.setDisable(true);
        }

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
        com.airline.util.UserSession session = com.airline.util.UserSession.getInstance();
        boolean isAdmin = (session != null && session.isAdmin());

        if (flight == null) {
            // Nothing selected — reset to default state
            btnBook.setDisable(true);
            btnBook.setStyle("-fx-background-color: #94A3B8; -fx-opacity: 0.75;");
            lblSelectHint.setVisible(true);
            lblSelectHint.setManaged(true);
            selectedFlightPanel.setVisible(false);
            selectedFlightPanel.setManaged(false);

            if (btnEditFlight != null) btnEditFlight.setDisable(true);
            if (btnDeleteFlight != null) btnDeleteFlight.setDisable(true);
        } else {
            // A row is selected — activate the Book button
            btnBook.setDisable(false);
            btnBook.setStyle(""); // reverts to .success-button CSS (green)

            if (btnEditFlight != null) btnEditFlight.setDisable(!isAdmin);
            if (btnDeleteFlight != null) btnDeleteFlight.setDisable(!isAdmin);

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

    @FXML
    void handleAddFlight(ActionEvent event) {
        Stage addStage = new Stage();
        addStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        addStage.setTitle("SkyLink Pro - Add New Flight");

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(12);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 24;");
        root.setPrefWidth(420);

        Label title = new Label("Add New Scheduled Flight");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #003366;");

        TextField txtId = new TextField();
        txtId.setPromptText("Flight ID (e.g. 106)");
        txtId.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        TextField txtAirline = new TextField();
        txtAirline.setPromptText("Airline (e.g. Emirates, SriLankan)");
        txtAirline.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        TextField txtDep = new TextField();
        txtDep.setPromptText("Departure City (e.g. Colombo)");
        txtDep.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        TextField txtArr = new TextField();
        txtArr.setPromptText("Arrival City (e.g. Melbourne)");
        txtArr.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        TextField txtTime = new TextField();
        txtTime.setPromptText("YYYY-MM-DD HH:MM:SS (e.g. 2026-09-25 15:30:00)");
        txtTime.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        TextField txtPrice = new TextField();
        txtPrice.setPromptText("Price in USD (e.g. 520.00)");
        txtPrice.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        TextField txtSeats = new TextField();
        txtSeats.setPromptText("Total Capacity (e.g. 200)");
        txtSeats.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");

        Button btnSave = new Button("Create Flight");
        btnSave.setStyle("-fx-background-color: #003366; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 9 18; -fx-background-radius: 6; -fx-cursor: hand;");

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-padding: 9 18; -fx-background-radius: 6; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> addStage.close());

        btnSave.setOnAction(e -> {
            try {
                int flightId = Integer.parseInt(txtId.getText().trim());
                String airline = txtAirline.getText().trim();
                String dep = txtDep.getText().trim();
                String arr = txtArr.getText().trim();
                String time = txtTime.getText().trim();
                double price = Double.parseDouble(txtPrice.getText().trim());
                int totalSeats = Integer.parseInt(txtSeats.getText().trim());

                if (airline.isEmpty() || dep.isEmpty() || arr.isEmpty() || time.isEmpty()) {
                    lblErr.setText("Please fill all required fields.");
                    return;
                }

                String insertSql = "INSERT INTO flights (flight_id, airline, departure_city, arrival_city, departure_datetime, price, total_seats, available_seats) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    pstmt.setInt(1, flightId);
                    pstmt.setString(2, airline);
                    pstmt.setString(3, dep);
                    pstmt.setString(4, arr);
                    pstmt.setString(5, time);
                    pstmt.setDouble(6, price);
                    pstmt.setInt(7, totalSeats);
                    pstmt.setInt(8, totalSeats);
                    pstmt.executeUpdate();

                    addStage.close();
                    showAlert(Alert.AlertType.INFORMATION, "Flight Added", "Flight #" + flightId + " (" + airline + ") was created successfully.");
                    loadFlightsFromDatabase("", "");
                }
            } catch (NumberFormatException nfe) {
                lblErr.setText("Flight ID, Price, and Seats must be valid numbers.");
            } catch (SQLException ex) {
                lblErr.setText("Database error: " + ex.getMessage());
            }
        });

        HBox btns = new HBox(10, btnSave, btnCancel);
        btns.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        root.getChildren().addAll(
            title,
            new Label("Flight Number / ID:"), txtId,
            new Label("Airline Name:"), txtAirline,
            new Label("Departure City:"), txtDep,
            new Label("Arrival City:"), txtArr,
            new Label("Departure Date & Time:"), txtTime,
            new Label("Ticket Price ($):"), txtPrice,
            new Label("Total Available Seats:"), txtSeats,
            lblErr,
            btns
        );

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        addStage.setScene(scene);
        addStage.showAndWait();
    }

    @FXML
    void handleEditFlight(ActionEvent event) {
        Flight selected = flightTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Flight Selected", "Please select a flight to edit.");
            return;
        }

        Stage editStage = new Stage();
        editStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        editStage.setTitle("SkyLink Pro - Edit Flight #" + selected.getFlightId());

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(12);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 24;");
        root.setPrefWidth(420);

        Label title = new Label("Edit Flight #" + selected.getFlightId());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #003366;");

        TextField txtAirline = new TextField(selected.getAirline());
        txtAirline.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        TextField txtDep = new TextField(selected.getDepartureCity());
        txtDep.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        TextField txtArr = new TextField(selected.getArrivalCity());
        txtArr.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        TextField txtTime = new TextField(selected.getDepartureDatetime());
        txtTime.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        TextField txtPrice = new TextField(String.valueOf(selected.getPrice()));
        txtPrice.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        TextField txtSeats = new TextField(String.valueOf(selected.getAvailableSeats()));
        txtSeats.setStyle("-fx-padding: 8; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");

        Button btnUpdate = new Button("Update Flight");
        btnUpdate.setStyle("-fx-background-color: #003366; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 9 18; -fx-background-radius: 6; -fx-cursor: hand;");

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-padding: 9 18; -fx-background-radius: 6; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> editStage.close());

        btnUpdate.setOnAction(e -> {
            try {
                String airline = txtAirline.getText().trim();
                String dep = txtDep.getText().trim();
                String arr = txtArr.getText().trim();
                String time = txtTime.getText().trim();
                double price = Double.parseDouble(txtPrice.getText().trim());
                int availableSeats = Integer.parseInt(txtSeats.getText().trim());

                if (airline.isEmpty() || dep.isEmpty() || arr.isEmpty() || time.isEmpty()) {
                    lblErr.setText("Please fill all required fields.");
                    return;
                }

                String updateSql = "UPDATE flights SET airline = ?, departure_city = ?, arrival_city = ?, departure_datetime = ?, price = ?, available_seats = ? WHERE flight_id = ?";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, airline);
                    pstmt.setString(2, dep);
                    pstmt.setString(3, arr);
                    pstmt.setString(4, time);
                    pstmt.setDouble(5, price);
                    pstmt.setInt(6, availableSeats);
                    pstmt.setInt(7, selected.getFlightId());
                    pstmt.executeUpdate();

                    editStage.close();
                    showAlert(Alert.AlertType.INFORMATION, "Flight Updated", "Flight #" + selected.getFlightId() + " updated successfully.");
                    loadFlightsFromDatabase("", "");
                }
            } catch (NumberFormatException nfe) {
                lblErr.setText("Price and Seats must be valid numbers.");
            } catch (SQLException ex) {
                lblErr.setText("Database error: " + ex.getMessage());
            }
        });

        HBox btns = new HBox(10, btnUpdate, btnCancel);
        btns.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        root.getChildren().addAll(
            title,
            new Label("Airline Name:"), txtAirline,
            new Label("Departure City:"), txtDep,
            new Label("Arrival City:"), txtArr,
            new Label("Departure Date & Time:"), txtTime,
            new Label("Ticket Price ($):"), txtPrice,
            new Label("Available Seats:"), txtSeats,
            lblErr,
            btns
        );

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        editStage.setScene(scene);
        editStage.showAndWait();
    }

    @FXML
    void handleDeleteFlight(ActionEvent event) {
        Flight selected = flightTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Flight Selected", "Please select a flight to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Flight Deletion");
        confirm.setHeaderText("Delete Flight #" + selected.getFlightId() + " (" + selected.getAirline() + ")?");
        confirm.setContentText("Warning: Deleting this flight will also remove its associated reservations. This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    conn.setAutoCommit(false);
                    // Remove check_ins for this flight's reservations
                    try (PreparedStatement ps1 = conn.prepareStatement("DELETE FROM check_in WHERE reservation_id IN (SELECT reservation_id FROM reservations WHERE flight_id = ?)")) {
                        ps1.setInt(1, selected.getFlightId());
                        ps1.executeUpdate();
                    }
                    // Remove reservations
                    try (PreparedStatement ps2 = conn.prepareStatement("DELETE FROM reservations WHERE flight_id = ?")) {
                        ps2.setInt(1, selected.getFlightId());
                        ps2.executeUpdate();
                    }
                    // Delete flight
                    try (PreparedStatement ps3 = conn.prepareStatement("DELETE FROM flights WHERE flight_id = ?")) {
                        ps3.setInt(1, selected.getFlightId());
                        ps3.executeUpdate();
                    }
                    conn.commit();
                    conn.setAutoCommit(true);

                    showAlert(Alert.AlertType.INFORMATION, "Flight Deleted", "Flight #" + selected.getFlightId() + " has been successfully removed.");
                    loadFlightsFromDatabase("", "");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Deletion Failed", "Database error: " + ex.getMessage());
                }
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}