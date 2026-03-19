package com.gym.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic pagination response wrapper for all paginated endpoints.
 * Used across all microservices to maintain consistent pagination responses.
 *
 * @param <T> The type of data being paginated
 *
 * Example response:
 * {
 *   "data": [...],
 *   "currentPage": 0,
 *   "pageSize": 20,
 *   "totalElements": 150,
 *   "totalPages": 8,
 *   "hasNext": true,
 *   "hasPrevious": false
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {
    /**
     * List of items in current page
     */
    private List<T> data;
    
    /**
     * Current page number (0-indexed)
     * Page 0 = first page
     */
    private int currentPage;
    
    /**
     * Number of items per page
     * Default: 20, Max: 100
     */
    private int pageSize;
    
    /**
     * Total number of records across all pages
     */
    private long totalElements;
    
    /**
     * Total number of pages
     * Calculated as: ceil(totalElements / pageSize)
     */
    private int totalPages;
    
    /**
     * Whether there is a next page
     * Useful for pagination UI
     */
    private boolean hasNext;
    
    /**
     * Whether there is a previous page
     * Useful for pagination UI
     */
    private boolean hasPrevious;
    
    /**
     * Factory method to convert Spring Data Page to PageResponse
     *
     * @param page Spring Data Page object
     * @param <T>  Type of data
     * @return PageResponse with metadata
     */
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
        return PageResponse.<T>builder()
                .data(page.getContent())
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
