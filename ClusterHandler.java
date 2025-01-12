package org.example;

import org.apache.lucene.document.Document;
import java.util.*;

// Class to hold document and its frequency information
class DocumentFrequency {
    Document document;
    int frequency;

    public DocumentFrequency(Document document, int frequency) {
        this.document = document;
        this.frequency = frequency;
    }

    public Document getDocument() {
        return document;
    }

    public int getFrequency() {
        return frequency;
    }
}

// Main clustering handler class
public class ClusterHandler {
    public List<DocumentFrequency> analyzeDocuments(List<Document> documents, String queryTerm) {
        Map<String, DocumentFrequency> uniqueDocs = new HashMap<>();
        queryTerm = queryTerm.toLowerCase();

        for (Document doc : documents) {
            String fileName = doc.get("FileName");
            // Only process if we haven't seen this document before
            if (!uniqueDocs.containsKey(fileName)) {
                String content = doc.get("Content").toLowerCase();
                int frequency = countOccurrences(content, queryTerm);
                if (frequency > 0) {
                    uniqueDocs.put(fileName, new DocumentFrequency(doc, frequency));
                }
            }
        }

        // Convert map values to list and sort by frequency
        List<DocumentFrequency> results = new ArrayList<>(uniqueDocs.values());
        results.sort((df1, df2) -> Integer.compare(df2.frequency, df1.frequency));
        return results;
    }

    private int countOccurrences(String content, String term) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(term, index)) != -1) {
            count++;
            index += term.length();
        }
        return count;
    }
}
