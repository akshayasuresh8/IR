package org.example;

import org.apache.lucene.document.Document;
import java.util.*;

class DocumentCluster {
    private List<Document> documents;
    private double similarity;
    private String representativeTerm;

    public DocumentCluster(List<Document> documents, double similarity, String representativeTerm) {
        this.documents = documents;
        this.similarity = similarity;
        this.representativeTerm = representativeTerm;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public int getSize() {
        return documents.size();
    }

    public double getSimilarity() {
        return similarity;
    }

    public String getRepresentativeTerm() {
        return representativeTerm;
    }
}

public class DocumentClusterHandler {
    private static final double SIMILARITY_THRESHOLD = 0.1; // Lowered threshold

    public List<DocumentCluster> createClusters(List<Document> documents) {
        // Debug information
        System.out.println("Creating clusters from " + documents.size() + " documents");

        List<DocumentCluster> clusters = new ArrayList<>();
        Map<String, List<Document>> termClusters = new HashMap<>();

        // First pass: Group documents by common significant terms
        for (Document doc : documents) {
            String content = doc.get("Content").toLowerCase();
            String fileName = doc.get("FileName");
            System.out.println("Processing document: " + fileName);

            String[] terms = content.split("\\W+");
            Map<String, Integer> termFrequency = new HashMap<>();

            // Calculate term frequencies
            for (String term : terms) {
                if (term.length() > 3) { // Filter out very short terms
                    termFrequency.merge(term, 1, Integer::sum);
                }
            }

            // Find the most significant term for this document
            String significantTerm = findMostSignificantTerm(termFrequency);
            termClusters.computeIfAbsent(significantTerm, k -> new ArrayList<>()).add(doc);
        }

        // Create clusters from the grouped documents
        for (Map.Entry<String, List<Document>> entry : termClusters.entrySet()) {
            if (entry.getValue().size() > 1) { // Only create clusters with multiple documents
                double similarity = calculateClusterSimilarity(entry.getValue());
                if (similarity >= SIMILARITY_THRESHOLD) {
                    clusters.add(new DocumentCluster(entry.getValue(), similarity, entry.getKey()));
                }
            }
        }

        // Sort clusters by size and similarity
        clusters.sort((c1, c2) -> {
            int sizeCompare = Integer.compare(c2.getSize(), c1.getSize());
            if (sizeCompare != 0) return sizeCompare;
            return Double.compare(c2.getSimilarity(), c1.getSimilarity());
        });

        return clusters;
    }

    private String findMostSignificantTerm(Map<String, Integer> termFrequency) {
        return termFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
    }

    private double calculateClusterSimilarity(List<Document> documents) {
        // Simple similarity measure based on common terms
        Set<String> commonTerms = new HashSet<>();
        boolean first = true;

        for (Document doc : documents) {
            Set<String> docTerms = new HashSet<>(
                    Arrays.asList(doc.get("Content").toLowerCase().split("\\W+"))
            );

            if (first) {
                commonTerms.addAll(docTerms);
                first = false;
            } else {
                commonTerms.retainAll(docTerms);
            }
        }

        return (double) commonTerms.size() / documents.size();
    }
}
