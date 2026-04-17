import java.io.FileNotFoundException;
import java.io.PrintWriter;
import service.RouteMap;
import utils.Functions;

public class Main {
  public static void main(String[] args) {
    // Hold flights network
    RouteMap routeMap = new RouteMap();

    String errorFileName = "error.txt";
    String reportFileName = "report.txt";

    // Close report/error files automatically when all process finish successfully
    try (
        PrintWriter errorFile = new PrintWriter(errorFileName);
        PrintWriter reportFile = new PrintWriter(reportFileName);) {

      // load all airports and flights data into the system
      Functions.loadSystem(routeMap, reportFile, errorFile);

      // show error if file management process fails
    } catch (FileNotFoundException e) {
      System.out.println("Could not create output files: " + e.getMessage());

    }
  }

}
