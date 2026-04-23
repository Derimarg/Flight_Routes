package utils;

import java.io.FileWriter;
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
import model.Itinerary;
import model.Node;
import service.RouteFinder;
import service.RouteMap;

public class Functions {
  private Functions() {
    // private constructor to prevent instantiation
  }

  private static String currentTheme = "dark"; // Default

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
    toggleTheme();
    exportFullNetwork(routeMap);
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
      case "KSEA", "SEA" -> new int[] { 110, 70 };
      case "KPDX", "PDX" -> new int[] { 100, 115 };
      case "KSFO", "SFO" -> new int[] { 40, 325 };
      case "KLAX", "LAX" -> new int[] { 90, 450 };
      case "KSAN", "SAN" -> new int[] { 125, 475 };

      // --- Mountain / Southwest ---
      case "KSLC", "SLC" -> new int[] { 280, 280 };
      case "KLAS", "LAS" -> new int[] { 180, 400 };
      case "KPHX", "PHX" -> new int[] { 250, 480 };
      case "KDEN", "DEN" -> new int[] { 420, 330 };

      // --- Texas / Central ---
      case "KDFW", "DFW" -> new int[] { 570, 540 };
      case "KIAH", "IAH" -> new int[] { 610, 620 };
      case "KAUS", "AUS" -> new int[] { 550, 610 };
      case "KMSP", "MSP" -> new int[] { 650, 200 };
      case "KORD", "ORD" -> new int[] { 760, 275 };
      case "KDTW", "DTW" -> new int[] { 830, 260 };

