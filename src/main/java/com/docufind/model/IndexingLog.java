package com.docufind.model;

import java.sql.Timestamp;

public class IndexingLog {
    private long logId;
    private long userId;
    private Long folderId;
    private String filePath;
    private String status;
    private String errorMessage;
    private Timestamp loggedAt;

    public IndexingLog() {}

    public IndexingLog(long logId, long userId, Long folderId, String filePath, String status, String errorMessage, Timestamp loggedAt) {
        this.logId = logId;
        this.userId = userId;
        this.folderId = folderId;
        this.filePath = filePath;
        this.status = status;
        this.errorMessage = errorMessage;
        this.loggedAt = loggedAt;
    }

    public long getLogId() { return logId; }
    public void setLogId(long logId) { this.logId = logId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Timestamp getLoggedAt() { return loggedAt; }
    public void setLoggedAt(Timestamp loggedAt) { this.loggedAt = loggedAt; }
}
