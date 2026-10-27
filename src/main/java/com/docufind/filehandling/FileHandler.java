package com.docufind.filehandling;

import com.docufind.model.DocumentRecord;
import com.docufind.model.IndexingLog;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileHandler {

    /**
     * Sanitizes a cell value to prevent CSV Formula Injection (=, +, -, @)
     * and escapes quotation marks.
     */
    public static String escapeCsv(String value) {
        if (value == null) {
            return "\"\"";
        }

        String str = value;
        // Formula injection protection: if starts with =, +, -, @, prepend a single quote
        if (!str.isEmpty() && (str.startsWith("=") || str.startsWith("+") || str.startsWith("-") || str.startsWith("@"))) {
            str = "'" + str;
        }

        // Escape double quotes by doubling them
        str = str.replace("\"", "\"\"");
        return "\"" + str + "\"";
    }

    /**
     * Exports a list of search result documents to a CSV file.
     */
    public static void exportSearchResultsToCsv(List<DocumentRecord> docs, Path targetPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8)) {
            // Write BOM for Excel UTF-8 compatibility
            writer.write('\ufeff');

            // Header
            writer.write("Document ID,File Name,File Extension,File Size (Bytes),Last Modified,Relevance Score,File Path,Snippet");
            writer.newLine();

            for (DocumentRecord doc : docs) {
                StringBuilder line = new StringBuilder();
                line.append(doc.getDocumentId()).append(",");
                line.append(escapeCsv(doc.getFileName())).append(",");
                line.append(escapeCsv(doc.getFileExtension())).append(",");
                line.append(doc.getFileSize()).append(",");
                line.append(escapeCsv(doc.getLastModified() != null ? doc.getLastModified().toString() : "")).append(",");
                line.append(String.format(java.util.Locale.US, "%.1f", doc.getRelevanceScore())).append(",");
                line.append(escapeCsv(doc.getFilePath())).append(",");
                line.append(escapeCsv(doc.getMatchSnippet()));
                writer.write(line.toString());
                writer.newLine();
            }
        }
    }

    /**
     * Exports indexing error logs to a plain text file.
     */
    public static void exportIndexingLogsToText(List<IndexingLog> logs, Path targetPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8)) {
            writer.write("=== DocuFind Indexing Error Logs ===");
            writer.newLine();
            writer.write("Exported at: " + new java.util.Date());
            writer.newLine();
            writer.write("--------------------------------------------------------------------------------");
            writer.newLine();

            for (IndexingLog log : logs) {
                writer.write(String.format("[%s] Status: %s | File: %s",
                        log.getLoggedAt(), log.getStatus(), log.getFilePath()));
                writer.newLine();
                writer.write("Error: " + (log.getErrorMessage() != null ? log.getErrorMessage() : "None"));
                writer.newLine();
                writer.write("--------------------------------------------------------------------------------");
                writer.newLine();
            }
        }
    }

    /**
     * Generates a readable plain-text search report.
     */
    public static void generateSearchReport(String query, String mode, List<DocumentRecord> docs, Path targetPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8)) {
            writer.write("================================================================================");
            writer.newLine();
            writer.write("                      DOCUFIND SEARCH REPORT                                     ");
            writer.newLine();
            writer.write("================================================================================");
            writer.newLine();
            writer.write("Query       : " + query);
            writer.newLine();
            writer.write("Search Mode : " + mode);
            writer.newLine();
            writer.write("Total Found : " + docs.size());
            writer.newLine();
            writer.write("Generated On: " + new java.util.Date());
            writer.newLine();
            writer.write("================================================================================");
            writer.newLine();
            writer.newLine();

            int rank = 1;
            for (DocumentRecord doc : docs) {
                writer.write(String.format("#%d - %s (Score: %.1f%%)", rank++, doc.getFileName(), doc.getRelevanceScore()));
                writer.newLine();
                writer.write("Path     : " + doc.getFilePath());
                writer.newLine();
                writer.write("Size     : " + doc.getFileSize() + " bytes | Modified: " + doc.getLastModified());
                writer.newLine();
                writer.write("Snippet  : " + (doc.getMatchSnippet() != null ? doc.getMatchSnippet() : ""));
                writer.newLine();
                writer.write("--------------------------------------------------------------------------------");
                writer.newLine();
            }
        }
    }
}
