package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
      if (parts.length < 11) // 3
        continue; // skip lines that don't have the expected format

      // Remove quotes if exist and remove whitespace
      String code = parts[10].replace("\"", "").trim(); // iata_code // 0/10
      String city = parts[7].replace("\"", "").trim(); // municipality // 1/7
      String country = parts[5].replace("\"", "").trim(); // iso_country // 2/5
      double lat = Double.parseDouble(parts[1].trim());
      double lon = Double.parseDouble(parts[2].trim());

      // Add IATA code only if exists
      if (!code.isEmpty())
        airports.add(new Airport(code, city, country, lat, lon));

    }
    System.out.println("Loaded airports...");

  }

  // PRE: airports list, flights list, reportWriter and errWriter ready to use
  // POST: flights list is populated with data from the csv file, reportWriter
  // and errWriter are used to log the process
  public static void parseFlightRoutesData(List<Airport> airports, List<Flight> flights, PrintWriter reportWriter,
      PrintWriter errWriter) {
    List<String[]> rawLines = getRawData("flight_routes.csv", errWriter);
    int successCount = 0;

    // Build a temporary map to find airports instantly by their code
    Map<String, Airport> lookup = new HashMap<>();
    for (Airport a : airports) {
      lookup.put(a.getCode().toUpperCase(), a);
    }

    for (String[] parts : rawLines) {
      // check for empty rows
      if (parts.length >= 4 && !parts[0].isEmpty() && !parts[1].isEmpty()) {

        String sourceCode = parts[0].trim().toUpperCase();
        String destinationCode = parts[1].trim().toUpperCase();

        Airport source = lookup.get(sourceCode);
        Airport destination = lookup.get(destinationCode);

        try {
          // Parse both numbers
          double price = Double.parseDouble(parts[2].trim());
          int durationMinutes = Integer.parseInt(parts[3].trim());

          if (source != null && destination != null) {
            flights.add(new Flight(source, destination, price, durationMinutes, null));
            successCount++;
          } else {
            // Log if a flight references an airport code not in airports.csv
            errWriter.println("Missing Airport Object for codes: " + sourceCode + " or " + destinationCode);
          }
        } catch (NumberFormatException e) {
          errWriter.println("Invalid numeric data for flight " + sourceCode + "->" + destinationCode);
        }
      }
    }
    System.out.println("Loaded " + successCount + " flights.");
  }

  // PRE: fileName is the name of a valid csv file, errWriter is ready to use
  // POST: returns a list of string arrays, where each array represents a line of
  // the csv file split by commas. Logs any errors encountered during file reading
  // to errWriter.
  private static List<String[]> getRawData(String fileName, PrintWriter errWriter) {
    List<String[]> data = new ArrayList<>();

    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
      String line = br.readLine(); // Skip header
      while ((line = br.readLine()) != null) {
        if (line.trim().isEmpty())
          continue; // Skip empty line

        // Handles commas within quotes. Used specifically for LBA airport: "Leeds, West
        // Yorkshire"
        // Split by comma only if not inside quotes
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

        if (parts.length > 0) {
          // Clean up each par, remove external quotes and whitespace
          for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].replace("\"", "").trim();
          }

          data.add(parts);
        }
      }

    } catch (IOException e) {
      errWriter.println("Error reading file: " + e.getMessage());
    }
    return data;
  }
}
