package com.docufind.security;

import com.docufind.model.User;

/**
 * Holds the current logged-in user session.
 */
public class UserSession {
    private static User currentUser;

    public static synchronized void setCurrentUser(User user) {
        currentUser = user;
    }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public static synchronized boolean isAdmin() {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
    }

    public static synchronized void clear() {
        currentUser = null;
    }
}
