package com.tariffsheriff.backend.chatbot.service.tools;

import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.service.AgreementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

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
        return "getTradeAgreements";
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
            "Retrieve trade agreements for a specific country including FTAs and preferential agreements. " +
            "Returns agreement names, types, status, and RVC thresholds. " +
            "Use when user asks about trade agreements, FTAs, or preferential rates.",
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
                return ToolResult.error(getName(), 
                    "I need to know which country you're asking about. Please provide a 2-letter country code (e.g., 'US', 'CA', 'JP').");
            }
            
            // Normalize parameters
            countryIso2 = countryIso2.trim().toUpperCase();
            
            // Validate ISO2 format
            if (countryIso2.length() != 2 || !countryIso2.matches("[A-Z]{2}")) {
                return ToolResult.error(getName(), 
                    String.format("The country code '%s' doesn't look right. Please use a 2-letter ISO code like 'US', 'CA', or 'JP'. Not sure? Ask me to list available countries.", countryIso2));
            }
            
            logger.info("Looking up trade agreements for country: {}", countryIso2);
            
            // Get agreements from service
            List<Agreement> agreements = agreementService.getAgreementsByCountry(countryIso2);
            
            // Format result
            String formattedResult = formatAgreementResult(agreements, countryIso2);
            
            ToolResult toolResult = ToolResult.success(getName(), formattedResult);
            toolResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            logger.info("Completed agreement lookup in {}ms, found {} agreements", 
                       toolResult.getExecutionTimeMs(), agreements.size());
            
            return toolResult;
            
        } catch (Exception e) {
            logger.error("Error executing agreement tool for country: {}", 
                    toolCall.getStringArgument("countryIso2"), e);
            
            String userMessage = "I couldn't retrieve trade agreement information. ";
            
            // Provide helpful guidance
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("not found")) {
                userMessage += "The country code you provided might not be in our database. " +
                        "Try asking me to list available countries or search for the country by name.";
            } else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("database")) {
                userMessage += "There's a problem connecting to the trade agreement database. Please try again in a moment.";
            } else {
                userMessage += "Please try:\n" +
                        "• Verifying the country code is correct (e.g., 'US', 'CA', 'JP')\n" +
                        "• Asking me to list available countries\n" +
                        "• Rephrasing your question";
            }
            
            ToolResult errorResult = ToolResult.error(getName(), userMessage);
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
