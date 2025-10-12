package com.tariffsheriff.backend.chatbot.service.tools;

import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.tariff.model.HsProduct;
import com.tariffsheriff.backend.tariff.service.HsProductService;
import com.tariffsheriff.backend.data.ExternalDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tool for finding HS codes based on product descriptions
 */
@Component
public class HsCodeFinderTool implements ChatbotTool {
    
    private static final Logger logger = LoggerFactory.getLogger(HsCodeFinderTool.class);
    private static final int MAX_RESULTS = 5; // Limit results for LLM consumption
    
    private final HsProductService hsProductService;
    
    @Autowired(required = false)
    private ExternalDataService externalDataService;
    
    public HsCodeFinderTool(HsProductService hsProductService) {
        this.hsProductService = hsProductService;
    }
    
    @Override
    public String getName() {
        return "findHsCodeForProduct";
    }
    
    @Override
    public ToolDefinition getDefinition() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // Product description parameter
        Map<String, Object> descriptionParam = new HashMap<>();
        descriptionParam.put("type", "string");
        descriptionParam.put("description", "Description of the product to find HS code for (e.g., 'electric skateboards', 'leather handbags', 'fresh avocados')");
        properties.put("productDescription", descriptionParam);
        
        // Search mode parameter
        Map<String, Object> modeParam = new HashMap<>();
        modeParam.put("type", "string");
        modeParam.put("description", "Search mode: 'fuzzy' (default), 'hierarchical', 'validate', 'browse'");
        modeParam.put("enum", Arrays.asList("fuzzy", "hierarchical", "validate", "browse"));
        properties.put("searchMode", modeParam);
        
        // HS code parameter for validation or browsing
        Map<String, Object> hsCodeParam = new HashMap<>();
        hsCodeParam.put("type", "string");
        hsCodeParam.put("description", "HS code for validation or hierarchical browsing (e.g., '8504' for chapter browsing)");
        properties.put("hsCode", hsCodeParam);
        
        // Confidence threshold parameter
        Map<String, Object> confidenceParam = new HashMap<>();
        confidenceParam.put("type", "number");
        confidenceParam.put("description", "Minimum confidence score (0.0-1.0) for results (default: 0.3)");
        properties.put("minConfidence", confidenceParam);
        
