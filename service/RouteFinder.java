package service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import model.Airport;
import model.Flight;
import model.Itinerary;

public class RouteFinder {
  public static Itinerary getFastestRoute(Airport source, Airport destination, RouteMap routes) {
    Set<Airport> visited = new HashSet<>();
    Map<Airport, Integer> distances = new HashMap<>();
    Map<Airport, Flight> previousFlight = new HashMap<>();

    // Initialize distance map
    for (Airport airport : routes.getAllAirports()) {
      // Distance from source to any airport currently infinity (-1)
      distances.put(airport, Integer.MAX_VALUE);
    }
    // Distance from source to source is 0
    distances.put(source, 0);

    // Create priorityqueue of airports in order of their distance from the origin.
    PriorityQueue<Airport> airports = new PriorityQueue<>(Comparator.comparingInt(distances::get));
    airports.add(source);

    // Go through every airport
    while (!airports.isEmpty()) {
      // Get the first/next airport and mark visited
      Airport v = airports.poll();
      if (visited.contains(v)) continue;
      visited.add(v);
      // Get the current airport's distance from the origin
      Integer currentDistance = distances.get(v);
      if (currentDistance == Integer.MAX_VALUE) continue;

      // For every connecting flight
      for (Flight connection: routes.getDirectConnections(v)) {
        // If we already visited the destination, skip this one.
        if (visited.contains(connection.getDestination())) continue;
        // Get the flight destination's current distance from the origin.
        Integer connectedDistance = distances.get(connection.getDestination());
        // If the destination's distance is "infinity" or greater than the duration, update it with the sum of the current distance and the duration.
        if (connectedDistance > (currentDistance + connection.getDurationMinutes())) {
          distances.put(connection.getDestination(), (currentDistance + connection.getDurationMinutes()));
          airports.add(connection.getDestination());
          previousFlight.put(connection.getDestination(), connection);
        }
      }

    }
    // At this point we have a map containing the shortest distance from the origin to each airport.
    Itinerary itinerary = new Itinerary();
    LinkedList<Flight> path = new LinkedList<>();
    Airport step = destination;

    // Go through the steps and add them to a linked list
    while (!step.equals(source)) {
      Flight flight = previousFlight.get(step);
      if (flight == null) {
        return null;
      }
      path.addFirst(flight);
      step = flight.getSource();
    }

    // Add each flight in linked list to itinerary.
    itinerary.addAllFlights(path);
    return itinerary;
  }

  public static Itinerary getCheapestRoute(Airport source, Airport destination, RouteMap routes) {
    Set<Airport> visited = new HashSet<>();
    Map<Airport, Double> costs = new HashMap<>();
    Map<Airport, Flight> previousFlight = new HashMap<>();

    // Initialize distance map
    for (Airport airport : routes.getAllAirports()) {
      // Distance from source to any airport currently infinity (-1)
      costs.put(airport, Double.MAX_VALUE);
    }
    // Distance from source to source is 0
    costs.put(source, 0.0);

    // Create priorityqueue of airports in order of their distance from the origin.
    PriorityQueue<Airport> airports = new PriorityQueue<>(Comparator.comparingDouble(costs::get));
    airports.add(source);

    // Go through every airport
    while (!airports.isEmpty()) {
      // Get the first/next airport and mark visited
      Airport v = airports.poll();
      if (visited.contains(v)) continue;
      visited.add(v);
      // Get the current airport's distance from the origin
      Double currentCost = costs.get(v);
      if (currentCost == Integer.MAX_VALUE) continue; // If this airport hasn't been reached yet, continue. (This shouldn't happen.)

      // For every connecting flight
      for (Flight connection: routes.getDirectConnections(v)) {
        // If we already visited the destination, skip this one.
        if (visited.contains(connection.getDestination())) continue;
        // Get the flight destination's current distance from the origin.
        Double connectedDistance = costs.get(connection.getDestination());
        // If the destination's distance is "infinity" or greater than the duration, update it with the sum of the current distance and the duration.
        if (connectedDistance > (currentCost + connection.getDurationMinutes())) {
          costs.put(connection.getDestination(), (currentCost + connection.getDurationMinutes()));
          airports.add(connection.getDestination());
          previousFlight.put(connection.getDestination(), connection);
        }
      }

    }
    // At this point we have a map containing the shortest distance from the origin to each airport.
    LinkedList<Flight> path = new LinkedList<>();
    Airport step = destination;

    // Go through the steps and add them to a linked list
    while (!step.equals(source)) {
      Flight flight = previousFlight.get(step);
      if (flight == null) {
        return null;
      }
      path.addFirst(flight);
      step = flight.getSource();
    }

    // Add each flight in linked list to itinerary.
    Itinerary itinerary = new Itinerary(path);
    return itinerary;
  }
}
