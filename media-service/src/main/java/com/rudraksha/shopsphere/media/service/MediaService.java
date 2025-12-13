package com.rudraksha.shopsphere.media.service;

import com.rudraksha.shopsphere.media.entity.Media;
import com.rudraksha.shopsphere.media.exception.MediaNotFoundException;
import com.rudraksha.shopsphere.media.exception.MediaStorageException;
import com.rudraksha.shopsphere.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaRepository mediaRepository;

    @Value("${media.upload.dir:uploads}")
    private String uploadDir;

    @Value("${media.max-file-size:10485760}") // 10 MB
    private Long maxFileSize;

    private final List<String> allowedMimeTypes = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/mpeg", "video/quicktime",
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    /**
     * Uploads a multipart file to the local disk and saves metadata to DB.
     *
     * @param file       multipart file
     * @param entityType logical entity type (product, gallery, etc.)
     * @param entityId   entity id
     * @param uploadedBy optional username
     * @return persisted Media entity
     */
    @Transactional
    public Media uploadMedia(MultipartFile file, String entityType, Long entityId, String uploadedBy) {
        // Validate inputs
        if (entityType == null || entityType.isBlank()) {
            throw new MediaStorageException("entityType is required");
        }
        if (entityId == null) {
            throw new MediaStorageException("entityId is required");
        }

        validateFile(file);

        String original = file.getOriginalFilename() == null ? "unknown" : sanitizeFileName(file.getOriginalFilename());
        String storedFileName = generateUniqueFileName(original);
        Path targetDir = getTargetDirectory(entityType, entityId);

        try {
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(storedFileName);

            // Write to a temp file in same directory and then atomic move to the final location
            Path tempFile = Files.createTempFile(targetDir, "upload-", ".tmp");
            try (OutputStream os = Files.newOutputStream(tempFile, StandardOpenOption.TRUNCATE_EXISTING)) {
                os.write(file.getBytes());
                os.flush();
            }

            // Use ATOMIC_MOVE where possible
            try {
                Files.move(tempFile, targetPath, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                // Fallback to non-atomic move if filesystem doesn't support atomic move
                log.warn("Atomic move not supported for {}. Falling back to regular move.", targetPath);
                Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            Media media = Media.builder()
                    .fileName(original)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .filePath(targetPath.toString())
                    .entityType(entityType)
                    .entityId(entityId)
                    .uploadedBy(uploadedBy)
                    .uploadedAt(LocalDateTime.now())
                    .isActive(true)
                    .build();

            Media saved = mediaRepository.save(media);
            log.info("Stored media id={} entity={}/{} path={}", saved.getId(), entityType, entityId, targetPath);
            return saved;

        } catch (IOException e) {
            log.error("Failed to store file {} for {}/{}. Error: {}", original, entityType, entityId, e.getMessage(), e);
            throw new MediaStorageException("Failed to store file: " + e.getMessage(), e);
        }
    }

    public List<Media> getMediaByEntity(String entityType, Long entityId) {
        return mediaRepository.findByEntityTypeAndEntityIdAndIsActiveTrue(entityType, entityId);
    }

    public List<Media> getAllMediaByType(String entityType) {
        return mediaRepository.findByEntityTypeAndIsActiveTrue(entityType);
    }

    public Media getMediaById(Long id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new MediaNotFoundException("Media not found with id: " + id));

        if (media.getIsActive() != null && !media.getIsActive()) {
            throw new MediaNotFoundException("Media not found with id: " + id);
        }

        return media;
    }

    @Transactional
    public void softDelete(Long id) {
        Media m = getMediaById(id);
        m.setIsActive(false);
        mediaRepository.save(m);
        log.info("Soft deleted media id={}", id);
    }

    @Transactional
    public void hardDelete(Long id) {
        Media m = getMediaById(id);

        // delete file first
        try {
            Path p = Paths.get(m.getFilePath());
            Files.deleteIfExists(p);
            log.info("Deleted file on disk for media id={}", id);
        } catch (IOException e) {
            log.warn("Error deleting file from disk for id {}: {}. Proceeding to delete DB record.", id, e.getMessage());
        }

        mediaRepository.deleteById(id);
        log.info("Hard deleted DB record for media id={}", id);
    }

    // -------------------------
    // Private helpers
    // -------------------------

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MediaStorageException("File cannot be empty");
        }

        if (file.getSize() <= 0) {
            throw new MediaStorageException("File size is zero");
        }

        if (file.getSize() > maxFileSize) {
            throw new MediaStorageException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new MediaStorageException("Could not determine file content type");
        }

        // Explicitly reject generic/unknown octet-stream to avoid accepting executables or raw binaries
        if ("application/octet-stream".equalsIgnoreCase(contentType)) {
            log.warn("Rejected upload with contentType=application/octet-stream");
            throw new MediaStorageException("File type not allowed: application/octet-stream");
        }

        if (!allowedMimeTypes.contains(contentType.toLowerCase(Locale.ROOT))) {
            log.warn("Rejected upload with unsupported contentType={}", contentType);
            throw new MediaStorageException("File type not allowed: " + contentType);
        }

        // Best-effort: check extension consistency (not authoritative, just additional guard)
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = FilenameUtils.getExtension(original).toLowerCase(Locale.ROOT);
        if (ext.isBlank()) {
            // no extension — acceptable but logged
            log.debug("Uploaded file has no extension: original='{}'", original);
            return;
        }

        // Map common extensions to expected mime groups
        boolean extMatchesType = switch (ext) {
            case "jpg", "jpeg" -> contentType.startsWith("image/");
            case "png", "gif", "webp" -> contentType.startsWith("image/");
            case "mp4", "mpeg", "mov", "quicktime" -> contentType.startsWith("video/");
            case "pdf" -> contentType.equalsIgnoreCase("application/pdf");
            case "doc", "docx" -> contentType.contains("wordprocessingml") || contentType.contains("msword");
            default -> true; // unknown ext -> don't block, only warn
        };

        if (!extMatchesType) {
            log.warn("File extension '{}' does not appear to match content type '{}'. Proceeding but this may indicate tampering.", ext, contentType);
            // We do NOT throw here (to avoid false negatives), but we log the mismatch.
        }
    }

    private Path getTargetDirectory(String entityType, Long entityId) {
        return Paths.get(uploadDir, entityType, String.valueOf(entityId));
    }

    private String sanitizeFileName(String original) {
        if (original == null) return "unknown";
        return original.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }

    private String generateUniqueFileName(String originalFileName) {
        String ext = FilenameUtils.getExtension(originalFileName);
        String normalizedExt = (ext == null || ext.isBlank()) ? "" : "." + ext;
        return UUID.randomUUID().toString() + normalizedExt;
    }
}
