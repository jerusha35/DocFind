package com.docufind.model;

import java.sql.Timestamp;

public class SavedSearch {
    private long searchId;
    private long userId;
    private String searchName;
    private String searchQuery;
    private String searchMode;
    private String filterSettings;
    private Timestamp createdAt;

    public SavedSearch() {}

    public SavedSearch(long searchId, long userId, String searchName, String searchQuery, String searchMode, String filterSettings, Timestamp createdAt) {
        this.searchId = searchId;
        this.userId = userId;
        this.searchName = searchName;
        this.searchQuery = searchQuery;
        this.searchMode = searchMode;
        this.filterSettings = filterSettings;
        this.createdAt = createdAt;
    }

    public long getSearchId() { return searchId; }
    public void setSearchId(long searchId) { this.searchId = searchId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getSearchName() { return searchName; }
    public void setSearchName(String searchName) { this.searchName = searchName; }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }

    public String getSearchMode() { return searchMode; }
    public void setSearchMode(String searchMode) { this.searchMode = searchMode; }

    public String getFilterSettings() { return filterSettings; }
    public void setFilterSettings(String filterSettings) { this.filterSettings = filterSettings; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
