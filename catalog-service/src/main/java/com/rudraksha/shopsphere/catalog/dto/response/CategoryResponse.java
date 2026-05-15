package com.rudraksha.shopsphere.catalog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private String id;
    private String name;
    private String description;
    private String parentId;
    private int level;
    private String path;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
