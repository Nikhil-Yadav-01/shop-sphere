package com.rudraksha.shopsphere.catalog.service.impl;

import com.rudraksha.shopsphere.catalog.dto.request.CreateCategoryRequest;
import com.rudraksha.shopsphere.catalog.dto.response.CategoryResponse;
import com.rudraksha.shopsphere.catalog.entity.Category;
import com.rudraksha.shopsphere.catalog.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;
    private String categoryId = "cat-123";

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(categoryId)
                .name("Electronics")
                .level(0)
                .path("Electronics")
                .active(true)
                .build();
    }

    @Test
    void createCategory_Success_NoParent() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Electronics");

        when(categoryRepository.existsByName("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals("Electronics", response.getName());
        assertEquals(0, response.getLevel());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_Success_WithParent() {
        Category parent = Category.builder()
                .id("parent-id")
                .name("Root")
                .level(0)
                .path("Root")
                .build();

        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Electronics");
        request.setParentId("parent-id");

        when(categoryRepository.existsByName("Electronics")).thenReturn(false);
        when(categoryRepository.findById("parent-id")).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals(1, response.getLevel());
        assertTrue(response.getPath().contains("Root > Electronics"));
    }

    @Test
    void getCategoryById_Success() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.getCategoryById(categoryId);

        assertNotNull(response);
        assertEquals(categoryId, response.getId());
    }

    @Test
    void deleteCategory_Success() {
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        doNothing().when(categoryRepository).deleteById(categoryId);

        assertDoesNotThrow(() -> categoryService.deleteCategory(categoryId));
        verify(categoryRepository).deleteById(categoryId);
    }

    @Test
    void getAllCategories_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Category> page = new PageImpl<>(Collections.singletonList(category));
        when(categoryRepository.findAll(pageable)).thenReturn(page);

        Page<CategoryResponse> response = categoryService.getAllCategories(pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }
}
