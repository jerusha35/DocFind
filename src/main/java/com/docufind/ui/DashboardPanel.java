package com.docufind.ui;

import com.docufind.dao.DocumentDAO;
import com.docufind.dao.FolderDAO;
import com.docufind.dao.IndexingLogDAO;
import com.docufind.dao.SearchHistoryDAO;
import com.docufind.model.DocumentRecord;
import com.docufind.model.IndexingLog;
import com.docufind.security.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

public class DashboardPanel extends JPanel {
    private final FolderDAO folderDAO = new FolderDAO();
    private final DocumentDAO documentDAO = new DocumentDAO();
    private final SearchHistoryDAO historyDAO = new SearchHistoryDAO();
    private final IndexingLogDAO logDAO = new IndexingLogDAO();

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private JLabel lblTotalDocs;
    private JLabel lblTotalFolders;
    private JLabel lblLastIndexed;
    private JLabel lblTotalSearches;

    private DefaultTableModel recentFilesModel;
    private DefaultTableModel recentLogsModel;

    private final Runnable onNavigateToFolders;
    private final Runnable onNavigateToSearch;

    public DashboardPanel(Runnable onNavigateToFolders, Runnable onNavigateToSearch) {
        this.onNavigateToFolders = onNavigateToFolders;
        this.onNavigateToSearch = onNavigateToSearch;

        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initComponents();
    }

    private void initComponents() {
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Document Search Dashboard");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        JLabel lblWelcome = new JLabel("Welcome back, " + (UserSession.getCurrentUser() != null ? UserSession.getCurrentUser().getUsername() : "User"));
        lblWelcome.setFont(UITheme.FONT_REGULAR);
        lblWelcome.setForeground(UITheme.TEXT_MUTED);

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblWelcome, BorderLayout.SOUTH);

        // Stats Cards Row
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        statsPanel.setOpaque(false);
        statsPanel.setPreferredSize(new Dimension(800, 105));

        lblTotalDocs = new JLabel("0");
        lblTotalFolders = new JLabel("0");
        lblLastIndexed = new JLabel("Never");
        lblTotalSearches = new JLabel("0");

        statsPanel.add(createStatCard("Total Indexed Docs", lblTotalDocs, "📄"));
        statsPanel.add(createStatCard("Indexed Folders", lblTotalFolders, "📁"));
        statsPanel.add(createStatCard("Last Indexed", lblLastIndexed, "⏱"));
        statsPanel.add(createStatCard("Searches Performed", lblTotalSearches, "🔍"));

        // Quick Actions Row
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actionsPanel.setOpaque(false);

        JButton btnQuickSearch = UITheme.createButton("🔍 Search Documents", true);
        btnQuickSearch.addActionListener(e -> {
            if (onNavigateToSearch != null) onNavigateToSearch.run();
        });

        JButton btnQuickAddFolder = UITheme.createButton("📁 Manage Folders", false);
        btnQuickAddFolder.addActionListener(e -> {
            if (onNavigateToFolders != null) onNavigateToFolders.run();
        });

        JButton btnRefreshStats = UITheme.createButton("🔄 Refresh Stats", false);
        btnRefreshStats.addActionListener(e -> refreshDashboardData());

        actionsPanel.add(btnQuickSearch);
        actionsPanel.add(btnQuickAddFolder);
        actionsPanel.add(btnRefreshStats);

        // Tables Row (Recent Documents & Recent Activity)
        JPanel tablesPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        tablesPanel.setOpaque(false);

        // Recent Indexed Documents Card
        JPanel cardRecentDocs = UITheme.createCard();
        cardRecentDocs.setLayout(new BorderLayout(8, 8));
        JLabel lblRecentTitle = new JLabel("Recently Modified Indexed Documents");
        lblRecentTitle.setFont(UITheme.FONT_SUBTITLE);
        lblRecentTitle.setForeground(UITheme.TEXT_MAIN);
        cardRecentDocs.add(lblRecentTitle, BorderLayout.NORTH);

