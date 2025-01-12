package org.example;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.nio.file.*;

public class ZipFileHandler implements Iterator<ZipEntry>, Closeable {
    private final String zipFilePath;
    private ZipFile zipFile;
    private final Enumeration<? extends ZipEntry> entries;
    private ZipEntry currentEntry;
    // Add the ZIP file handling methods
    /**
     * Constructor that initializes the ZIP file handler
     * @param zipFilePath Path to the ZIP file
     * @throws IOException If the ZIP file cannot be opened
     */
    public ZipFileHandler(String zipFilePath) throws IOException {
        this.zipFilePath = zipFilePath;
        this.zipFile = new ZipFile(zipFilePath);
        this.entries = zipFile.entries();
    }

    /**
     * Lists all contents of the ZIP file
     * @return List of file names in the ZIP
     */
    public List<String> listContents() {
        List<String> fileList = new ArrayList<>();
        Collections.list(zipFile.entries()).forEach(
                entry -> fileList.add(entry.getName())
        );
        return fileList;
    }

    /**
     * Reads content of a specific file by name
     * @param fileName Name of the file to read
     * @return Content of the file as String
     * @throws IOException If file cannot be read
     */
    public String readFile(String fileName) throws IOException {
        ZipEntry entry = zipFile.getEntry(fileName);
        if (entry == null) {
            throw new FileNotFoundException("File not found: " + fileName);
        }
        return readEntry(entry);
    }

    /**
     * Reads content of a file by its index in the ZIP
     * @param index Index of the file (0-based)
     * @return Content of the file as String
     * @throws IOException If file cannot be read
     */
    public String readFileByIndex(int index) throws IOException {
        List<? extends ZipEntry> entryList = Collections.list(zipFile.entries());
        if (index < 0 || index >= entryList.size()) {
            throw new IndexOutOfBoundsException("Invalid index: " + index);
        }
        return readEntry(entryList.get(index));
    }

    /**
     * Extracts a specific file from the ZIP
     * @param fileName Name of the file to extract
     * @param destPath Destination path
     * @throws IOException If file cannot be extracted
     */
    public void extractFile(String fileName, String destPath) throws IOException {
        ZipEntry entry = zipFile.getEntry(fileName);
        if (entry == null) {
            throw new FileNotFoundException("File not found: " + fileName);
        }

        Path destFilePath = Paths.get(destPath, fileName);
        Files.createDirectories(destFilePath.getParent());

        try (InputStream is = zipFile.getInputStream(entry);
             FileOutputStream fos = new FileOutputStream(destFilePath.toFile())) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }
        }
    }

    /**
     * Extracts all contents of the ZIP file
     * @param destPath Destination directory
     * @throws IOException If extraction fails
     */
    public void extractAll(String destPath) throws IOException {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            Path destFilePath = Paths.get(destPath, entry.getName());

            if (entry.isDirectory()) {
                Files.createDirectories(destFilePath);
            } else {
                Files.createDirectories(destFilePath.getParent());
                try (InputStream is = zipFile.getInputStream(entry);
                     FileOutputStream fos = new FileOutputStream(destFilePath.toFile())) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, length);
                    }
                }
            }
        }
    }

    // Iterator methods
    @Override
    public boolean hasNext() {
        return entries.hasMoreElements();
    }

    @Override
    public ZipEntry next() {
        currentEntry = entries.nextElement();
        return currentEntry;
    }

    /**
     * Gets the content of the current entry in the iteration
     * @return Content of current file as String
     * @throws IOException If file cannot be read
     */
    public String getCurrentEntryContent() throws IOException {
        if (currentEntry == null) {
            throw new IllegalStateException("No current entry. Call next() first.");
        }
        return readEntry(currentEntry);
    }

    // Helper method to read ZipEntry content
    private String readEntry(ZipEntry entry) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(zipFile.getInputStream(entry)))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }

    @Override
    public void close() throws IOException {
        if (zipFile != null) {
            zipFile.close();
            zipFile = null;
        }
    }
}
