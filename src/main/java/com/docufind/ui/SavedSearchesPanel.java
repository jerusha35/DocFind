package com.docufind.ui;

import com.docufind.dao.SavedSearchDAO;
import com.docufind.model.SavedSearch;
import com.docufind.security.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.function.BiConsumer;

public class SavedSearchesPanel extends JPanel {
    private final SavedSearchDAO savedSearchDAO = new SavedSearchDAO();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final BiConsumer<String, String> onRunSearch;

    private DefaultTableModel tableModel;
    private JTable table;
    private List<SavedSearch> currentList;

    public SavedSearchesPanel(BiConsumer<String, String> onRunSearch) {
        this.onRunSearch = onRunSearch;
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initComponents();
    }

    private void initComponents() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Saved Searches");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        JLabel lblDesc = new JLabel("Easily rerun your frequently used document queries.");
        lblDesc.setFont(UITheme.FONT_REGULAR);
        lblDesc.setForeground(UITheme.TEXT_MUTED);

        topPanel.add(lblTitle);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(lblDesc);
        topPanel.add(Box.createVerticalStrut(16));

        // Action Buttons Row
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionRow.setOpaque(false);

        JButton btnRun = UITheme.createButton("▶ Run Saved Search", true);
        btnRun.addActionListener(e -> handleRun());

        JButton btnRename = UITheme.createButton("✏ Rename", false);
        btnRename.addActionListener(e -> handleRename());

        JButton btnDelete = UITheme.createButton("🗑 Delete", false);
        btnDelete.addActionListener(e -> handleDelete());

        JButton btnRefresh = UITheme.createButton("🔄 Refresh", false);
        btnRefresh.addActionListener(e -> refreshData());

        actionRow.add(btnRun);
        actionRow.add(btnRename);
        actionRow.add(btnDelete);
        actionRow.add(btnRefresh);

        topPanel.add(actionRow);

        // Center Table Card
        JPanel cardTable = UITheme.createCard();
        cardTable.setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(new String[]{"ID", "Search Name", "Query", "Mode", "Filters", "Saved Date"}, 0) {
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

        SwingWorker<List<SavedSearch>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<SavedSearch> doInBackground() throws Exception {
                return savedSearchDAO.getSavedSearchesByUserId(userId);
            }

            @Override
            protected void done() {
                try {
                    currentList = get();
                    tableModel.setRowCount(0);
                    for (SavedSearch s : currentList) {
                        tableModel.addRow(new Object[]{
                                s.getSearchId(),
                                s.getSearchName(),
                                s.getSearchQuery(),
                                s.getSearchMode(),
                                s.getFilterSettings(),
                                s.getCreatedAt() != null ? dateFormat.format(s.getCreatedAt()) : ""
                        });
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void handleRun() {
        int row = table.getSelectedRow();
        if (row < 0 || currentList == null || row >= currentList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a saved search to run.");
            return;
        }
        SavedSearch s = currentList.get(row);
        if (onRunSearch != null) {
            onRunSearch.accept(s.getSearchQuery(), s.getSearchMode());
        }
    }

    private void handleRename() {
        int row = table.getSelectedRow();
        if (row < 0 || currentList == null || row >= currentList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a saved search to rename.");
            return;
        }
        SavedSearch s = currentList.get(row);
        String newName = JOptionPane.showInputDialog(this, "Enter new name:", s.getSearchName());
        if (newName != null && !newName.trim().isEmpty()) {
            long userId = UserSession.getCurrentUser().getUserId();
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return savedSearchDAO.renameSavedSearch(s.getSearchId(), userId, newName.trim());
                }

                @Override
                protected void done() {
                    refreshData();
                }
            };
            worker.execute();
        }
    }

    private void handleDelete() {
        int row = table.getSelectedRow();
        if (row < 0 || currentList == null || row >= currentList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a saved search to delete.");
            return;
        }
        SavedSearch s = currentList.get(row);
        int opt = JOptionPane.showConfirmDialog(this, "Delete saved search: " + s.getSearchName() + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            long userId = UserSession.getCurrentUser().getUserId();
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return savedSearchDAO.deleteSavedSearch(s.getSearchId(), userId);
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
