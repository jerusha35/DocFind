package com.docufind;

import com.docufind.dao.DocumentDAO;
import com.docufind.dao.FolderDAO;
import com.docufind.indexing.IndexingService;
import com.docufind.model.DocumentRecord;
import com.docufind.search.SearchService;
import com.docufind.security.AuthService;
import com.docufind.security.UserSession;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class EndToEndWorkflowTest {
    public static void main(String[] args) {
        System.out.println("=== Starting End-to-End Workflow Test ===");
        try {
            AuthService authService = new AuthService();

            // 1. Register test user
            String username = "testuser_" + System.currentTimeMillis();
            String email = username + "@example.com";
            char[] pass = "Password123!".toCharArray();

            System.out.println("Registering test user: " + username);
            String regErr = authService.registerUser(username, email, pass, pass);
            if (regErr != null) {
                System.err.println("Registration failed: " + regErr);
                System.exit(1);
            }
            System.out.println("User registered successfully.");

            // 2. Login
            boolean loginOk = authService.login(username, pass);
            if (!loginOk) {
                System.err.println("Login failed for: " + username);
                System.exit(1);
            }
            System.out.println("Login successful. Active User ID: " + UserSession.getCurrentUser().getUserId() + " (Role: " + UserSession.getCurrentUser().getRole() + ")");
            long userId = UserSession.getCurrentUser().getUserId();

            // 3. Create a temporary folder with sample documents
            Path tempDir = Files.createTempDirectory("docufind_sample_docs_");
            System.out.println("Created test directory on disk: " + tempDir);

            Files.writeString(tempDir.resolve("project_specs.md"), "# Project Specification\nDocuFind is an offline document search engine using Oracle SQL and Java Swing.");
            Files.writeString(tempDir.resolve("database_config.properties"), "oracle.host=localhost\noracle.port=1521\noracle.sid=orcl\n");
            Files.writeString(tempDir.resolve("notes.txt"), "Important memo: Remember to inspect the quarterly financial report and database logs.");

            // 4. Register folder in Oracle SQL
            FolderDAO folderDAO = new FolderDAO();
            long folderId = folderDAO.addFolder(userId, tempDir.toAbsolutePath().toString());
            System.out.println("Registered folder in Oracle with ID: " + folderId);

            // 5. Index the folder using IndexingService
            IndexingService indexingService = new IndexingService();
            System.out.println("Executing indexing on test folder...");
            indexingService.indexFolder(userId, folderId, new IndexingService.IndexingProgressListener() {
                @Override
                public void onProgress(String currentFile, int processedCount, int errorCount) {
                    System.out.println("  Indexed: " + currentFile + " (Total: " + processedCount + ", Errors: " + errorCount + ")");
                }

                @Override
                public void onStatusChange(String statusMessage) {
                    System.out.println("  Status: " + statusMessage);
                }

                @Override
                public void onComplete(int totalProcessed, int totalErrors) {
                    System.out.println("  Indexing Complete: " + totalProcessed + " indexed, " + totalErrors + " errors.");
                }
            });

            // 6. Search the indexed documents
            SearchService searchService = new SearchService();
            System.out.println("\nTesting search for keyword 'DocuFind'...");
            List<DocumentRecord> results1 = searchService.search(userId, "DocuFind", "All keywords", null, null, null, "Relevance", 100);
            System.out.println("Search 'DocuFind' returned " + results1.size() + " document(s):");
            for (DocumentRecord d : results1) {
                System.out.println(" - " + d.getFileName() + " (Score: " + d.getRelevanceScore() + "%) | Snippet: " + d.getMatchSnippet());
            }

            if (results1.isEmpty()) {
                System.err.println("Expected search results for 'DocuFind' but got none!");
                System.exit(1);
            }

            System.out.println("\nTesting search for keyword 'quarterly financial'...");
            List<DocumentRecord> results2 = searchService.search(userId, "quarterly financial", "Exact phrase", null, null, null, "Relevance", 100);
            System.out.println("Search 'quarterly financial' returned " + results2.size() + " document(s):");
            for (DocumentRecord d : results2) {
                System.out.println(" - " + d.getFileName() + " (Score: " + d.getRelevanceScore() + "%) | Snippet: " + d.getMatchSnippet());
            }

            if (results2.isEmpty()) {
                System.err.println("Expected search results for 'quarterly financial' but got none!");
                System.exit(1);
            }

            // Cleanup temp directory files
            for (File f : tempDir.toFile().listFiles()) {
                f.delete();
            }
            tempDir.toFile().delete();

            System.out.println("\n=== All End-to-End Workflow Tests Succeeded! ===");
        } catch (Exception e) {
            System.err.println("End-to-End test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
