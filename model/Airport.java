package model;

public class Airport {
  private String code;
  private String city;
  private String country;

  // Default constructor
  public Airport() {
  }

  // Overloaded construnctor
  public Airport(String code, String city, String country) {
    this.code = code;
    this.city = city;
    this.country = country;
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

  @Override
  public String toString() {
    return "Airport{" +
        "code='" + code + '\'' +
        ", city='" + city + '\'' +
        ", country='" + country + '\'' +
        '}';
  }
}
