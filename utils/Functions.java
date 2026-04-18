package utils;

// libraries
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import model.Airport;
import model.Flight;
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
    int cols = 4; // spread out horizontally
    int rows = (int) Math.ceil((double) totalActive / cols);
    int cellWidth = window.getWidth() / cols;
    int cellHeight = window.getHeight() / Math.max(rows, 1);

    DrawGraph gui = new DrawGraph();
    window.add(gui);

    // track airport index
    Map<Airport, Integer> airportToIndex = new HashMap<>();
    int index = 0;

    for (Airport airport : activeAirports) {
      // Calulate grid position
      int col = index % cols;
      int row = index / cols;

      // Pick a random spot WITHIN that grid cell
      int x = (col * cellWidth) + (cellWidth / 4) + (int) (Math.random() * (cellWidth / 2));
      int y = (row * cellHeight) + (cellHeight / 4) + (int) (Math.random() * (cellHeight / 2));

      // Map airport object to the indexit holds in gui list
      gui.addNode(airport.getCode(), x, y);
      airportToIndex.put(airport, index);
      index++;
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
}
