package com.tariffsheriff.backend.tariff.repository;

import com.tariffsheriff.backend.tariff.model.HsProduct;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HsProductRepository extends JpaRepository<HsProduct, Long> {
    Optional<HsProduct> findByHsCode(String hsCode);

    Optional<HsProduct> findByDestinationIdAndHsCode(Long destinationId, String hsCode);
    
    /**
     * Search for HS products by description using case-insensitive LIKE matching
     * Orders results by HS code for consistency
     */
    @Query("SELECT h FROM HsProduct h WHERE LOWER(h.hsLabel) LIKE LOWER(CONCAT('%', :description, '%')) ORDER BY h.hsCode")
    List<HsProduct> findByHsLabelContainingIgnoreCase(@Param("description") String description);
    
    /**
     * Search for HS products by description with limit
     */
    @Query(value = "SELECT * FROM hs_product WHERE LOWER(hs_label) LIKE LOWER(CONCAT('%', :description, '%')) ORDER BY hs_code LIMIT :limit", nativeQuery = true)
    List<HsProduct> findByHsLabelContainingIgnoreCaseWithLimit(@Param("description") String description, @Param("limit") int limit);
    
    /**
     * Search for HS products using multiple keywords (all must match)
     */
    @Query("SELECT h FROM HsProduct h WHERE " +
           "(:keyword1 IS NULL OR LOWER(h.hsLabel) LIKE LOWER(CONCAT('%', :keyword1, '%'))) AND " +
           "(:keyword2 IS NULL OR LOWER(h.hsLabel) LIKE LOWER(CONCAT('%', :keyword2, '%'))) AND " +
           "(:keyword3 IS NULL OR LOWER(h.hsLabel) LIKE LOWER(CONCAT('%', :keyword3, '%'))) " +
           "ORDER BY h.hsCode")
    List<HsProduct> findByMultipleKeywords(@Param("keyword1") String keyword1, 
                                          @Param("keyword2") String keyword2, 
                                          @Param("keyword3") String keyword3);
}
