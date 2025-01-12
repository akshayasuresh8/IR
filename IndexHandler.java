package org.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class IndexHandler {
    private Directory index;
    private IndexWriter writer;

    public IndexHandler() {
        try {
            this.index = FSDirectory.open(Paths.get("index"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void index(File source) throws IOException {
        try {
            IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
            writer = new IndexWriter(index, config);

            if (source.isDirectory()) {
                File[] files = source.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            indexFile(file);
                        }
                    }
                }
            }
        } finally {
            closeWriter();
        }
    }

    private void indexFile(File file) {
        try {
            String content = new TextExtractor().extract(file.getPath());
            Document doc = createDocument(file.getName(), content);
            writer.addDocument(doc);
        } catch (IOException e) {
            System.err.println("Error processing file: " + file.getName());
            e.printStackTrace();
        }
    }

    protected Document createDocument(String fileName, String text) {
        Document doc = new Document();
        doc.add(new TextField("FileName", fileName, Field.Store.YES));
        doc.add(new TextField("Content", text, Field.Store.YES));
        return doc;
    }

    private void closeWriter() {
        try {
            if (writer != null) {
                writer.commit();
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (index != null) {
                index.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
