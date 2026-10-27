package com.docufind;

import com.docufind.db.DBConnection;
import com.docufind.ui.LoginFrame;
import com.docufind.ui.MainFrame;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

// java -cp "bin;lib/ojdbc8.jar" com.docufind.Main (for code run)

/**
 * Main application entry point for DocuFind.
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Set modern Windows Look and Feel if available
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "System look and feel not available, continuing with default.", e);
        }

        // Initialize Oracle DB schema in background
        new Thread(() -> {
            try {
                LOGGER.info("Checking and initializing Oracle database schema...");
                DBConnection.initializeSchema();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Schema check deferred or failed: " + e.getMessage());
            }
        }).start();

        // Launch GUI on Swing Event Dispatch Thread
        SwingUtilities.invokeLater(() -> launchLogin());
    }

    private static void launchLogin() {
        LoginFrame loginFrame = new LoginFrame(() -> {
            // Callback upon successful authentication
            MainFrame mainFrame = new MainFrame(() -> {
                // Callback upon logout
                launchLogin();
            });
            mainFrame.setVisible(true);
        });
        loginFrame.setVisible(true);
    }
}
