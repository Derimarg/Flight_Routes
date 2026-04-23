package model;

import java.util.ArrayList;
import java.util.List;
import utils.MenuUI;

public class Itinerary {
  private ArrayList<Flight> flights;
  private Double totalCost;
  private int totalDuration;
  private int numberOfStops;

  public Itinerary() {
    flights = new ArrayList<>();
    totalCost = 0.0;
    totalDuration = 0;
    numberOfStops = 0;
  }

  public Itinerary(List<Flight> flights) {
    this();
    this.flights.addAll(flights);
    recalculate();
  }

  // Getters
  public ArrayList<Flight> getFlights() {
    return this.flights;
  }

  public Double getTotalCost() {
    return totalCost;
  }

  public int getTotalDuration() {
    return totalDuration;
  }

  public int getNumberOfStops() {
    return numberOfStops;
  }

  // Setters

  private void recalculate() {
    totalCost = 0.0;
    totalDuration = 0;
    for (Flight flight : flights) {
      totalCost += flight.getPrice();
      totalDuration += flight.getDurationMinutes();
    }
    numberOfStops = flights.size() - 1;
  }

  public void addFlight(Flight flight) {
    flights.add(flight);
    recalculate();
  }

  public void addAllFlights(List<Flight> flights) {
    for (Flight flight : flights) {
      addFlight(flight);
    }
  }

  public boolean removeFlight(Flight flight) {
    boolean removed = flights.remove(flight);
    recalculate();
    return removed;
  }

  public String toJSON() {
    StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"totalCost\": ").append(totalCost).append(",\n");
    json.append("  \"totalDuration\": ").append(totalDuration).append(",\n");
    json.append("  \"flights\": [\n");

    for (int i = 0; i < flights.size(); i++) {
      Flight f = flights.get(i);
      json.append("    {\n");
      json.append("      \"src\": \"").append(f.getSource().getCode()).append("\",\n");
      json.append("      \"dest\": \"").append(f.getDestination().getCode()).append("\",\n");
      json.append("      \"srcLat\": ").append(f.getSource().getLat()).append(",\n");
      json.append("      \"srcLong\": ").append(f.getSource().getLon()).append(",\n");
      json.append("      \"destLat\": ").append(f.getDestination().getLat()).append(",\n");
      json.append("      \"destLong\": ").append(f.getDestination().getLon()).append("\n");
      json.append("    }").append(i < flights.size() - 1 ? "," : "").append("\n");
    }

    json.append("  ]\n");
    json.append("}");
    return json.toString();
  }

  @Override
  public String toString() {
    return MenuUI.printItinerary(this);
  }
}
