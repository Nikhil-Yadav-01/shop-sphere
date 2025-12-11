package com.rudraksha.shopsphere.media.controller;

import com.rudraksha.shopsphere.media.entity.Media;
import com.rudraksha.shopsphere.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") Long entityId,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy) {

        log.info("Received upload request for entity {} with id {}", entityType, entityId);

        Media media = mediaService.uploadMedia(file, entityType, entityId, uploadedBy);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "File uploaded successfully");
        response.put("data", media);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<Map<String, Object>> getMediaByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {

        log.info("Fetching media for entity {} with id {}", entityType, entityId);

        List<Media> mediaList = mediaService.getMediaByEntity(entityType, entityId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("count", mediaList.size());
        response.put("data", mediaList);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMediaById(@PathVariable Long id) {
        log.info("Fetching media with id {}", id);

        Media media = mediaService.getMediaById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", media);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/type/{entityType}")
    public ResponseEntity<Map<String, Object>> getAllMediaByType(@PathVariable String entityType) {
        log.info("Fetching all media for entity type {}", entityType);

        List<Media> mediaList = mediaService.getAllMedia(entityType);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("count", mediaList.size());
        response.put("data", mediaList);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMedia(@PathVariable Long id) {
        log.info("Deleting media with id {}", id);

        mediaService.deleteMedia(id);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Media deleted successfully");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/soft-delete")
    public ResponseEntity<Map<String, Object>> softDeleteMedia(@PathVariable Long id) {
        log.info("Soft deleting media with id {}", id);

        mediaService.softDeleteMedia(id);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Media marked as inactive");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "media-service");
        return ResponseEntity.ok(response);
    }
}
