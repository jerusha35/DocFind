package com.docufind;

import com.docufind.filehandling.FileHandler;
import com.docufind.model.DocumentRecord;
import com.docufind.search.SearchService;
import com.docufind.security.PasswordHasher;
import com.docufind.util.ChecksumUtil;

import java.nio.file.Files;
import java.nio.file.Path;

public class CoreLogicVerification {
    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;

        System.out.println("=== Running DocuFind Core Logic Unit Verifications ===");

        // Test 1: Password Hashing and Constant-time Verification
        try {
            String salt = PasswordHasher.generateSalt();
            char[] pass = "SecretPass123!".toCharArray();
            String hash = PasswordHasher.hashPassword(pass, salt);

            boolean matchTrue = PasswordHasher.verifyPassword(pass, hash, salt);
            boolean matchFalse = PasswordHasher.verifyPassword("WrongPassword".toCharArray(), hash, salt);

            if (matchTrue && !matchFalse) {
                System.out.println("✅ [PASS] Password PBKDF2WithHmacSHA256 hashing & verification");
                passed++;
            } else {
                System.err.println("❌ [FAIL] Password verification did not match expected values");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("❌ [FAIL] Password hashing threw exception: " + e.getMessage());
            failed++;
        }

        // Test 2: CSV Escaping & Injection Protection
        try {
            String malicious1 = "=cmd|'/C calc'!A0";
            String escaped1 = FileHandler.escapeCsv(malicious1);
            String normalText = "Hello, \"World\"!";
            String escaped2 = FileHandler.escapeCsv(normalText);

            if (escaped1.equals("\"'=cmd|'/C calc'!A0\"") && escaped2.equals("\"Hello, \"\"World\"\"!\"")) {
                System.out.println("✅ [PASS] CSV Escaping and Formula Injection protection");
                passed++;
            } else {
                System.err.println("❌ [FAIL] CSV escaping output mismatch. Got: " + escaped1 + " & " + escaped2);
                failed++;
            }
        } catch (Exception e) {
            System.err.println("❌ [FAIL] CSV escaping threw exception: " + e.getMessage());
            failed++;
        }

        // Test 3: SHA-256 Checksum calculation
        try {
            Path temp = Files.createTempFile("docufind_test", ".txt");
            Files.writeString(temp, "Hello DocuFind Indexer 2026");
            String checksum = ChecksumUtil.computeSHA256(temp);
            Files.deleteIfExists(temp);

            if (checksum != null && checksum.length() == 64) {
                System.out.println("✅ [PASS] SHA-256 Checksum generation (Hash: " + checksum + ")");
                passed++;
            } else {
                System.err.println("❌ [FAIL] SHA-256 checksum invalid");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("❌ [FAIL] SHA-256 computation threw exception: " + e.getMessage());
            failed++;
        }

        // Test 4: Relevance Ranking and Snippet Generation
        try {
            SearchService searchService = new SearchService(null, null);
            DocumentRecord doc = new DocumentRecord();
            doc.setFileName("Invoice_2026_Report.pdf.txt");
            doc.setExtractedText("This is a confidential financial document regarding quarterly invoice billing and audit records.");

            double scoreKeyword = searchService.calculateRelevance(doc, "invoice", "All keywords");
            double scoreMismatch = searchService.calculateRelevance(doc, "irrelevant", "All keywords");
            String snippet = searchService.generateSnippet(doc, "invoice");

            if (scoreKeyword > scoreMismatch && snippet.contains("invoice")) {
                System.out.println("✅ [PASS] Search relevance ranking & text snippet contextual extraction");
                passed++;
            } else {
                System.err.println("❌ [FAIL] Search ranking mismatch (ScoreKeyword=" + scoreKeyword + ", ScoreMismatch=" + scoreMismatch + ")");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("❌ [FAIL] Relevance calculation threw exception: " + e.getMessage());
            failed++;
        }

        System.out.println("======================================================");
        System.out.println("Summary: " + passed + " Passed, " + failed + " Failed.");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
