import java.io.PrintWriter;
import java.util.Scanner;
import service.RouteMap;
import utils.Functions;
import utils.MenuUI;

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
            // System.out.println("\nResult saved to report.txt.");

            // MenuUI.pressEnterToContinue(input);
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
  }

}
