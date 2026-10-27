package com.docufind.ui;

import com.docufind.dao.FolderDAO;
import com.docufind.indexing.IndexingService;
import com.docufind.model.IndexedFolder;
import com.docufind.security.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

public class FoldersPanel extends JPanel {
    private final FolderDAO folderDAO = new FolderDAO();
    private final IndexingService indexingService = new IndexingService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private DefaultTableModel tableModel;
    private JTable folderTable;
    private List<IndexedFolder> currentFolders;

    private JProgressBar progressBar;
    private JLabel lblStatus;
    private JButton btnAddFolder;
    private JButton btnIndexFolder;
    private JButton btnIndexAll;
    private JButton btnRemoveFolder;
    private JButton btnCancelIndexing;

    public FoldersPanel() {
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initComponents();
    }

    private void initComponents() {
        // Top Header and Controls
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Indexed Folders Management");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        JLabel lblDesc = new JLabel("Register local directories to index supported files (.txt, .md, .java, .log, .csv, .xml, .json, .html, .sql, .properties).");
        lblDesc.setFont(UITheme.FONT_REGULAR);
        lblDesc.setForeground(UITheme.TEXT_MUTED);

        topPanel.add(lblTitle);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(lblDesc);
        topPanel.add(Box.createVerticalStrut(16));

        // Action Buttons Row
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionRow.setOpaque(false);

        btnAddFolder = UITheme.createButton("➕ Add Folder", true);
        btnAddFolder.addActionListener(e -> handleAddFolder());

        btnIndexFolder = UITheme.createButton("🔄 Index Selected", false);
        btnIndexFolder.addActionListener(e -> handleIndexSelected());

        btnIndexAll = UITheme.createButton("⚡ Reindex All Folders", false);
        btnIndexAll.addActionListener(e -> handleIndexAll());

        btnRemoveFolder = UITheme.createButton("🗑 Remove Folder", false);
        btnRemoveFolder.addActionListener(e -> handleRemoveFolder());

        btnCancelIndexing = UITheme.createButton("⏹ Cancel Indexing", false);
        btnCancelIndexing.setEnabled(false);
        btnCancelIndexing.addActionListener(e -> {
            indexingService.cancel();
            lblStatus.setText("Cancelling indexing...");
        });

        actionRow.add(btnAddFolder);
        actionRow.add(btnIndexFolder);
        actionRow.add(btnIndexAll);
        actionRow.add(btnRemoveFolder);
        actionRow.add(btnCancelIndexing);

        topPanel.add(actionRow);
        topPanel.add(Box.createVerticalStrut(14));

        // Progress Panel
        JPanel progressPanel = new JPanel(new BorderLayout(8, 4));
        progressPanel.setOpaque(false);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setPreferredSize(new Dimension(100, 22));

        lblStatus = new JLabel("Select a folder to view details or start indexing.");
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.TEXT_MUTED);

        progressPanel.add(progressBar, BorderLayout.NORTH);
        progressPanel.add(lblStatus, BorderLayout.SOUTH);

        topPanel.add(progressPanel);

