import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import io.javalin.Javalin;
import model.Airport;
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
          String rawInput = input.nextLine(); // Read the whole line as a String
          int choice;

          try {
            choice = Integer.parseInt(rawInput); // Try to convert it to a number
          } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number (1-5).");
            continue; // Skip the rest of the loop and ask for COMMAND again
          }

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

            case 3 -> { // Add a finHubs methos in Functions
              System.out.print("How many top hubs would you like to see? ");
              try {
                int userLimit = input.nextInt();

                if (userLimit <= 0) {
                  System.out.println("Please enter a positive number.");
                } else {
                  Functions.identifyHubs(routeMap, userLimit);
                }
              } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid integer.");
              }

            }

            case 4 -> {
              System.out.print("Enter airport code: ");
              String code = input.next().toUpperCase();

              // Calculate BFS once
              Set<Airport> reachableSet = Functions.getReachability(code, routeMap);
              Airport startAp = routeMap.getAirport(code);

              if (startAp == null) {
                System.out.println("\n[!] Error: Could not find airport " + code.toUpperCase());
              } else {
                // Display Summary report
                Functions.printReachabilitySummary(startAp, reachableSet, routeMap.getAllAirports().size());

                // Ask for the Full Report
                System.out.print("Would you like to see the detailed connectivity lists? (y/n): ");
                if (input.next().equalsIgnoreCase("y")) {
                  // Display Unreachable FIRST, then Reachable
                  Functions.printCategorizedLists(reachableSet, new ArrayList<>(routeMap.getAllAirports()));
                }
              }

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
