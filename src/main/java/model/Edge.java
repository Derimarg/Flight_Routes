package model;

import java.awt.*;

public class Edge {
  public int sourceIdx, destIdx;
  public String label; // weight label for the edge distance, flight time, etc.
  public Color color; // color for the edge

  public Edge() {
  }

  public Edge(int i, int j, String label) {
    this.sourceIdx = i;
    this.destIdx = j;
    this.label = label;
    this.color = Color.BLACK; // default color
  }

  public void setHighlighted(Color c) {
    this.color = c;
  }
}
