package com.rudraksha.shopsphere.media.mapper;

import com.rudraksha.shopsphere.media.dto.MediaResponseDto;
import com.rudraksha.shopsphere.media.entity.Media;
import org.springframework.stereotype.Component;

@Component
public class MediaMapper {

    public MediaResponseDto toResponseDto(Media media) {
        if (media == null) {
            return null;
        }

        return MediaResponseDto.builder()
                .status("success")
                .id(media.getId())
                .fileName(media.getFileName())
                .fileType(media.getFileType())
                .fileSize(media.getFileSize())
                .filePath(media.getFilePath())
                .entityType(media.getEntityType())
                .entityId(media.getEntityId())
                .uploadedBy(media.getUploadedBy())
                .uploadedAt(media.getUploadedAt())
                .build();
    }
}