        // Center Table Card
        JPanel cardTable = UITheme.createCard();
        cardTable.setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(new String[]{"ID", "Folder Path", "Status", "Indexed Docs", "Last Indexed", "Added Date"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        folderTable = new JTable(tableModel);
        folderTable.setRowHeight(28);
        folderTable.setFont(UITheme.FONT_REGULAR);
        folderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Column widths
        folderTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        folderTable.getColumnModel().getColumn(1).setPreferredWidth(450);
        folderTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        folderTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        folderTable.getColumnModel().getColumn(4).setPreferredWidth(140);
        folderTable.getColumnModel().getColumn(5).setPreferredWidth(140);

        cardTable.add(new JScrollPane(folderTable), BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(cardTable, BorderLayout.CENTER);
    }

    public void refreshFolderList() {
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
                    currentFolders = get();
                    tableModel.setRowCount(0);
                    for (IndexedFolder f : currentFolders) {
                        tableModel.addRow(new Object[]{
                                f.getFolderId(),
                                f.getFolderPath(),
                                f.getIndexStatus(),
                                f.getDocumentCount(),
                                f.getLastIndexedAt() != null ? dateFormat.format(f.getLastIndexedAt()) : "Never",
                                f.getAddedAt() != null ? dateFormat.format(f.getAddedAt()) : ""
                        });
                    }
                } catch (Exception e) {
                    lblStatus.setText("Error loading folders: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void handleAddFolder() {
        if (!UserSession.isLoggedIn()) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Folder to Index");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            String path = selectedDir.getAbsolutePath();

            long userId = UserSession.getCurrentUser().getUserId();
            SwingWorker<Long, Void> worker = new SwingWorker<>() {
                @Override
                protected Long doInBackground() throws Exception {
                    return folderDAO.addFolder(userId, path);
                }

                @Override
                protected void done() {
                    try {
                        long folderId = get();
                        if (folderId > 0) {
                            lblStatus.setText("Folder added: " + path);
                            refreshFolderList();
                            // Prompt to index immediately
                            int opt = JOptionPane.showConfirmDialog(FoldersPanel.this,
                                    "Folder added successfully. Would you like to index it now?",
                                    "Index Folder Now?", JOptionPane.YES_NO_OPTION);
                            if (opt == JOptionPane.YES_OPTION) {
                                startIndexingFolder(folderId, path);
                            }
                        } else {
                            JOptionPane.showMessageDialog(FoldersPanel.this, "Folder is already registered or could not be added.");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(FoldersPanel.this, "Error adding folder: " + ex.getMessage());
                    }
                }
            };
            worker.execute();
        }
    }

    private void handleIndexSelected() {
        int row = folderTable.getSelectedRow();
        if (row < 0 || currentFolders == null || row >= currentFolders.size()) {
            JOptionPane.showMessageDialog(this, "Please select a folder from the table to index.");
            return;
        }
        IndexedFolder folder = currentFolders.get(row);
        startIndexingFolder(folder.getFolderId(), folder.getFolderPath());
    }

    private void startIndexingFolder(long folderId, String folderPath) {
        if (!UserSession.isLoggedIn()) return;
        long userId = UserSession.getCurrentUser().getUserId();

        setIndexingState(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Scanning files...");
        lblStatus.setText("Indexing folder: " + folderPath);

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                indexingService.indexFolder(userId, folderId, new IndexingService.IndexingProgressListener() {
                    @Override
                    public void onProgress(String currentFile, int processedCount, int errorCount) {
                        publish("Processing: " + currentFile + " (" + processedCount + " indexed, " + errorCount + " errors)");
                    }

                    @Override
                    public void onStatusChange(String statusMessage) {
                        publish(statusMessage);
                    }

                    @Override
                    public void onComplete(int totalProcessed, int totalErrors) {
                        publish("Done: " + totalProcessed + " files indexed (" + totalErrors + " errors).");
                    }
                });
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    String latest = chunks.get(chunks.size() - 1);
                    lblStatus.setText(latest);
                    progressBar.setString(latest);
                }
            }

            @Override
            protected void done() {
                setIndexingState(false);
                progressBar.setIndeterminate(false);
                progressBar.setString("Indexing finished");
                refreshFolderList();
            }
        };
        worker.execute();
    }

    private void handleIndexAll() {
        if (!UserSession.isLoggedIn()) return;
        long userId = UserSession.getCurrentUser().getUserId();

        setIndexingState(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Reindexing all folders...");

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                indexingService.indexAllFolders(userId, new IndexingService.IndexingProgressListener() {
                    @Override
                    public void onProgress(String currentFile, int processedCount, int errorCount) {
                        publish("File: " + currentFile + " (Count: " + processedCount + ")");
                    }

                    @Override
                    public void onStatusChange(String statusMessage) {
                        publish(statusMessage);
                    }

                    @Override
                    public void onComplete(int totalProcessed, int totalErrors) {
                        publish("Completed all folders.");
                    }
                });
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    String msg = chunks.get(chunks.size() - 1);
                    lblStatus.setText(msg);
                    progressBar.setString(msg);
                }
            }

            @Override
            protected void done() {
                setIndexingState(false);
                progressBar.setIndeterminate(false);
                progressBar.setString("All folders indexed");
                refreshFolderList();
            }
        };
        worker.execute();
    }

    private void handleRemoveFolder() {
        int row = folderTable.getSelectedRow();
        if (row < 0 || currentFolders == null || row >= currentFolders.size()) {
            JOptionPane.showMessageDialog(this, "Please select a folder to remove.");
            return;
        }

        IndexedFolder folder = currentFolders.get(row);
        int opt = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove this folder from DocuFind?\n" +
                folder.getFolderPath() + "\n\n(Original files on disk will NOT be touched.)",
                "Remove Folder", JOptionPane.YES_NO_OPTION);

        if (opt == JOptionPane.YES_OPTION) {
            long userId = UserSession.getCurrentUser().getUserId();
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return folderDAO.deleteFolder(folder.getFolderId(), userId);
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            lblStatus.setText("Folder removed from index.");
                            refreshFolderList();
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(FoldersPanel.this, "Error removing folder: " + e.getMessage());
                    }
                }
            };
            worker.execute();
        }
    }

    private void setIndexingState(boolean indexing) {
        btnAddFolder.setEnabled(!indexing);
        btnIndexFolder.setEnabled(!indexing);
        btnIndexAll.setEnabled(!indexing);
        btnRemoveFolder.setEnabled(!indexing);
        btnCancelIndexing.setEnabled(indexing);
    }
}
