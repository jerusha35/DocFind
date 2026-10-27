package com.docufind.dao;

import com.docufind.db.DBConnection;
import com.docufind.model.IndexingLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IndexingLogDAO {

    public boolean log(IndexingLog logItem) throws SQLException {
        String sql = "INSERT INTO INDEXING_LOGS (LOG_ID, USER_ID, FOLDER_ID, FILE_PATH, STATUS, ERROR_MESSAGE) " +
                     "VALUES (SEQ_INDEXING_LOGS.NEXTVAL, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, logItem.getUserId());
            if (logItem.getFolderId() != null && logItem.getFolderId() > 0) {
                ps.setLong(2, logItem.getFolderId());
            } else {
                ps.setNull(2, Types.NUMERIC);
            }
            ps.setString(3, logItem.getFilePath());
            ps.setString(4, logItem.getStatus());
            ps.setString(5, logItem.getErrorMessage());
            return ps.executeUpdate() > 0;
        }
    }

    public List<IndexingLog> getLogsByUserId(long userId, int limit) throws SQLException {
        List<IndexingLog> list = new ArrayList<>();
        String sql = "SELECT LOG_ID, USER_ID, FOLDER_ID, FILE_PATH, STATUS, ERROR_MESSAGE, LOGGED_AT " +
                     "FROM (SELECT * FROM INDEXING_LOGS WHERE USER_ID = ? ORDER BY LOGGED_AT DESC) WHERE ROWNUM <= ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit > 0 ? limit : 100);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    IndexingLog item = new IndexingLog();
                    item.setLogId(rs.getLong("LOG_ID"));
                    item.setUserId(rs.getLong("USER_ID"));
                    long fId = rs.getLong("FOLDER_ID");
                    item.setFolderId(rs.wasNull() ? null : fId);
                    item.setFilePath(rs.getString("FILE_PATH"));
                    item.setStatus(rs.getString("STATUS"));
                    item.setErrorMessage(rs.getString("ERROR_MESSAGE"));
                    item.setLoggedAt(rs.getTimestamp("LOGGED_AT"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    public List<IndexingLog> getAllLogsForAdmin(int limit) throws SQLException {
        List<IndexingLog> list = new ArrayList<>();
        String sql = "SELECT LOG_ID, USER_ID, FOLDER_ID, FILE_PATH, STATUS, ERROR_MESSAGE, LOGGED_AT " +
                     "FROM (SELECT * FROM INDEXING_LOGS ORDER BY LOGGED_AT DESC) WHERE ROWNUM <= ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 200);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    IndexingLog item = new IndexingLog();
                    item.setLogId(rs.getLong("LOG_ID"));
                    item.setUserId(rs.getLong("USER_ID"));
                    long fId = rs.getLong("FOLDER_ID");
                    item.setFolderId(rs.wasNull() ? null : fId);
                    item.setFilePath(rs.getString("FILE_PATH"));
                    item.setStatus(rs.getString("STATUS"));
                    item.setErrorMessage(rs.getString("ERROR_MESSAGE"));
                    item.setLoggedAt(rs.getTimestamp("LOGGED_AT"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    public int countTotalErrors() throws SQLException {
        String sql = "SELECT COUNT(*) FROM INDEXING_LOGS WHERE STATUS = 'ERROR'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
