package com.docufind.dao;

import com.docufind.db.DBConnection;
import com.docufind.model.DocumentRecord;

import java.io.StringReader;
import java.sql.*;
import java.util.*;

public class DocumentDAO {

    public boolean upsertDocument(DocumentRecord doc) throws SQLException {
        // Check if document already exists for this user and file path
        String checkSql = "SELECT DOCUMENT_ID FROM DOCUMENTS WHERE USER_ID = ? AND FILE_PATH = ?";
        long existingDocId = -1;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setLong(1, doc.getUserId());
            ps.setString(2, doc.getFilePath());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    existingDocId = rs.getLong("DOCUMENT_ID");
                }
            }
        }

        if (existingDocId > 0) {
            // Update existing
            String updateSql = "UPDATE DOCUMENTS SET FOLDER_ID = ?, FILE_NAME = ?, FILE_EXTENSION = ?, FILE_SIZE = ?, " +
                               "LAST_MODIFIED = ?, SHA256_HASH = ?, EXTRACTED_TEXT = ?, INDEXED_AT = CURRENT_TIMESTAMP, INDEX_STATUS = 'INDEXED' " +
                               "WHERE DOCUMENT_ID = ? AND USER_ID = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setLong(1, doc.getFolderId());
                ps.setString(2, doc.getFileName());
                ps.setString(3, doc.getFileExtension());
                ps.setLong(4, doc.getFileSize());
                ps.setTimestamp(5, doc.getLastModified());
                ps.setString(6, doc.getSha256Hash());
                if (doc.getExtractedText() != null) {
                    ps.setCharacterStream(7, new StringReader(doc.getExtractedText()));
                } else {
                    ps.setNull(7, Types.CLOB);
                }
                ps.setLong(8, existingDocId);
                ps.setLong(9, doc.getUserId());
                return ps.executeUpdate() > 0;
            }
        } else {
            // Insert new
            String insertSql = "INSERT INTO DOCUMENTS (DOCUMENT_ID, USER_ID, FOLDER_ID, FILE_NAME, FILE_PATH, " +
                               "FILE_EXTENSION, FILE_SIZE, LAST_MODIFIED, SHA256_HASH, EXTRACTED_TEXT, INDEXED_AT, INDEX_STATUS) " +
                               "VALUES (SEQ_DOCUMENTS.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 'INDEXED')";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setLong(1, doc.getUserId());
                ps.setLong(2, doc.getFolderId());
                ps.setString(3, doc.getFileName());
                ps.setString(4, doc.getFilePath());
                ps.setString(5, doc.getFileExtension());
                ps.setLong(6, doc.getFileSize());
                ps.setTimestamp(7, doc.getLastModified());
                ps.setString(8, doc.getSha256Hash());
                if (doc.getExtractedText() != null) {
                    ps.setCharacterStream(9, new StringReader(doc.getExtractedText()));
                } else {
                    ps.setNull(9, Types.CLOB);
                }
                return ps.executeUpdate() > 0;
            }
        }
    }

    public List<String> getIndexedFilePathsInFolder(long userId, long folderId) throws SQLException {
        List<String> paths = new ArrayList<>();
        String sql = "SELECT FILE_PATH FROM DOCUMENTS WHERE USER_ID = ? AND FOLDER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, folderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    paths.add(rs.getString("FILE_PATH"));
                }
            }
        }
        return paths;
    }

    public boolean markOrRemoveStaleDocument(long userId, String filePath) throws SQLException {
        String sql = "DELETE FROM DOCUMENTS WHERE USER_ID = ? AND FILE_PATH = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, filePath);
            return ps.executeUpdate() > 0;
        }
    }

    public int countDocumentsByUserId(long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM DOCUMENTS WHERE USER_ID = ?";
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

    public List<DocumentRecord> getRecentDocuments(long userId, int limit) throws SQLException {
        List<DocumentRecord> list = new ArrayList<>();
        String sql = "SELECT DOCUMENT_ID, USER_ID, FOLDER_ID, FILE_NAME, FILE_PATH, FILE_EXTENSION, FILE_SIZE, " +
                     "LAST_MODIFIED, SHA256_HASH, INDEXED_AT, INDEX_STATUS " +
                     "FROM (SELECT * FROM DOCUMENTS WHERE USER_ID = ? ORDER BY LAST_MODIFIED DESC) WHERE ROWNUM <= ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DocumentRecord doc = new DocumentRecord();
                    doc.setDocumentId(rs.getLong("DOCUMENT_ID"));
                    doc.setUserId(rs.getLong("USER_ID"));
                    doc.setFolderId(rs.getLong("FOLDER_ID"));
                    doc.setFileName(rs.getString("FILE_NAME"));
                    doc.setFilePath(rs.getString("FILE_PATH"));
                    doc.setFileExtension(rs.getString("FILE_EXTENSION"));
                    doc.setFileSize(rs.getLong("FILE_SIZE"));
                    doc.setLastModified(rs.getTimestamp("LAST_MODIFIED"));
                    doc.setSha256Hash(rs.getString("SHA256_HASH"));
                    doc.setIndexedAt(rs.getTimestamp("INDEXED_AT"));
                    doc.setIndexStatus(rs.getString("INDEX_STATUS"));
                    list.add(doc);
                }
            }
        }
        return list;
    }

    public Optional<DocumentRecord> getDocumentWithContent(long documentId, long userId) throws SQLException {
        String sql = "SELECT DOCUMENT_ID, USER_ID, FOLDER_ID, FILE_NAME, FILE_PATH, FILE_EXTENSION, FILE_SIZE, " +
                     "LAST_MODIFIED, SHA256_HASH, EXTRACTED_TEXT, INDEXED_AT, INDEX_STATUS " +
                     "FROM DOCUMENTS WHERE DOCUMENT_ID = ? AND USER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, documentId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DocumentRecord doc = new DocumentRecord();
                    doc.setDocumentId(rs.getLong("DOCUMENT_ID"));
                    doc.setUserId(rs.getLong("USER_ID"));
                    doc.setFolderId(rs.getLong("FOLDER_ID"));
                    doc.setFileName(rs.getString("FILE_NAME"));
                    doc.setFilePath(rs.getString("FILE_PATH"));
                    doc.setFileExtension(rs.getString("FILE_EXTENSION"));
                    doc.setFileSize(rs.getLong("FILE_SIZE"));
                    doc.setLastModified(rs.getTimestamp("LAST_MODIFIED"));
                    doc.setSha256Hash(rs.getString("SHA256_HASH"));
                    doc.setExtractedText(rs.getString("EXTRACTED_TEXT"));
                    doc.setIndexedAt(rs.getTimestamp("INDEXED_AT"));
                    doc.setIndexStatus(rs.getString("INDEX_STATUS"));
                    return Optional.of(doc);
                }
            }
        }
        return Optional.empty();
    }

    public List<DocumentRecord> searchDocuments(long userId, String query, String searchMode,
                                               String extensionFilter, Long folderId,
                                               java.util.Date modifiedAfter, String sortBy, int maxResults) throws SQLException {
        List<DocumentRecord> results = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DOCUMENT_ID, USER_ID, FOLDER_ID, FILE_NAME, FILE_PATH, FILE_EXTENSION, FILE_SIZE, ")
           .append("LAST_MODIFIED, SHA256_HASH, EXTRACTED_TEXT, INDEXED_AT, INDEX_STATUS ")
           .append("FROM DOCUMENTS WHERE USER_ID = ? ");

        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (extensionFilter != null && !extensionFilter.isBlank() && !extensionFilter.equalsIgnoreCase("All")) {
            sql.append("AND LOWER(FILE_EXTENSION) = LOWER(?) ");
            params.add(extensionFilter.startsWith(".") ? extensionFilter : "." + extensionFilter);
        }

        if (folderId != null && folderId > 0) {
            sql.append("AND FOLDER_ID = ? ");
            params.add(folderId);
        }

        if (modifiedAfter != null) {
            sql.append("AND LAST_MODIFIED >= ? ");
            params.add(new Timestamp(modifiedAfter.getTime()));
        }

        // Apply textual conditions based on search mode
        if (query != null && !query.trim().isEmpty()) {
            String cleanQuery = query.trim().toLowerCase();
            switch (searchMode) {
                case "Filename only":
                    sql.append("AND LOWER(FILE_NAME) LIKE ? ");
                    params.add("%" + cleanQuery + "%");
                    break;
                case "Exact phrase":
                    sql.append("AND (LOWER(FILE_NAME) LIKE ? OR LOWER(EXTRACTED_TEXT) LIKE ?) ");
                    params.add("%" + cleanQuery + "%");
                    params.add("%" + cleanQuery + "%");
                    break;
                case "All keywords":
                    String[] allWords = cleanQuery.split("\\s+");
                    for (String w : allWords) {
                        if (!w.isEmpty()) {
                            sql.append("AND (LOWER(FILE_NAME) LIKE ? OR LOWER(EXTRACTED_TEXT) LIKE ?) ");
                            params.add("%" + w + "%");
                            params.add("%" + w + "%");
                        }
                    }
                    break;
                case "Any keyword":
                default:
                    String[] anyWords = cleanQuery.split("\\s+");
                    sql.append("AND (");
                    boolean first = true;
                    for (String w : anyWords) {
                        if (!w.isEmpty()) {
                            if (!first) sql.append(" OR ");
                            sql.append("LOWER(FILE_NAME) LIKE ? OR LOWER(EXTRACTED_TEXT) LIKE ?");
                            params.add("%" + w + "%");
                            params.add("%" + w + "%");
                            first = false;
                        }
                    }
                    if (first) {
                        // In case of all blank words
                        sql.append("1=1");
                    }
                    sql.append(") ");
                    break;
            }
        }

        // Sorting
        if ("Date Modified (Newest)".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY LAST_MODIFIED DESC ");
        } else if ("Date Modified (Oldest)".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY LAST_MODIFIED ASC ");
        } else if ("Filename (A-Z)".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY FILE_NAME ASC ");
        } else if ("Filename (Z-A)".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY FILE_NAME DESC ");
        } else if ("File Size (Largest)".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY FILE_SIZE DESC ");
        } else {
            // Default Relevance / Recent
            sql.append("ORDER BY LAST_MODIFIED DESC ");
        }

        // Limit results
        String boundedSql = "SELECT * FROM (" + sql.toString() + ") WHERE ROWNUM <= ?";
        params.add(maxResults > 0 ? maxResults : 500);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(boundedSql)) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Long) {
                    ps.setLong(i + 1, (Long) p);
                } else if (p instanceof Integer) {
                    ps.setInt(i + 1, (Integer) p);
                } else if (p instanceof Timestamp) {
                    ps.setTimestamp(i + 1, (Timestamp) p);
                } else {
                    ps.setString(i + 1, p.toString());
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DocumentRecord doc = new DocumentRecord();
                    doc.setDocumentId(rs.getLong("DOCUMENT_ID"));
                    doc.setUserId(rs.getLong("USER_ID"));
                    doc.setFolderId(rs.getLong("FOLDER_ID"));
                    doc.setFileName(rs.getString("FILE_NAME"));
                    doc.setFilePath(rs.getString("FILE_PATH"));
                    doc.setFileExtension(rs.getString("FILE_EXTENSION"));
                    doc.setFileSize(rs.getLong("FILE_SIZE"));
                    doc.setLastModified(rs.getTimestamp("LAST_MODIFIED"));
                    doc.setSha256Hash(rs.getString("SHA256_HASH"));
                    doc.setExtractedText(rs.getString("EXTRACTED_TEXT"));
                    doc.setIndexedAt(rs.getTimestamp("INDEXED_AT"));
                    doc.setIndexStatus(rs.getString("INDEX_STATUS"));
                    results.add(doc);
                }
            }
        }
        return results;
    }
}
