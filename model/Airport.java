package model;

public class Airport {
  private String code;
  private String city;
  private String country;
  private double lat;
  private double lon;

  // Default constructor
  public Airport() {
  }

  // Overloaded construnctor
  public Airport(String code, String city, String country, double lat, double lon) {
    this.code = code;
    this.city = city;
    this.country = country;
    this.lat = lat;
    this.lon = lon;
  }

  // Getters
  public String getCode() {
    return code;
  }

  public String getCity() {
    return city;
  }

  public String getCountry() {
    return country;
  }

  public double getLat() {
    return lat;
  }

  public double getLon() {
    return lon;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Airport airport = (Airport) o;
    // Check that airports are the same
    return code != null ? code.equals(airport.code) : airport.code == null;
  }

  @Override
  public int hashCode() {
    // Always generate the same index in a HashMap
    return code != null ? code.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "Airport{" +
        "code='" + code + '\'' +
        ", city='" + city + '\'' +
        ", country='" + country + '\'' +
        '}';
  }
}
