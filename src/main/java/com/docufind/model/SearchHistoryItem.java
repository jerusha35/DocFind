package com.docufind.model;

import java.sql.Timestamp;

public class SearchHistoryItem {
    private long historyId;
    private long userId;
    private String searchQuery;
    private String searchMode;
    private int resultCount;
    private Timestamp searchedAt;

    public SearchHistoryItem() {}

    public SearchHistoryItem(long historyId, long userId, String searchQuery, String searchMode, int resultCount, Timestamp searchedAt) {
        this.historyId = historyId;
        this.userId = userId;
        this.searchQuery = searchQuery;
        this.searchMode = searchMode;
        this.resultCount = resultCount;
        this.searchedAt = searchedAt;
    }

    public long getHistoryId() { return historyId; }
    public void setHistoryId(long historyId) { this.historyId = historyId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }

    public String getSearchMode() { return searchMode; }
    public void setSearchMode(String searchMode) { this.searchMode = searchMode; }

    public int getResultCount() { return resultCount; }
    public void setResultCount(int resultCount) { this.resultCount = resultCount; }

    public Timestamp getSearchedAt() { return searchedAt; }
    public void setSearchedAt(Timestamp searchedAt) { this.searchedAt = searchedAt; }
}
