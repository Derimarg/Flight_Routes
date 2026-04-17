package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import model.Airport;
import model.Flight;

public class DataLoader {
  private DataLoader() {
    // private constructor to prevent instantiation
  }

  // PRE: airports list, reportWriter and errWriter ready to use
  // POST: airports list is populated with data from the csv file, reportWriter
  // and errWriter are used to log the process
  public static void parseAirportData(List<Airport> airports, PrintWriter reportWriter, PrintWriter errWriter) {
    List<String[]> rawLines = getRawData("airports.csv", errWriter);

    for (String[] parts : rawLines) {
      if (parts.length != 3)
        continue; // skip lines that don't have the expected format

      String code = parts[0];
      String city = parts[1];
      String country = parts[2];

      // Add airport object to the list
      airports.add(new Airport(code, city, country));

    }
    System.out.println("Loaded airports...");

  }

  // PRE: airports list, flights list, reportWriter and errWriter ready to use
  // POST: flights list is populated with data from the csv file, reportWriter
  // and errWriter are used to log the process
  public static void parseFlightRoutesData(List<Airport> airports, List<Flight> flights, PrintWriter reportWriter,
      PrintWriter errWriter) {
    List<String[]> rawLines = getRawData("flight_routes.csv", errWriter);

    for (String[] parts : rawLines) {

      if (parts.length != 5)
        continue; // skip lines that don't have the expected format

      String sourceCode = parts[0];
      String destinationCode = parts[1];
      Airport source = null;
      Airport destination = null;

      for (Airport airport : airports) {
        if (airport.getCode().equals(sourceCode)) {
          source = airport;
        }

        if (airport.getCode().equals(destinationCode)) {
          destination = airport;
        }
      }

      String price = parts[2];
      String durationStr = parts[3];
      String flightNumber = parts[4];

      // Validate and parse duration
      int durationMinutes;
      try {
        durationMinutes = Integer.parseInt(durationStr);
      } catch (NumberFormatException e) {
        errWriter.println("Invalid duration for flight " + flightNumber + ": " + durationStr);
        continue; // skip this flight if duration is invalid
      }

      // Create a Flight object (assuming a Flight class exists)
      flights.add(new Flight(source, destination, Double.parseDouble(price), durationMinutes, flightNumber));

    }
    System.out.println("Loaded flights...");
  }

  // PRE: fileName is the name of a valid csv file, errWriter is ready to use
  // POST: returns a list of string arrays, where each array represents a line of
  // the csv file split by commas. Logs any errors encountered during file reading
  // to errWriter.
  private static List<String[]> getRawData(String fileName, PrintWriter errWriter) {
    List<String[]> data = new ArrayList<>();

    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
      String line = br.readLine();
      while ((line = br.readLine()) != null) {
        data.add(line.split(","));
      }

    } catch (IOException e) {
      errWriter.println("Error reading file: " + e.getMessage());
    }
    return data;
  }
}
