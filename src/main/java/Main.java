import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import io.javalin.Javalin;
import model.Itinerary;
import service.RouteFinder;
import service.RouteMap;
import utils.Functions;
import utils.MenuUI;

public class Main {

  // list to hold the history
  private static List<Map<String, Object>> searchHistory = new ArrayList<>();

  private static void saveSearchToFile(Map<String, Object> data, String filePath) {
    try (PrintWriter out = new PrintWriter(filePath)) {
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

      // adds indentation and line breaks
      mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

      String json = mapper.writeValueAsString(data);

      // Write the variable declaration followed by the pretty-printed JSON
      out.print("const lastSearch = " + json + ";");

    } catch (Exception e) {
      System.err.println("Failed to save search: " + e.getMessage());
    }
  }

  public static void main(String[] args) {
    // Hold flights network
    RouteMap routeMap = new RouteMap();
    Scanner input = new Scanner(System.in);

    String errorFileName = "error.txt";
    String reportFileName = "report.txt";

    // Start the server on port 8080
    Javalin app = Javalin.create(config -> {
      config.bundledPlugins.enableCors(cors -> {
        cors.addRule(it -> {
          it.anyHost(); // This allows HTML file to "talk" to Java
        });
      });
    }).start(8080);

    /*
     * APIS ROUTES
     */
    // Set up the endpoint
    app.get("/getHistory", ctx -> {
      Map<String, Object> response = new HashMap<>();
      response.put("history", searchHistory);
      ctx.json(response); // Just returns the list, no file writing = no refresh
    });

    // Ensure /findPath route matches this exactly
    app.get("/findPath", ctx -> {
      Itinerary result = RouteFinder.handleRouteRequest(ctx, routeMap);

      if (result != null) {
        Map<String, Object> resultMap = result.toMap();
        searchHistory.add(0, resultMap);

        while (searchHistory.size() > 5) {
          searchHistory.remove(searchHistory.size() - 1);
        }

        saveSearchToFile(resultMap, "web/last_search.js");

        Map<String, Object> response = new HashMap<>();
        response.put("currentRoute", resultMap);
        response.put("history", searchHistory);
        ctx.json(response);
      } else {
        ctx.status(404).json(Map.of("error", "No route found"));
      }
    });

    // Background thread to prevent the console input to interupt the web refresh
    new Thread(() -> {
      // Close report/error files automatically when all process finish successfully
      try (
          PrintWriter errorFile = new PrintWriter(errorFileName);
          PrintWriter reportFile = new PrintWriter(reportFileName);) {
        System.out.println();
        MenuUI.printLogo();

        // Load the system
        System.out.println("[SYSTEM] Initializing Graph Database... ");
        Functions.loadSystem(routeMap, reportFile, errorFile);
        Thread.sleep(400); // Gives a second to see the status
        System.out.println("READY");

        boolean running = true;
        // MenuUI.clearScreen();
        MenuUI.printHeader();
        MenuUI.displayMenu();

        while (running) {
          System.out.println("=".repeat(50));
          System.out.print("COMMAND > ");
          int choice = input.nextInt();
          input.nextLine(); // Clear buffer

          switch (choice) {
            case 1 -> {
              System.out.println("\n--- INITIATING PRICE OPTIMIZATION ---");
              System.out.print("Enter Source Airport Code: ");
              String src = input.nextLine().toUpperCase();
              System.out.print("Enter Destination Airport Code: ");

              String dest = input.nextLine().toUpperCase();

              System.out.println("\nComputing optimal vertices...");
              Functions.getCheapestPath(src, dest, routeMap, reportFile);
            }

            case 2 -> {
              System.out.print("Enter Source Airport Code (e.g., MCO): ");
              String srcFast = input.nextLine().toUpperCase();

              System.out.print("Enter Destination Code (e.g., LAX): ");
              String destFast = input.nextLine().toUpperCase();

              System.out.println("\nComputing optimal vertices...");
              Functions.getShortestPath(srcFast, destFast, routeMap, reportFile);
            }

            case 3 -> {
              System.out.println("Analyzing hub data...");
              System.out.println("\nComputing optimal vertices...");
              // Add a finHibs methos in Functions
              // Functions.identifyHubs(routeMap, reportFile);

            }

            case 4 -> {
              System.out.println("Enter starting airport to check connectivity: ");
              String startNode = input.nextLine().toUpperCase();
              System.out.println("\nComputing optimal vertices...");
              // Functions.checkReachability(startNode, routeMap, reportFile);
            }

            case 5 -> {
              running = false;
              System.out.println("Exiting system...");
            }

            default -> System.out.println("Invalid choice. Please try again.");
          }
        }

        // show error if file management process fails
      } catch (Exception e) {
        System.out.println("System Error: " + e.getMessage());

      } finally {
        input.close();
      }
    }).start();

  }

}
