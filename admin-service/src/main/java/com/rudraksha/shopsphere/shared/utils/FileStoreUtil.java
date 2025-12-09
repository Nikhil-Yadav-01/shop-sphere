package com.rudraksha.shopsphere.shared.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public final class FileStoreUtil {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("pdf", "doc", "docx", "xls", "xlsx", "txt", "csv");
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    private FileStoreUtil() {
    }

    public static String getExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1).toLowerCase();
    }

    public static String getFilenameWithoutExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return filename;
        }
        return filename.substring(0, lastDot);
    }

    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^[._-]+", "")
                .replaceAll("[._-]+$", "");
    }

    public static String generateStoragePath(String baseDir, String filename) {
        if (baseDir == null || filename == null) {
            return null;
        }
        String uniqueName = IdGenerator.generateShortId() + "_" + sanitizeFilename(filename);
        return Paths.get(baseDir, uniqueName).toString();
    }

    public static String generateStoragePath(String baseDir, String subDir, String filename) {
        if (baseDir == null || filename == null) {
            return null;
        }
        String uniqueName = IdGenerator.generateShortId() + "_" + sanitizeFilename(filename);
        Path path = subDir != null
                ? Paths.get(baseDir, subDir, uniqueName)
                : Paths.get(baseDir, uniqueName);
        return path.toString();
    }

    public static boolean isImageFile(String filename) {
        String ext = getExtension(filename);
        return IMAGE_EXTENSIONS.contains(ext);
    }

    public static boolean isDocumentFile(String filename) {
        String ext = getExtension(filename);
        return DOCUMENT_EXTENSIONS.contains(ext);
    }

    public static boolean isAllowedExtension(String filename, Set<String> allowedExtensions) {
        if (allowedExtensions == null || allowedExtensions.isEmpty()) {
            return true;
        }
        String ext = getExtension(filename);
        return allowedExtensions.contains(ext.toLowerCase());
    }

    public static boolean isValidFileSize(long sizeInBytes) {
        return sizeInBytes > 0 && sizeInBytes <= MAX_FILE_SIZE_BYTES;
    }

    public static boolean isValidFileSize(long sizeInBytes, long maxSizeBytes) {
        return sizeInBytes > 0 && sizeInBytes <= maxSizeBytes;
    }

    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < 1024 * 1024) {
            return String.format("%.2f KB", sizeInBytes / 1024.0);
        } else if (sizeInBytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", sizeInBytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", sizeInBytes / (1024.0 * 1024 * 1024));
        }
    }

    public static String getContentType(String filename) {
        String ext = getExtension(filename);
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "pdf" -> "application/pdf";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "html" -> "text/html";
            default -> "application/octet-stream";
        };
    }
}
