package com.tariffsheriff.backend.tariff.service;

import com.tariffsheriff.backend.tariff.model.HsProduct;
import com.tariffsheriff.backend.tariff.repository.HsProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HsProductServiceImpl implements HsProductService {
    
    private static final Logger logger = LoggerFactory.getLogger(HsProductServiceImpl.class);
    private static final int DEFAULT_SEARCH_LIMIT = 10;
    
    private final HsProductRepository hsProductRepository;
    
    public HsProductServiceImpl(HsProductRepository hsProductRepository) {
        this.hsProductRepository = hsProductRepository;
    }
    
    @Override
    public List<HsProduct> searchByDescription(String description, int limit) {
        if (description == null || description.trim().isEmpty()) {
            logger.warn("Empty description provided for HS product search");
            return new ArrayList<>();
        }
        
        String cleanDescription = description.trim();
        logger.info("Searching for HS products with description: '{}' (limit: {})", cleanDescription, limit);
        
        try {
            // First try exact phrase matching
            List<HsProduct> exactMatches = hsProductRepository.findByHsLabelContainingIgnoreCaseWithLimit(
                cleanDescription, limit);
            
            if (!exactMatches.isEmpty()) {
                logger.info("Found {} exact matches for description: '{}'", exactMatches.size(), cleanDescription);
                return exactMatches;
            }
            
            // If no exact matches, try fuzzy matching with keywords
            List<HsProduct> fuzzyMatches = performFuzzySearch(cleanDescription, limit);
            
            logger.info("Found {} fuzzy matches for description: '{}'", fuzzyMatches.size(), cleanDescription);
            return fuzzyMatches;
            
        } catch (Exception e) {
            logger.error("Error searching for HS products with description: '{}'", cleanDescription, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<HsProduct> searchByDescription(String description) {
        return searchByDescription(description, DEFAULT_SEARCH_LIMIT);
    }
    
    @Override
    public HsProduct getByHsCode(String hsCode) {
        if (hsCode == null || hsCode.trim().isEmpty()) {
            return null;
        }
        
        try {
            return hsProductRepository.findByHsCode(hsCode.trim()).orElse(null);
        } catch (Exception e) {
            logger.error("Error retrieving HS product by code: '{}'", hsCode, e);
            return null;
        }
    }
    
    /**
     * Perform fuzzy search by breaking description into keywords and matching
     */
    private List<HsProduct> performFuzzySearch(String description, int limit) {
        // Split description into keywords and clean them
        List<String> keywords = Arrays.stream(description.toLowerCase().split("\\s+"))
            .filter(word -> word.length() > 2) // Filter out very short words
            .filter(word -> !isStopWord(word)) // Filter out common stop words
            .distinct()
            .collect(Collectors.toList());
        
        if (keywords.isEmpty()) {
            logger.warn("No meaningful keywords found in description: '{}'", description);
            return new ArrayList<>();
        }
        
        logger.debug("Extracted keywords for fuzzy search: {}", keywords);
        
        // Try different combinations of keywords
        List<HsProduct> results = new ArrayList<>();
        
        // Try with all keywords first (most restrictive)
        if (keywords.size() >= 3) {
            results.addAll(hsProductRepository.findByMultipleKeywords(
                keywords.get(0), keywords.get(1), keywords.get(2)));
        } else if (keywords.size() == 2) {
            results.addAll(hsProductRepository.findByMultipleKeywords(
                keywords.get(0), keywords.get(1), null));
        }
        
        // If we don't have enough results, try individual keywords
        if (results.size() < limit) {
            for (String keyword : keywords) {
                if (results.size() >= limit) break;
                
                List<HsProduct> keywordResults = hsProductRepository.findByHsLabelContainingIgnoreCaseWithLimit(
                    keyword, limit - results.size());
                
                // Add results that aren't already in the list
                for (HsProduct product : keywordResults) {
                    if (results.size() >= limit) break;
                    if (results.stream().noneMatch(p -> p.getId().equals(product.getId()))) {
                        results.add(product);
                    }
                }
            }
        }
        
        // Limit final results
        return results.stream().limit(limit).collect(Collectors.toList());
    }
    
    /**
     * Check if a word is a common stop word that should be filtered out
     */
    private boolean isStopWord(String word) {
        // Common stop words that don't help with product identification
        String[] stopWords = {"the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "from", "up", "about", "into", "through", "during", "before", "after", "above", "below", "between", "among", "within", "without", "under", "over"};
        
        for (String stopWord : stopWords) {
            if (stopWord.equals(word)) {
                return true;
            }
        }
        return false;
    }
}