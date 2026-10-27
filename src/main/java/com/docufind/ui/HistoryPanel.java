package com.docufind.ui;

import com.docufind.dao.SearchHistoryDAO;
import com.docufind.model.SearchHistoryItem;
import com.docufind.security.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.function.BiConsumer;

public class HistoryPanel extends JPanel {
    private final SearchHistoryDAO historyDAO = new SearchHistoryDAO();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final BiConsumer<String, String> onRepeatSearch;

    private DefaultTableModel tableModel;
    private JTable table;
    private List<SearchHistoryItem> currentList;

    public HistoryPanel(BiConsumer<String, String> onRepeatSearch) {
        this.onRepeatSearch = onRepeatSearch;
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initComponents();
    }

    private void initComponents() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Search History");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        JLabel lblDesc = new JLabel("View your search history and quickly re-run past queries.");
        lblDesc.setFont(UITheme.FONT_REGULAR);
        lblDesc.setForeground(UITheme.TEXT_MUTED);

        topPanel.add(lblTitle);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(lblDesc);
        topPanel.add(Box.createVerticalStrut(16));

        // Action Buttons Row
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionRow.setOpaque(false);

        JButton btnRepeat = UITheme.createButton("🔁 Repeat Selected Search", true);
        btnRepeat.addActionListener(e -> handleRepeat());

        JButton btnClear = UITheme.createButton("🗑 Clear My History", false);
        btnClear.addActionListener(e -> handleClearHistory());

        JButton btnRefresh = UITheme.createButton("🔄 Refresh", false);
        btnRefresh.addActionListener(e -> refreshData());

        actionRow.add(btnRepeat);
        actionRow.add(btnClear);
        actionRow.add(btnRefresh);

        topPanel.add(actionRow);

        // Center Table Card
        JPanel cardTable = UITheme.createCard();
        cardTable.setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(new String[]{"ID", "Search Query", "Search Mode", "Results Count", "Timestamp"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(UITheme.FONT_REGULAR);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        cardTable.add(new JScrollPane(table), BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(cardTable, BorderLayout.CENTER);
    }

    public void refreshData() {
        if (!UserSession.isLoggedIn()) return;
        long userId = UserSession.getCurrentUser().getUserId();

        SwingWorker<List<SearchHistoryItem>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<SearchHistoryItem> doInBackground() throws Exception {
                return historyDAO.getHistoryByUserId(userId, 100);
            }

            @Override
            protected void done() {
                try {
                    currentList = get();
                    tableModel.setRowCount(0);
                    for (SearchHistoryItem h : currentList) {
                        tableModel.addRow(new Object[]{
                                h.getHistoryId(),
                                h.getSearchQuery(),
                                h.getSearchMode(),
                                h.getResultCount(),
                                h.getSearchedAt() != null ? dateFormat.format(h.getSearchedAt()) : ""
                        });
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void handleRepeat() {
        int row = table.getSelectedRow();
        if (row < 0 || currentList == null || row >= currentList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a past search to repeat.");
            return;
        }
        SearchHistoryItem item = currentList.get(row);
        if (onRepeatSearch != null) {
            onRepeatSearch.accept(item.getSearchQuery(), item.getSearchMode());
        }
    }

    private void handleClearHistory() {
        if (!UserSession.isLoggedIn()) return;
        int opt = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear your search history?", "Confirm Clear", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            long userId = UserSession.getCurrentUser().getUserId();
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return historyDAO.clearHistoryByUserId(userId);
                }

                @Override
                protected void done() {
                    refreshData();
                }
            };
            worker.execute();
        }
    }
}
