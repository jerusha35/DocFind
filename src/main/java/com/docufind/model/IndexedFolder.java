package com.docufind.model;

import java.sql.Timestamp;

public class IndexedFolder {
    private long folderId;
    private long userId;
    private String folderPath;
    private Timestamp addedAt;
    private Timestamp lastIndexedAt;
    private String indexStatus;
    private int documentCount; // aggregated/helper field

    public IndexedFolder() {}

    public IndexedFolder(long folderId, long userId, String folderPath, Timestamp addedAt, Timestamp lastIndexedAt, String indexStatus) {
        this.folderId = folderId;
        this.userId = userId;
        this.folderPath = folderPath;
        this.addedAt = addedAt;
        this.lastIndexedAt = lastIndexedAt;
        this.indexStatus = indexStatus;
    }

    public long getFolderId() { return folderId; }
    public void setFolderId(long folderId) { this.folderId = folderId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getFolderPath() { return folderPath; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }

    public Timestamp getAddedAt() { return addedAt; }
    public void setAddedAt(Timestamp addedAt) { this.addedAt = addedAt; }

    public Timestamp getLastIndexedAt() { return lastIndexedAt; }
    public void setLastIndexedAt(Timestamp lastIndexedAt) { this.lastIndexedAt = lastIndexedAt; }

    public String getIndexStatus() { return indexStatus; }
    public void setIndexStatus(String indexStatus) { this.indexStatus = indexStatus; }

    public int getDocumentCount() { return documentCount; }
    public void setDocumentCount(int documentCount) { this.documentCount = documentCount; }
}