        // Max results parameter
        Map<String, Object> maxResultsParam = new HashMap<>();
        maxResultsParam.put("type", "integer");
        maxResultsParam.put("description", "Maximum number of results to return (default: 5, max: 20)");
        properties.put("maxResults", maxResultsParam);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"productDescription"});
        
        return new ToolDefinition(
            getName(),
            "Find HS codes (Harmonized System codes) for products based on descriptions or validate existing classifications. " +
            "USE WHEN: User needs to find HS code for a product, classify goods, verify HS code accuracy, or browse product categories. " +
            "REQUIRES: Product description (e.g., 'electric skateboards', 'leather handbags', 'fresh avocados'). " +
            "RETURNS: Matching HS codes with confidence scores, official descriptions, classification guidance, and alternative suggestions. " +
            "EXAMPLES: 'What's the HS code for laptops?', 'Find HS code for organic cotton t-shirts', 'Validate HS code 8504 for my product', 'Browse HS codes in chapter 84'. " +
            "SUPPORTS: Fuzzy search with confidence scoring, hierarchical category browsing, HS code validation, and product-specific classification guidance.",
            parameters
        );
    }
    
    @Override
    public ToolResult execute(ToolCall toolCall) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract and validate parameters
            String productDescription = toolCall.getStringArgument("productDescription");
            String searchMode = toolCall.getStringArgument("searchMode", "fuzzy");
            String hsCode = toolCall.getStringArgument("hsCode");
            BigDecimal minConfidence = toolCall.getBigDecimalArgument("minConfidence");
            Integer maxResults = getIntegerArgument(toolCall, "maxResults");
            
            // Set defaults
            if (minConfidence == null) {
                minConfidence = new BigDecimal("0.3");
            }
            if (maxResults == null) {
                maxResults = MAX_RESULTS;
            } else {
                maxResults = Math.min(maxResults, 20); // Cap at 20 results
            }
            
            // Validate required parameter
            if (productDescription == null || productDescription.trim().isEmpty()) {
                return ToolResult.error(getName(), "Missing required parameter: productDescription");
            }
            
            // Normalize parameters
            productDescription = productDescription.trim();
            searchMode = searchMode.toLowerCase();
            
            // Validate description length
            if (productDescription.length() < 2) {
                return ToolResult.error(getName(), "Product description too short. Please provide at least 2 characters.");
            }
            
            if (productDescription.length() > 500) {
                return ToolResult.error(getName(), "Product description too long. Please limit to 500 characters.");
            }
            
            logger.info("Executing {} search for HS codes: '{}'", searchMode, productDescription);
            
            // Execute search based on mode with market context enrichment
            String formattedResult = switch (searchMode) {
                case "hierarchical" -> performHierarchicalSearchWithEnrichment(productDescription, hsCode, maxResults);
                case "validate" -> performValidationSearchWithEnrichment(productDescription, hsCode);
                case "browse" -> performBrowseSearchWithEnrichment(hsCode, maxResults);
                default -> performFuzzySearchWithEnrichment(productDescription, minConfidence, maxResults);
            };
            
            ToolResult toolResult = ToolResult.success(getName(), formattedResult);
            toolResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            logger.info("Completed {} search in {}ms", searchMode, toolResult.getExecutionTimeMs());
            
            return toolResult;
            
        } catch (Exception e) {
            logger.error("Error executing enhanced HS code finder tool", e);
            ToolResult errorResult = ToolResult.error(getName(), 
                "Failed to find HS codes: " + e.getMessage());
            errorResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return errorResult;
        }
    }
    
    /**
     * Get integer argument from tool call
     */
    private Integer getIntegerArgument(ToolCall toolCall, String key) {
        Object value = toolCall.getArgument(key);
        if (value == null) return null;
        
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Perform enhanced fuzzy search with confidence scoring
     */
    private String performFuzzySearch(String productDescription, BigDecimal minConfidence, int maxResults) {
        StringBuilder result = new StringBuilder();
        result.append("Enhanced HS Code Search Results\n");
        result.append("=====================================\n");
        result.append("Query: \"").append(productDescription).append("\"\n");
        result.append("Min Confidence: ").append(minConfidence).append("\n\n");
        
        // Get initial matches
        List<HsProduct> matchingProducts = hsProductService.searchByDescription(productDescription, maxResults * 2);
        
        // Calculate confidence scores and filter
        List<HsCodeMatch> scoredMatches = matchingProducts.stream()
            .map(product -> calculateConfidenceScore(product, productDescription))
            .filter(match -> match.confidence.compareTo(minConfidence) >= 0)
            .sorted((m1, m2) -> m2.confidence.compareTo(m1.confidence))
            .limit(maxResults)
            .collect(Collectors.toList());
        
        if (scoredMatches.isEmpty()) {
            result.append("No matches found above confidence threshold.\n\n");
            result.append("Suggestions:\n");
            result.append("- Lower the confidence threshold (try 0.1-0.2)\n");
            result.append("- Use more general terms\n");
            result.append("- Try different keywords or synonyms\n");
            result.append("- Use hierarchical search to browse categories\n");
            return result.toString();
        }
        
        result.append("Found ").append(scoredMatches.size()).append(" matches:\n\n");
        
        for (int i = 0; i < scoredMatches.size(); i++) {
            HsCodeMatch match = scoredMatches.get(i);
            result.append(String.format("%d. HS Code: %s (Confidence: %.1f%%)\n", 
                i + 1, match.product.getHsCode(), match.confidence.multiply(new BigDecimal("100"))));
            result.append("   Description: ").append(match.product.getHsLabel()).append("\n");
            result.append("   Match Reasons: ").append(String.join(", ", match.matchReasons)).append("\n");
            
            if (match.product.getDestination() != null) {
                result.append("   Country: ").append(match.product.getDestination().getName()).append(" (")
                         .append(match.product.getDestination().getIso2()).append(")\n");
            }
            
            // Add classification guidance
            String guidance = getClassificationGuidance(match.product.getHsCode());
            if (!guidance.isEmpty()) {
                result.append("   Guidance: ").append(guidance).append("\n");
            }
            
            result.append("\n");
        }
        
        // Add recommendations
        result.append("Recommendations:\n");
        if (scoredMatches.get(0).confidence.compareTo(new BigDecimal("0.8")) >= 0) {
            result.append("- High confidence match found - likely correct classification\n");
        } else if (scoredMatches.get(0).confidence.compareTo(new BigDecimal("0.6")) >= 0) {
            result.append("- Good match found - verify description details\n");
        } else {
            result.append("- Moderate confidence - consider expert review\n");
        }
        
        result.append("- Use 'validate' mode to verify specific HS codes\n");
        result.append("- Use 'hierarchical' mode to explore related categories\n");
        
        return result.toString();
    }
    
    /**
     * Perform enhanced fuzzy search with market context enrichment
     */
    private String performFuzzySearchWithEnrichment(String productDescription, BigDecimal minConfidence, int maxResults) {
        String basicResult = performFuzzySearch(productDescription, minConfidence, maxResults);
        
        if (externalDataService == null) {
            return basicResult;
        }
        
        try {
            StringBuilder enrichedResult = new StringBuilder(basicResult);
            enrichedResult.append("\n").append("=".repeat(40)).append("\n");
            enrichedResult.append("MARKET CONTEXT FOR PRODUCT\n");
            enrichedResult.append("=".repeat(40)).append("\n\n");
            
            // Get market intelligence for the product
            CompletableFuture<List<ExternalDataService.NewsItem>> newsFuture = 
                externalDataService.getTradeNews(productDescription, "global", 3);
            
            List<ExternalDataService.NewsItem> news = newsFuture.get(5, TimeUnit.SECONDS);
            
            if (!news.isEmpty()) {
                enrichedResult.append("Recent Market News:\n");
                news.forEach(item -> {
                    enrichedResult.append("• ").append(item.getTitle()).append("\n");
                    enrichedResult.append("  Source: ").append(item.getSource())
                        .append(" (").append(item.getPublishedAt().toString().substring(0, 10)).append(")\n");
                });
                enrichedResult.append("\n");
            }
            
            // Add classification insights
            enrichedResult.append("Classification Insights:\n");
            enrichedResult.append("• Product classification affects duty rates and trade agreements\n");
            enrichedResult.append("• Consider product variations that might fall under different HS codes\n");
            enrichedResult.append("• Verify classification with customs authorities for high-value shipments\n");
            enrichedResult.append("• Monitor for HS code updates and reclassifications\n");
            
            return enrichedResult.toString();
            
        } catch (Exception e) {
            logger.warn("Failed to enrich HS code search with market context: {}", e.getMessage());
            return basicResult + "\n\nNote: Market context enrichment temporarily unavailable.";
        }
    }
    
    /**
     * Perform hierarchical search starting from a chapter or heading
     */
    private String performHierarchicalSearch(String productDescription, String hsCode, int maxResults) {
        StringBuilder result = new StringBuilder();
        result.append("Hierarchical HS Code Search\n");
        result.append("=============================\n");
        result.append("Product: \"").append(productDescription).append("\"\n");
        
        if (hsCode != null && !hsCode.trim().isEmpty()) {
            result.append("Starting from HS Code: ").append(hsCode).append("\n\n");
            
            // Browse hierarchy starting from provided code
            String hierarchyResult = browseHierarchy(hsCode, productDescription, maxResults);
            result.append(hierarchyResult);
        } else {
            result.append("Auto-detecting category...\n\n");
            
            // First, identify likely chapters/headings
            List<String> suggestedChapters = identifyLikelyChapters(productDescription);
            
            result.append("Suggested HS Chapters:\n");
            for (String chapter : suggestedChapters) {
                result.append("- Chapter ").append(chapter).append(": ").append(getChapterDescription(chapter)).append("\n");
            }
            result.append("\n");
            
            // Show products from most likely chapter
            if (!suggestedChapters.isEmpty()) {
                String topChapter = suggestedChapters.get(0);
                result.append("Products in Chapter ").append(topChapter).append(":\n");
                String chapterProducts = browseHierarchy(topChapter, productDescription, maxResults);
                result.append(chapterProducts);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Perform validation of a specific HS code against product description
     */
    private String performValidationSearch(String productDescription, String hsCode) {
        StringBuilder result = new StringBuilder();
        result.append("HS Code Classification Validation\n");
        result.append("===================================\n");
        result.append("Product: \"").append(productDescription).append("\"\n");
        
        if (hsCode == null || hsCode.trim().isEmpty()) {
            result.append("Error: HS code required for validation mode\n");
            return result.toString();
        }
        
        result.append("HS Code: ").append(hsCode).append("\n\n");
        
        // Get the specific HS product
        HsProduct product = hsProductService.getByHsCode(hsCode.trim());
        
        if (product == null) {
            result.append("HS Code not found in database.\n");
            result.append("Please verify the code is correct and try again.\n");
            return result.toString();
        }
        
        result.append("Official Description: ").append(product.getHsLabel()).append("\n\n");
        
        // Calculate match confidence
        HsCodeMatch match = calculateConfidenceScore(product, productDescription);
        
        result.append("Validation Results:\n");
        result.append("- Confidence Score: ").append(match.confidence.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP)).append("%\n");
        result.append("- Match Factors: ").append(String.join(", ", match.matchReasons)).append("\n\n");
        
        // Provide validation assessment
        if (match.confidence.compareTo(new BigDecimal("0.8")) >= 0) {
            result.append("✅ STRONG MATCH: This HS code appears to be correct for your product.\n");
        } else if (match.confidence.compareTo(new BigDecimal("0.6")) >= 0) {
            result.append("⚠️ MODERATE MATCH: This HS code may be correct but requires verification.\n");
        } else if (match.confidence.compareTo(new BigDecimal("0.3")) >= 0) {
            result.append("❓ WEAK MATCH: This HS code may not be the best classification.\n");
        } else {
            result.append("❌ POOR MATCH: This HS code is likely incorrect for your product.\n");
        }
        
        result.append("\n");
        
        // Suggest alternatives if confidence is low
        if (match.confidence.compareTo(new BigDecimal("0.7")) < 0) {
            result.append("Alternative Classifications:\n");
            List<HsProduct> alternatives = hsProductService.searchByDescription(productDescription, 3);
            for (HsProduct alt : alternatives) {
                if (!alt.getHsCode().equals(hsCode)) {
                    HsCodeMatch altMatch = calculateConfidenceScore(alt, productDescription);
                    result.append("- ").append(alt.getHsCode()).append(" (").append(altMatch.confidence.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP)).append("%): ");
                    result.append(alt.getHsLabel()).append("\n");
                }
            }
        }
        
        // Add expert review recommendation
        result.append("\nRecommendations:\n");
        result.append("- Review the official HS code description carefully\n");
        result.append("- Consider consulting with a customs broker or trade specialist\n");
        result.append("- Check for any exclusions or special provisions\n");
        result.append("- Verify with customs authorities if uncertain\n");
        
        return result.toString();
    }
    
    /**
     * Browse HS code hierarchy
     */
    private String performBrowseSearch(String hsCode, int maxResults) {
        StringBuilder result = new StringBuilder();
        result.append("HS Code Hierarchy Browser\n");
        result.append("==========================\n");
        
        if (hsCode == null || hsCode.trim().isEmpty()) {
            result.append("Error: HS code required for browse mode\n");
            return result.toString();
        }
        
        result.append("Browsing HS Code: ").append(hsCode).append("\n\n");
        
        return browseHierarchy(hsCode, null, maxResults);
    }
    
    /**
     * Calculate confidence score for HS code match
     */
    private HsCodeMatch calculateConfidenceScore(HsProduct product, String productDescription) {
        List<String> matchReasons = new ArrayList<>();
        BigDecimal confidence = BigDecimal.ZERO;
        
        String description = productDescription.toLowerCase();
        String hsLabel = product.getHsLabel().toLowerCase();
        
        // Exact phrase match (high confidence)
        if (hsLabel.contains(description) || description.contains(hsLabel)) {
            confidence = confidence.add(new BigDecimal("0.4"));
            matchReasons.add("exact phrase match");
        }
        
        // Keyword matching
        String[] descWords = description.split("\\s+");
        String[] hsWords = hsLabel.split("\\s+");
        
        int matchingWords = 0;
        int totalWords = descWords.length;
        
        for (String descWord : descWords) {
            if (descWord.length() > 2) { // Skip very short words
                for (String hsWord : hsWords) {
                    if (hsWord.contains(descWord) || descWord.contains(hsWord)) {
                        matchingWords++;
                        break;
                    }
                }
            }
        }
        
        if (totalWords > 0) {
            BigDecimal wordMatchRatio = new BigDecimal(matchingWords).divide(new BigDecimal(totalWords), 4, RoundingMode.HALF_UP);
            confidence = confidence.add(wordMatchRatio.multiply(new BigDecimal("0.3")));
            
            if (matchingWords > 0) {
                matchReasons.add(matchingWords + "/" + totalWords + " keywords");
            }
        }
        
        // Material/category matching
        String materialMatch = findMaterialMatch(description, hsLabel);
        if (!materialMatch.isEmpty()) {
            confidence = confidence.add(new BigDecimal("0.2"));
            matchReasons.add("material: " + materialMatch);
        }
        
        // Function/use matching
        String functionMatch = findFunctionMatch(description, hsLabel);
        if (!functionMatch.isEmpty()) {
            confidence = confidence.add(new BigDecimal("0.1"));
            matchReasons.add("function: " + functionMatch);
        }
        
        // Ensure confidence doesn't exceed 1.0
        confidence = confidence.min(BigDecimal.ONE);
        
        if (matchReasons.isEmpty()) {
            matchReasons.add("general similarity");
        }
        
        return new HsCodeMatch(product, confidence, matchReasons);
    }
    
    /**
     * Helper class for HS code matches with confidence scores
     */
    private static class HsCodeMatch {
        final HsProduct product;
        final BigDecimal confidence;
        final List<String> matchReasons;
        
        HsCodeMatch(HsProduct product, BigDecimal confidence, List<String> matchReasons) {
            this.product = product;
            this.confidence = confidence;
            this.matchReasons = matchReasons;
        }
    }
    
    /**
     * Find material matches between description and HS label
     */
    private String findMaterialMatch(String description, String hsLabel) {
        String[] materials = {"cotton", "wool", "silk", "leather", "plastic", "metal", "wood", "glass", "ceramic", "rubber", "steel", "aluminum", "iron", "gold", "silver", "copper"};
        
        for (String material : materials) {
            if (description.contains(material) && hsLabel.contains(material)) {
                return material;
            }
        }
        return "";
    }
    
    /**
     * Find function matches between description and HS label
     */
    private String findFunctionMatch(String description, String hsLabel) {
        String[] functions = {"cooking", "heating", "cooling", "cleaning", "cutting", "measuring", "transport", "storage", "communication", "entertainment", "medical", "agricultural", "industrial"};
        
        for (String function : functions) {
            if (description.contains(function) && hsLabel.contains(function)) {
                return function;
            }
        }
        return "";
    }
    
    /**
     * Identify likely HS chapters based on product description
     */
    private List<String> identifyLikelyChapters(String description) {
        Map<String, List<String>> categoryKeywords = new HashMap<>();
        categoryKeywords.put("01-15", Arrays.asList("food", "meat", "fish", "dairy", "vegetable", "fruit", "grain", "oil", "animal", "agricultural"));
        categoryKeywords.put("16-24", Arrays.asList("prepared", "beverage", "alcohol", "tobacco", "drink", "processed"));
        categoryKeywords.put("25-27", Arrays.asList("mineral", "salt", "stone", "fuel", "oil", "gas", "coal"));
        categoryKeywords.put("28-38", Arrays.asList("chemical", "pharmaceutical", "medicine", "paint", "soap", "fertilizer"));
        categoryKeywords.put("39-40", Arrays.asList("plastic", "rubber", "polymer", "synthetic"));
        categoryKeywords.put("41-43", Arrays.asList("leather", "fur", "hide", "skin"));
        categoryKeywords.put("44-46", Arrays.asList("wood", "cork", "basket", "timber"));
        categoryKeywords.put("47-49", Arrays.asList("paper", "cardboard", "book", "printed"));
        categoryKeywords.put("50-63", Arrays.asList("textile", "fabric", "clothing", "cotton", "wool", "silk", "garment"));
        categoryKeywords.put("64-67", Arrays.asList("footwear", "shoe", "hat", "umbrella"));
        categoryKeywords.put("68-70", Arrays.asList("stone", "ceramic", "glass", "concrete"));
        categoryKeywords.put("71", Arrays.asList("jewelry", "precious", "gold", "silver", "diamond", "gem"));
        categoryKeywords.put("72-83", Arrays.asList("metal", "iron", "steel", "aluminum", "copper", "tool"));
        categoryKeywords.put("84-85", Arrays.asList("machine", "mechanical", "electrical", "computer", "electronic"));
        categoryKeywords.put("86-89", Arrays.asList("vehicle", "railway", "aircraft", "ship", "transport"));
        categoryKeywords.put("90-92", Arrays.asList("optical", "medical", "precision", "instrument", "clock", "music"));
        categoryKeywords.put("93", Arrays.asList("weapon", "ammunition", "arms"));
        categoryKeywords.put("94-96", Arrays.asList("furniture", "toy", "game", "sport", "miscellaneous"));
        
        String lowerDesc = description.toLowerCase();
        List<String> matches = new ArrayList<>();
        
        for (Map.Entry<String, List<String>> entry : categoryKeywords.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerDesc.contains(keyword)) {
                    matches.add(entry.getKey());
                    break;
                }
            }
        }
        
        return matches.isEmpty() ? Arrays.asList("84-85") : matches; // Default to machinery if no match
    }
    
    /**
     * Get chapter description
     */
    private String getChapterDescription(String chapter) {
        Map<String, String> chapterDescriptions = new HashMap<>();
        chapterDescriptions.put("01-15", "Live animals and animal products, vegetable products");
        chapterDescriptions.put("16-24", "Prepared foodstuffs, beverages, spirits, tobacco");
        chapterDescriptions.put("25-27", "Mineral products");
        chapterDescriptions.put("28-38", "Products of chemical or allied industries");
        chapterDescriptions.put("39-40", "Plastics and rubber");
        chapterDescriptions.put("41-43", "Raw hides, skins, leather, furskins");
        chapterDescriptions.put("44-46", "Wood and articles of wood, cork, basketwork");
        chapterDescriptions.put("47-49", "Pulp of wood, paper and paperboard");
        chapterDescriptions.put("50-63", "Textiles and textile articles");
        chapterDescriptions.put("64-67", "Footwear, headgear, umbrellas, walking sticks");
        chapterDescriptions.put("68-70", "Articles of stone, plaster, cement, asbestos, mica, ceramics, glass");
        chapterDescriptions.put("71", "Natural or cultured pearls, precious stones, precious metals");
        chapterDescriptions.put("72-83", "Base metals and articles of base metal");
        chapterDescriptions.put("84-85", "Machinery and mechanical appliances, electrical equipment");
        chapterDescriptions.put("86-89", "Vehicles, aircraft, vessels and associated transport equipment");
        chapterDescriptions.put("90-92", "Optical, photographic, cinematographic, medical instruments, clocks, musical instruments");
        chapterDescriptions.put("93", "Arms and ammunition");
        chapterDescriptions.put("94-96", "Miscellaneous manufactured articles");
        
        return chapterDescriptions.getOrDefault(chapter, "Various products");
    }
    
    /**
     * Browse hierarchy starting from a specific HS code
     */
    private String browseHierarchy(String hsCode, String productDescription, int maxResults) {
        StringBuilder result = new StringBuilder();
        
        // Determine hierarchy level based on code length
        String cleanCode = hsCode.trim();
        
        if (cleanCode.length() <= 2) {
            // Chapter level - show headings
            result.append("Chapter ").append(cleanCode).append(" - ").append(getChapterDescription(cleanCode)).append("\n\n");
            result.append("This would show headings within the chapter (not implemented in basic version)\n");
        } else if (cleanCode.length() <= 4) {
            // Heading level - show subheadings
            result.append("Heading ").append(cleanCode).append("\n\n");
            result.append("This would show subheadings within the heading (not implemented in basic version)\n");
        } else {
            // Specific code - show related codes
            HsProduct product = hsProductService.getByHsCode(cleanCode);
            if (product != null) {
                result.append("HS Code: ").append(product.getHsCode()).append("\n");
                result.append("Description: ").append(product.getHsLabel()).append("\n\n");
                
                if (productDescription != null) {
                    HsCodeMatch match = calculateConfidenceScore(product, productDescription);
                    result.append("Match Confidence: ").append(match.confidence.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP)).append("%\n");
                    result.append("Match Reasons: ").append(String.join(", ", match.matchReasons)).append("\n\n");
                }
            }
            
            result.append("Related codes in same heading would be shown here (not implemented in basic version)\n");
        }
        
        return result.toString();
    }
    
    /**
     * Get classification guidance for specific HS code
     */
    private String getClassificationGuidance(String hsCode) {
        if (hsCode.length() < 2) return "";
        
        String chapter = hsCode.substring(0, 2);
        
        return switch (chapter) {
            case "84" -> "Verify if item is mechanical or electrical";
            case "85" -> "Check if item requires electrical power";
            case "39" -> "Confirm primary material is plastic";
            case "73" -> "Verify if primary material is iron/steel";
            case "62", "61" -> "Check if item is knitted or woven";
            case "87" -> "Confirm vehicle type and engine size";
            default -> "";
        };
    }
    
    /**
     * Format the HS code search result for LLM consumption (legacy method)
     */
    private String formatHsCodeResult(List<HsProduct> matchingProducts, String productDescription) {
        StringBuilder formatted = new StringBuilder();
        
        formatted.append("HS Code Search Results for: \"").append(productDescription).append("\"\n\n");
        
        if (matchingProducts == null || matchingProducts.isEmpty()) {
            formatted.append("No matching HS codes found for the product description.\n\n");
            formatted.append("Suggestions:\n");
            formatted.append("- Try using more general terms (e.g., 'shoes' instead of 'running shoes')\n");
            formatted.append("- Use different keywords or synonyms\n");
            formatted.append("- Check spelling and try simpler descriptions\n");
            formatted.append("- Consider the main material or function of the product\n");
            return formatted.toString();
        }
        
        formatted.append("Found ").append(matchingProducts.size()).append(" matching HS code(s):\n\n");
        
        for (int i = 0; i < matchingProducts.size(); i++) {
            HsProduct product = matchingProducts.get(i);
            formatted.append(i + 1).append(". HS Code: ").append(product.getHsCode()).append("\n");
            formatted.append("   Description: ").append(product.getHsLabel()).append("\n");
            formatted.append("   HS Version: ").append(product.getHsVersion()).append("\n");
            
            if (product.getDestination() != null) {
                formatted.append("   Country: ").append(product.getDestination().getName()).append(" (")
                         .append(product.getDestination().getIso2()).append(")\n");
            }
            
            formatted.append("\n");
        }
        
        // Add guidance for multiple matches
        if (matchingProducts.size() > 1) {
            formatted.append("Multiple matches found. Consider:\n");
            formatted.append("- Review each description to find the most specific match\n");
            formatted.append("- The HS code hierarchy goes from general (4 digits) to specific (8+ digits)\n");
            formatted.append("- Choose the most detailed code that accurately describes your product\n");
            formatted.append("- When in doubt, consult with a trade specialist or customs broker\n");
        } else {
            formatted.append("Single match found. This HS code appears to be the most relevant for your product.\n");
            formatted.append("Please verify that the description accurately matches your specific product.\n");
        }
        
        return formatted.toString();
    }
    
    /**
     * Perform hierarchical search with market context enrichment
     */
    private String performHierarchicalSearchWithEnrichment(String productDescription, String hsCode, int maxResults) {
        String basicResult = performHierarchicalSearch(productDescription, hsCode, maxResults);
        
        if (externalDataService == null) {
            return basicResult;
        }
        
        try {
            StringBuilder enrichedResult = new StringBuilder(basicResult);
            enrichedResult.append("\n").append("=".repeat(35)).append("\n");
            enrichedResult.append("HIERARCHICAL MARKET CONTEXT\n");
            enrichedResult.append("=".repeat(35)).append("\n\n");
            
            enrichedResult.append("Classification Strategy:\n");
            enrichedResult.append("• Start with broader categories and narrow down\n");
            enrichedResult.append("• Consider product variations and similar items\n");
            enrichedResult.append("• Review trade statistics for category insights\n");
            
            return enrichedResult.toString();
            
        } catch (Exception e) {
            logger.warn("Failed to enrich hierarchical search: {}", e.getMessage());
            return basicResult + "\n\nNote: Market context enrichment temporarily unavailable.";
        }
    }
    
    /**
     * Perform validation search with market context enrichment
     */
    private String performValidationSearchWithEnrichment(String productDescription, String hsCode) {
        String basicResult = performValidationSearch(productDescription, hsCode);
        
        if (externalDataService == null) {
            return basicResult;
        }
        
        try {
            StringBuilder enrichedResult = new StringBuilder(basicResult);
            enrichedResult.append("\n").append("=".repeat(35)).append("\n");
            enrichedResult.append("VALIDATION MARKET CONTEXT\n");
            enrichedResult.append("=".repeat(35)).append("\n\n");
            
            enrichedResult.append("Validation Best Practices:\n");
            enrichedResult.append("• Cross-reference with multiple classification sources\n");
            enrichedResult.append("• Consider recent HS code updates and amendments\n");
            enrichedResult.append("• Verify with customs authorities for high-value goods\n");
            
            return enrichedResult.toString();
            
        } catch (Exception e) {
            logger.warn("Failed to enrich validation search: {}", e.getMessage());
            return basicResult + "\n\nNote: Market context enrichment temporarily unavailable.";
        }
    }
    
    /**
     * Perform browse search with market context enrichment
     */
    private String performBrowseSearchWithEnrichment(String hsCode, int maxResults) {
        String basicResult = performBrowseSearch(hsCode, maxResults);
        
        if (externalDataService == null) {
            return basicResult;
        }
        
        try {
            StringBuilder enrichedResult = new StringBuilder(basicResult);
            enrichedResult.append("\n").append("=".repeat(30)).append("\n");
            enrichedResult.append("BROWSE MARKET CONTEXT\n");
            enrichedResult.append("=".repeat(30)).append("\n\n");
            
            enrichedResult.append("Navigation Tips:\n");
            enrichedResult.append("• Use chapter-level browsing for broad categories\n");
            enrichedResult.append("• Drill down to specific codes for precise classification\n");
            enrichedResult.append("• Compare similar products within the same heading\n");
            
            return enrichedResult.toString();
            
        } catch (Exception e) {
            logger.warn("Failed to enrich browse search: {}", e.getMessage());
            return basicResult + "\n\nNote: Market context enrichment temporarily unavailable.";
        }
    }
}