package org.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class QueryHandler {
    private ClusterHandler clusterHandler;
    private BiConsumer<List<DocumentFrequency>, String> onResultsAnalyzed;

    public QueryHandler() {
        this.clusterHandler = new ClusterHandler();
    }

    public void setOnResultsAnalyzed(BiConsumer<List<DocumentFrequency>, String> callback) {
        this.onResultsAnalyzed = callback;
    }

    protected void handle(String queryStr) {
        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("index")))) {
            // Create and execute search
            QueryBuilder builder = new QueryBuilder(new StandardAnalyzer());
            Query query = builder.createBooleanQuery("Content", queryStr);
            IndexSearcher searcher = new IndexSearcher(reader);
            TopScoreDocCollector collector = TopScoreDocCollector.create(20);
            searcher.search(query, collector);

            // Get results
            ScoreDoc[] hits = collector.topDocs().scoreDocs;
            Set<String> seenDocuments = new HashSet<>();
            List<Document> documents = new ArrayList<>();

            for (ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                String fileName = doc.get("FileName");
                // Only add if we haven't seen this document before
                if (!seenDocuments.contains(fileName)) {
                    documents.add(doc);
                    seenDocuments.add(fileName);
                }
            }

            // Analyze document frequencies
            if (!documents.isEmpty()) {
                List<DocumentFrequency> analyzed = clusterHandler.analyzeDocuments(documents, queryStr);
                if (onResultsAnalyzed != null) {
                    onResultsAnalyzed.accept(analyzed, queryStr);
                }
            }

        } catch (Exception e) {
            System.err.println("Error handling query: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
