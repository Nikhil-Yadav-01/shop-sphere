package com.rudraksha.shopsphere.search.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultResponse {
    private Page<SearchResponse> results;
    private Long totalResults;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;
}
