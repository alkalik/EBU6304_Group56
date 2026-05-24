package com.recruitment.service;

import com.recruitment.model.User;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * File-system backup and restore for application JSON data.
 * <p>
 * Copies all {@code *.json} files from the workspace {@code data/} directory into
 * timestamped folders under {@code data/backups/}, optionally packaging them as a ZIP.
 * Restore copies JSON files back from a backup folder or extracts a backup ZIP into {@code data/}.
 * </p>
 * <p>
 * This service does not use {@link com.recruitment.util.JsonUtil}; it operates directly on
 * the file system. Role-based access control limits backup and restore to
 * {@link User.Role#ADMIN} and {@link User.Role#MO} users. UI feedback is shown via
 * {@link JOptionPane} dialogs.
 * </p>
 */
public class BackupService {

    private static final Path DATA_DIR = Paths.get("data");
    private static final Path BACKUPS_DIR = DATA_DIR.resolve("backups");

    /**
     * Determines whether the given role may perform backup or restore operations.
     *
     * @param role the user's role
     * @return {@code true} for ADMIN or MO; {@code false} otherwise
     */
    public static boolean canBackup(User.Role role) {
        return role == User.Role.ADMIN || role == User.Role.MO;
    }

    /**
     * Creates a timestamped backup of all JSON files in {@code data/}.
     * <p>
     * Optionally also writes a ZIP archive alongside the backup folder. Shows success or
     * error dialogs via Swing.
     * </p>
     *
     * @param user      the acting user (must have ADMIN or MO role)
     * @param createZip if {@code true}, also create a {@code .zip} of the copied JSON files
     * @return a {@link BackupResult} describing success, message, and backup directory path
     */
    public static BackupResult backupAllData(User user, boolean createZip) {
        if (user == null || !canBackup(user.getRole())) {
            String msg = "You do not have permission to perform backups.";
            JOptionPane.showMessageDialog(null, msg, "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return new BackupResult(false, msg, null);
        }

        try {
            if (!Files.exists(DATA_DIR) || !Files.isDirectory(DATA_DIR)) {
                String msg = "Data directory not found: " + DATA_DIR.toAbsolutePath();
                JOptionPane.showMessageDialog(null, msg, "Backup Failed", JOptionPane.ERROR_MESSAGE);
                return new BackupResult(false, msg, null);
            }

            Files.createDirectories(BACKUPS_DIR);
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Path dest = BACKUPS_DIR.resolve("backup-" + ts);
            Files.createDirectories(dest);

            List<Path> copied = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(DATA_DIR, "*.json")) {
                for (Path p : stream) {
                    Path target = dest.resolve(p.getFileName());
                    copyFile(p, target);
                    copied.add(target);
                }
            }

            Path zipPath = null;
            if (createZip) {
                zipPath = BACKUPS_DIR.resolve("backup-" + ts + ".zip");
                try (OutputStream fos = Files.newOutputStream(zipPath);
                     ZipOutputStream zos = new ZipOutputStream(fos)) {
                    for (Path p : copied) {
                        ZipEntry entry = new ZipEntry(p.getFileName().toString());
                        zos.putNextEntry(entry);
                        Files.copy(p, zos);
                        zos.closeEntry();
                    }
                }
            }

            String successMsg = "Backup succeeded: " + dest.toAbsolutePath();
            if (zipPath != null) successMsg += " (zip: " + zipPath.getFileName() + ")";
            JOptionPane.showMessageDialog(null, successMsg, "Backup Complete", JOptionPane.INFORMATION_MESSAGE);
            return new BackupResult(true, successMsg, dest);

        } catch (Exception e) {
            String msg = "Backup failed: " + e.getMessage();
            JOptionPane.showMessageDialog(null, msg, "Backup Failed", JOptionPane.ERROR_MESSAGE);
            return new BackupResult(false, msg, null);
        }
    }

    /**
     * Lists the names of entries in {@code data/backups/} (folders and files).
     *
     * @return backup folder or file names; empty if the backups directory does not exist
     */
    public static List<String> listBackups() {
        List<String> out = new ArrayList<>();
        if (!Files.exists(BACKUPS_DIR)) return out;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(BACKUPS_DIR)) {
            for (Path p : ds) {
                out.add(p.getFileName().toString());
            }
        } catch (IOException ignored) {
        }
        return out;
    }

    /**
     * Restores JSON data from a named backup folder or ZIP under {@code data/backups/}.
     * <p>
     * If {@code backupName} is a directory, copies {@code *.json} files into {@code data/}.
     * If it is a file (e.g. ZIP), extracts contents into {@code data/}.
     * </p>
     *
     * @param user       the acting user (must have ADMIN or MO role)
     * @param backupName the folder or file name within {@code data/backups/}
     * @return a {@link BackupResult} describing success, message, and source path used
     */
    public static BackupResult restoreBackup(User user, String backupName) {
        if (user == null || !canBackup(user.getRole())) {
            String msg = "You do not have permission to restore backups.";
            JOptionPane.showMessageDialog(null, msg, "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return new BackupResult(false, msg, null);
        }

        try {
            Path candidateDir = BACKUPS_DIR.resolve(backupName);
            Path candidateZip = BACKUPS_DIR.resolve(backupName);

            if (Files.exists(candidateDir) && Files.isDirectory(candidateDir)) {
                // copy files back to DATA_DIR
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(candidateDir, "*.json")) {
                    for (Path p : ds) {
                        Path target = DATA_DIR.resolve(p.getFileName());
                        copyFile(p, target);
                    }
                }
                String msg = "Restore succeeded from: " + candidateDir.toAbsolutePath();
                JOptionPane.showMessageDialog(null, msg, "Restore Complete", JOptionPane.INFORMATION_MESSAGE);
                return new BackupResult(true, msg, candidateDir);
            }

            // If backupName refers to a zip file
            if (Files.exists(candidateZip) && !Files.isDirectory(candidateZip)) {
                // try to extract zip entries into DATA_DIR
                unzipTo(candidateZip, DATA_DIR);
                String msg = "Restore succeeded from zip: " + candidateZip.toAbsolutePath();
                JOptionPane.showMessageDialog(null, msg, "Restore Complete", JOptionPane.INFORMATION_MESSAGE);
                return new BackupResult(true, msg, candidateZip);
            }

            String msg = "Backup not found: " + backupName;
            JOptionPane.showMessageDialog(null, msg, "Restore Failed", JOptionPane.ERROR_MESSAGE);
            return new BackupResult(false, msg, null);
        } catch (Exception e) {
            String msg = "Restore failed: " + e.getMessage();
            JOptionPane.showMessageDialog(null, msg, "Restore Failed", JOptionPane.ERROR_MESSAGE);
            return new BackupResult(false, msg, null);
        }
    }

    private static void copyFile(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (InputStream in = Files.newInputStream(source);
             OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }
    }

    private static void unzipTo(Path zipFile, Path targetDir) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(zipFile, (ClassLoader) null)) {
            for (Path root : fs.getRootDirectories()) {
                Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path rel = root.relativize(file);
                        Path dest = targetDir.resolve(rel.toString());
                        Files.createDirectories(dest.getParent());
                        Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
    }

    /**
     * Outcome DTO for backup and restore operations.
     */
    public static class BackupResult {
        private final boolean success;
        private final String message;
        private final Path backupPath;

        /**
         * Constructs a backup operation result.
         *
         * @param success    whether the operation completed successfully
         * @param message    human-readable status or error message
         * @param backupPath path to the backup folder or archive used; {@code null} on failure
         */
        public BackupResult(boolean success, String message, Path backupPath) {
            this.success = success;
            this.message = message;
            this.backupPath = backupPath;
        }

        /**
         * @return {@code true} if the operation succeeded
         */
        public boolean isSuccess() { return success; }

        /**
         * @return status or error message suitable for display
         */
        public String getMessage() { return message; }

        /**
         * @return the backup directory or archive path; may be {@code null}
         */
        public Path getBackupPath() { return backupPath; }
    }
}
