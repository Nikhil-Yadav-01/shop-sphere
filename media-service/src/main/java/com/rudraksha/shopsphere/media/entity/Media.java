package com.rudraksha.shopsphere.media.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "media")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String entityType; // product, category, user, etc.

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column
    private String uploadedBy;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean isActive = true;
}
