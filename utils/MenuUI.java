package utils;

import java.util.ArrayList;
import java.util.Scanner;

import model.Airport;
import model.Flight;
import model.Itinerary;

public class MenuUI {
  public static final String UMKC_BLUE = "\u001B[34;1m"; // 1m for Bold
  public static final String UMKC_GOLD = "\u001B[33;1m";
  public static final String RESET = "\u001B[0m";
  public static final String CYAN = "\u001B[36m";

  public static void printLogo() {
    // System.out.println(UMKC_GOLD + " _ _ __ __ _ _______ ");
    // System.out.println(UMKC_GOLD + " | | | | \\/ | |/ / ____|");
    // System.out.println(UMKC_GOLD + " | | | | \\ / | ' / | ");
    // System.out.println(UMKC_GOLD + " | | | | |\\/| | <| | ");
    // System.out.println(UMKC_GOLD + " | |__| | | | | . \\ |____ ");
    // System.out.println(UMKC_GOLD + " \\____/|_| |_|_|\\_\\______|" + RESET);
    System.out.println(UMKC_BLUE + " UNIVERSITY OF MISSOURI - KANSAS CITY");
    System.out.println("  School of Science and Engineering");
    System.out.println("  Discrete Structures II - UMKC");
    System.out.println("--------------------------------------------" + RESET);
  }

  public static void printHeader() {
    System.out.println("\n" + "=".repeat(50));
    System.out.println("    FLIGHT ROUTE PLANNING SYSTEM");
    System.out.println("=".repeat(50));
  }

  public static void displayMenu() {
    System.out.println("  [1] PRICE OPTIMIZATION (Cheapest Path)");
    System.out.println("  [2] TEMPORAL OPTIMIZATION (Fastest Path)");
    System.out.println("  [3] NETWORK HUB IDENTIFICATION");
    System.out.println("  [4] SYSTEM CONNECTIVITY AUDIT");
    System.out.println("  [5] EXIT SYSTEM");
    // System.out.println("=".repeat(50));
    // System.out.print("SECURE COMMAND > ");
  }

  public static void pressEnterToContinue(Scanner input) {
    System.out.println("\n>>> Press ENTER to return to the main menu...");
    input.nextLine();
  }

  public static String printItinerary(Itinerary itinerary) {
    ArrayList<Flight> flights = itinerary.getFlights();
    Double totalCost = itinerary.getTotalCost();
    int totalDuration = itinerary.getTotalDuration();

    if (flights == null || flights.isEmpty()) {
      return " [!] Error: No valid flight path found.";
    }

    StringBuilder sb = new StringBuilder();
    String source = flights.get(0).getSource().getCode();
    String destination = flights.get(flights.size() - 1).getDestination().getCode();

    // Calculate Hours and Minutes properly
    int hours = totalDuration / 60;
    int minutes = totalDuration % 60;

    sb.append("\n==============================================");
    sb.append("\n   FLIGHT ITINERARY: ").append(source).append(" >>> ").append(destination);
    sb.append("\n==============================================");

    sb.append(String.format("\n SUMMARY:"));
    sb.append(String.format("\n  - Total Price:    $%8.2f", totalCost));
    sb.append(String.format("\n  - Est. Duration:  %d hrs %d mins", hours, minutes));
    sb.append(String.format("\n  - Connections:    %d", (flights.size() - 1)));

    sb.append("\n\n ROUTE DETAILS:");
    int step = 1;
    for (Flight flight : flights) {
      sb.append(String.format("\n  %d. %s -> %s  ($%.2f | %d min)",
          step++,
          flight.getSource().getCode(),
          flight.getDestination().getCode(),
          flight.getPrice(),
          flight.getDurationMinutes()));
    }
    // sb.append("\n==============================================\n");

    return sb.toString();
  }

  public static void printSearchResult(Itinerary itinerary, Airport source, Airport destination, String type) {
    if (itinerary != null) {
      System.out.println("\n--- " + type + " Route Result ---");
      System.out.println(itinerary.toString());
    } else {
      System.out.println("\nNo route found between " + source.getCode() + " and " + destination.getCode());
    }
  }

  // This clears the terminal window
  public static void clearScreen() {
    System.out.print("\033[H\033[2J");
    System.out.flush();
  }
}
