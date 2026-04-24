package service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import io.javalin.http.Context;
import model.Airport;
import model.Flight;
import model.Itinerary;

public class RouteFinder {
  // Public API
  public static Itinerary getFastestRoute(Airport source, Airport destination, RouteMap routes) {
    return findPath(source, destination, routes, false);
  }

  public static Itinerary getCheapestRoute(Airport source, Airport destination, RouteMap routes) {
    return findPath(source, destination, routes, true);
  }

  // Private Engine
  /**
   * Dijkstra Algorithm
   * 
   * @param usePrice: if true, weights edges by price,; if false, by duration
   *                  minutes.
   */
  private static Itinerary findPath(Airport source, Airport destination, RouteMap routes, boolean usePrice) {
    Set<Airport> visited = new HashSet<>();
    Map<Airport, Double> scores = new HashMap<>();
    Map<Airport, Flight> previousFlight = new HashMap<>();

    // Initialize distance map
    for (Airport airport : routes.getAllAirports()) {
      // Distance from source to any airport currently infinity (-1)
      scores.put(airport, Double.MAX_VALUE);
    }
    // Distance from source to source is 0
    scores.put(source, 0.0);

    // Create priorityqueue of airports in order of their distance from the origin.
    PriorityQueue<Airport> queue = new PriorityQueue<>(Comparator.comparingDouble(scores::get));
    queue.add(source);

    // Go through every airport
    while (!queue.isEmpty()) {
      // Get the first/next airport and mark visited
      Airport current = queue.poll();
      if (visited.contains(current))
        continue;

      visited.add(current);

      // Stop if destination is reached
      if (current.equals(destination))
        break;

      // For every connecting flight
      for (Flight flight : routes.getDirectConnections(current)) {
        Airport neighbor = flight.getDestination();
        if (visited.contains(neighbor))
          continue;

        // Determine weight based on mode
        double weight = usePrice ? flight.getPrice() : flight.getDurationMinutes();
        double newScore = scores.get(current) + weight;

        if (newScore < scores.get(neighbor)) {
          queue.remove(neighbor);
          scores.put(neighbor, newScore);
          previousFlight.put(neighbor, flight);
          queue.add(neighbor);
        }
      }

    }

    return constructItinerary(source, destination, previousFlight);
  }

  /*
   * Backtracks through the previuosFlight map to build the final route.
   */
  private static Itinerary constructItinerary(Airport source, Airport destination,
      Map<Airport, Flight> previousFlight) {
    LinkedList<Flight> path = new LinkedList<>();
    Airport step = destination;

    while (step != null && !step.equals(source)) {
      Flight flight = previousFlight.get(step);

      if (flight == null)
        return null;
      path.addFirst(flight);
      step = flight.getSource();
    }

    return path.isEmpty() ? null : new Itinerary(path);
  }

  /**
   * Web Handler: This is what the Browser calls via Fetch
   */
  public static void handleRouteRequest(Context ctx, RouteMap routeMap) {
    // Get data from the Browser URL (e.g., ?source=ORD&dest=LAX&mode=price)
    String srcCode = ctx.queryParam("source");
    String destCode = ctx.queryParam("dest");
    String mode = ctx.queryParam("mode");

    // Map codes to Airport Objects
    Airport source = findAirportByCode(routeMap, srcCode);
    Airport destination = findAirportByCode(routeMap, destCode);

    if (source == null || destination == null) {
      ctx.status(404).result("{\"error\": \"Airport not found\"}");
      return;
    }

    // Run Dijkstra Algorithm
    Itinerary result;
    if ("price".equalsIgnoreCase(mode)) {
      result = getCheapestRoute(source, destination, routeMap);
    } else {
      result = getFastestRoute(source, destination, routeMap);
    }

    // Send the JSON back to the Browser
    if (result != null) {
      ctx.contentType("application/json");
      ctx.result(result.toJSON());
    } else {
      ctx.status(404).result("{\"error\": \"No route found\"}");
    }
  }

  // Helper to find airport object by its string code
  private static Airport findAirportByCode(RouteMap map, String code) {
    return map.getAllAirports().stream()
        .filter(a -> a.getCode().equalsIgnoreCase(code))
        .findFirst()
        .orElse(null);
  }
}
