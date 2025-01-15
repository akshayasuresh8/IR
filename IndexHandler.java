package org.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;


public class IndexHandler {

    private Directory index;

    public IndexHandler(){}
    protected void index(File source) throws IOException {
        createIndex();
        System.out.println("IndexHandler->");
        try (IndexWriter writer = createIndexWriter()) {
            System.out.println("3. Indexing files...");

            if (source.isDirectory()) {
                File[] files = source.listFiles();
                if (files == null || files.length == 0) {
                    System.out.println("No files to index.");
                    return;
                }

                for (File file : files) {
                    if (file.isFile()) {
                        try {
                            String content = getString(file.getPath());
                            Document doc = createDocument(file.getName(), content);
                            updateIndex(writer, doc);
                        } catch (IOException e) {
                            System.err.println("Error processing file: " + file.getName());
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                System.out.println("Source is not a directory.");
            }
        }
    }


    private String getString(String fileName) throws IOException {
        System.out.println("2. extracting tokens...");
        return new TextExtractor().extract(fileName);
    }

    protected Document createDocument(String FileName, String text){
        System.out.println("4. creating document...");
        Document doc = new Document();
        doc.add(new TextField("FileName",FileName, Field.Store.YES));

        FieldType fieldType = new FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        fieldType.setStoreTermVectors(true);  // Enable term vectors
        fieldType.setStoreTermVectorPositions(true);
        fieldType.setStoreTermVectorOffsets(true);
        fieldType.setStored(true);
        fieldType.setTokenized(true);

        doc.add(new Field("Content",text,fieldType));

        return doc;
    }

    protected IndexWriter createIndexWriter() throws IOException {
        System.out.println("2. creating index writer ...");
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());

        return new IndexWriter(index,config);
    }

    protected void createIndex() throws IOException {
        System.out.println("1. creating index ...");
        System.out.println("1. Clearing and creating index ...");

        // Path to the index directory
        String indexPath = "index";
        File indexDir = new File(indexPath);

        // Delete existing index files
        if (indexDir.exists()) {
            for (File file : indexDir.listFiles()) {
                if (!file.delete()) {
                    System.err.println("Failed to delete file: " + file.getAbsolutePath());
                }
            }
        }
        this.index = FSDirectory.open(Paths.get("index"));
    }

    protected void updateIndex(IndexWriter indexWriter,Document doc) throws IOException {
        System.out.println("5. updating index ...");
        try{
            indexWriter.addDocument(doc);
        } catch (IOException e) {
            throw new RuntimeException("Error updating Index !!",e);
        }


    }
}
