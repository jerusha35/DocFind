package com.docufind.ui;

import com.docufind.security.AuthService;
import com.docufind.security.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MainFrame extends JFrame {
    private final AuthService authService = new AuthService();
    private final Runnable onLogout;

    private JPanel contentPanel;
    private CardLayout cardLayout;

    // View Panels
    private DashboardPanel dashboardPanel;
    private SearchPanel searchPanel;
    private FoldersPanel foldersPanel;
    private SavedSearchesPanel savedSearchesPanel;
    private HistoryPanel historyPanel;
    private SettingsPanel settingsPanel;
    private AdminPanel adminPanel;

    private final Map<String, JButton> navButtons = new HashMap<>();
    private String currentView = "DASHBOARD";

    public MainFrame(Runnable onLogout) {
        this.onLogout = onLogout;

        setTitle("DocuFind — Offline Personal Search Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 780);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout());

        initComponents();
    }

    private void initComponents() {
        // Left Sidebar Navigation
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UITheme.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(240, 780));
        sidebar.setBorder(new EmptyBorder(24, 16, 24, 16));

        // Brand Title in Sidebar
        JLabel lblBrand = new JLabel("🔍 DocuFind");
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblBrand.setForeground(Color.WHITE);
        lblBrand.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTagline = new JLabel("Offline Personal Engine");
        lblTagline.setFont(UITheme.FONT_SMALL);
        lblTagline.setForeground(new Color(0xA0, 0xAC, 0xC4));
        lblTagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(lblBrand);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(lblTagline);
        sidebar.add(Box.createVerticalStrut(28));

        // Navigation Items
        addNavButton(sidebar, "Dashboard", "DASHBOARD", "📊");
        addNavButton(sidebar, "Search Documents", "SEARCH", "🔍");
        addNavButton(sidebar, "Indexed Folders", "FOLDERS", "📁");
        addNavButton(sidebar, "Saved Searches", "SAVED", "⭐");
        addNavButton(sidebar, "Search History", "HISTORY", "⏱");
        addNavButton(sidebar, "Settings", "SETTINGS", "⚙");

        // Admin Button (visible only if current user is ADMIN)
        if (UserSession.isAdmin()) {
            addNavButton(sidebar, "Admin Panel", "ADMIN", "🛡");
        }

        sidebar.add(Box.createVerticalGlue());

        // User Identity Badge in Sidebar
        JPanel userBadge = new JPanel(new GridLayout(2, 1));
        userBadge.setOpaque(false);
        userBadge.setAlignmentX(Component.LEFT_ALIGNMENT);

        String username = UserSession.getCurrentUser() != null ? UserSession.getCurrentUser().getUsername() : "User";
        String role = UserSession.getCurrentUser() != null ? UserSession.getCurrentUser().getRole() : "USER";

        JLabel lblUser = new JLabel("👤 " + username);
        lblUser.setFont(UITheme.FONT_BOLD);
        lblUser.setForeground(Color.WHITE);

        JLabel lblRole = new JLabel("Role: " + role + " (Offline)");
        lblRole.setFont(UITheme.FONT_SMALL);
        lblRole.setForeground(UITheme.ACCENT_PRIMARY);

        userBadge.add(lblUser);
        userBadge.add(lblRole);

        sidebar.add(userBadge);
        sidebar.add(Box.createVerticalStrut(14));

        // Logout Button
        JButton btnLogout = new JButton("🚪 Log Out");
        btnLogout.setFont(UITheme.FONT_BOLD);
        btnLogout.setForeground(new Color(0xFF, 0xBB, 0xBB));
        btnLogout.setBackground(new Color(0x32, 0x3E, 0x5D));
        btnLogout.setFocusPainted(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogout.setMaximumSize(new Dimension(210, 36));
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> handleLogout());
        sidebar.add(btnLogout);

        add(sidebar, BorderLayout.WEST);

        // Center Content Area with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setOpaque(false);

        // Instantiate view panels
        dashboardPanel = new DashboardPanel(
                () -> navigateTo("FOLDERS"),
                () -> navigateTo("SEARCH")
        );
        searchPanel = new SearchPanel();
        foldersPanel = new FoldersPanel();
        savedSearchesPanel = new SavedSearchesPanel((query, mode) -> {
            navigateTo("SEARCH");
            searchPanel.setQueryAndSearch(query, mode);
        });
        historyPanel = new HistoryPanel((query, mode) -> {
            navigateTo("SEARCH");
            searchPanel.setQueryAndSearch(query, mode);
        });
        settingsPanel = new SettingsPanel();

        contentPanel.add(dashboardPanel, "DASHBOARD");
        contentPanel.add(searchPanel, "SEARCH");
        contentPanel.add(foldersPanel, "FOLDERS");
        contentPanel.add(savedSearchesPanel, "SAVED");
        contentPanel.add(historyPanel, "HISTORY");
        contentPanel.add(settingsPanel, "SETTINGS");

        if (UserSession.isAdmin()) {
            adminPanel = new AdminPanel();
            contentPanel.add(adminPanel, "ADMIN");
        }

        add(contentPanel, BorderLayout.CENTER);

        // Default view
        navigateTo("DASHBOARD");
    }

    private void addNavButton(JPanel sidebar, String text, String viewKey, String icon) {
        JButton btn = new JButton(icon + "  " + text);
        btn.setFont(UITheme.FONT_REGULAR);
        btn.setForeground(new Color(0xDC, 0xE2, 0xF0));
        btn.setBackground(UITheme.SIDEBAR_BG);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(210, 40));
        btn.setPreferredSize(new Dimension(210, 40));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> navigateTo(viewKey));

        navButtons.put(viewKey, btn);
        sidebar.add(btn);
        sidebar.add(Box.createVerticalStrut(6));
    }

    public void navigateTo(String viewKey) {
        currentView = viewKey;
        cardLayout.show(contentPanel, viewKey);

        // Update button highlight
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            if (entry.getKey().equals(viewKey)) {
                entry.getValue().setBackground(UITheme.ACCENT_PRIMARY);
                entry.getValue().setForeground(Color.WHITE);
                entry.getValue().setFont(UITheme.FONT_BOLD);
            } else {
                entry.getValue().setBackground(UITheme.SIDEBAR_BG);
                entry.getValue().setForeground(new Color(0xDC, 0xE2, 0xF0));
                entry.getValue().setFont(UITheme.FONT_REGULAR);
            }
        }

        // Trigger refresh hook on target panel
        switch (viewKey) {
            case "DASHBOARD" -> dashboardPanel.refreshDashboardData();
            case "SEARCH" -> searchPanel.reloadFoldersDropdown();
            case "FOLDERS" -> foldersPanel.refreshFolderList();
            case "SAVED" -> savedSearchesPanel.refreshData();
            case "HISTORY" -> historyPanel.refreshData();
            case "ADMIN" -> {
                if (adminPanel != null) adminPanel.refreshAdminData();
            }
        }
    }

    private void handleLogout() {
        int opt = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            authService.logout();
            dispose();
            if (onLogout != null) {
                onLogout.run();
            }
        }
    }
}
