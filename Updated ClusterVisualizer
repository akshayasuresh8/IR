package org.example;

import org.apache.lucene.document.Document;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class ClusterVisualizer extends JPanel {
    private List<DocumentCluster> clusters;
    private Map<Shape, DocumentCluster> clusterShapes;
    private Consumer<Document> onDocumentSelected;
    private static final int MIN_CIRCLE_SIZE = 80;
    private static final int MAX_CIRCLE_SIZE = 250;
    private JPopupMenu documentMenu;
    private Point lastClickLocation;

    public ClusterVisualizer() {
        this.clusterShapes = new HashMap<>();
        this.documentMenu = new JPopupMenu();
        setPreferredSize(new Dimension(800, 600));
        setupMouseListener();
        setBackground(Color.WHITE);
    }

    public void setClusters(List<DocumentCluster> clusters) {
        this.clusters = clusters;
        repaint();
    }

    public void setOnDocumentSelected(Consumer<Document> callback) {
        this.onDocumentSelected = callback;
    }

    private void setupMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                lastClickLocation = e.getPoint();
                handleClick(e.getPoint());
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateTooltip(e.getPoint());
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (clusters == null || clusters.isEmpty()) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        clusterShapes.clear();
        drawClusters(g2d);
        drawLegend(g2d);
    }

    private void drawClusters(Graphics2D g2d) {
        int maxSize = clusters.stream()
                .mapToInt(DocumentCluster::getSize)
                .max()
                .orElse(1);

        // Calculate layout using force-directed placement
        Map<DocumentCluster, Point2D.Double> positions = calculateClusterPositions();

        // Draw connections between similar clusters
        drawClusterConnections(g2d, positions);

        // Draw clusters
        for (DocumentCluster cluster : clusters) {
            Point2D.Double pos = positions.get(cluster);
            int x = (int) pos.x;
            int y = (int) pos.y;

            // Calculate circle size based on number of documents
            int size = calculateCircleSize(cluster.getSize(), maxSize);

            // Draw cluster circle
            Shape circle = new Ellipse2D.Double(x - size/2, y - size/2, size, size);
            Color clusterColor = getClusterColor(cluster.getSimilarity());
            g2d.setColor(clusterColor);
            g2d.fill(circle);
            g2d.setColor(Color.BLACK);
            g2d.draw(circle);

            // Draw cluster label
            drawClusterLabel(g2d, cluster, x, y, size);

            // Store shape for interaction
            clusterShapes.put(circle, cluster);
        }
    }

    private Map<DocumentCluster, Point2D.Double> calculateClusterPositions() {
        int width = getWidth() - 100;
        int height = getHeight() - 100;
        Map<DocumentCluster, Point2D.Double> positions = new HashMap<>();

        // Calculate positions in a circular layout
        int numClusters = clusters.size();
        double angleStep = 2 * Math.PI / numClusters;
        int radius = Math.min(width, height) / 3;

        for (int i = 0; i < numClusters; i++) {
            double angle = i * angleStep;
            int x = width/2 + (int)(radius * Math.cos(angle));
            int y = height/2 + (int)(radius * Math.sin(angle));
            positions.put(clusters.get(i), new Point2D.Double(x, y));
        }

        return positions;
    }

    private void drawClusterConnections(Graphics2D g2d, Map<DocumentCluster, Point2D.Double> positions) {
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.setColor(new Color(200, 200, 200));

        for (DocumentCluster c1 : clusters) {
            for (DocumentCluster c2 : clusters) {
                if (c1 != c2 && Math.abs(c1.getSimilarity() - c2.getSimilarity()) < 0.2) {
                    Point2D.Double p1 = positions.get(c1);
                    Point2D.Double p2 = positions.get(c2);
                    g2d.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
                }
            }
        }
    }

    private void drawClusterLabel(Graphics2D g2d, DocumentCluster cluster, int x, int y, int size) {
        String label = String.format("%s (%d docs)",
                cluster.getRepresentativeTerm(),
                cluster.getSize());

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int textHeight = fm.getHeight();

        // Draw text with background
        int padding = 5;
        int textY = y;
        int bgX = x - textWidth/2 - padding;
        int bgY = textY - textHeight + padding;

        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillRect(bgX, bgY, textWidth + 2*padding, textHeight + padding);

        g2d.setColor(Color.BLACK);
        g2d.drawString(label, x - textWidth/2, textY);
    }

    private void drawLegend(Graphics2D g2d) {
        int legendX = 20;
        int legendY = getHeight() - 100;

        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Similarity Level:", legendX, legendY);

        String[] levels = {"High", "Medium", "Low"};
        Color[] colors = {
                new Color(0, 150, 0, 180),
                new Color(150, 150, 0, 180),
                new Color(150, 0, 0, 180)
        };

        for (int i = 0; i < levels.length; i++) {
            int y = legendY + 20 + (i * 20);
            g2d.setColor(colors[i]);
            g2d.fillRect(legendX, y, 20, 15);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(legendX, y, 20, 15);
            g2d.drawString(levels[i], legendX + 30, y + 12);
        }
    }

    private void handleClick(Point point) {
        for (Map.Entry<Shape, DocumentCluster> entry : clusterShapes.entrySet()) {
            if (entry.getKey().contains(point)) {
                showDocumentMenu(entry.getValue(), point);
                break;
            }
        }
    }

    private void showDocumentMenu(DocumentCluster cluster, Point location) {
        documentMenu.removeAll();

        JLabel titleLabel = new JLabel("Documents in cluster:");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        documentMenu.add(titleLabel);
        documentMenu.addSeparator();

        for (Document doc : cluster.getDocuments()) {
            JMenuItem item = new JMenuItem(doc.get("FileName"));
            item.addActionListener(e -> {
                if (onDocumentSelected != null) {
                    onDocumentSelected.accept(doc);
                }
            });
            documentMenu.add(item);
        }

        documentMenu.show(this, location.x, location.y);
    }

    private int calculateCircleSize(int clusterSize, int maxSize) {
        double ratio = (double) clusterSize / maxSize;
        return (int) (MIN_CIRCLE_SIZE + ratio * (MAX_CIRCLE_SIZE - MIN_CIRCLE_SIZE));
    }

    private Color getClusterColor(double similarity) {
        // Green for high similarity, yellow for medium, red for low
        if (similarity >= 0.7) {
            return new Color(0, 150, 0, 180);  // Green
        } else if (similarity >= 0.5) {
            return new Color(150, 150, 0, 180);  // Yellow
        } else {
            return new Color(150, 0, 0, 180);  // Red
        }
    }

    private void updateTooltip(Point point) {
        for (Map.Entry<Shape, DocumentCluster> entry : clusterShapes.entrySet()) {
            if (entry.getKey().contains(point)) {
                DocumentCluster cluster = entry.getValue();
                String tooltip = String.format("<html>Cluster: %s<br>Documents: %d<br>Similarity: %.2f<br>Click to view documents</html>",
                        cluster.getRepresentativeTerm(),
                        cluster.getSize(),
                        cluster.getSimilarity());
                setToolTipText(tooltip);
                return;
            }
        }
        setToolTipText(null);
    }
}
