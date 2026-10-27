package com.docufind.security;

import com.docufind.dao.UserDAO;
import com.docufind.model.User;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthService {
    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());
    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Authenticates a user against Oracle SQL.
     */
    public boolean login(String usernameOrEmail, char[] password) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank() || password == null || password.length == 0) {
            return false;
        }

        try {
            Optional<User> userOpt = userDAO.findByUsername(usernameOrEmail.trim());
            if (userOpt.isEmpty()) {
                userOpt = userDAO.findByEmail(usernameOrEmail.trim());
            }

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (!user.isActive()) {
                    LOGGER.warning("Authentication failed: User account is deactivated - " + user.getUsername());
                    return false;
                }

                boolean matches = PasswordHasher.verifyPassword(password, user.getPasswordHash(), user.getPasswordSalt());
                if (matches) {
                    UserSession.setCurrentUser(user);
                    LOGGER.info("User logged in successfully: " + user.getUsername() + " (Role: " + user.getRole() + ")");
                    return true;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during login authentication", e);
        }
        return false;
    }

    /**
     * Registers a new user account.
     * The very first user registered in the system is automatically assigned the ADMIN role.
     */
    public String registerUser(String username, String email, char[] password, char[] confirmPassword) {
        if (username == null || username.trim().length() < 3) {
            return "Username must be at least 3 characters long.";
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return "Please provide a valid email address.";
        }
        if (password == null || password.length < 6) {
            return "Password must be at least 6 characters long.";
        }
        if (!java.util.Arrays.equals(password, confirmPassword)) {
            return "Passwords do not match.";
        }

        try {
            if (userDAO.findByUsername(username.trim()).isPresent()) {
                return "Username is already taken.";
            }
            if (userDAO.findByEmail(email.trim()).isPresent()) {
                return "Email is already registered.";
            }

            int existingUserCount = userDAO.countUsers();
            String role = (existingUserCount == 0) ? "ADMIN" : "USER";

            String salt = PasswordHasher.generateSalt();
            String hash = PasswordHasher.hashPassword(password, salt);

            User user = new User();
            user.setUsername(username.trim());
            user.setEmail(email.trim());
            user.setPasswordHash(hash);
            user.setPasswordSalt(salt);
            user.setRole(role);
            user.setActive(true);

            boolean success = userDAO.createUser(user);
            if (success) {
                LOGGER.info("Registered new user: " + username + " with role: " + role);
                return null; // null represents success
            } else {
                return "Failed to save user in database.";
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during user registration", e);
            return "Database error: " + e.getMessage();
        }
    }

    public void logout() {
        if (UserSession.isLoggedIn()) {
            LOGGER.info("Logging out user: " + UserSession.getCurrentUser().getUsername());
        }
        UserSession.clear();
    }
}
