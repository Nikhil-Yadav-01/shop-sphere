package com.rudraksha.shopsphere.media.controller;

import com.rudraksha.shopsphere.media.dto.MediaResponseDto;
import com.rudraksha.shopsphere.media.dto.PagedMediaResponseDto;
import com.rudraksha.shopsphere.media.entity.Media;
import com.rudraksha.shopsphere.media.service.MediaService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(
                java.util.Map.of(
                        "status", "ok",
                        "service", "media",
                        "timestamp", LocalDateTime.now().toString()
                )
        );
    }

    @PostMapping("/upload")
    public ResponseEntity<MediaResponseDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entityType") @NotBlank String entityType,
            @RequestParam("entityId") @NotNull Long entityId,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy
    ) {
        Media saved = mediaService.uploadMedia(file, entityType, entityId, uploadedBy);

        MediaResponseDto resp = MediaResponseDto.builder()
                .status("success")
                .id(saved.getId())
                .fileName(saved.getFileName())
                .fileType(saved.getFileType())
                .fileSize(saved.getFileSize())
                .filePath(saved.getFilePath())
                .entityType(saved.getEntityType())
                .entityId(saved.getEntityId())
                .uploadedBy(saved.getUploadedBy())
                .uploadedAt(saved.getUploadedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<PagedMediaResponseDto> getByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId
    ) {
        List<Media> list = mediaService.getMediaByEntity(entityType, entityId);
        List<MediaResponseDto> dtos = list.stream().map(this::toDto).collect(Collectors.toList());
        PagedMediaResponseDto resp = PagedMediaResponseDto.builder()
                .status("success")
                .count(dtos.size())
                .data(dtos)
                .build();
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/type/{entityType}")
    public ResponseEntity<PagedMediaResponseDto> getAllByType(@PathVariable String entityType) {
        List<Media> list = mediaService.getAllMediaByType(entityType);
        List<MediaResponseDto> dtos = list.stream().map(this::toDto).collect(Collectors.toList());
        PagedMediaResponseDto resp = PagedMediaResponseDto.builder()
                .status("success")
                .count(dtos.size())
                .data(dtos)
                .build();
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediaResponseDto> getById(@PathVariable Long id) {
        Media m = mediaService.getMediaById(id);
        return ResponseEntity.ok(toDto(m));
    }

    @PutMapping("/{id}/soft-delete")
    public ResponseEntity<?> softDelete(@PathVariable Long id) {
        mediaService.softDelete(id);
        return ResponseEntity.ok(java.util.Map.of("status", "success", "id", id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> hardDelete(@PathVariable Long id) {
        mediaService.hardDelete(id);
        return ResponseEntity.ok(java.util.Map.of("status", "success", "id", id));
    }

    // helper
    private MediaResponseDto toDto(Media m) {
        return MediaResponseDto.builder()
                .status("success")
                .id(m.getId())
                .fileName(m.getFileName())
                .fileType(m.getFileType())
                .fileSize(m.getFileSize())
                .filePath(m.getFilePath())
                .entityType(m.getEntityType())
                .entityId(m.getEntityId())
                .uploadedBy(m.getUploadedBy())
                .uploadedAt(m.getUploadedAt())
                .build();
    }
}
