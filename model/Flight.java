package model;

public class Flight {
  private Airport source;
  private Airport destination;
  private double price;
  private int durationMinutes;
  private String flightNumber;

  // Default constructor
  public Flight() {
  }

  // Overloaded constructor
  public Flight(Airport source, Airport destination, double price, int durationMinutes, String flightNumber) {
    this.source = source;
    this.destination = destination;
    this.price = price;
    this.durationMinutes = durationMinutes;
    this.flightNumber = flightNumber;
  }

  // Getters
  public Airport getSource() {
    return source;
  }

  public Airport getDestination() {
    return destination;
  }

  public double getPrice() {
    return price;
  }

  public int getDurationMinutes() {
    return durationMinutes;
  }

  public String getFlightNumber() {
    return flightNumber;
  }

  @Override
  public String toString() {
    int hours = durationMinutes / 60;
    int minutes = durationMinutes % 60;

    return String.format("%-37s%-15s%-10s $%1.2f",
        destination.getCode() + " - " + destination.getCity() + ", " + destination.getCountry(), flightNumber,
        String.format("%d:%02d", hours, minutes), price);
  }
}
