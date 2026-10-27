package com.docufind.ui;

import com.docufind.dao.DocumentDAO;
import com.docufind.dao.FolderDAO;
import com.docufind.dao.SavedSearchDAO;
import com.docufind.filehandling.FileHandler;
import com.docufind.model.DocumentRecord;
import com.docufind.model.IndexedFolder;
import com.docufind.model.SavedSearch;
import com.docufind.search.SearchService;
import com.docufind.security.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class SearchPanel extends JPanel {
    private final SearchService searchService = new SearchService();
    private final FolderDAO folderDAO = new FolderDAO();
    private final DocumentDAO documentDAO = new DocumentDAO();
    private final SavedSearchDAO savedSearchDAO = new SavedSearchDAO();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    // Search Controls
    private JTextField txtSearchQuery;
    private JComboBox<String> cmbSearchMode;
    private JComboBox<String> cmbFileExtension;
    private JComboBox<FolderItem> cmbFolders;
    private JComboBox<String> cmbSortBy;
    private JButton btnSearch;
    private JButton btnClear;
    private JLabel lblResultStats;

    // Results Table
    private DefaultTableModel resultsTableModel;
    private JTable resultsTable;
    private List<DocumentRecord> currentResults = new ArrayList<>();

    // Preview Panel Controls
    private JLabel lblPreviewName;
    private JLabel lblPreviewPath;
    private JLabel lblPreviewSizeMod;
    private JLabel lblPreviewScore;
    private JTextArea txtPreviewContent;
    private JButton btnOpenFile;
    private JButton btnOpenFolder;
    private JButton btnCopyPath;
    private JButton btnSaveSearch;
    private JButton btnExportCsv;
    private JButton btnExportReport;

    private DocumentRecord selectedDocument;

    private static class FolderItem {
        final Long id;
        final String name;

        FolderItem(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public SearchPanel() {
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initComponents();
    }

    private void initComponents() {
        // TOP: Filter & Search Bar
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        JLabel lblTitle = new JLabel("Search Indexed Documents");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        // Search Bar Row
        JPanel searchBarRow = new JPanel(new BorderLayout(10, 0));
        searchBarRow.setOpaque(false);

        txtSearchQuery = new JTextField();
        txtSearchQuery.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        txtSearchQuery.setPreferredSize(new Dimension(txtSearchQuery.getPreferredSize().width, 40));

        btnSearch = UITheme.createButton("🔍 Search", true);
        btnSearch.setPreferredSize(new Dimension(130, 40));
        btnSearch.addActionListener(e -> executeSearch());
        txtSearchQuery.addActionListener(e -> executeSearch());

        btnClear = UITheme.createButton("Clear", false);
        btnClear.setPreferredSize(new Dimension(90, 40));
        btnClear.addActionListener(e -> {
            txtSearchQuery.setText("");
            cmbFileExtension.setSelectedIndex(0);
            cmbFolders.setSelectedIndex(0);
            cmbSearchMode.setSelectedIndex(0);
            clearResults();
        });

        JPanel searchBtnWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchBtnWrapper.setOpaque(false);
        searchBtnWrapper.add(btnSearch);
        searchBtnWrapper.add(btnClear);

        searchBarRow.add(txtSearchQuery, BorderLayout.CENTER);
        searchBarRow.add(searchBtnWrapper, BorderLayout.EAST);

        // Filter Controls Row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        filterRow.setOpaque(false);

        cmbSearchMode = new JComboBox<>(new String[]{"All keywords", "Exact phrase", "Any keyword", "Filename only"});
        cmbFileExtension = new JComboBox<>(new String[]{"All Extensions", ".txt", ".md", ".java", ".log", ".csv", ".json", ".xml", ".html", ".sql", ".properties"});
        cmbFolders = new JComboBox<>();
        cmbFolders.addItem(new FolderItem(null, "All Folders"));
        cmbSortBy = new JComboBox<>(new String[]{"Relevance", "Date Modified (Newest)", "Date Modified (Oldest)", "Filename (A-Z)", "File Size (Largest)"});

        filterRow.add(createLabeledComponent("Mode:", cmbSearchMode));
        filterRow.add(createLabeledComponent("Type:", cmbFileExtension));
        filterRow.add(createLabeledComponent("Folder:", cmbFolders));
        filterRow.add(createLabeledComponent("Sort By:", cmbSortBy));

        lblResultStats = new JLabel("Enter a query and click Search.");
        lblResultStats.setFont(UITheme.FONT_SMALL);
        lblResultStats.setForeground(UITheme.TEXT_MUTED);

        topContainer.add(lblTitle);
        topContainer.add(Box.createVerticalStrut(12));
        topContainer.add(searchBarRow);
        topContainer.add(Box.createVerticalStrut(4));
        topContainer.add(filterRow);
        topContainer.add(Box.createVerticalStrut(4));
        topContainer.add(lblResultStats);
        topContainer.add(Box.createVerticalStrut(10));

        add(topContainer, BorderLayout.NORTH);

        // CENTER & EAST: SplitPane (Results Table + Preview Panel)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.58);
        splitPane.setDividerSize(8);
        splitPane.setOpaque(false);

        // Left Card: Results Table
        JPanel cardResults = UITheme.createCard();
        cardResults.setLayout(new BorderLayout());

        resultsTableModel = new DefaultTableModel(new String[]{"File Name", "Extension", "Size (Bytes)", "Modified", "Relevance", "Snippet"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        resultsTable = new JTable(resultsTableModel);
        resultsTable.setRowHeight(26);
        resultsTable.setFont(UITheme.FONT_REGULAR);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleResultSelected();
            }
        });

        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        resultsTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        resultsTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        resultsTable.getColumnModel().getColumn(5).setPreferredWidth(200);

        cardResults.add(new JScrollPane(resultsTable), BorderLayout.CENTER);

        // Right Card: Preview Panel
        JPanel cardPreview = UITheme.createCard();
        cardPreview.setLayout(new BorderLayout(8, 8));

        JPanel previewHeader = new JPanel();
        previewHeader.setLayout(new BoxLayout(previewHeader, BoxLayout.Y_AXIS));
        previewHeader.setOpaque(false);

        lblPreviewName = new JLabel("Document Preview");
        lblPreviewName.setFont(UITheme.FONT_SUBTITLE);
        lblPreviewName.setForeground(UITheme.TEXT_MAIN);

        lblPreviewScore = new JLabel("Select a result on the left to preview.");
        lblPreviewScore.setFont(UITheme.FONT_BOLD);
        lblPreviewScore.setForeground(UITheme.ACCENT_PRIMARY);

        lblPreviewPath = new JLabel(" ");
        lblPreviewPath.setFont(UITheme.FONT_SMALL);
        lblPreviewPath.setForeground(UITheme.TEXT_MUTED);

        lblPreviewSizeMod = new JLabel(" ");
        lblPreviewSizeMod.setFont(UITheme.FONT_SMALL);
        lblPreviewSizeMod.setForeground(UITheme.TEXT_MUTED);

        previewHeader.add(lblPreviewName);
        previewHeader.add(Box.createVerticalStrut(4));
        previewHeader.add(lblPreviewScore);
        previewHeader.add(Box.createVerticalStrut(4));
        previewHeader.add(lblPreviewPath);
        previewHeader.add(Box.createVerticalStrut(2));
        previewHeader.add(lblPreviewSizeMod);
        previewHeader.add(Box.createVerticalStrut(8));

        txtPreviewContent = new JTextArea();
        txtPreviewContent.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtPreviewContent.setEditable(false);
        txtPreviewContent.setLineWrap(true);
        txtPreviewContent.setWrapStyleWord(true);
        txtPreviewContent.setText("No document selected.");

        // Preview Actions Row
        JPanel previewActions = new JPanel(new GridLayout(2, 3, 6, 6));
        previewActions.setOpaque(false);

        btnOpenFile = UITheme.createButton("📄 Open File", true);
        btnOpenFile.setEnabled(false);
        btnOpenFile.addActionListener(e -> handleOpenFile());

        btnOpenFolder = UITheme.createButton("📁 Open Folder", false);
        btnOpenFolder.setEnabled(false);
        btnOpenFolder.addActionListener(e -> handleOpenContainingFolder());

        btnCopyPath = UITheme.createButton("📋 Copy Path", false);
        btnCopyPath.setEnabled(false);
        btnCopyPath.addActionListener(e -> handleCopyPath());

        btnSaveSearch = UITheme.createButton("⭐ Save Search", false);
        btnSaveSearch.addActionListener(e -> handleSaveSearch());

        btnExportCsv = UITheme.createButton("📊 Export CSV", false);
        btnExportCsv.addActionListener(e -> handleExportCsv());

        btnExportReport = UITheme.createButton("📝 Export Report", false);
        btnExportReport.addActionListener(e -> handleExportReport());

        previewActions.add(btnOpenFile);
        previewActions.add(btnOpenFolder);
        previewActions.add(btnCopyPath);
        previewActions.add(btnSaveSearch);
        previewActions.add(btnExportCsv);
        previewActions.add(btnExportReport);

        cardPreview.add(previewHeader, BorderLayout.NORTH);
        cardPreview.add(new JScrollPane(txtPreviewContent), BorderLayout.CENTER);
        cardPreview.add(previewActions, BorderLayout.SOUTH);

        splitPane.setLeftComponent(cardResults);
        splitPane.setRightComponent(cardPreview);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createLabeledComponent(String labelText, JComponent comp) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_MAIN);
        p.add(lbl);
        p.add(comp);
        return p;
    }

    public void reloadFoldersDropdown() {
        if (!UserSession.isLoggedIn()) return;
        long userId = UserSession.getCurrentUser().getUserId();

        SwingWorker<List<IndexedFolder>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<IndexedFolder> doInBackground() throws Exception {
                return folderDAO.getFoldersByUserId(userId);
            }

            @Override
            protected void done() {
                try {
                    List<IndexedFolder> folders = get();
                    cmbFolders.removeAllItems();
                    cmbFolders.addItem(new FolderItem(null, "All Folders"));
                    for (IndexedFolder f : folders) {
                        File dir = new File(f.getFolderPath());
                        cmbFolders.addItem(new FolderItem(f.getFolderId(), dir.getName() + " (" + f.getFolderId() + ")"));
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    public void executeSearch() {
        if (!UserSession.isLoggedIn()) return;
        long userId = UserSession.getCurrentUser().getUserId();

        String query = txtSearchQuery.getText().trim();
        String mode = (String) cmbSearchMode.getSelectedItem();
        String ext = (String) cmbFileExtension.getSelectedItem();
        if ("All Extensions".equals(ext)) ext = null;

        FolderItem folderItem = (FolderItem) cmbFolders.getSelectedItem();
        Long folderId = folderItem != null ? folderItem.id : null;
        String sortBy = (String) cmbSortBy.getSelectedItem();

        lblResultStats.setText("Searching Oracle database...");
        btnSearch.setEnabled(false);
        long startTime = System.currentTimeMillis();

        String finalExt = ext;
        SwingWorker<List<DocumentRecord>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<DocumentRecord> doInBackground() {
                return searchService.search(userId, query, mode, finalExt, folderId, null, sortBy, 500);
            }

            @Override
            protected void done() {
                btnSearch.setEnabled(true);
                long duration = System.currentTimeMillis() - startTime;
                try {
                    currentResults = get();
                    resultsTableModel.setRowCount(0);
                    if (currentResults.isEmpty()) {
                        lblResultStats.setText("No matching documents found (" + duration + " ms).");
                        clearPreview();
                    } else {
                        lblResultStats.setText("Found " + currentResults.size() + " matching document(s) in " + duration + " ms.");
                        for (DocumentRecord doc : currentResults) {
                            resultsTableModel.addRow(new Object[]{
                                    doc.getFileName(),
                                    doc.getFileExtension(),
                                    doc.getFileSize(),
                                    doc.getLastModified() != null ? dateFormat.format(doc.getLastModified()) : "",
                                    String.format("%.0f%%", doc.getRelevanceScore()),
                                    doc.getMatchSnippet()
                            });
                        }
                        resultsTable.setRowSelectionInterval(0, 0);
                    }
                } catch (Exception ex) {
                    lblResultStats.setText("Search error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void handleResultSelected() {
        int row = resultsTable.getSelectedRow();
        if (row < 0 || currentResults == null || row >= currentResults.size()) {
            clearPreview();
            return;
        }

        DocumentRecord summaryDoc = currentResults.get(row);
        selectedDocument = summaryDoc;

        lblPreviewName.setText(summaryDoc.getFileName());
        lblPreviewScore.setText("Relevance: " + String.format("%.1f%%", summaryDoc.getRelevanceScore()));
        lblPreviewPath.setText(summaryDoc.getFilePath());
        lblPreviewSizeMod.setText("Size: " + summaryDoc.getFileSize() + " bytes | Modified: " +
                (summaryDoc.getLastModified() != null ? dateFormat.format(summaryDoc.getLastModified()) : "N/A"));

        // Check file status on disk
        File f = new File(summaryDoc.getFilePath());
        if (!f.exists()) {
            txtPreviewContent.setText("⚠️ WARNING: Original file no longer exists at this location:\n" + summaryDoc.getFilePath());
        } else {
            // Check stale modification
            if (summaryDoc.getLastModified() != null && Math.abs(f.lastModified() - summaryDoc.getLastModified().getTime()) > 2000) {
                lblPreviewSizeMod.setText(lblPreviewSizeMod.getText() + " (⚠️ Stale Index - File Modified on Disk)");
            }
            txtPreviewContent.setText(summaryDoc.getExtractedText() != null ? summaryDoc.getExtractedText() : "[No text content extracted]");
        }
        txtPreviewContent.setCaretPosition(0);

        btnOpenFile.setEnabled(true);
        btnOpenFolder.setEnabled(true);
        btnCopyPath.setEnabled(true);
    }

    private void clearResults() {
        currentResults.clear();
        resultsTableModel.setRowCount(0);
        clearPreview();
        lblResultStats.setText("Search cleared.");
    }

    private void clearPreview() {
        selectedDocument = null;
        lblPreviewName.setText("Document Preview");
        lblPreviewScore.setText("Select a result on the left to preview.");
        lblPreviewPath.setText(" ");
        lblPreviewSizeMod.setText(" ");
        txtPreviewContent.setText("");
        btnOpenFile.setEnabled(false);
        btnOpenFolder.setEnabled(false);
        btnCopyPath.setEnabled(false);
    }

    private void handleOpenFile() {
        if (selectedDocument == null) return;
        File f = new File(selectedDocument.getFilePath());
        if (!f.exists()) {
            JOptionPane.showMessageDialog(this, "File does not exist: " + f.getAbsolutePath(), "File Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(f);
            } else {
                // Windows fallback
                new ProcessBuilder("explorer.exe", f.getAbsolutePath()).start();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not open file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleOpenContainingFolder() {
        if (selectedDocument == null) return;
        File f = new File(selectedDocument.getFilePath());
        File parent = f.getParentFile();
        if (parent == null || !parent.exists()) {
            JOptionPane.showMessageDialog(this, "Containing directory does not exist.", "Folder Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            // Select file in Windows Explorer
            new ProcessBuilder("explorer.exe", "/select,", f.getAbsolutePath()).start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not open folder: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCopyPath() {
        if (selectedDocument == null) return;
        StringSelection sel = new StringSelection(selectedDocument.getFilePath());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
        JOptionPane.showMessageDialog(this, "File path copied to clipboard!", "Copied", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleSaveSearch() {
        if (!UserSession.isLoggedIn()) return;
        String query = txtSearchQuery.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search query before saving.", "Save Search", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String searchName = JOptionPane.showInputDialog(this, "Enter a name for this saved search:", "Save Search", JOptionPane.QUESTION_MESSAGE);
        if (searchName == null || searchName.trim().isEmpty()) return;

        SavedSearch item = new SavedSearch();
        item.setUserId(UserSession.getCurrentUser().getUserId());
        item.setSearchName(searchName.trim());
        item.setSearchQuery(query);
        item.setSearchMode((String) cmbSearchMode.getSelectedItem());
        item.setFilterSettings("Extension: " + cmbFileExtension.getSelectedItem());

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return savedSearchDAO.createSavedSearch(item);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(SearchPanel.this, "Search saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(SearchPanel.this, "Error saving search: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void handleExportCsv() {
        if (currentResults.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No search results to export.", "Export CSV", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("docufind_search_results.csv"));
        int opt = chooser.showSaveDialog(this);
        if (opt == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            if (dest.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(this, "File already exists. Overwrite?", "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
                if (overwrite != JOptionPane.YES_OPTION) return;
            }

            try {
                FileHandler.exportSearchResultsToCsv(currentResults, dest.toPath());
                JOptionPane.showMessageDialog(this, "Search results exported to:\n" + dest.getAbsolutePath(), "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to export CSV: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleExportReport() {
        if (currentResults.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No search results to export.", "Export Report", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("docufind_search_report.txt"));
        int opt = chooser.showSaveDialog(this);
        if (opt == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            if (dest.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(this, "File already exists. Overwrite?", "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
                if (overwrite != JOptionPane.YES_OPTION) return;
            }

            try {
                FileHandler.generateSearchReport(txtSearchQuery.getText().trim(),
                        (String) cmbSearchMode.getSelectedItem(), currentResults, dest.toPath());
                JOptionPane.showMessageDialog(this, "Search report generated at:\n" + dest.getAbsolutePath(), "Report Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to generate report: " + ex.getMessage(), "Report Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void setQueryAndSearch(String query, String mode) {
        txtSearchQuery.setText(query);
        if (mode != null) {
            cmbSearchMode.setSelectedItem(mode);
        }
        executeSearch();
    }
}
