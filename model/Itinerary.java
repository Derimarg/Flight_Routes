package model;

import java.util.ArrayList;
import java.util.List;

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

  public Double getTotalCost() {
    return totalCost;
  }

  public int getTotalDuration() {
    return totalDuration;
  }

  public int getNumberOfStops() {
    return numberOfStops;
  }

  @Override
  public String toString() {
    if (flights.size() == 0)
      return "Path length 0.";
    StringBuilder sb = new StringBuilder();
    sb.append("Route " + flights.get(0).getSource().getCode() + " -> "
        + flights.get(flights.size() - 1).getDestination().getCode());
    sb.append("\nTotal cost: $" + totalCost);
    sb.append("\nTotal duration: " + totalDuration / 60 + " hours");
    sb.append("\nFlights:");
    for (Flight flight : flights) {
      sb.append("\n" + flight.getSource().getCode() + " -> " + flight.getDestination().getCode());
    }

    return sb.toString();
  }
}
