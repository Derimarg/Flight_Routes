package service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.Airport;
import model.Flight;

public class RouteMap {
  private final Map<Airport, List<Flight>> adjacencyList;

  // Initilize map internally
  public RouteMap() {
    this.adjacencyList = new HashMap<>();
  }

  public void addAirport(Airport airport) {
    if (airport != null) {
      adjacencyList.putIfAbsent(airport, new ArrayList<>());
    }
  }

  public void addFlight(Flight flight) {
    // Validate null data
    if (flight == null || flight.getSource() == null)
      return;

    // Get the airport source
    Airport src = flight.getSource();
    adjacencyList.putIfAbsent(src, new ArrayList<>());

    // add flights to the airport
    adjacencyList.get(src).add(flight);
  }

  // List of flights leaving from a specific airport
  public List<Flight> getDirectConnections(Airport airport) {
    return adjacencyList.getOrDefault(airport, new ArrayList<>());
  }

  public Set<Airport> getAllAirports() {
    return adjacencyList.keySet();
  }

  // Check is ariport is in the list
  public boolean hasAirport(Airport airport) {
    return adjacencyList.containsKey(airport);
  }

  public int getTotalAirportsCount() {
    return adjacencyList.size();
  }

}
