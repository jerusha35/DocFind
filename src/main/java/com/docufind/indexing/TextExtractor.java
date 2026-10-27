package com.docufind.indexing;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extracts plain text from supported document types with safe buffering and encoding detection.
 */
public class TextExtractor {
    private static final Logger LOGGER = Logger.getLogger(TextExtractor.class.getName());

    public static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
        ".txt", ".md", ".java", ".log", ".csv", ".properties", ".xml", ".json", ".html", ".htm", ".sql"
    ));

    /**
     * Checks if the file extension is supported for text extraction.
     */
    public static boolean isSupported(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return false;
        }
        String ext = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(ext);
    }

    /**
     * Extracts text content safely using UTF-8 with ISO-8859-1 fallback.
     * Imposes a sensible max character limit to prevent out-of-memory errors on massive logs.
     */
    public static String extractText(Path filePath, int maxChars) throws IOException {
        long fileSize = Files.size(filePath);
        // Do not process empty files
        if (fileSize == 0) {
            return "";
        }

        // Try UTF-8 first
        try {
            return readBounded(filePath, StandardCharsets.UTF_8, maxChars);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "UTF-8 read failed for " + filePath + ", trying ISO-8859-1 fallback");
            return readBounded(filePath, StandardCharsets.ISO_8859_1, maxChars);
        }
    }

    private static String readBounded(Path filePath, Charset charset, int maxChars) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(filePath, charset)) {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                if (sb.length() + read > maxChars) {
                    sb.append(buffer, 0, maxChars - sb.length());
                    break;
                }
                sb.append(buffer, 0, read);
            }
        }
        return sb.toString();
    }
}
