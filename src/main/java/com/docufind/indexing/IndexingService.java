package com.docufind.indexing;

import com.docufind.config.AppConfig;
import com.docufind.dao.DocumentDAO;
import com.docufind.dao.FolderDAO;
import com.docufind.dao.IndexingLogDAO;
import com.docufind.model.DocumentRecord;
import com.docufind.model.IndexedFolder;
import com.docufind.model.IndexingLog;
import com.docufind.util.ChecksumUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core service for indexing folders and files into Oracle SQL.
 */
public class IndexingService {
    private static final Logger LOGGER = Logger.getLogger(IndexingService.class.getName());

    public interface IndexingProgressListener {
        void onProgress(String currentFile, int processedCount, int errorCount);
        void onStatusChange(String statusMessage);
        void onComplete(int totalProcessed, int totalErrors);
    }

    private final FolderDAO folderDAO;
    private final DocumentDAO documentDAO;
    private final IndexingLogDAO logDAO;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public IndexingService() {
        this.folderDAO = new FolderDAO();
        this.documentDAO = new DocumentDAO();
        this.logDAO = new IndexingLogDAO();
    }

    public void cancel() {
        cancelled.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Indexes or reindexes a specific folder registered by the user.
     */
    public void indexFolder(long userId, long folderId, IndexingProgressListener listener) {
        cancelled.set(false);
        int maxMb = AppConfig.getInt("app.max_file_size_mb", 25);
        long maxBytes = (long) maxMb * 1024 * 1024;
        int maxChars = AppConfig.getInt("app.preview_char_limit", 3000) * 100; // allow generous text extraction

        try {
            Optional<IndexedFolder> folderOpt = folderDAO.findById(folderId, userId);
            if (folderOpt.isEmpty()) {
                if (listener != null) listener.onStatusChange("Folder not found.");
                return;
            }

            IndexedFolder folder = folderOpt.get();
            File dir = new File(folder.getFolderPath());
            if (!dir.exists() || !dir.isDirectory()) {
                folderDAO.updateStatusAndTimestamp(folderId, userId, "MISSING");
                logError(userId, folderId, folder.getFolderPath(), "Folder does not exist or is not a directory.");
                if (listener != null) listener.onStatusChange("Folder directory is missing.");
                return;
            }

            folderDAO.updateStatusAndTimestamp(folderId, userId, "INDEXING");
            if (listener != null) listener.onStatusChange("Scanning folder: " + dir.getName());

            // Track currently existing files on disk to detect deletions
            Set<String> diskNormalizedPaths = new HashSet<>();
            List<Path> filesToIndex = new ArrayList<>();

            // Walk file tree safely without following symlinks to prevent infinite loops
            Files.walkFileTree(dir.toPath(), EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (cancelled.get()) {
                        return FileVisitResult.TERMINATE;
                    }
                    String fileName = file.getFileName().toString();
                    if (TextExtractor.isSupported(fileName) && attrs.isRegularFile()) {
                        diskNormalizedPaths.add(file.toAbsolutePath().normalize().toString());
                        filesToIndex.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    LOGGER.warning("Access denied or failed visiting: " + file);
                    logError(userId, folderId, file.toString(), "Access failed: " + (exc != null ? exc.getMessage() : "Unknown"));
                    return FileVisitResult.CONTINUE;
                }
            });

            if (cancelled.get()) {
                folderDAO.updateStatusAndTimestamp(folderId, userId, "CANCELLED");
                if (listener != null) listener.onStatusChange("Indexing cancelled by user.");
                return;
            }

            int processedCount = 0;
            int errorCount = 0;

            for (Path path : filesToIndex) {
                if (cancelled.get()) {
                    folderDAO.updateStatusAndTimestamp(folderId, userId, "CANCELLED");
                    if (listener != null) listener.onStatusChange("Indexing cancelled.");
                    return;
                }

                String absPath = path.toAbsolutePath().normalize().toString();
                String fileName = path.getFileName().toString();
                String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")).toLowerCase() : "";

                try {
                    long size = Files.size(path);
                    if (size > maxBytes) {
                        LOGGER.info("Skipping file exceeding size limit (" + maxMb + "MB): " + absPath);
                        logError(userId, folderId, absPath, "Skipped: Exceeds max allowed size of " + maxMb + " MB.");
                        errorCount++;
                        continue;
                    }

                    Timestamp lastMod = new Timestamp(Files.getLastModifiedTime(path).toMillis());
                    String sha256 = ChecksumUtil.computeSHA256(path);
                    String text = TextExtractor.extractText(path, maxChars);

                    DocumentRecord doc = new DocumentRecord();
                    doc.setUserId(userId);
                    doc.setFolderId(folderId);
                    doc.setFileName(fileName);
                    doc.setFilePath(absPath);
                    doc.setFileExtension(ext);
                    doc.setFileSize(size);
                    doc.setLastModified(lastMod);
                    doc.setSha256Hash(sha256);
                    doc.setExtractedText(text);

                    documentDAO.upsertDocument(doc);
                    processedCount++;

                    if (listener != null) {
                        listener.onProgress(fileName, processedCount, errorCount);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error indexing file: " + absPath, e);
                    logError(userId, folderId, absPath, "Indexing error: " + e.getMessage());
                    errorCount++;
                }
            }

            // Clean up files that were deleted from disk
            List<String> dbIndexedPaths = documentDAO.getIndexedFilePathsInFolder(userId, folderId);
            for (String dbPath : dbIndexedPaths) {
                if (!diskNormalizedPaths.contains(dbPath)) {
                    documentDAO.markOrRemoveStaleDocument(userId, dbPath);
                    LOGGER.info("Removed stale index entry for deleted file: " + dbPath);
                }
            }

            folderDAO.updateStatusAndTimestamp(folderId, userId, "INDEXED");
            if (listener != null) {
                listener.onStatusChange("Indexing completed successfully.");
                listener.onComplete(processedCount, errorCount);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fatal error indexing folder: " + folderId, e);
            try {
                folderDAO.updateStatusAndTimestamp(folderId, userId, "ERROR");
            } catch (SQLException ignored) {}
            logError(userId, folderId, "Folder #" + folderId, "Fatal: " + e.getMessage());
            if (listener != null) {
                listener.onStatusChange("Indexing failed: " + e.getMessage());
            }
        }
    }

    /**
     * Reindexes all folders registered for the user.
     */
    public void indexAllFolders(long userId, IndexingProgressListener listener) {
        cancelled.set(false);
        try {
            List<IndexedFolder> folders = folderDAO.getFoldersByUserId(userId);
            int totalProcessed = 0;
            int totalErrors = 0;

            for (IndexedFolder folder : folders) {
                if (cancelled.get()) break;
                indexFolder(userId, folder.getFolderId(), new IndexingProgressListener() {
                    @Override
                    public void onProgress(String currentFile, int processedCount, int errorCount) {
                        if (listener != null) listener.onProgress(currentFile, processedCount, errorCount);
                    }

                    @Override
                    public void onStatusChange(String statusMessage) {
                        if (listener != null) listener.onStatusChange(statusMessage);
                    }

                    @Override
                    public void onComplete(int totalProcessed, int totalErrors) {
                        // handled after loop
                    }
                });
            }

            if (listener != null) {
                listener.onStatusChange("All folders processed.");
                listener.onComplete(totalProcessed, totalErrors);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving folders for user " + userId, e);
        }
    }

    private void logError(long userId, Long folderId, String filePath, String errorMsg) {
        try {
            IndexingLog log = new IndexingLog();
            log.setUserId(userId);
            log.setFolderId(folderId);
            log.setFilePath(filePath);
            log.setStatus("ERROR");
            log.setErrorMessage(errorMsg != null && errorMsg.length() > 3900 ? errorMsg.substring(0, 3900) : errorMsg);
            logDAO.log(log);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to persist indexing log to database", e);
        }
    }
}
