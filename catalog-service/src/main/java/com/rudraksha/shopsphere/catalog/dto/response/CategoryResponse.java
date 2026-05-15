package com.rudraksha.shopsphere.catalog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private String id;
    private String name;
    private String description;
    private String parentId;
    private List<String> ancestors;
    private int level;
    private String path;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
