package model;

public class Node {
  public int x, y;
  public String name;

  public Node() {
  }

  // Loaded constructor
  public Node(String n, int x, int y) {
    this.name = n;
    this.x = x;
    this.y = y;
  }
}
