package com.airline.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

/**
 * Controller for the Flight Analytics view.
 * Populates the Pie Chart and Bar Chart with sample booking/destination data.
 */
public class AnalyticsController {

    @FXML private PieChart destinationPieChart;
    @FXML private BarChart<String, Number> bookingBarChart;

    @FXML
    public void initialize() {
        setupPieChart();
        setupBarChart();
    }

    private void setupPieChart() {
        destinationPieChart.getData().clear();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Dubai (45%)", 45),
                new PieChart.Data("London (30%)", 30),
                new PieChart.Data("Singapore (15%)", 15),
                new PieChart.Data("Other (10%)", 10)
        );
        destinationPieChart.setData(pieData);
        destinationPieChart.setTitle("Route Share");
    }

    private void setupBarChart() {
        bookingBarChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Bookings");
        series.getData().add(new XYChart.Data<>("Mon", 120));
        series.getData().add(new XYChart.Data<>("Tue", 180));
        series.getData().add(new XYChart.Data<>("Wed", 90));
        series.getData().add(new XYChart.Data<>("Thu", 210));
        series.getData().add(new XYChart.Data<>("Fri", 160));
        series.getData().add(new XYChart.Data<>("Sat", 135));
        series.getData().add(new XYChart.Data<>("Sun", 75));

        bookingBarChart.getData().add(series);
    }
}
