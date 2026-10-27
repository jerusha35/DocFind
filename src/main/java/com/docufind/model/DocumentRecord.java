package com.docufind.model;

import java.sql.Timestamp;

public class DocumentRecord {
    private long documentId;
    private long userId;
    private long folderId;
    private String fileName;
    private String filePath;
    private String fileExtension;
    private long fileSize;
    private Timestamp lastModified;
    private String sha256Hash;
    private String extractedText;
    private Timestamp indexedAt;
    private String indexStatus;

    // Search and display helper fields
    private double relevanceScore;
    private String matchSnippet;

    public DocumentRecord() {}

    public DocumentRecord(long documentId, long userId, long folderId, String fileName, String filePath, 
                          String fileExtension, long fileSize, Timestamp lastModified, String sha256Hash, 
                          String extractedText, Timestamp indexedAt, String indexStatus) {
        this.documentId = documentId;
        this.userId = userId;
        this.folderId = folderId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileExtension = fileExtension;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.sha256Hash = sha256Hash;
        this.extractedText = extractedText;
        this.indexedAt = indexedAt;
        this.indexStatus = indexStatus;
    }

    public long getDocumentId() { return documentId; }
    public void setDocumentId(long documentId) { this.documentId = documentId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public long getFolderId() { return folderId; }
    public void setFolderId(long folderId) { this.folderId = folderId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public Timestamp getLastModified() { return lastModified; }
    public void setLastModified(Timestamp lastModified) { this.lastModified = lastModified; }

    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public Timestamp getIndexedAt() { return indexedAt; }
    public void setIndexedAt(Timestamp indexedAt) { this.indexedAt = indexedAt; }

    public String getIndexStatus() { return indexStatus; }
    public void setIndexStatus(String indexStatus) { this.indexStatus = indexStatus; }

    public double getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }

    public String getMatchSnippet() { return matchSnippet; }
    public void setMatchSnippet(String matchSnippet) { this.matchSnippet = matchSnippet; }
}
