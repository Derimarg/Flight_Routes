package utils;

// libraries
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import model.Airport;
import model.Flight;

public class Functions {
  private Functions() {
    // private constructor to prevent instantiation
  }

  public static void loadSystem(Map<Airport, List<Flight>> routes, PrintWriter reportFile, PrintWriter errorFile) {
    List<Airport> airports = new ArrayList<>();
    List<Flight> flights = new ArrayList<>();

    // load airports data
    DataLoader.parseAirportData(airports, reportFile, errorFile);

    // load flights data
    DataLoader.parseFlightRoutesData(airports, flights, reportFile, errorFile);

    // Initialize the routes map with each airport and its flight routes
    for (Airport airport : airports) {
      for (Flight flight : flights) {
        if (flight.getSource().equals(airport)) {
          routes.computeIfAbsent(airport, k -> new ArrayList<>()).add(flight);
        }
      }
    }

  }

  // PRE: routes map, reportFile and errorFile ready to use
  // POST: routes are printed to the console and report file in a formatted manner
  public static void displayRoutes(Map<Airport, List<Flight>> routes, PrintWriter reportFile, PrintWriter errorFile) {
    routes.forEach((key, value) -> {
      String code = key.getCode();
      String city = key.getCity();
      String country = key.getCountry();

      // Print airport information
      String title = "Airport " + code + ": " + city + ", " + country;

      Functions.sectionHeader(title, reportFile);

      for (Flight flight : value) {
        System.out.println(flight.toString());
        reportFile.println(flight.toString());
      }
      reportFile.println("\n\n");
      System.out.println("\n\n");
    });
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

    // console divider
    System.out.println(displayTitle);
    System.out.println("+".repeat(70));
    if (hasHeader) {
      reportWriter.println(header.toUpperCase());
      System.out.println(header.toUpperCase());
    }
  }
}
