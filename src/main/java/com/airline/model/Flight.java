package com.airline.model;

import javafx.beans.property.*;

public class Flight {
    private final IntegerProperty flightId;
    private final StringProperty airline;
    private final StringProperty departureCity;
    private final StringProperty arrivalCity;
    private final StringProperty departureDatetime;
    private final DoubleProperty price;
    private final IntegerProperty availableSeats;

    public Flight(int flightId, String airline, String departureCity, String arrivalCity, String departureDatetime, double price, int availableSeats) {
        this.flightId = new SimpleIntegerProperty(flightId);
        this.airline = new SimpleStringProperty(airline);
        this.departureCity = new SimpleStringProperty(departureCity);
        this.arrivalCity = new SimpleStringProperty(arrivalCity);
        this.departureDatetime = new SimpleStringProperty(departureDatetime);
        this.price = new SimpleDoubleProperty(price);
        this.availableSeats = new SimpleIntegerProperty(availableSeats);
    }

    // JavaFX TableView uses these exact getter methods to populate columns automatically!
    public int getFlightId() { return flightId.get(); }
    public String getAirline() { return airline.get(); }
    public String getDepartureCity() { return departureCity.get(); }
    public String getArrivalCity() { return arrivalCity.get(); }
    public String getDepartureDatetime() { return departureDatetime.get(); }
    public double getPrice() { return price.get(); }
    public int getAvailableSeats() { return availableSeats.get(); }
}
