import java.io.PrintWriter;
import java.util.Scanner;
import service.RouteMap;
import utils.Functions;

public class Main {
  public static void main(String[] args) {
    // Hold flights network
    RouteMap routeMap = new RouteMap();
    Scanner input = new Scanner(System.in);

    String errorFileName = "error.txt";
    String reportFileName = "report.txt";

    // Close report/error files automatically when all process finish successfully
    try (
        PrintWriter errorFile = new PrintWriter(errorFileName);
        PrintWriter reportFile = new PrintWriter(reportFileName);) {

      // Load the system
      Functions.loadSystem(routeMap, reportFile, errorFile);
      // Functions.getShortestPath("ORD", "LAX", routeMap, reportFile);

      boolean running = true;
      System.out.println("=== Flight Route Planning System ===");

      while (running) {
        System.out.println("\nSelect an option:");
        System.out.println("1. Find Cheapest Route (Price)");
        System.out.println("2. Find Fastest Route (Time)");
        System.out.println("3. Identify Hub Airports (Most Connections)");
        System.out.println("4. Check Reachability (Unreachable Airports)");
        System.out.println("5. Exit");
        System.out.print("Choice: ");

        int choice = input.nextInt();
        input.nextLine(); // Consume line

        switch (choice) {
          case 1 -> {
            System.out.print("Enter Source Airport Code: ");
            String srcCheap = input.nextLine().toUpperCase();
            System.out.print("Enter Destination Code: ");
            String destCheap = input.nextLine().toUpperCase();
            Functions.getCheapestPath(srcCheap, destCheap, routeMap, reportFile);
            System.out.println("\nResult saved to report.txt.");
          }

          case 2 -> {
            System.out.print("Enter Source Airport Code (e.g., MCO): ");
            String srcFast = input.nextLine().toUpperCase();

            System.out.print("Enter Destination Code (e.g., LAX): ");
            String destFast = input.nextLine().toUpperCase();

            Functions.getShortestPath(srcFast, destFast, routeMap, reportFile);
            System.out.println("Result saved to report.txt and displayed on map.");
          }

          case 3 -> {
            System.out.println("Analyzing hub data...");
            // Add a finHibs methos in Functions
            // Functions.identifyHubs(routeMap, reportFile);
          }

          case 4 -> {
            System.out.println("Enter starting airport to check connectivity: ");
            String startNode = input.nextLine().toUpperCase();
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
  }

}
