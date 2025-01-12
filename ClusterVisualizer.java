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
    private List<DocumentFrequency> documents;
    private Map<Shape, Document> documentShapes;
    private Consumer<Document> onDocumentSelected;
    private static final int MIN_CIRCLE_SIZE = 50;
    private static final int MAX_CIRCLE_SIZE = 200;
    private String queryTerm;

    public ClusterVisualizer() {
        this.documentShapes = new HashMap<>();
        setPreferredSize(new Dimension(800, 600));
        setupMouseListener();
        setBackground(Color.WHITE);
    }

    public void setDocuments(List<DocumentFrequency> documents, String queryTerm) {
        this.documents = documents;
        this.queryTerm = queryTerm;
        repaint();
    }

    public void setOnDocumentSelected(Consumer<Document> callback) {
        this.onDocumentSelected = callback;
    }

    private void setupMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
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
        if (documents == null || documents.isEmpty()) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        documentShapes.clear();
        drawDocuments(g2d);
    }

    private void drawLegend(Graphics2D g2d, int maxFreq) {
        int legendX = 20;
        int legendY = getHeight() - 150;
        int swatchSize = 20;
        int textOffset = 25;

        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("Frequency ranges:", legendX, legendY);

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        String[] ranges = {
                "80-100%: High",
                "60-80%: Medium-High",
                "40-60%: Medium",
                "20-40%: Medium-Low",
                "0-20%: Low"
        };

        Color[] colors = {
                new Color(0, 255, 0, 180),
                new Color(128, 255, 0, 180),
                new Color(255, 255, 0, 180),
                new Color(255, 128, 0, 180),
                new Color(255, 0, 0, 180)
        };

        for (int i = 0; i < ranges.length; i++) {
            int y = legendY + 20 + (i * 20);
            g2d.setColor(colors[i]);
            g2d.fillRect(legendX, y, swatchSize, swatchSize);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(legendX, y, swatchSize, swatchSize);
            g2d.drawString(ranges[i], legendX + textOffset + swatchSize, y + 15);
        }
    }

    private void drawDocumentLabel(Graphics2D g2d, DocumentFrequency df, int x, int y, int circleSize) {
        // Truncate filename if too long
        String filename = df.document.get("FileName");
        int maxChars = circleSize / 8;  // Approximate characters that can fit
        String displayName = filename;
        if (displayName.length() > maxChars) {
            displayName = displayName.substring(0, maxChars - 3) + "...";
        }

        // Center text
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(displayName);
        int textHeight = fm.getHeight();

        // Draw text with white background for better readability
        int padding = 2;
        int textY = y - 5;
        int bgX = x - textWidth/2 - padding;
        int bgY = textY - textHeight + padding;

        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillRect(bgX, bgY, textWidth + 2*padding, textHeight + padding);

        g2d.setColor(Color.BLACK);
        g2d.drawString(displayName, x - textWidth/2, textY);

        // Draw frequency count below
        String freqText = "Freq: " + df.frequency;
        textWidth = fm.stringWidth(freqText);
        g2d.drawString(freqText, x - textWidth/2, y + 15);
    }

    private void drawDocuments(Graphics2D g2d) {
        if (documents.isEmpty()) return;

        int maxFreq = documents.stream()
                .mapToInt(df -> df.frequency)
                .max()
                .orElse(1);

        // Calculate layout parameters
        int totalDocs = documents.size();
        double angleStep = 2 * Math.PI / totalDocs;
        int radius = Math.min(getWidth(), getHeight()) / 3;
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // Draw title showing search term
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        String title = "Documents containing '" + queryTerm + "'";
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, centerX - titleWidth/2, 30);

        // Draw each document as a circle
        for (int i = 0; i < documents.size(); i++) {
            DocumentFrequency df = documents.get(i);
            double angle = i * angleStep;

            // Calculate position
            int x = (int) (centerX + radius * Math.cos(angle));
            int y = (int) (centerY + radius * Math.sin(angle));

            // Calculate circle size based on term frequency
            int size = calculateCircleSize(df.frequency, maxFreq);

            // Draw circle with frequency-based color
            Shape circle = new Ellipse2D.Double(x - size/2, y - size/2, size, size);
            Color circleColor = getFrequencyColor(df.frequency, maxFreq);
            g2d.setColor(circleColor);
            g2d.fill(circle);
            g2d.setColor(Color.BLACK);
            g2d.draw(circle);

            // Draw filename and frequency with background
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            drawDocumentLabel(g2d, df, x, y, size);

            // Store shape for interaction
            documentShapes.put(circle, df.document);
        }
    }

    private int calculateCircleSize(int frequency, int maxFrequency) {
        double ratio = (double) frequency / maxFrequency;
        return (int) (MIN_CIRCLE_SIZE + ratio * (MAX_CIRCLE_SIZE - MIN_CIRCLE_SIZE));
    }

    private void handleClick(Point point) {
        for (Map.Entry<Shape, Document> entry : documentShapes.entrySet()) {
            if (entry.getKey().contains(point) && onDocumentSelected != null) {
                onDocumentSelected.accept(entry.getValue());
                break;
            }
        }
    }

    private Color getFrequencyColor(int frequency, int maxFrequency) {
        float ratio = (float) frequency / maxFrequency;

        // Colors for different frequency ranges:
        // Low (red) -> medium-low (orange) -> medium (yellow) -> medium-high (yellow-green) -> high (green)
        if (ratio < 0.2) {
            return new Color(255, 0, 0, 180);  // Red
        } else if (ratio < 0.4) {
            return new Color(255, 128, 0, 180);  // Orange
        } else if (ratio < 0.6) {
            return new Color(255, 255, 0, 180);  // Yellow
        } else if (ratio < 0.8) {
            return new Color(128, 255, 0, 180);  // Yellow-green
        } else {
            return new Color(0, 255, 0, 180);  // Green
        }
    }

    private void updateTooltip(Point point) {
        for (Map.Entry<Shape, Document> entry : documentShapes.entrySet()) {
            if (entry.getKey().contains(point)) {
                Document doc = entry.getValue();
                String tooltip = String.format("<html>Document: %s<br>Click to view content</html>",
                        doc.get("FileName"));
                setToolTipText(tooltip);
                return;
            }
        }
        setToolTipText(null);
    }
}
