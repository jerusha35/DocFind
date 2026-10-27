package com.docufind.dao;

import com.docufind.db.DBConnection;
import com.docufind.model.IndexedFolder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FolderDAO {

    public long addFolder(long userId, String folderPath) throws SQLException {
        String sql = "INSERT INTO INDEXED_FOLDERS (FOLDER_ID, USER_ID, FOLDER_PATH, INDEX_STATUS) " +
                     "VALUES (SEQ_INDEXED_FOLDERS.NEXTVAL, ?, ?, 'PENDING')";
        String returnSql = "SELECT SEQ_INDEXED_FOLDERS.CURRVAL FROM DUAL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, folderPath);
            ps.executeUpdate();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(returnSql)) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }

    public List<IndexedFolder> getFoldersByUserId(long userId) throws SQLException {
        List<IndexedFolder> list = new ArrayList<>();
        String sql = "SELECT f.FOLDER_ID, f.USER_ID, f.FOLDER_PATH, f.ADDED_AT, f.LAST_INDEXED_AT, f.INDEX_STATUS, " +
                     "(SELECT COUNT(*) FROM DOCUMENTS d WHERE d.FOLDER_ID = f.FOLDER_ID AND d.USER_ID = f.USER_ID) AS DOC_COUNT " +
                     "FROM INDEXED_FOLDERS f WHERE f.USER_ID = ? ORDER BY f.ADDED_AT DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    IndexedFolder f = new IndexedFolder();
                    f.setFolderId(rs.getLong("FOLDER_ID"));
                    f.setUserId(rs.getLong("USER_ID"));
                    f.setFolderPath(rs.getString("FOLDER_PATH"));
                    f.setAddedAt(rs.getTimestamp("ADDED_AT"));
                    f.setLastIndexedAt(rs.getTimestamp("LAST_INDEXED_AT"));
                    f.setIndexStatus(rs.getString("INDEX_STATUS"));
                    f.setDocumentCount(rs.getInt("DOC_COUNT"));
                    list.add(f);
                }
            }
        }
        return list;
    }

    public Optional<IndexedFolder> findById(long folderId, long userId) throws SQLException {
        String sql = "SELECT FOLDER_ID, USER_ID, FOLDER_PATH, ADDED_AT, LAST_INDEXED_AT, INDEX_STATUS, " +
                     "(SELECT COUNT(*) FROM DOCUMENTS d WHERE d.FOLDER_ID = f.FOLDER_ID AND d.USER_ID = f.USER_ID) AS DOC_COUNT " +
                     "FROM INDEXED_FOLDERS f WHERE f.FOLDER_ID = ? AND f.USER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, folderId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    IndexedFolder f = new IndexedFolder();
                    f.setFolderId(rs.getLong("FOLDER_ID"));
                    f.setUserId(rs.getLong("USER_ID"));
                    f.setFolderPath(rs.getString("FOLDER_PATH"));
                    f.setAddedAt(rs.getTimestamp("ADDED_AT"));
                    f.setLastIndexedAt(rs.getTimestamp("LAST_INDEXED_AT"));
                    f.setIndexStatus(rs.getString("INDEX_STATUS"));
                    f.setDocumentCount(rs.getInt("DOC_COUNT"));
                    return Optional.of(f);
                }
            }
        }
        return Optional.empty();
    }

    public boolean updateStatusAndTimestamp(long folderId, long userId, String status) throws SQLException {
        String sql = "UPDATE INDEXED_FOLDERS SET INDEX_STATUS = ?, LAST_INDEXED_AT = CURRENT_TIMESTAMP WHERE FOLDER_ID = ? AND USER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, folderId);
            ps.setLong(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteFolder(long folderId, long userId) throws SQLException {
        String sql = "DELETE FROM INDEXED_FOLDERS WHERE FOLDER_ID = ? AND USER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, folderId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public int countFoldersByUserId(long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INDEXED_FOLDERS WHERE USER_ID = ?";
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

    public Timestamp getLastIndexedTime(long userId) throws SQLException {
        String sql = "SELECT MAX(LAST_INDEXED_AT) FROM INDEXED_FOLDERS WHERE USER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getTimestamp(1);
                }
            }
        }
        return null;
    }
}
