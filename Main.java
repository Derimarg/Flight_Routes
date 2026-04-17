import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Airport;
import model.Flight;
import utils.Functions;

public class Main {
  public static void main(String[] args) {
    // Hold flights network
    Map<Airport, List<Flight>> routes = new HashMap<>();

    String errorFileName = "error.txt";
    String reportFileName = "report.txt";

    // Close report/error files automatically when all process finish successfully
    try (
        PrintWriter errorFile = new PrintWriter(errorFileName);
        PrintWriter reportFile = new PrintWriter(reportFileName);) {

      // load all airports and flights data into the system
      Functions.loadSystem(routes, reportFile, errorFile);

      // show error if file management process fails
    } catch (FileNotFoundException e) {
      System.out.println("Could not create output files: " + e.getMessage());

    }
  }

}
