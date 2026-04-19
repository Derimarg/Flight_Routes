package utils;

// libraries
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import model.Airport;
import model.Flight;
import model.Node;
import model.Itinerary;
import service.RouteFinder;
import service.RouteMap;

public class Functions {
  private Functions() {
    // private constructor to prevent instantiation
  }

  // PRE:
  // POST:
  public static void loadSystem(RouteMap routeMap, PrintWriter reportFile, PrintWriter errorFile) {
    List<Airport> airports = new ArrayList<>();
    List<Flight> flights = new ArrayList<>();

    // load airports/flights data
    DataLoader.parseAirportData(airports, reportFile, errorFile);
    DataLoader.parseFlightRoutesData(airports, flights, reportFile, errorFile);

    // Initialize map with all airports
    for (Airport airport : airports) {
      routeMap.addAirport(airport);
    }

    // Populate flights
    for (Flight flight : flights) {
      routeMap.addFlight(flight);
      Airport src = flight.getSource();
    }

    // loaded routes
    Functions.displayRoutes(routeMap, reportFile, errorFile);

  }

  // PRE: routes map, reportFile and errorFile ready to use
  // POST: routes are printed to the console and report file in a formatted manner
  public static void displayRoutes(RouteMap routeMap, PrintWriter reportFile, PrintWriter errorFile) {
    JFrame window = new JFrame("Flight Route Visualizer");
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.setSize(1200, 800);
    window.setLocationRelativeTo(null);

    // look for only airports that have flights
    List<Airport> activeAirports = new ArrayList<>();
    for (Airport a : routeMap.getAllAirports()) {
      if (a.getCode().equalsIgnoreCase("iata_code") || routeMap.getDirectConnections(a).isEmpty()) {
        continue;
      }
      activeAirports.add(a);
    }

    int totalActive = activeAirports.size();
    int width = 1200;
    int height = 800;
    int padding = 0;

    DrawGraph gui = new DrawGraph();
    window.add(gui);

    // track airport index
    Map<Airport, Integer> airportToIndex = new HashMap<>();
    int index = 0;

    for (Airport airport : activeAirports) {
      double lonMin = -128.0; // Pushes nodes WEST (left)
      double lonMax = -62.5; // Adjusts the Eastern edge
      double latMin = 14.0; // Pushes nodes SOUTH (down)
      double latMax = 51.5; // Adjusts the Northern edge

      int x = (int) ((airport.getLon() - lonMin) * (width) / (lonMax - lonMin));
      int y = (int) ((latMax - airport.getLat()) * (height) / (latMax - latMin));

      int[] manual = getManualLocation(airport.getCode(), x, y);
      int finalX = manual[0];
      int finalY = manual[1];

      boolean collision = true;
      int attempts = 0;
      while (collision && attempts < 10) { // Limit attempts to prevent infinit loops
        collision = false;
        for (Node existing : gui.getNodes()) {
          double dx = finalX - existing.x;
          double dy = finalY - existing.y;
          double dist = Math.sqrt(dx * dx + dy * dy);

          if (dist < 35) {
            collision = true;

            if (dist == 0) {
              finalX += 10; // Nudge East
              finalY -= 15; // Nudge North

            } else {
              finalX += (int) ((dx / dist) * 15);
              finalY += (int) ((dy / dist) * 15);
            }
            break;
          }
        }
        attempts++;
      }

      // Map airport object to the indexit holds in gui list
      gui.addNode(airport.getCode(), finalX, finalY);
      airportToIndex.put(airport, index++);
    }

    // Log and add edges
    for (Airport airport : activeAirports) {
      int srcIdx = airportToIndex.get(airport);
      List<Flight> flightList = routeMap.getDirectConnections(airport);

      // Print airport information
      String title = "Airport " + airport.getCode() + ": " + airport.getCity() + ", " + airport.getCountry();
      Functions.sectionHeader(title, reportFile);

      for (Flight flight : flightList) {
        reportFile.println(flight.toString());

        // Only draw edge if the destination airport was also added as a node
        if (airportToIndex.containsKey(flight.getDestination())) {
          int destIdx = airportToIndex.get(flight.getDestination());
          gui.addEdge(srcIdx, destIdx, "$" + flight.getPrice());
        }
      }
      reportFile.println("\n\n");
    }

    System.out.println("Loaded routes...\n");

    // Show window
    window.setVisible(true);
  }

