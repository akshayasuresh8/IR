package org.example;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.*;
import org.apache.lucene.search.*;
import org.apache.commons.math3.ml.clustering.*;
import org.apache.commons.math3.ml.clustering.DoublePoint;

public class QueryHandler {
    private Map<DoublePoint, Document> docMap;


    public List<CentroidCluster<DoublePoint>> handle(String query, int k) {
        QueryBuilder builder = new QueryBuilder(new StandardAnalyzer());
        Query q = builder.createBooleanQuery("Content", query);
        List<CentroidCluster<DoublePoint>> clusters = null;
        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("index")))) {
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs topDocs = searcher.search(q, 100);
            ScoreDoc[] hits = topDocs.scoreDocs;

            System.out.println("Retrieved " + hits.length + " documents.");

            // Build global vocabulary
            Set<String> vocabulary = buildGlobalVocabulary(reader, "Content");

            List<DoublePoint> documentVectors = new ArrayList<>();
            this.docMap = new HashMap<>();

            // Create TF-IDF vectors for each document
            for (ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                Terms terms = reader.getTermVector(hit.doc, "Content");
                if (terms != null) {
                    DoublePoint vector = createTFIDFVectorWithVocabulary(terms, reader, vocabulary);
                    documentVectors.add(vector);
                    docMap.put(vector, doc);
                }
            }

            System.out.println("Extracted " + documentVectors.size() + " consistent TF-IDF vectors.");

            if (documentVectors.isEmpty()) {
                System.out.println("No valid documents to cluster. Ensure the query retrieves results.");
                return clusters;
            }

            int adjustedK = Math.min(k, documentVectors.size());
            if (adjustedK < 2) {
                System.out.println("Not enough documents for clustering. Minimum 2 required.");
                return clusters;
            }

            KMeansPlusPlusClusterer<DoublePoint> kMeans = new KMeansPlusPlusClusterer<>(adjustedK, 100);
            clusters = kMeans.cluster(documentVectors);


            for (int i = 0; i < clusters.size(); i++) {
                System.out.println("Cluster " + (i + 1) + ":");
                for (DoublePoint point : clusters.get(i).getPoints()) {
                    Document doc = docMap.get(point);
                    System.out.println(" - " + doc.get("FileName"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return clusters;
    }

    public ArrayList<String> openCluster(List<CentroidCluster<DoublePoint>> clusters, int ClusterNumber) {
        ArrayList<String> files = new ArrayList<>();
        System.out.println("ClusterNumber: " + ClusterNumber);

        // Validate the ClusterNumber
        if (ClusterNumber < 0 || ClusterNumber >= clusters.size()) {
            System.out.println("Invalid ClusterNumber: " + ClusterNumber);
            return files;  // Return empty list
        }

        // Retrieve the cluster
        CentroidCluster<DoublePoint> cluster = clusters.get(ClusterNumber);
        if (cluster.getPoints().isEmpty()) {
            System.out.println("Cluster " + ClusterNumber + " has no points.");
            return files;  // Return empty list
        }

        // Iterate over points in the cluster
        for (DoublePoint point : cluster.getPoints()) {
            Document doc = docMap.get(point);
            if (doc == null) {
                System.out.println("No document found for point: " + point);
                continue;  // Skip if document is not found
            }

            String fileName = doc.get("FileName");
            if (fileName != null) {
                System.out.println(" - " + fileName);
                files.add(fileName);
            } else {
                System.out.println("FileName is null for document.");
            }
        }

        System.out.println("ArrayList: " + files);
        return files;
    }

    private Set<String> buildGlobalVocabulary(IndexReader reader, String fieldName) throws IOException {
        Set<String> vocabulary = new HashSet<>();

        for (int docId = 0; docId < reader.maxDoc(); docId++) {
            Terms terms = reader.getTermVector(docId, fieldName);
            if (terms != null) {
                TermsEnum termsEnum = terms.iterator();
                while (termsEnum.next() != null) {
                    vocabulary.add(termsEnum.term().utf8ToString());
                }
            }
        }
        System.out.println("Global Vocabulary Size: " + vocabulary.size());
        return vocabulary;
    }

    private DoublePoint createTFIDFVectorWithVocabulary(Terms terms, IndexReader reader, Set<String> vocabulary) throws IOException {
        Map<String, Double> tfidfMap = new HashMap<>();

        // Initialize TF-IDF vector with all terms in the vocabulary
        for (String term : vocabulary) {
            tfidfMap.put(term, 0.0);
        }

        if (terms != null) {
            TermsEnum termsEnum = terms.iterator();
            while (termsEnum.next() != null) {
                String term = termsEnum.term().utf8ToString();
                long termFreq = termsEnum.totalTermFreq();
                if (vocabulary.contains(term)) {
                    double idf = Math.log(1 + (reader.numDocs() / (double) reader.docFreq(new Term("Content", term))));
                    tfidfMap.put(term, termFreq * idf);
                }
            }
        }

        // Convert TF-IDF map to a vector
        double[] vector = tfidfMap.values().stream().mapToDouble(Double::doubleValue).toArray();
        return new DoublePoint(vector);
    }

    public static void main(String[] args) {
        String query = "newton";
        QueryHandler handler = new QueryHandler();
        handler.handle(query, 5);

    }
}
