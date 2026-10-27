package com.docufind.dao;

import com.docufind.db.DBConnection;
import com.docufind.model.SavedSearch;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SavedSearchDAO {

    public boolean createSavedSearch(SavedSearch item) throws SQLException {
        String sql = "INSERT INTO SAVED_SEARCHES (SEARCH_ID, USER_ID, SEARCH_NAME, SEARCH_QUERY, SEARCH_MODE, FILTER_SETTINGS) " +
                     "VALUES (SEQ_SAVED_SEARCHES.NEXTVAL, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, item.getUserId());
            ps.setString(2, item.getSearchName());
            ps.setString(3, item.getSearchQuery());
            ps.setString(4, item.getSearchMode());
            ps.setString(5, item.getFilterSettings());
            return ps.executeUpdate() > 0;
        }
    }

    public List<SavedSearch> getSavedSearchesByUserId(long userId) throws SQLException {
        List<SavedSearch> list = new ArrayList<>();
        String sql = "SELECT SEARCH_ID, USER_ID, SEARCH_NAME, SEARCH_QUERY, SEARCH_MODE, FILTER_SETTINGS, CREATED_AT " +
                     "FROM SAVED_SEARCHES WHERE USER_ID = ? ORDER BY CREATED_AT DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SavedSearch s = new SavedSearch();
                    s.setSearchId(rs.getLong("SEARCH_ID"));
                    s.setUserId(rs.getLong("USER_ID"));
                    s.setSearchName(rs.getString("SEARCH_NAME"));
                    s.setSearchQuery(rs.getString("SEARCH_QUERY"));
                    s.setSearchMode(rs.getString("SEARCH_MODE"));
                    s.setFilterSettings(rs.getString("FILTER_SETTINGS"));
                    s.setCreatedAt(rs.getTimestamp("CREATED_AT"));
                    list.add(s);
                }
            }
        }
        return list;
    }

    public boolean deleteSavedSearch(long searchId, long userId) throws SQLException {
        String sql = "DELETE FROM SAVED_SEARCHES WHERE SEARCH_ID = ? AND USER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, searchId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean renameSavedSearch(long searchId, long userId, String newName) throws SQLException {
        String sql = "UPDATE SAVED_SEARCHES SET SEARCH_NAME = ? WHERE SEARCH_ID = ? AND USER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setLong(2, searchId);
            ps.setLong(3, userId);
            return ps.executeUpdate() > 0;
        }
    }
}
