package com.tariffsheriff.backend.chatbot.service.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.model.TariffRate;
import com.tariffsheriff.backend.tariff.service.TariffRateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * Tool for comparing tariff rates across multiple origin countries
 */
@Component
public class TariffComparisonTool implements ChatbotTool {
    
    private static final Logger logger = LoggerFactory.getLogger(TariffComparisonTool.class);
    
    private final TariffRateService tariffRateService;
    private final ObjectMapper objectMapper;
    
    public TariffComparisonTool(TariffRateService tariffRateService, ObjectMapper objectMapper) {
        this.tariffRateService = tariffRateService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "compareTariffRoutes";
    }
    
    @Override
    public ToolDefinition getDefinition() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // Importer country parameter
        Map<String, Object> importerParam = new HashMap<>();
        importerParam.put("type", "string");
        importerParam.put("description", "ISO2 country code of the importing country (e.g., 'US', 'CA', 'JP')");
        properties.put("importerIso2", importerParam);
        
        // Origin countries parameter (array)
        Map<String, Object> originsParam = new HashMap<>();
        originsParam.put("type", "array");
        Map<String, Object> itemsParam = new HashMap<>();
        itemsParam.put("type", "string");
        originsParam.put("items", itemsParam);
        originsParam.put("description", "Array of ISO2 country codes for origin/exporting countries to compare (e.g., ['MX', 'CN', 'CA'])");
        properties.put("originCountries", originsParam);
        
