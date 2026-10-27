package com.docufind.ui;

import com.docufind.dao.IndexingLogDAO;
import com.docufind.dao.UserDAO;
import com.docufind.filehandling.FileHandler;
import com.docufind.model.IndexingLog;
import com.docufind.model.User;
import com.docufind.security.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class AdminPanel extends JPanel {
    private final UserDAO userDAO = new UserDAO();
    private final IndexingLogDAO logDAO = new IndexingLogDAO();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private DefaultTableModel userTableModel;
    private JTable userTable;
    private List<User> userList;

    private DefaultTableModel logTableModel;
    private JTable logTable;
    private List<IndexingLog> logList;

    private JLabel lblTotalUsers;
    private JLabel lblTotalErrors;

    public AdminPanel() {
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initComponents();
    }

    private void initComponents() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("System Administrator Panel");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        JLabel lblDesc = new JLabel("Audit user accounts, manage account status, and inspect application-wide indexing error logs.");
        lblDesc.setFont(UITheme.FONT_REGULAR);
        lblDesc.setForeground(UITheme.TEXT_MUTED);

        topPanel.add(lblTitle);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(lblDesc);
        topPanel.add(Box.createVerticalStrut(14));

        // Stats & Action Row
        JPanel statsActionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        statsActionRow.setOpaque(false);

        lblTotalUsers = new JLabel("Total Accounts: 0");
        lblTotalUsers.setFont(UITheme.FONT_BOLD);
        lblTotalUsers.setForeground(UITheme.SIDEBAR_BG);

        lblTotalErrors = new JLabel("Total System Errors: 0");
        lblTotalErrors.setFont(UITheme.FONT_BOLD);
        lblTotalErrors.setForeground(UITheme.ERROR);

        JButton btnToggleStatus = UITheme.createButton("Toggle Active/Deactivate", false);
        btnToggleStatus.addActionListener(e -> handleToggleStatus());

        JButton btnExportLogs = UITheme.createButton("Export Logs to Text", false);
        btnExportLogs.addActionListener(e -> handleExportLogs());

        JButton btnRefresh = UITheme.createButton("🔄 Refresh Admin Data", true);
        btnRefresh.addActionListener(e -> refreshAdminData());

        statsActionRow.add(lblTotalUsers);
        statsActionRow.add(new JLabel(" | "));
        statsActionRow.add(lblTotalErrors);
        statsActionRow.add(Box.createHorizontalStrut(20));
        statsActionRow.add(btnToggleStatus);
        statsActionRow.add(btnExportLogs);
        statsActionRow.add(btnRefresh);

        topPanel.add(statsActionRow);
        add(topPanel, BorderLayout.NORTH);

        // Center split: Users Table (top) and Error Logs Table (bottom)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(8);
        splitPane.setOpaque(false);

        // Users Card
        JPanel cardUsers = UITheme.createCard();
        cardUsers.setLayout(new BorderLayout(8, 8));
        JLabel lblUsersTitle = new JLabel("Application Registered Accounts");
        lblUsersTitle.setFont(UITheme.FONT_SUBTITLE);
        lblUsersTitle.setForeground(UITheme.TEXT_MAIN);
        cardUsers.add(lblUsersTitle, BorderLayout.NORTH);

        userTableModel = new DefaultTableModel(new String[]{"User ID", "Username", "Email", "Role", "Active Status", "Created At"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        userTable = new JTable(userTableModel);
        userTable.setRowHeight(26);
        userTable.setFont(UITheme.FONT_REGULAR);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cardUsers.add(new JScrollPane(userTable), BorderLayout.CENTER);

        // Logs Card
        JPanel cardLogs = UITheme.createCard();
        cardLogs.setLayout(new BorderLayout(8, 8));
        JLabel lblLogsTitle = new JLabel("System-Wide Indexing Activity & Error Logs");
        lblLogsTitle.setFont(UITheme.FONT_SUBTITLE);
        lblLogsTitle.setForeground(UITheme.TEXT_MAIN);
        cardLogs.add(lblLogsTitle, BorderLayout.NORTH);

        logTableModel = new DefaultTableModel(new String[]{"Log ID", "User ID", "Status", "Time", "File Path", "Error Message"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        logTable = new JTable(logTableModel);
        logTable.setRowHeight(26);
        logTable.setFont(UITheme.FONT_REGULAR);
        logTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cardLogs.add(new JScrollPane(logTable), BorderLayout.CENTER);

        splitPane.setTopComponent(cardUsers);
        splitPane.setBottomComponent(cardLogs);

        add(splitPane, BorderLayout.CENTER);
    }

    public void refreshAdminData() {
        if (!UserSession.isAdmin()) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            int userCount = 0;
            int errorCount = 0;

            @Override
            protected Void doInBackground() throws Exception {
                userList = userDAO.findAllUsers();
                userCount = userList.size();
                logList = logDAO.getAllLogsForAdmin(200);
                errorCount = logDAO.countTotalErrors();
                return null;
            }

            @Override
            protected void done() {
                lblTotalUsers.setText("Total Accounts: " + userCount);
                lblTotalErrors.setText("Total System Errors: " + errorCount);

                userTableModel.setRowCount(0);
                if (userList != null) {
                    for (User u : userList) {
                        userTableModel.addRow(new Object[]{
                                u.getUserId(),
                                u.getUsername(),
                                u.getEmail(),
                                u.getRole(),
                                u.isActive() ? "Active (1)" : "Deactivated (0)",
                                u.getCreatedAt() != null ? dateFormat.format(u.getCreatedAt()) : ""
                        });
                    }
                }

                logTableModel.setRowCount(0);
                if (logList != null) {
                    for (IndexingLog l : logList) {
                        logTableModel.addRow(new Object[]{
                                l.getLogId(),
                                l.getUserId(),
                                l.getStatus(),
                                l.getLoggedAt() != null ? dateFormat.format(l.getLoggedAt()) : "",
                                l.getFilePath(),
                                l.getErrorMessage()
                        });
                    }
                }
            }
        };
        worker.execute();
    }

    private void handleToggleStatus() {
        int row = userTable.getSelectedRow();
        if (row < 0 || userList == null || row >= userList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a user to toggle status.");
            return;
        }

        User selected = userList.get(row);
        if (selected.getUserId() == UserSession.getCurrentUser().getUserId()) {
            JOptionPane.showMessageDialog(this, "You cannot deactivate your own active admin account.");
            return;
        }

        boolean newStatus = !selected.isActive();
        String actionName = newStatus ? "activate" : "deactivate";

        int opt = JOptionPane.showConfirmDialog(this, "Are you sure you want to " + actionName + " account: " + selected.getUsername() + "?",
                "Confirm Account Status Change", JOptionPane.YES_NO_OPTION);

        if (opt == JOptionPane.YES_OPTION) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return userDAO.updateUserStatus(selected.getUserId(), newStatus);
                }

                @Override
                protected void done() {
                    refreshAdminData();
                }
            };
            worker.execute();
        }
    }

    private void handleExportLogs() {
        if (logList == null || logList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No logs available to export.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("docufind_admin_error_logs.txt"));
        int opt = chooser.showSaveDialog(this);
        if (opt == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            try {
                FileHandler.exportIndexingLogsToText(logList, dest.toPath());
                JOptionPane.showMessageDialog(this, "Logs exported successfully to:\n" + dest.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
            }
        }
    }
}
