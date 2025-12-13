package com.rudraksha.shopsphere.media.controller;

import com.rudraksha.shopsphere.media.dto.MediaResponseDto;
import com.rudraksha.shopsphere.media.dto.PagedMediaResponseDto;
import com.rudraksha.shopsphere.media.entity.Media;
import com.rudraksha.shopsphere.media.mapper.MediaMapper;
import com.rudraksha.shopsphere.media.service.MediaService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Validated
public class MediaController {

    private final MediaService mediaService;
    private final MediaMapper mediaMapper;

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, String> body = new java.util.HashMap<>();
        body.put("status", "ok");
        body.put("service", "media");
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(
                body
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
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaMapper.toResponseDto(saved));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<PagedMediaResponseDto> getByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId
    ) {
        List<Media> list = mediaService.getMediaByEntity(entityType, entityId);
        List<MediaResponseDto> dtos = list.stream().map(mediaMapper::toResponseDto).collect(Collectors.toList());
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
        List<MediaResponseDto> dtos = list.stream().map(mediaMapper::toResponseDto).collect(Collectors.toList());
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
        return ResponseEntity.ok(mediaMapper.toResponseDto(m));
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

}