        // HS Code parameter
        Map<String, Object> hsCodeParam = new HashMap<>();
        hsCodeParam.put("type", "string");
        hsCodeParam.put("description", "Harmonized System code for the product (e.g., '080440' for avocados)");
        properties.put("hsCode", hsCodeParam);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"importerIso2", "originCountries", "hsCode"});
        
        return new ToolDefinition(
            getName(),
            "Compare tariff rates for importing the same product from multiple origin countries. " +
            "Returns tariff rates for each route and identifies which has the lowest tariff. " +
            "Use when user asks to compare tariff rates, find the best sourcing country, or evaluate multiple trade routes.",
            parameters
        );
    }
    
    @Override
    public ToolResult execute(ToolCall toolCall) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract and validate parameters
            String importerIso2 = toolCall.getStringArgument("importerIso2");
            Object originCountriesObj = toolCall.getArgument("originCountries");
            String hsCode = toolCall.getStringArgument("hsCode");
            
            // Validate required parameters
            if (importerIso2 == null || importerIso2.trim().isEmpty()) {
                return ToolResult.error(getName(), 
                    "I need to know which country is importing. Please specify the destination country using its 2-letter code (e.g., 'US', 'CA', 'JP').");
            }
            if (originCountriesObj == null) {
                return ToolResult.error(getName(), 
                    "I need to know which countries you want to compare. Please provide at least 2 origin countries (e.g., 'MX', 'CN', 'CA').");
            }
            if (hsCode == null || hsCode.trim().isEmpty()) {
                return ToolResult.error(getName(), 
                    "I need the HS code for the product you want to compare. If you don't know it, ask me to find the HS code first.");
            }
            
            // Parse origin countries array
            List<String> originCountries = parseOriginCountries(originCountriesObj);
            if (originCountries.isEmpty()) {
                return ToolResult.error(getName(), 
                    "I need at least one origin country to compare. Please provide country codes like 'MX', 'CN', or 'CA'.");
            }
            
            if (originCountries.size() < 2) {
                return ToolResult.error(getName(), 
                    "To compare tariff routes, I need at least 2 origin countries. You provided only " + originCountries.size() + ". Please add more countries to compare.");
            }
            
            // Normalize parameters
            importerIso2 = importerIso2.trim().toUpperCase();
            hsCode = hsCode.trim();
            
            // Validate formats
            if (importerIso2.length() != 2) {
                return ToolResult.error(getName(), 
                    String.format("The importing country code '%s' doesn't look right. Please use a 2-letter ISO code like 'US', 'CA', or 'JP'.", importerIso2));
            }
            
            if (!hsCode.matches("\\d{4,10}")) {
                return ToolResult.error(getName(), 
                    String.format("The HS code '%s' doesn't look valid. HS codes should be 4-10 digits (e.g., '0804' or '080440').", hsCode));
            }
            
            logger.info("Comparing tariff routes: {} origins -> {} for HS code: {}", 
                originCountries.size(), importerIso2, hsCode);
            
            // Query tariff rates for each origin country
            List<RouteComparison> comparisons = new ArrayList<>();
            for (String originIso2 : originCountries) {
                String normalizedOrigin = originIso2.trim().toUpperCase();
                
                if (normalizedOrigin.length() != 2) {
                    logger.warn("Skipping invalid origin country code: {}", originIso2);
                    continue;
                }
                
                try {
                    TariffRateLookupDto result = tariffRateService.getTariffRateWithAgreement(
                        importerIso2, normalizedOrigin, hsCode);
                    comparisons.add(new RouteComparison(normalizedOrigin, result));
                } catch (Exception e) {
                    logger.warn("Failed to get tariff rate for {} -> {}: {}", 
                        normalizedOrigin, importerIso2, e.getMessage());
                    comparisons.add(new RouteComparison(normalizedOrigin, null, e.getMessage()));
                }
            }
            
            if (comparisons.isEmpty()) {
                return ToolResult.error(getName(), 
                    "I couldn't find tariff rates for any of the origin countries you specified. " +
                    "This might mean:\n" +
                    "• The country codes are not in our database\n" +
                    "• The HS code doesn't exist for these trade routes\n" +
                    "• The data is not available yet\n\n" +
                    "Try asking me to list available countries or verify the HS code.");
            }
            
            // Format comparison results
            String formattedResult = formatComparisonResults(comparisons, importerIso2, hsCode);
            
            ToolResult toolResult = ToolResult.success(getName(), formattedResult);
            toolResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            logger.info("Successfully completed tariff comparison in {}ms", toolResult.getExecutionTimeMs());
            
            return toolResult;
            
        } catch (Exception e) {
            logger.error("Error executing tariff comparison tool for importer: {}, HS code: {}", 
                    toolCall.getStringArgument("importerIso2"), 
                    toolCall.getStringArgument("hsCode"), e);
            
            String userMessage = "I had trouble comparing the tariff routes. ";
            
            // Provide specific guidance based on error type
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("database")) {
                userMessage += "There's a problem connecting to the tariff database. Please try again in a moment.";
            } else {
                userMessage += "Please try:\n" +
                        "• Verifying all country codes are correct (2-letter codes like 'US', 'MX', 'CN')\n" +
                        "• Checking that the HS code is valid\n" +
                        "• Asking me to list available countries\n" +
                        "• Comparing fewer countries at once";
            }
            
            ToolResult errorResult = ToolResult.error(getName(), userMessage);
            errorResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return errorResult;
        }
    }
    
    /**
     * Parse origin countries from various input formats
     */
    @SuppressWarnings("unchecked")
    private List<String> parseOriginCountries(Object originCountriesObj) {
        List<String> result = new ArrayList<>();
        
        if (originCountriesObj instanceof List) {
            List<?> list = (List<?>) originCountriesObj;
            for (Object item : list) {
                if (item instanceof String) {
                    result.add((String) item);
                }
            }
        } else if (originCountriesObj instanceof String) {
            // Handle comma-separated string
            String str = (String) originCountriesObj;
            String[] parts = str.split("[,;\\s]+");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    result.add(part.trim());
                }
            }
        }
        
        return result;
    }
    
    /**
     * Format the comparison results for LLM consumption
     */
    private String formatComparisonResults(List<RouteComparison> comparisons, String importerIso2, String hsCode) {
        StringBuilder formatted = new StringBuilder();
        
        formatted.append("Tariff Rate Comparison Results:\n");
        formatted.append("Importing to: ").append(importerIso2).append("\n");
        formatted.append("HS Code: ").append(hsCode).append("\n");
        formatted.append("Comparing ").append(comparisons.size()).append(" trade routes:\n\n");
        
        // Find the route with lowest effective tariff
        RouteComparison lowestRoute = null;
        BigDecimal lowestRate = null;
        
        // Display each route
        for (int i = 0; i < comparisons.size(); i++) {
            RouteComparison comparison = comparisons.get(i);
            formatted.append((i + 1)).append(". Route: ").append(comparison.originIso2)
                .append(" → ").append(importerIso2).append("\n");
            
            if (comparison.error != null) {
                formatted.append("   Status: Error - ").append(comparison.error).append("\n\n");
                continue;
            }
            
            if (comparison.result == null) {
                formatted.append("   Status: No data available\n\n");
                continue;
            }
            
            TariffRateLookupDto result = comparison.result;
            
            // Determine effective rate (preferential if available, otherwise MFN)
            TariffRate effectiveRate = result.tariffRatePref() != null ? 
                result.tariffRatePref() : result.tariffRateMfn();
            
            if (effectiveRate != null) {
                String rateStr = formatRate(effectiveRate);
                BigDecimal numericRate = extractNumericRate(effectiveRate);
                
                formatted.append("   Effective Rate: ").append(rateStr);
                
                if (result.tariffRatePref() != null && result.agreement() != null) {
                    formatted.append(" (Preferential - ").append(result.agreement().getName()).append(")");
                } else {
                    formatted.append(" (MFN)");
                }
                formatted.append("\n");
                
                // Track lowest rate
                if (numericRate != null && (lowestRate == null || numericRate.compareTo(lowestRate) < 0)) {
                    lowestRate = numericRate;
                    lowestRoute = comparison;
                }
                
                // Show MFN rate if different from effective
                if (result.tariffRatePref() != null && result.tariffRateMfn() != null) {
                    formatted.append("   MFN Rate: ").append(formatRate(result.tariffRateMfn())).append("\n");
                }
                
                // Show agreement details if applicable
                if (result.agreement() != null) {
                    formatted.append("   Trade Agreement: ").append(result.agreement().getName()).append("\n");
                    if (result.agreement().getRvcThreshold() != null) {
                        formatted.append("   RVC Requirement: ").append(result.agreement().getRvcThreshold()).append("%\n");
                    }
                }
            } else {
                formatted.append("   Status: No tariff rate data available\n");
            }
            
            formatted.append("\n");
        }
        
        // Highlight the best route
        if (lowestRoute != null) {
            formatted.append("RECOMMENDATION:\n");
            formatted.append("The lowest tariff route is from ").append(lowestRoute.originIso2)
                .append(" with an effective rate of ").append(formatRate(getEffectiveRate(lowestRoute.result)));
            
            if (lowestRoute.result.tariffRatePref() != null && lowestRoute.result.agreement() != null) {
                formatted.append(" under the ").append(lowestRoute.result.agreement().getName()).append(" agreement");
            }
            formatted.append(".\n");
        }
        
        return formatted.toString();
    }
    
    /**
     * Get the effective tariff rate (preferential if available, otherwise MFN)
     */
    private TariffRate getEffectiveRate(TariffRateLookupDto result) {
        return result.tariffRatePref() != null ? result.tariffRatePref() : result.tariffRateMfn();
    }
    
    /**
     * Extract numeric rate for comparison (ad valorem percentage)
     */
    private BigDecimal extractNumericRate(TariffRate rate) {
        if (rate == null) {
            return null;
        }
        
        // Use ad valorem rate for comparison
        if (rate.getAdValoremRate() != null) {
            return rate.getAdValoremRate();
        }
        
        // If only specific rate, we can't easily compare
        // Return a high value to deprioritize in comparison
        if (rate.getSpecificAmount() != null) {
            return new BigDecimal("999.99");
        }
        
        // Free rate
        return BigDecimal.ZERO;
    }
    
    /**
     * Format tariff rate for display
     */
    private String formatRate(TariffRate rate) {
        if (rate == null) {
            return "Not available";
        }
        
        StringBuilder formatted = new StringBuilder();
        
        if (rate.getAdValoremRate() != null) {
            formatted.append(rate.getAdValoremRate()).append("%");
        }
        
        if (rate.getSpecificAmount() != null) {
            if (formatted.length() > 0) formatted.append(" + ");
            formatted.append(rate.getSpecificAmount());
            if (rate.getSpecificUnit() != null) {
                formatted.append(" ").append(rate.getSpecificUnit());
            }
        }
        
        if (formatted.length() == 0) {
            formatted.append("Free (0%)");
        }
        
        return formatted.toString();
    }
    
    /**
     * Internal class to hold comparison data for each route
     */
    private static class RouteComparison {
        final String originIso2;
        final TariffRateLookupDto result;
        final String error;
        
        RouteComparison(String originIso2, TariffRateLookupDto result) {
            this.originIso2 = originIso2;
            this.result = result;
            this.error = null;
        }
        
        RouteComparison(String originIso2, TariffRateLookupDto result, String error) {
            this.originIso2 = originIso2;
            this.result = result;
            this.error = error;
        }
    }
}
