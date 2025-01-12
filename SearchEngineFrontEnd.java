package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import org.apache.lucene.document.Document;

public class SearchEngineFrontEnd extends JFrame {
    // GUI Components
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JLabel loadingLabel;
    private JLabel statusLabel;
    private ClusterVisualizer clusterVisualizer;
    private JTextArea documentViewer;
    private JProgressBar progressBar;
    private JLabel progressLabel;

    // Business Logic Components
    private IndexHandler indexHandler;
    private QueryHandler queryHandler;
    private ZipFileHandler zipHandler;

    public SearchEngineFrontEnd() {
        initializeFrame();
        initializeComponents();
        setupCallbacks();
        createAndAddPanels();
    }

    private void initializeFrame() {
        setTitle("Search Engine with Clustering");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        // Initialize layouts and panels
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Initialize visualization components
        clusterVisualizer = new ClusterVisualizer();
        documentViewer = new JTextArea();
        documentViewer.setEditable(false);
        documentViewer.setFont(new Font("Monospace", Font.PLAIN, 14));
        documentViewer.setMargin(new Insets(10, 10, 10, 10));

        // Initialize progress components
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressLabel = new JLabel("Ready");

        // Initialize status components
        loadingLabel = new JLabel("Processing...");
        loadingLabel.setVisible(false);
        statusLabel = new JLabel("");

        // Initialize handlers
        indexHandler = new IndexHandler();
        queryHandler = new QueryHandler();
    }

    private void setupCallbacks() {
        // Setup cluster visualization callback
        queryHandler.setOnResultsAnalyzed((documents, queryTerm) -> {
            SwingUtilities.invokeLater(() -> {
                clusterVisualizer.setDocuments(documents, queryTerm);
                if (documents.isEmpty()) {
                    showMessage("No documents found for this query");
                }
            });
        });

        // Setup document selection callback
        clusterVisualizer.setOnDocumentSelected(this::displayDocument);
    }

    private void createAndAddPanels() {
        mainPanel.add(createWelcomePanel(), "WELCOME");
        mainPanel.add(createUploadPanel(), "UPLOAD");
        mainPanel.add(createIndexingPanel(), "INDEXING");
        mainPanel.add(createSearchPanel(), "SEARCH");
        add(mainPanel);
    }

    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(245, 245, 250));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Welcome message
        JLabel welcomeLabel = new JLabel("Document Search & Clustering Engine");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(welcomeLabel, gbc);

        // Start button
        JButton startButton = createStyledButton("Start");
        startButton.addActionListener(e -> cardLayout.show(mainPanel, "UPLOAD"));
        gbc.gridy = 1;
        panel.add(startButton, gbc);

        return panel;
    }

    private JPanel createUploadPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(245, 245, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Upload instructions
        JLabel instructionsLabel = new JLabel("Select a folder or ZIP file containing documents to index");
        instructionsLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(instructionsLabel, gbc);

        // Upload buttons
        JButton folderButton = createStyledButton("Select Folder");
        JButton zipButton = createStyledButton("Select ZIP");

        folderButton.addActionListener(e -> handleFolderUpload());
        zipButton.addActionListener(e -> handleZipUpload());

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(folderButton, gbc);

        gbc.gridx = 1;
        panel.add(zipButton, gbc);

        // Status label
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(statusLabel, gbc);

        return panel;
    }

    private JPanel createIndexingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(245, 245, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Indexing status
        JLabel statusLabel = new JLabel("Preparing to index documents...");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(statusLabel, gbc);

        // Progress bar
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(progressBar, gbc);

        // Start indexing button
        JButton indexButton = createStyledButton("Start Indexing");
        indexButton.addActionListener(e -> startIndexing());
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(indexButton, gbc);

        return panel;
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 245, 250));

        // Search controls at top
        JPanel searchControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchControls.setBackground(new Color(245, 245, 250));

        JTextField queryField = new JTextField(40);
        queryField.setPreferredSize(new Dimension(400, 30));
        JButton searchButton = createStyledButton("Search");

        searchControls.add(new JLabel("Query:"));
        searchControls.add(queryField);
        searchControls.add(searchButton);

        // Main content area
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createClusterPanel(),
                createDocumentPanel()
        );
        splitPane.setResizeWeight(0.6);
        splitPane.setBorder(null);

        // Search action
        searchButton.addActionListener(e -> {
            String query = queryField.getText().trim();
            if (!query.isEmpty()) {
                documentViewer.setText("Searching...");
                queryHandler.handle(query);
            }
        });

        // Add components to main panel
        panel.add(searchControls, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createClusterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Document Clusters"));
        panel.add(clusterVisualizer, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDocumentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Document Preview"));

        JScrollPane scrollPane = new JScrollPane(documentViewer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void handleFolderUpload() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Folder to Index");

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            File uploadDir = new File("uploads");
            uploadDir.mkdirs();

            try {
                copyDirectory(selectedDir, uploadDir);
                statusLabel.setText("Folder uploaded successfully: " + selectedDir.getName());
                cardLayout.show(mainPanel, "INDEXING");
            } catch (IOException ex) {
                showError("Error uploading folder: " + ex.getMessage());
            }
        }
    }

    private void handleZipUpload() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".zip");
            }
            public String getDescription() {
                return "ZIP Files (*.zip)";
            }
        });

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                zipHandler = new ZipFileHandler(selectedFile.getPath());
                File extractDir = new File("uploads");
                extractDir.mkdirs();

                zipHandler.extractAll(extractDir.getPath());
                statusLabel.setText("ZIP file extracted successfully: " + selectedFile.getName());
                cardLayout.show(mainPanel, "INDEXING");
            } catch (IOException ex) {
                showError("Error processing ZIP file: " + ex.getMessage());
            }
        }
    }

    private void startIndexing() {
        loadingLabel.setVisible(true);
        progressBar.setIndeterminate(true);

        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                File uploadDir = new File("uploads");
                indexHandler.index(uploadDir);
                return null;
            }

            @Override
            protected void done() {
                loadingLabel.setVisible(false);
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                cardLayout.show(mainPanel, "SEARCH");
            }
        };

        worker.execute();
    }

    private void displayDocument(Document doc) {
        documentViewer.setText("");
        documentViewer.append("Filename: " + doc.get("FileName") + "\n");
        documentViewer.append(createSeparator(50) + "\n\n");
        documentViewer.append(doc.get("Content"));
        documentViewer.setCaretPosition(0);
    }

    private void copyDirectory(File source, File dest) throws IOException {
        if (source.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdirs();
            }

            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    File srcFile = new File(source, file);
                    File destFile = new File(dest, file);
                    copyDirectory(srcFile, destFile);
                }
            }
        } else {
            copyFile(source, dest);
        }
    }

    private void copyFile(File source, File dest) throws IOException {
        try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
             FileChannel destChannel = new FileOutputStream(dest).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.BLUE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(100, 149, 237));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(70, 130, 180));
            }
        });

        return button;
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Information",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private String createSeparator(int length) {
        StringBuilder separator = new StringBuilder();
        for (int i = 0; i < length; i++) {
            separator.append("=");
        }
        return separator.toString();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new SearchEngineFrontEnd().setVisible(true);
        });
    }
}
