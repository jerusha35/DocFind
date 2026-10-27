package com.docufind.dao;

import com.docufind.db.DBConnection;
import com.docufind.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    public boolean createUser(User user) throws SQLException {
        String sql = "INSERT INTO APP_USERS (USER_ID, USERNAME, EMAIL, PASSWORD_HASH, PASSWORD_SALT, ROLE, IS_ACTIVE) " +
                     "VALUES (SEQ_APP_USERS.NEXTVAL, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getPasswordSalt());
            ps.setString(5, user.getRole() != null ? user.getRole() : "USER");
            ps.setInt(6, user.isActive() ? 1 : 0);
            return ps.executeUpdate() > 0;
        }
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT USER_ID, USERNAME, EMAIL, PASSWORD_HASH, PASSWORD_SALT, ROLE, CREATED_AT, IS_ACTIVE " +
                     "FROM APP_USERS WHERE LOWER(USERNAME) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT USER_ID, USERNAME, EMAIL, PASSWORD_HASH, PASSWORD_SALT, ROLE, CREATED_AT, IS_ACTIVE " +
                     "FROM APP_USERS WHERE LOWER(EMAIL) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public int countUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM APP_USERS";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public List<User> findAllUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT USER_ID, USERNAME, EMAIL, PASSWORD_HASH, PASSWORD_SALT, ROLE, CREATED_AT, IS_ACTIVE " +
                     "FROM APP_USERS ORDER BY CREATED_AT DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        }
        return list;
    }

    public boolean updateUserStatus(long userId, boolean active) throws SQLException {
        String sql = "UPDATE APP_USERS SET IS_ACTIVE = ? WHERE USER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getLong("USER_ID"));
        user.setUsername(rs.getString("USERNAME"));
        user.setEmail(rs.getString("EMAIL"));
        user.setPasswordHash(rs.getString("PASSWORD_HASH"));
        user.setPasswordSalt(rs.getString("PASSWORD_SALT"));
        user.setRole(rs.getString("ROLE"));
        user.setCreatedAt(rs.getTimestamp("CREATED_AT"));
        user.setActive(rs.getInt("IS_ACTIVE") == 1);
        return user;
    }
}
