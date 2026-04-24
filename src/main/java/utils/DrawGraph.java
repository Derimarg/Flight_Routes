package utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import model.Edge;
import model.Node;

public class DrawGraph extends JPanel {
  private List<Node> nodes = new ArrayList<>();
  private List<Edge> edges = new ArrayList<>();
  private final int NODE_SIZE = 30;
  private Image backgroundImage;

  public DrawGraph() {
    this.setBackground(Color.WHITE);

    try {
      backgroundImage = new ImageIcon("map.png").getImage();
    } catch (Exception e) {
      System.out.println("Coul not load background image");
    }
  }

  public List<Node> getNodes() {
    return this.nodes;
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

    if (backgroundImage != null) {
      g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
    }

    g2.setColor(new Color(225, 255, 255, 150)); // White with 150/225 transparency
    g2.fillRect(0, 0, getWidth(), getHeight());

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    FontMetrics f = g2.getFontMetrics();

    // Draw edges first so that nodes will be on top of edges
    for (Edge e : edges) {
      Node s = nodes.get(e.sourceIdx);
      Node d = nodes.get(e.destIdx);

      double dist = Math.sqrt(Math.pow(d.x - s.x, 2) + Math.pow(d.y - s.y, 2));
      // Calculate the angle of the line
      double angle = Math.atan2(d.y - s.y, d.x - s.x);

      int curveAmount = 40;
      int midX = (s.x + d.x) / 2 + (int) (curveAmount * Math.cos(angle + Math.PI / 2));
      int midY = (s.y + d.y) / 2 + (int) (curveAmount * Math.sin(angle + Math.PI / 2));

      g2.setColor(e.color != null ? e.color : Color.GRAY);
      g2.setStroke(new BasicStroke(1.5f));

      QuadCurve2D.Double curve = new QuadCurve2D.Double(s.x, s.y, midX, midY, d.x, d.y);
      g2.draw(curve);

      String label = e.label;
      g2.setFont(new Font("SansSerif", Font.BOLD, 10));

      // drw a white pill behind label
      int textWidth = f.stringWidth(label);
      g2.setColor(new Color(225, 255, 255, 200)); // semi opaque white
      g2.fillRoundRect(midX - 2, midY - 12, textWidth + 4, 15, 5, 5);

      g2.setColor(new Color(200, 0, 0)); // Dark red
      g2.drawString(label, midX, midY);

      // Draw directed edge arrow
      drawArrowOnCurve(g2, midX, midY, d.x, d.y);
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

  public void drawArrowOnCurve(Graphics2D g2, int ctrlX, int ctrlY, int destX, int destY) {
    double angle = Math.atan2(destY - ctrlY, destX - ctrlX);
    int size = 10;
    int r = 15;

    int tx = (int) (destX - r * Math.cos(angle));
    int ty = (int) (destY - r * Math.sin(angle));

    g2.drawLine(tx, ty, (int) (tx - size * Math.cos(angle - 0.5)), (int) (ty - size * Math.sin(angle - 0.5)));
    g2.drawLine(tx, ty, (int) (tx - size * Math.cos(angle + 0.5)), (int) (ty - size * Math.sin(angle + 0.5)));
  }

}