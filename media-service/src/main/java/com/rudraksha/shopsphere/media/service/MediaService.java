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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaRepository mediaRepository;

    @Value("${media.upload.dir:uploads}")
    private String uploadDir;

    @Value("${media.max-file-size:10485760}")
    private Long maxFileSize;

    private final List<String> allowedMimeTypes = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/mpeg", "video/quicktime",
            "application/pdf", "application/msword"
    );

    public Media uploadMedia(MultipartFile file, String entityType, Long entityId, String uploadedBy) {
        try {
            validateFile(file);

            String fileName = generateFileName(file.getOriginalFilename());
            Path uploadPath = Paths.get(uploadDir, entityType, entityId.toString());
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, file.getBytes());

            Media media = new Media();
            media.setFileName(file.getOriginalFilename());
            media.setFileType(file.getContentType());
            media.setFileSize(file.getSize());
            media.setFilePath(filePath.toString());
            media.setEntityType(entityType);
            media.setEntityId(entityId);
            media.setUploadedAt(LocalDateTime.now());
            media.setUploadedBy(uploadedBy);
            media.setIsActive(true);

            log.info("Uploading media: {} for entity {} with id {} to path {}", fileName, entityType, entityId, filePath);
            return media;

        } catch (IOException e) {
            log.error("Error uploading file", e);
            throw new MediaStorageException("Failed to upload file: " + e.getMessage());
        }
    }

    public List<Media> getMediaByEntity(String entityType, Long entityId) {
        log.info("Fetching media for entity {} with id {}", entityType, entityId);
        return mediaRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    public Media getMediaById(Long id) {
        log.info("Fetching media with id {}", id);
        return mediaRepository.findById(id)
                .orElseThrow(() -> new MediaNotFoundException("Media not found with id: " + id));
    }

    public void deleteMedia(Long id) {
        try {
            Media media = getMediaById(id);
            Path filePath = Paths.get(media.getFilePath());
            Files.deleteIfExists(filePath);
            mediaRepository.deleteById(id);
            log.info("Deleted media with id {}", id);
        } catch (IOException e) {
            log.error("Error deleting file: {}", e.getMessage());
            throw new MediaStorageException("Failed to delete file: " + e.getMessage());
        }
    }

    public void softDeleteMedia(Long id) {
        Media media = getMediaById(id);
        media.setIsActive(false);
        mediaRepository.save(media);
        log.info("Soft deleted media with id {}", id);
    }

    public List<Media> getAllMedia(String entityType) {
        log.info("Fetching all media for entity type {}", entityType);
        return mediaRepository.findByEntityType(entityType);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MediaStorageException("File cannot be empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new MediaStorageException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }

        if (!allowedMimeTypes.contains(file.getContentType())) {
            throw new MediaStorageException("File type not allowed: " + file.getContentType());
        }
    }

    private String generateFileName(String originalFileName) {
        String extension = FilenameUtils.getExtension(originalFileName);
        return UUID.randomUUID() + "." + extension;
    }
}
