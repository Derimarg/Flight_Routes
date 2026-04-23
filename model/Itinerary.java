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

  @Override
  public String toString() {
    return MenuUI.printItinerary(this);
  }
}