  /**
   * PRE: title is a category name, reportWriter is initialized and open.
   * POST: Prints a formatted header with optional column names to the report file
   */
  public static void sectionHeader(String title, PrintWriter reportWriter) {
    sectionHeader(title, reportWriter, true, true);
  }

  public static void sectionHeader(String title, PrintWriter reportWriter, boolean hasCustomTitle, boolean hasHeader) {

    String displayTitle = !hasCustomTitle ? "YOUR " + title.toUpperCase() + " LIST" : title;

    String header = String.format("%-37s%8s%11s%12s", "DESTINATION", "FLIGHT #", "TIME", "PRICE");

    // report divider
    reportWriter.println(displayTitle);
    reportWriter.println("+".repeat(70));
    if (hasHeader) {
      reportWriter.println(header.toUpperCase());
    }
  }

  public static int[] getManualLocation(String iataCode, int calculatedX, int calculatedY) {
    // only hard-code the ones that the math keeps missing
    // (x,y) -> { 150, 145 }
    // move lef: decrease x
    // move right: increase y
    // move down: increase y
    // move up: decrease y
    return switch (iataCode) {

      // --- West Coast ---
      case "KSEA", "SEA" -> new int[] { 150, 145 };
      case "KPDX", "PDX" -> new int[] { 145, 195 };
      case "KSFO", "SFO" -> new int[] { 80, 240 };
      case "KLAX", "LAX" -> new int[] { 80, 325 };
      case "KSAN", "SAN" -> new int[] { 90, 450 };

      // --- Mountain / Southwest ---
      case "KSLC", "SLC" -> new int[] { 280, 280 };
      case "KLAS", "LAS" -> new int[] { 220, 380 };
      case "KPHX", "PHX" -> new int[] { 250, 450 };
      case "KDEN", "DEN" -> new int[] { 420, 310 };

      // --- Texas / Central ---
      case "KDFW", "DFW" -> new int[] { 540, 500 };
      case "KIAH", "IAH" -> new int[] { 560, 580 };
      case "KAUS", "AUS" -> new int[] { 500, 550 };
      case "KMSP", "MSP" -> new int[] { 610, 180 };
      case "KORD", "ORD" -> new int[] { 730, 255 };
      case "KDTW", "DTW" -> new int[] { 830, 260 };

      // --- East Coast / Southeast ---
      case "KBOS", "BOS" -> new int[] { 1080, 245 };
      case "KJFK", "JFK" -> new int[] { 1055, 290 };
      case "KEWR", "EWR" -> new int[] { 1030, 295 };
      case "KPHL", "PHL" -> new int[] { 1010, 315 };
      case "KDCA", "DCA" -> new int[] { 980, 340 };
      case "KBNA", "BNA" -> new int[] { 790, 410 };
      case "KATL", "ATL" -> new int[] { 850, 480 };
      case "KMCO", "MCO" -> new int[] { 940, 640 };
      case "KFLL", "FLL" -> new int[] { 970, 690 };
      case "KMIA", "MIA" -> new int[] { 980, 700 };
      default -> new int[] { calculatedX, calculatedY };
    };
  }

  public static void getShortestPath(String sourceCode, String destinationCode, RouteMap routeMap, PrintWriter report) {
    Set<Airport> airports = routeMap.getAllAirports();
    Airport source = null;
    Airport destination = null;
    for (Airport airport : airports) {
      if (airport.getCode().equals(sourceCode)) source = airport;
      if (airport.getCode().equals(destinationCode)) destination = airport;
    }
    if (source == null || destination == null) report.println("No such airport: " + sourceCode + " -> " + destinationCode);
    Itinerary route = RouteFinder.getFastestRoute(source, destination, routeMap);
    report.print(route);
  }

  public static void getCheapestPath(String sourceCode, String destinationCode, RouteMap routeMap, PrintWriter report) {
    Set<Airport> airports = routeMap.getAllAirports();
    Airport source = null;
    Airport destination = null;
    for (Airport airport : airports) {
      if (airport.getCode().equals(sourceCode)) source = airport;
      if (airport.getCode().equals(destinationCode)) destination = airport;
    }
    if (source == null || destination == null) report.println("No such airport: " + sourceCode + " -> " + destinationCode);
    Itinerary route = RouteFinder.getCheapestRoute(source, destination, routeMap);
    report.print(route);
  }
}
