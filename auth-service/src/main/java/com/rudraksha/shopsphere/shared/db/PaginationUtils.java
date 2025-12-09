package com.rudraksha.shopsphere.shared.db;

import com.rudraksha.shopsphere.shared.models.PagingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationUtils {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PaginationUtils() {
    }

    public static Pageable createPageRequest(Integer page, Integer size, Sort sort) {
        int pageNumber = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int pageSize = (size != null && size > 0) ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;

        if (sort != null && sort.isSorted()) {
            return PageRequest.of(pageNumber, pageSize, sort);
        }
        return PageRequest.of(pageNumber, pageSize);
    }

    public static Pageable createPageRequest(Integer page, Integer size, String sortField, Sort.Direction direction) {
        Sort sort = (sortField != null && !sortField.isBlank())
                ? Sort.by(direction != null ? direction : Sort.Direction.ASC, sortField)
                : Sort.unsorted();
        return createPageRequest(page, size, sort);
    }

    public static Pageable createPageRequest(Integer page, Integer size) {
        return createPageRequest(page, size, Sort.unsorted());
    }

    public static <T> PagingResponse<T> toPageResponse(Page<T> page) {
        return PagingResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
