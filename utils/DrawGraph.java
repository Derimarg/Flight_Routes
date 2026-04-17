package utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import model.Edge;
import model.Node;

public class DrawGraph extends JPanel {
  private List<Node> nodes = new ArrayList<>();
  private List<Edge> edges = new ArrayList<>();
  private final int NODE_SIZE = 30;

  public DrawGraph() {
    this.setBackground(Color.WHITE);
  }

  public void addNode(String name, int x, int y) {
    nodes.add(new Node(name, x, y));
    this.repaint();
  }

  public void addEdge(int i, int j, String weight) {
    edges.add(new Edge(i, j, weight));
    this.repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    FontMetrics f = g2.getFontMetrics();

    // Draw edges first so that nodes will be on top of edges
    for (Edge e : edges) {
      Node s = nodes.get(e.sourceIdx);
      Node d = nodes.get(e.destIdx);

      // Calculate the angle of the line
      double angle = Math.atan2(d.y - s.y, d.x - s.x);

      // Shift the start and end points 5 pixels perpendicular to the line
      // This separates the "there" and "back" lines
      int offset = 6;
      int x1 = (int) (s.x + offset * Math.cos(angle + Math.PI / 2));
      int y1 = (int) (s.y + offset * Math.sin(angle + Math.PI / 2));
      int x2 = (int) (d.x + offset * Math.cos(angle + Math.PI / 2));
      int y2 = (int) (d.y + offset * Math.sin(angle + Math.PI / 2));

      g2.setColor(Color.GRAY);
      g2.drawLine(x1, y1, x2, y2);

      // Calculate the mid-points using LERP (t = 0.35)
      double t = 0.35;
      int labelBaseX = (int) (x1 + t * (x2 - x1));
      int labelBaseY = (int) (y1 + t * (y2 - y1));

      // Calculate the perpendicular push
      int labelOffset = 15;
      int xLabelPush = (int) (labelOffset * Math.cos(angle + Math.PI / 2));
      int yLabelPush = (int) (labelOffset * Math.sin(angle + Math.PI / 2));

      g2.setColor(Color.RED);
      g2.drawString(e.label, labelBaseX + xLabelPush, labelBaseY + yLabelPush);

      // Draw directed edge arrow
      drawArrow(g2, x1, y1, x2, y2);
    }

    // Draw nodes
    for (Node n : nodes) {
      int w = Math.max(NODE_SIZE, f.stringWidth(n.name) + 10);
      int h = NODE_SIZE;

      // Draw node circle
      g2.setColor(Color.WHITE);
      g2.fillRoundRect(n.x - w / 2, n.y - h / 2, w, h, 10, 10);
      g2.setColor(Color.BLUE);
      g2.setStroke(new BasicStroke(2));
      g2.drawRoundRect(n.x - w / 2, n.y - h / 2, w, h, 10, 10);

      // Draw text
      g2.setColor(Color.BLACK);
      g2.drawString(n.name, n.x - f.stringWidth(n.name) / 2, n.y + f.getHeight() / 4);
    }
  }

  public static void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
    double angle = Math.atan2(y2 - y1, x2 - x1);
    int size = 8;

    // Offset arrow so it doesn't overlap with node circle
    int tx = (int) (x2 - 18 * Math.cos(angle));
    int ty = (int) (y2 - 18 * Math.sin(angle));

    g2.setColor(Color.DARK_GRAY);
    g2.drawLine(tx, ty, (int) (tx - size * Math.cos(angle - 0.5)), (int) (ty - size * Math.sin(angle - 0.5)));
    g2.drawLine(tx, ty, (int) (tx - size * Math.cos(angle + 0.5)), (int) (ty - size * Math.sin(angle + 0.5)));

  }
}