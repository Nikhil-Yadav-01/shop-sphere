package com.rudraksha.shopsphere.search.repository;

import com.rudraksha.shopsphere.search.entity.SearchIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SearchIndexRepository extends MongoRepository<SearchIndex, String> {

    Optional<SearchIndex> findByProductId(String productId);

    void deleteByProductId(String productId);

    @Query("{ $text: { $search: ?0 } }")
    Page<SearchIndex> searchByText(String keyword, Pageable pageable);

    Page<SearchIndex> findByStatus(String status, Pageable pageable);

    Page<SearchIndex> findByCategoryId(String categoryId, Pageable pageable);

    Page<SearchIndex> findByCategoryName(String categoryName, Pageable pageable);

    Page<SearchIndex> findByInStock(Boolean inStock, Pageable pageable);

    Page<SearchIndex> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    @Query("{ 'status': ?0, 'inStock': ?1 }")
    Page<SearchIndex> findByStatusAndInStock(String status, Boolean inStock, Pageable pageable);

    List<SearchIndex> findByRatingGreaterThanEqualAndStatusOrderByRatingDesc(BigDecimal minRating, String status);

    long deleteByStatusNotIn(List<String> statuses);

    @Query(value = "{ $text: { $search: ?0, $caseSensitive: false } }", sort = "{ 'score': { $meta: 'textScore' } }")
    Page<SearchIndex> searchWithScore(String keyword, Pageable pageable);
}
