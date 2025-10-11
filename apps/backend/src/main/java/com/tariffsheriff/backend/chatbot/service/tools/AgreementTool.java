package com.tariffsheriff.backend.chatbot.service.tools;

import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.service.AgreementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for retrieving trade agreements for specific countries
 */
@Component
public class AgreementTool implements ChatbotTool {
    
    private static final Logger logger = LoggerFactory.getLogger(AgreementTool.class);
    
    private final AgreementService agreementService;
    
    public AgreementTool(AgreementService agreementService) {
        this.agreementService = agreementService;
    }
    
    @Override
    public String getName() {
        return "getAgreementsByCountry";
    }
    
    @Override
    public ToolDefinition getDefinition() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // Country parameter
        Map<String, Object> countryParam = new HashMap<>();
        countryParam.put("type", "string");
        countryParam.put("description", "ISO2 country code to get trade agreements for (e.g., 'US', 'JP', 'CA')");
        properties.put("countryIso2", countryParam);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"countryIso2"});
        
        return new ToolDefinition(
            getName(),
            "Get trade agreements for a specific country. Returns comprehensive list of trade agreements including partner countries, agreement types, and status.",
            parameters
        );
    }
    
    @Override
    public ToolResult execute(ToolCall toolCall) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract and validate parameters
            String countryIso2 = toolCall.getStringArgument("countryIso2");
            
            // Validate required parameter
            if (countryIso2 == null || countryIso2.trim().isEmpty()) {
                return ToolResult.error(getName(), "Missing required parameter: countryIso2");
            }
            
            // Normalize parameter
            countryIso2 = countryIso2.trim().toUpperCase();
            
            // Validate ISO2 format (2 characters)
            if (countryIso2.length() != 2) {
                return ToolResult.error(getName(), "Invalid countryIso2 format. Must be 2-character ISO country code (e.g., 'US')");
            }
            
            // Validate that it contains only letters
            if (!countryIso2.matches("[A-Z]{2}")) {
                return ToolResult.error(getName(), "Invalid countryIso2 format. Must contain only letters (e.g., 'US', not 'U1')");
            }
            
            logger.info("Looking up trade agreements for country: {}", countryIso2);
            
            // Call the service
            List<Agreement> agreements = agreementService.getAgreementsByCountry(countryIso2);
            
            // Format result for LLM consumption
            String formattedResult = formatAgreementResult(agreements, countryIso2);
            
            ToolResult toolResult = ToolResult.success(getName(), formattedResult);
            toolResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            logger.info("Successfully retrieved {} agreements for country {} in {}ms", 
                       agreements.size(), countryIso2, toolResult.getExecutionTimeMs());
            
            return toolResult;
            
        } catch (Exception e) {
            logger.error("Error executing agreement lookup tool", e);
            ToolResult errorResult = ToolResult.error(getName(), 
                "Failed to retrieve trade agreements: " + e.getMessage());
            errorResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return errorResult;
        }
    }
    
    /**
     * Format the agreement lookup result for LLM consumption
     */
    private String formatAgreementResult(List<Agreement> agreements, String countryIso2) {
        StringBuilder formatted = new StringBuilder();
        
        formatted.append("Trade Agreements for Country: ").append(countryIso2).append("\n\n");
        
        if (agreements == null || agreements.isEmpty()) {
            formatted.append("No trade agreements found for ").append(countryIso2).append(".\n");
            formatted.append("This country may:\n");
            formatted.append("- Not have any preferential trade agreements in the database\n");
            formatted.append("- Only trade under Most Favored Nation (MFN) terms\n");
            formatted.append("- Have agreements that are not yet included in the system\n");
            return formatted.toString();
        }
        
        formatted.append("Found ").append(agreements.size()).append(" trade agreement(s):\n\n");
        
        for (int i = 0; i < agreements.size(); i++) {
            Agreement agreement = agreements.get(i);
            formatted.append(i + 1).append(". ").append(agreement.getName()).append("\n");
            formatted.append("   - Type: ").append(agreement.getType()).append("\n");
            formatted.append("   - Status: ").append(agreement.getStatus()).append("\n");
            
            if (agreement.getEnteredIntoForce() != null) {
                formatted.append("   - Entered into Force: ").append(agreement.getEnteredIntoForce()).append("\n");
            }
            
            if (agreement.getRvcThreshold() != null) {
                formatted.append("   - RVC Threshold: ").append(agreement.getRvcThreshold()).append("%\n");
            }
            
            formatted.append("\n");
        }
        
        // Add summary information
        formatted.append("Summary:\n");
        formatted.append("- Total Agreements: ").append(agreements.size()).append("\n");
        
        // Count by status
        long activeAgreements = agreements.stream()
            .filter(a -> "Active".equalsIgnoreCase(a.getStatus()) || "In Force".equalsIgnoreCase(a.getStatus()))
            .count();
        
        if (activeAgreements > 0) {
            formatted.append("- Active Agreements: ").append(activeAgreements).append("\n");
        }
        
        // Count by type
        Map<String, Long> typeCount = new HashMap<>();
        agreements.forEach(agreement -> {
            String type = agreement.getType();
            typeCount.put(type, typeCount.getOrDefault(type, 0L) + 1);
        });
        
        if (!typeCount.isEmpty()) {
            formatted.append("- Agreement Types: ");
            typeCount.forEach((type, count) -> 
                formatted.append(type).append(" (").append(count).append("), "));
            // Remove trailing comma and space
            formatted.setLength(formatted.length() - 2);
            formatted.append("\n");
        }
        
        return formatted.toString();
    }
}