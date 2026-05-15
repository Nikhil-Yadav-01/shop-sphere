package com.rudraksha.shopsphere.catalog.service.impl;

import com.rudraksha.shopsphere.catalog.dto.request.CreateCategoryRequest;
import com.rudraksha.shopsphere.catalog.dto.response.CategoryResponse;
import com.rudraksha.shopsphere.catalog.entity.Category;
import com.rudraksha.shopsphere.catalog.repository.CategoryRepository;
import com.rudraksha.shopsphere.catalog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Category with name " + request.getName() + " already exists");
        }

        Category.CategoryBuilder categoryBuilder = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(true);

        if (request.getParentId() != null && !request.getParentId().isEmpty()) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
            categoryBuilder.parentId(parent.getId());
            categoryBuilder.level(parent.getLevel() + 1);
            categoryBuilder.path(parent.getPath() + " > " + request.getName());
        } else {
            categoryBuilder.level(0);
            categoryBuilder.path(request.getName());
        }

        Category savedCategory = categoryRepository.save(categoryBuilder.build());
        log.info("Created category with ID: {}", savedCategory.getId());
        return mapToResponse(savedCategory);
    }

    @Override
    @Cacheable(value = "categories", key = "#id")
    public CategoryResponse getCategoryById(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));
        return mapToResponse(category);
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(String id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("Category with name " + request.getName() + " already exists");
            }
            category.setName(request.getName());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        if (request.getParentId() != null && !request.getParentId().equals(category.getParentId())) {
             if (request.getParentId().isEmpty()) {
                 category.setParentId(null);
                 category.setLevel(0);
                 category.setPath(category.getName());
             } else {
                 Category parent = categoryRepository.findById(request.getParentId())
                         .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
                 category.setParentId(parent.getId());
                 category.setLevel(parent.getLevel() + 1);
                 category.setPath(parent.getPath() + " > " + category.getName());
             }
        }

        Category updatedCategory = categoryRepository.save(category);
        log.info("Updated category with ID: {}", updatedCategory.getId());
        return mapToResponse(updatedCategory);
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(String id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Category not found with ID: " + id);
        }
        categoryRepository.deleteById(id);
        log.info("Deleted category with ID: {}", id);
    }

    @Override
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(category.getParentId())
                .level(category.getLevel())
                .path(category.getPath())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
