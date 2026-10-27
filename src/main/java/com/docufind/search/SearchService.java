package com.docufind.search;

import com.docufind.dao.DocumentDAO;
import com.docufind.dao.SearchHistoryDAO;
import com.docufind.model.DocumentRecord;
import com.docufind.model.SearchHistoryItem;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchService {
    private static final Logger LOGGER = Logger.getLogger(SearchService.class.getName());

    private final DocumentDAO documentDAO;
    private final SearchHistoryDAO historyDAO;

    public SearchService() {
        this.documentDAO = new DocumentDAO();
        this.historyDAO = new SearchHistoryDAO();
    }

    public SearchService(DocumentDAO documentDAO, SearchHistoryDAO historyDAO) {
        this.documentDAO = documentDAO;
        this.historyDAO = historyDAO;
    }

    /**
     * Executes document search with scoring, ranking, and snippet generation.
     */
    public List<DocumentRecord> search(long userId, String query, String searchMode,
                                       String extensionFilter, Long folderId,
                                       Date modifiedAfter, String sortBy, int maxResults) {
        long startTime = System.currentTimeMillis();
        List<DocumentRecord> results = new ArrayList<>();

        try {
            results = documentDAO.searchDocuments(userId, query, searchMode, extensionFilter, folderId, modifiedAfter, sortBy, maxResults);

            // Compute relevance score & generate text snippets
            for (DocumentRecord doc : results) {
                double score = calculateRelevance(doc, query, searchMode);
                doc.setRelevanceScore(score);
                doc.setMatchSnippet(generateSnippet(doc, query));
            }

            // Sort by relevance if requested
            if ("Relevance".equalsIgnoreCase(sortBy) || sortBy == null || sortBy.isBlank()) {
                results.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));
            }

            // Record into Search History
            if (query != null && !query.trim().isEmpty()) {
                SearchHistoryItem item = new SearchHistoryItem();
                item.setUserId(userId);
                item.setSearchQuery(query.trim());
                item.setSearchMode(searchMode != null ? searchMode : "All keywords");
                item.setResultCount(results.size());
                historyDAO.addHistory(item);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during document search", e);
        }

        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("Search executed in " + duration + "ms, returned " + results.size() + " results.");
        return results;
    }

    /**
     * Calculates a normalized relevance score from 0.0 to 100.0.
     * Weights: Exact filename match (100 pts), Filename contains query (40 pts),
     * Content exact phrase (30 pts), Content keyword occurrences (up to 20 pts).
     */
    public double calculateRelevance(DocumentRecord doc, String query, String searchMode) {
        if (query == null || query.trim().isEmpty()) {
            return 50.0; // default baseline for filter-only browse
        }

        String q = query.trim().toLowerCase();
        String name = doc.getFileName() != null ? doc.getFileName().toLowerCase() : "";
        String text = doc.getExtractedText() != null ? doc.getExtractedText().toLowerCase() : "";

        double score = 0;

        // Exact filename match
        if (name.equals(q) || name.startsWith(q + ".")) {
            score += 50.0;
        } else if (name.contains(q)) {
            score += 30.0;
        }

        // Keywords in filename
        String[] terms = q.split("\\s+");
        int nameHits = 0;
        for (String term : terms) {
            if (!term.isEmpty() && name.contains(term)) {
                nameHits++;
            }
        }
        score += (nameHits * 10.0);

        // Content matches
        if (!text.isEmpty()) {
            if (text.contains(q)) {
                score += 25.0;
            }
            int contentHits = 0;
            for (String term : terms) {
                if (!term.isEmpty()) {
                    int count = countOccurrences(text, term);
                    contentHits += Math.min(count, 5); // cap per term
                }
            }
            score += Math.min(contentHits * 3.0, 20.0);
        }

        return Math.min(100.0, Math.max(5.0, score));
    }

    private int countOccurrences(String text, String term) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(term, idx)) != -1) {
            count++;
            idx += term.length();
            if (count > 20) break;
        }
        return count;
    }

    /**
     * Extracts a contextual snippet of text surrounding the match.
     */
    public String generateSnippet(DocumentRecord doc, String query) {
        String text = doc.getExtractedText();
        if (text == null || text.isBlank()) {
            return "[No text content extracted]";
        }

        if (query == null || query.trim().isEmpty()) {
            return text.length() > 200 ? text.substring(0, 200).replace("\n", " ") + "..." : text.replace("\n", " ");
        }

        String[] terms = query.trim().split("\\s+");
        String lowerText = text.toLowerCase();

        int firstMatchIdx = -1;
        int matchedTermLen = 0;

        // Find earliest term occurrence
        for (String term : terms) {
            if (term.isEmpty()) continue;
            int idx = lowerText.indexOf(term.toLowerCase());
            if (idx != -1 && (firstMatchIdx == -1 || idx < firstMatchIdx)) {
                firstMatchIdx = idx;
                matchedTermLen = term.length();
            }
        }

        if (firstMatchIdx == -1) {
            // No content match, match was probably on filename
            return text.length() > 200 ? text.substring(0, 200).replace("\n", " ") + "..." : text.replace("\n", " ");
        }

        int snippetRadius = 80;
        int start = Math.max(0, firstMatchIdx - snippetRadius);
        int end = Math.min(text.length(), firstMatchIdx + matchedTermLen + snippetRadius);

        String snippet = text.substring(start, end).replace("\r", " ").replace("\n", " ").trim();
        String prefix = start > 0 ? "..." : "";
        String suffix = end < text.length() ? "..." : "";

        return prefix + snippet + suffix;
    }
}
