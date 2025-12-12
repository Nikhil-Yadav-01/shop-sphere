package com.rudraksha.shopsphere.media.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MediaResponseDto {
    private String status;      // e.g. "success"
    private Long id;            // root-level id for test compatibility
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String filePath;
    private String entityType;
    private Long entityId;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}
