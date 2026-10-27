package com.docufind.dao;

import com.docufind.db.DBConnection;
import com.docufind.model.SearchHistoryItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryDAO {

    public boolean addHistory(SearchHistoryItem item) throws SQLException {
        String sql = "INSERT INTO SEARCH_HISTORY (HISTORY_ID, USER_ID, SEARCH_QUERY, SEARCH_MODE, RESULT_COUNT) " +
                     "VALUES (SEQ_SEARCH_HISTORY.NEXTVAL, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, item.getUserId());
            ps.setString(2, item.getSearchQuery());
            ps.setString(3, item.getSearchMode());
            ps.setInt(4, item.getResultCount());
            return ps.executeUpdate() > 0;
        }
    }

    public List<SearchHistoryItem> getHistoryByUserId(long userId, int limit) throws SQLException {
        List<SearchHistoryItem> list = new ArrayList<>();
        String sql = "SELECT HISTORY_ID, USER_ID, SEARCH_QUERY, SEARCH_MODE, RESULT_COUNT, SEARCHED_AT " +
                     "FROM (SELECT * FROM SEARCH_HISTORY WHERE USER_ID = ? ORDER BY SEARCHED_AT DESC) WHERE ROWNUM <= ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit > 0 ? limit : 100);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SearchHistoryItem h = new SearchHistoryItem();
                    h.setHistoryId(rs.getLong("HISTORY_ID"));
                    h.setUserId(rs.getLong("USER_ID"));
                    h.setSearchQuery(rs.getString("SEARCH_QUERY"));
                    h.setSearchMode(rs.getString("SEARCH_MODE"));
                    h.setResultCount(rs.getInt("RESULT_COUNT"));
                    h.setSearchedAt(rs.getTimestamp("SEARCHED_AT"));
                    list.add(h);
                }
            }
        }
        return list;
    }

    public int countSearchesByUserId(long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM SEARCH_HISTORY WHERE USER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public boolean clearHistoryByUserId(long userId) throws SQLException {
        String sql = "DELETE FROM SEARCH_HISTORY WHERE USER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate() >= 0;
        }
    }
}
