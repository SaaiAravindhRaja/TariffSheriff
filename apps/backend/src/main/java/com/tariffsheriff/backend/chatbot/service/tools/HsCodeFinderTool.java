package com.tariffsheriff.backend.chatbot.service.tools;

import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.tariff.model.HsProduct;
import com.tariffsheriff.backend.tariff.service.HsProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool for finding HS codes based on product descriptions
 */
@Component
public class HsCodeFinderTool implements ChatbotTool {
    
    private static final Logger logger = LoggerFactory.getLogger(HsCodeFinderTool.class);
    private static final int MAX_RESULTS = 10;
    
    private final HsProductService hsProductService;
    
    public HsCodeFinderTool(HsProductService hsProductService) {
        this.hsProductService = hsProductService;
    }
    
    @Override
    public String getName() {
        return "findHsCode";
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
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"productDescription"});
        
        return new ToolDefinition(
            getName(),
            "Find HS codes (Harmonized System codes) for products based on descriptions. " +
            "Returns matching HS codes with descriptions. " +
            "Use when user needs to find HS code for a product or classify goods.",
            parameters
        );
    }
    
    @Override
    public ToolResult execute(ToolCall toolCall) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract and validate parameters
            String productDescription = toolCall.getStringArgument("productDescription");
            
            // Validate required parameter
            if (productDescription == null || productDescription.trim().isEmpty()) {
                return ToolResult.error(getName(), 
                    "I need a product description to search for HS codes. Please tell me what product you're looking for (e.g., 'coffee beans', 'leather handbags', 'electric motors').");
            }
            
            // Normalize parameters
            productDescription = productDescription.trim();
            
            // Validate description length
            if (productDescription.length() < 2) {
                return ToolResult.error(getName(), 
                    "The product description is too short. Please provide more details about the product (at least 2 characters).");
            }
            
            if (productDescription.length() > 500) {
                return ToolResult.error(getName(), 
                    "The product description is too long. Please keep it under 500 characters and focus on the key characteristics of the product.");
            }
            
            logger.info("Searching for HS codes: '{}'", productDescription);
            
            // Search for matching HS codes
            List<HsProduct> matchingProducts = hsProductService.searchByDescription(productDescription, MAX_RESULTS);
            
            // Format result
            String formattedResult = formatHsCodeResult(matchingProducts, productDescription);
            
            ToolResult toolResult = ToolResult.success(getName(), formattedResult);
            toolResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            logger.info("Completed HS code search in {}ms, found {} matches", 
                       toolResult.getExecutionTimeMs(), matchingProducts.size());
            
            return toolResult;
            
        } catch (Exception e) {
            logger.error("Error executing HS code finder tool for query: '{}'", 
                    toolCall.getStringArgument("productDescription"), e);
            
            String userMessage = "I had trouble searching for HS codes. ";
            
            // Provide helpful guidance
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("database")) {
                userMessage += "There's a problem connecting to the HS code database. Please try again in a moment.";
            } else {
                userMessage += "Try:\n" +
                        "• Using simpler, more general terms (e.g., 'shoes' instead of 'Nike Air Max running shoes')\n" +
                        "• Describing the main material or function\n" +
                        "• Breaking down complex products into components\n" +
                        "• Checking your spelling";
            }
            
            ToolResult errorResult = ToolResult.error(getName(), userMessage);
            errorResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return errorResult;
        }
    }
    
    /**
     * Format the HS code search result for LLM consumption
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
}