        recentFilesModel = new DefaultTableModel(new String[]{"File Name", "Size (Bytes)", "Modified"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tableRecentDocs = new JTable(recentFilesModel);
        tableRecentDocs.setRowHeight(24);
        tableRecentDocs.setFont(UITheme.FONT_REGULAR);
        cardRecentDocs.add(new JScrollPane(tableRecentDocs), BorderLayout.CENTER);

        // Recent Indexing Logs Card
        JPanel cardLogs = UITheme.createCard();
        cardLogs.setLayout(new BorderLayout(8, 8));
        JLabel lblLogsTitle = new JLabel("Recent Indexing Activity / Logs");
        lblLogsTitle.setFont(UITheme.FONT_SUBTITLE);
        lblLogsTitle.setForeground(UITheme.TEXT_MAIN);
        cardLogs.add(lblLogsTitle, BorderLayout.NORTH);

        recentLogsModel = new DefaultTableModel(new String[]{"Time", "Status", "File / Details"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tableLogs = new JTable(recentLogsModel);
        tableLogs.setRowHeight(24);
        tableLogs.setFont(UITheme.FONT_REGULAR);
        cardLogs.add(new JScrollPane(tableLogs), BorderLayout.CENTER);

        tablesPanel.add(cardRecentDocs);
        tablesPanel.add(cardLogs);

        // Assemble Top
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);
        topContainer.add(headerPanel);
        topContainer.add(Box.createVerticalStrut(16));
        topContainer.add(statsPanel);
        topContainer.add(Box.createVerticalStrut(14));
        topContainer.add(actionsPanel);
        topContainer.add(Box.createVerticalStrut(14));

        add(topContainer, BorderLayout.NORTH);
        add(tablesPanel, BorderLayout.CENTER);
    }

    private JPanel createStatCard(String title, JLabel valueLabel, String icon) {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout());

        JLabel lblTop = new JLabel(icon + " " + title);
        lblTop.setFont(UITheme.FONT_CARD_TITLE);
        lblTop.setForeground(UITheme.TEXT_MUTED);

        valueLabel.setFont(UITheme.FONT_CARD_VALUE);
        valueLabel.setForeground(UITheme.SIDEBAR_BG);

        card.add(lblTop, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    public void refreshDashboardData() {
        if (!UserSession.isLoggedIn()) return;
        long userId = UserSession.getCurrentUser().getUserId();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            int totalDocs = 0;
            int totalFolders = 0;
            int totalSearches = 0;
            Timestamp lastIndexed = null;
            List<DocumentRecord> recentDocs;
            List<IndexingLog> recentLogs;

            @Override
            protected Void doInBackground() {
                try {
                    totalDocs = documentDAO.countDocumentsByUserId(userId);
                    totalFolders = folderDAO.countFoldersByUserId(userId);
                    totalSearches = historyDAO.countSearchesByUserId(userId);
                    lastIndexed = folderDAO.getLastIndexedTime(userId);
                    recentDocs = documentDAO.getRecentDocuments(userId, 10);
                    recentLogs = logDAO.getLogsByUserId(userId, 10);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                lblTotalDocs.setText(String.valueOf(totalDocs));
                lblTotalFolders.setText(String.valueOf(totalFolders));
                lblTotalSearches.setText(String.valueOf(totalSearches));
                if (lastIndexed != null) {
                    lblLastIndexed.setFont(new Font("Segoe UI", Font.BOLD, 16));
                    lblLastIndexed.setText(dateFormat.format(lastIndexed));
                } else {
                    lblLastIndexed.setFont(UITheme.FONT_CARD_VALUE);
                    lblLastIndexed.setText("Never");
                }

                recentFilesModel.setRowCount(0);
                if (recentDocs != null && !recentDocs.isEmpty()) {
                    for (DocumentRecord doc : recentDocs) {
                        recentFilesModel.addRow(new Object[]{
                                doc.getFileName(),
                                doc.getFileSize(),
                                doc.getLastModified() != null ? dateFormat.format(doc.getLastModified()) : ""
                        });
                    }
                }

                recentLogsModel.setRowCount(0);
                if (recentLogs != null && !recentLogs.isEmpty()) {
                    for (IndexingLog log : recentLogs) {
                        recentLogsModel.addRow(new Object[]{
                                log.getLoggedAt() != null ? dateFormat.format(log.getLoggedAt()) : "",
                                log.getStatus(),
                                log.getFilePath()
                        });
                    }
                }
            }
        };
        worker.execute();
    }
}
