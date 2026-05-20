package com.rudraksha.shopsphere.catalog.service;

import com.rudraksha.shopsphere.catalog.dto.request.CreateCategoryRequest;
import com.rudraksha.shopsphere.catalog.dto.response.CategoryResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CreateCategoryRequest request);
    CategoryResponse getCategoryById(String id);
    CategoryResponse updateCategory(String id, CreateCategoryRequest request);
    void deleteCategory(String id);
    Page<CategoryResponse> getAllCategories(Pageable pageable);
}
