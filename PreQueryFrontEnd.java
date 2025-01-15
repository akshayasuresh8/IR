package org.example;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.List;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.DoublePoint;

public class PreQueryFrontEnd extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JLabel loadingLabel;
    private JLabel statusLabel;
    private List<CentroidCluster<DoublePoint>> clusters;
    private QueryHandler queryHandler;
    private String pathToUnzippedFiles;
    private JTextArea previewArea;
    private JSplitPane splitPane;

    public PreQueryFrontEnd() {
        setTitle("Search Engine");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        queryHandler = new QueryHandler();

        mainPanel.add(createUploadPanel(), "UPLOAD");
        mainPanel.add(createIndexingPanel(), "INDEXING");
        mainPanel.add(createClusterPanel(), "CLUSTERS");

        add(mainPanel);
        setLocationRelativeTo(null);
    }
    private JPanel createUploadPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        JButton uploadButton = new JButton("UPLOAD");
        uploadButton.setBackground(Color.WHITE);
        uploadButton.setForeground(Color.BLACK);
        uploadButton.setFont(new Font("Arial", Font.BOLD, 16));
        uploadButton.setPreferredSize(new Dimension(150, 50));

        statusLabel = new JLabel("");
        uploadButton.addActionListener(e -> {
            try {
                handleFileUpload();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        panel.add(uploadButton);
        panel.add(statusLabel);
        return panel;
    }

    private JPanel createIndexingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JButton indexButton = new JButton("START INDEXING");
        indexButton.setBackground(Color.WHITE);
        indexButton.setForeground(Color.BLACK);
        indexButton.setFont(new Font("Arial", Font.BOLD, 16));
        indexButton.setPreferredSize(new Dimension(150, 50));

        loadingLabel = new JLabel("Processing...");
        loadingLabel.setVisible(false);

        indexButton.addActionListener(e -> startIndexing());

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(indexButton, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(10, 0, 0, 0);
        panel.add(loadingLabel, gbc);

        return panel;
    }

    private JPanel createClusterPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Top panel for search
        JPanel topPanel = new JPanel(new FlowLayout());

        // Query input
        JTextField searchField = new JTextField(20);
        searchField.setBorder(BorderFactory.createTitledBorder("Search Query"));

        // Cluster number input with full label visibility
        JPanel spinnerPanel = new JPanel();
        spinnerPanel.setLayout(new BoxLayout(spinnerPanel, BoxLayout.Y_AXIS));
        JLabel spinnerLabel = new JLabel("Number of Clusters");
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(3, 1, 5, 1);  // default, min, max, step
        JSpinner clusterSpinner = new JSpinner(spinnerModel);
        JComponent editor = clusterSpinner.getEditor();
        JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
        tf.setColumns(2);

        spinnerPanel.add(spinnerLabel);
        spinnerPanel.add(clusterSpinner);

        JButton searchButton = new JButton("Search");

        // Add components to the top panel
        topPanel.add(searchField);
        topPanel.add(Box.createHorizontalStrut(10));  // Add some spacing
        topPanel.add(spinnerPanel);
        topPanel.add(Box.createHorizontalStrut(10));  // Add some spacing
        topPanel.add(searchButton);

        // Center panel with split pane
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        JScrollPane buttonScrollPane = new JScrollPane(buttonsPanel);

        // Create preview panel
        previewArea = new JTextArea();
        previewArea.setEditable(false);
        previewArea.setWrapStyleWord(true);
        previewArea.setLineWrap(true);
        JScrollPane previewScrollPane = new JScrollPane(previewArea);
        previewScrollPane.setBorder(BorderFactory.createTitledBorder("Document Preview"));

        // Create split pane
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buttonScrollPane, previewScrollPane);
        splitPane.setResizeWeight(0.5);

        // Search button action
        searchButton.addActionListener(e -> {
            String query = searchField.getText();
            if (!query.isEmpty()) {
                int numClusters = (Integer) clusterSpinner.getValue();
                clusters = queryHandler.handle(query, numClusters);
                updateClusterButtons(buttonsPanel);
            }
        });

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private void handleClusterClick(int clusterIndex, Component clickedButton, List<String> files) {
        JPopupMenu dropdown = new JPopupMenu();

        // Remove duplicates while preserving order
        List<String> uniqueFiles = new ArrayList<>(new LinkedHashSet<>(files));

        for (String fileName : uniqueFiles) {
            JMenuItem menuItem = new JMenuItem(fileName);
            menuItem.addActionListener(e -> showPreview(fileName));
            dropdown.add(menuItem);
        }

        dropdown.show(clickedButton, 0, clickedButton.getHeight());
    }

    private void showPreview(String fileName) {
        try {
            String filePath = pathToUnzippedFiles + File.separator + fileName;
            File file = new File(filePath);

            if (!file.exists()) {
                previewArea.setText("File not found: " + fileName);
                return;
            }

            // Use TextExtractor for supported files
            String content = new TextExtractor().extract(filePath);
            previewArea.setText(content);

        } catch (IOException ex) {
            previewArea.setText("Error loading preview: " + ex.getMessage());
        }
    }

    private void updateClusterButtons(JPanel buttonsPanel) {
        buttonsPanel.removeAll();

        if (clusters != null) {
            int maxSize = 0;
            for (CentroidCluster<DoublePoint> cluster : clusters) {
                maxSize = Math.max(maxSize, cluster.getPoints().size());
            }

            int nonEmptyClusters = 0;
            for (int i = 0; i < clusters.size() && nonEmptyClusters < 5; i++) {
                List<String> files = queryHandler.openCluster(clusters, i);

                // Remove duplicates while preserving order
                List<String> uniqueFiles = new ArrayList<>(new LinkedHashSet<>(files));

                if (!uniqueFiles.isEmpty()) {
                    final int clusterIndex = i;

                    StringBuilder buttonText = new StringBuilder();
                    buttonText.append("Cluster ").append(nonEmptyClusters + 1)
                            .append(" (").append(uniqueFiles.size()).append(" documents): ")
                            .append(String.join(", ", uniqueFiles));

                    JButton clusterButton = new JButton(buttonText.toString());
                    clusterButton.setPreferredSize(new Dimension(550, 60));
                    clusterButton.setMaximumSize(new Dimension(550, 60));

                    // Ensure the button will show custom colors
                    clusterButton.setContentAreaFilled(true);
                    clusterButton.setOpaque(true);
                    clusterButton.setBorderPainted(true);

                    Color buttonColor = getClusterColor(uniqueFiles.size(), maxSize);
                    clusterButton.setBackground(buttonColor);

                    if (isColorDark(buttonColor)) {
                        clusterButton.setForeground(Color.WHITE);
                    } else {
                        clusterButton.setForeground(Color.BLACK);
                    }

                    // Ensure the Look and Feel doesn't override our colors
                    clusterButton.setUI(new javax.swing.plaf.basic.BasicButtonUI());

                    clusterButton.addActionListener(e -> handleClusterClick(clusterIndex, clusterButton, uniqueFiles));

                    buttonsPanel.add(clusterButton);
                    buttonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                    nonEmptyClusters++;
                }
            }
        }

        buttonsPanel.revalidate();
        buttonsPanel.repaint();
    }

    // ... [Keep existing methods: getClusterColor, isColorDark, handleFileUpload, startIndexing, copyFile, main] ...

    private Color getClusterColor(int size, int maxSize) {
        if (maxSize == 0) return new Color(128, 128, 128); // Darker gray

        float ratio = (float) size / maxSize;

        if (ratio < 0.25) {
            return new Color(220, 53, 69);   // Bootstrap danger red
        } else if (ratio < 0.5) {
            return new Color(255, 193, 7);   // Bootstrap warning yellow
        } else if (ratio < 0.75) {
            return new Color(23, 162, 184);  // Bootstrap info blue
        } else {
            return new Color(40, 167, 69);   // Bootstrap success green
        }
    }

    private boolean isColorDark(Color color) {
        double brightness = (0.299 * color.getRed() +
                0.587 * color.getGreen() +
                0.114 * color.getBlue()) / 255;
        return brightness < 0.5;
    }

    private void handleFileUpload() throws IOException {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            File uploadDir = new File("uploads");
            uploadDir.mkdirs();

            File destFile = new File(uploadDir, selectedFile.getName());
            pathToUnzippedFiles = destFile.getPath().substring(0, destFile.getPath().indexOf('.'));

            try {
                copyFile(selectedFile, destFile);
                ZipFileHandler zipFileHandler = new ZipFileHandler(destFile.getPath());
                zipFileHandler.extractAll("uploads");
                statusLabel.setText("File uploaded: " + selectedFile.getName());
                cardLayout.show(mainPanel, "INDEXING");
            } catch (IOException ex) {
                statusLabel.setText("Upload failed: " + ex.getMessage());
            }
        }
    }

    private void startIndexing() {
        loadingLabel.setVisible(true);

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    IndexHandler indexHandler = new IndexHandler();
                    indexHandler.index(new File(pathToUnzippedFiles));
                    Thread.sleep(2000);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                loadingLabel.setVisible(false);
                cardLayout.show(mainPanel, "CLUSTERS");
            }
        };

        worker.execute();
    }

    private void copyFile(File source, File dest) throws IOException {
        try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
             FileChannel destChannel = new FileOutputStream(dest).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PreQueryFrontEnd().setVisible(true));
    }
}