      // --- East Coast / Southeast ---
      case "KBOS", "BOS" -> new int[] { 1090, 205 };
      case "KJFK", "JFK" -> new int[] { 1050, 260 };
      case "KEWR", "EWR" -> new int[] { 1045, 265 };
      case "KPHL", "PHL" -> new int[] { 1010, 315 }; // Couldn't find on map
      case "KDCA", "DCA" -> new int[] { 980, 320 };
      case "KBNA", "BNA" -> new int[] { 790, 430 };
      case "KATL", "ATL" -> new int[] { 860, 500 };
      case "KMCO", "MCO" -> new int[] { 950, 640 };
      case "KFLL", "FLL" -> new int[] { 970, 690 }; // Couldn't find on map
      case "KMIA", "MIA" -> new int[] { 980, 700 };
      default -> new int[] { calculatedX, calculatedY };
    };
  }

  public static void getShortestPath(String sourceCode, String destinationCode, RouteMap routeMap, PrintWriter report) {
    Set<Airport> airports = routeMap.getAllAirports();
    Airport source = null;
    Airport destination = null;

    for (Airport airport : airports) {
      if (airport.getCode().equals(sourceCode))
        source = airport;
      if (airport.getCode().equals(destinationCode))
        destination = airport;
    }

    if (source == null || destination == null) {

      String error = "No such airport: " + sourceCode + " -> " + destinationCode;
      report.println(error);
      System.out.println(error);
      return;
    }

    Itinerary itinerary = RouteFinder.getFastestRoute(source, destination, routeMap);

    // Print to console
    MenuUI.printSearchResult(itinerary, source, destination, "Fastest");
  }

  public static void getCheapestPath(String sourceCode, String destinationCode, RouteMap routeMap, PrintWriter report) {
    Set<Airport> airports = routeMap.getAllAirports();
    Airport source = null;
    Airport destination = null;

    for (Airport airport : airports) {
      if (airport.getCode().equalsIgnoreCase(sourceCode))
        source = airport;
      if (airport.getCode().equalsIgnoreCase(destinationCode))
        destination = airport;
    }

    if (source == null || destination == null) {
      String error = "No such airport: " + sourceCode + " -> " + destinationCode;
      report.println(error);
      System.out.println(error);
      return;
    }

    Itinerary itinerary = RouteFinder.getCheapestRoute(source, destination, routeMap);

    exportForWeb(itinerary);

    // Print to console
    MenuUI.printSearchResult(itinerary, source, destination, "Cheapest");
  }

  public static List<Airport> identifyHubs(RouteMap routeMap, PrintWriter reportFile) {
    if (routeMap.getAllAirports().size() == 0) return new ArrayList<>();

    ArrayList<Airport> hubs = new ArrayList<>();
    Integer highest = 0;

    for (Airport ap : routeMap.getAllAirports()) {
      Integer numDirectConnections = routeMap.getDirectConnections(ap).size();
      if (hubs.size() == 0 || numDirectConnections > highest) {
        hubs.clear();
        hubs.add(ap);
        highest = numDirectConnections;
      } else if (numDirectConnections == highest) {
        hubs.add(ap);
      }
    }

    reportFile.println("Identified the following hub airport(s):");
    for (Airport ap : hubs) {
      reportFile.println(ap.getCode() + " " + ap.getCity() + ", " + ap.getCountry());
    }
    return hubs;
  }

  public static void checkReachability(String startNode, RouteMap routeMap, PrintWriter reportFile) {
    reportFile.println("Checking reachability...");
    ArrayList<Airport> destinations = new ArrayList<>();
    ArrayList<Airport> sources = new ArrayList<>();
    Airport startAirport = routeMap.getAirport(startNode);

    if (startAirport == null) {
      reportFile.println("Could not find airport: " + startNode);
      return;
    }

    for (Flight fl : routeMap.getDirectConnections(startAirport)) {
      destinations.add(fl.getDestination());
    }

    for (Airport ap : routeMap.getAllAirports()) {
      if (ap.equals(startAirport)) continue;
      for (Flight fl : routeMap.getDirectConnections(ap)) {
        if (fl.getDestination().equals(routeMap.getAirport(startNode))) {
          sources.add(fl.getSource());
        }
      }
    }

    reportFile.println(startNode + " can reach the following:");
    for (Airport ap : destinations) {
      reportFile.println(ap.getCode() + " " + ap.getCity() + ", " + ap.getCountry());
    }
    reportFile.println("\n" + startNode + " can be reached by the following:");
    for (Airport ap : sources) {
      reportFile.println(ap.getCode() + " " + ap.getCity() + ", " + ap.getCountry());
    }
  }

  // Vertices and Edges representation for web map
  public static void exportFullNetwork(RouteMap routeMap) {
    try (PrintWriter out = new PrintWriter("web/network_data.js")) {
      StringBuilder json = new StringBuilder();
      json.append("const networkData = {\n");

      // Export all Airport Nodes
      json.append("  \"airports\": [\n");
      List<model.Airport> airports = new ArrayList<>(routeMap.getAllAirports());
      for (int i = 0; i < airports.size(); i++) {
        model.Airport a = airports.get(i);
        json.append(String.format("    {\"code\": \"%s\", \"lat\": %f, \"lng\": %f, \"name\": \"%s\"}%s\n",
            a.getCode(), a.getLat(), a.getLon(), a.getCity(), (i < airports.size() - 1 ? "," : "")));
      }
      json.append("  ],\n");

      // Export all Flight Edges (The Adjacency List)
      json.append("  \"routes\": [\n");
      int routeCount = 0;
      for (model.Airport src : routeMap.getAllAirports()) {
        for (model.Flight f : routeMap.getDirectConnections(src)) {
          if (routeCount > 0)
            json.append(",\n");
          json.append(String.format(
              "    {\"src\": \"%s\", \"dest\": \"%s\", \"srcLat\": %f, \"srcLong\": %f, \"destLat\": %f, \"destLong\": %f}",
              f.getSource().getCode(), f.getDestination().getCode(),
              f.getSource().getLat(), f.getSource().getLon(),
              f.getDestination().getLat(), f.getDestination().getLon()));
          routeCount++;
        }
      }
      json.append("\n  ]\n};");
      out.print(json.toString());
      System.out.println("[SYSTEM] Full network graph exported for visualization.");
    } catch (Exception e) {
      System.err.println("Export Error: " + e.getMessage());
    }
  }

  // Export the routes data found by the Algorithm into a JSON file
  public static void exportForWeb(Itinerary itinerary) {
    try (PrintWriter out = new PrintWriter("web/route_data.js")) {
      // save it as a global JS variable
      out.print("const routeData = " + itinerary.toJSON() + ";");
      System.out.println("[SYSTEM] Web data exported to web/route_data.js");
    } catch (Exception e) {
      System.out.println("Export error: " + e.getMessage());
    }
  }

  // Set Theme color for the web visualizer
  public static void toggleTheme() {
    currentTheme = currentTheme.equals("dark") ? "light" : "dark";
    exportSettings();
  }

  // EXport visual settings to js
  public static void exportSettings() {
    try (PrintWriter out = new PrintWriter("web/settings.js")) {
      out.print("const systemSettings = { \"theme\": \"" + currentTheme + "\" };");
    } catch (Exception e) {
      System.out.println("Settings export error: " + e.getMessage());
    }
  }
}
