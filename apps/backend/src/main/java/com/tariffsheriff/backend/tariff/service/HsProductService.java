package com.tariffsheriff.backend.tariff.service;

import com.tariffsheriff.backend.tariff.model.HsProduct;
import java.util.List;

public interface HsProductService {
    
    /**
     * Search for HS products by description using fuzzy matching
     * @param description Product description to search for
     * @param limit Maximum number of results to return
     * @return List of matching HS products ordered by relevance
     */
    List<HsProduct> searchByDescription(String description, int limit);
    
    /**
     * Search for HS products by description with default limit
     * @param description Product description to search for
     * @return List of matching HS products ordered by relevance (max 10)
     */
    List<HsProduct> searchByDescription(String description);
    
    /**
     * Get HS product by exact HS code
     * @param hsCode The HS code to look up
     * @return HsProduct if found, null otherwise
     */
    HsProduct getByHsCode(String hsCode);
}