package com.tariffsheriff.backend.chatbot.service.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.service.TariffRateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool for looking up tariff rates between countries for specific HS codes
 */
@Component
public class TariffLookupTool implements ChatbotTool {
    
    private static final Logger logger = LoggerFactory.getLogger(TariffLookupTool.class);
    
    private final TariffRateService tariffRateService;
    private final ObjectMapper objectMapper;
    
    public TariffLookupTool(TariffRateService tariffRateService, ObjectMapper objectMapper) {
        this.tariffRateService = tariffRateService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "getTariffRate";
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
        
        // Origin country parameter
        Map<String, Object> originParam = new HashMap<>();
        originParam.put("type", "string");
        originParam.put("description", "ISO2 country code of the origin/exporting country (e.g., 'MX', 'CN', 'DE')");
        properties.put("originIso2", originParam);
        
        // HS Code parameter
        Map<String, Object> hsCodeParam = new HashMap<>();
        hsCodeParam.put("type", "string");
        hsCodeParam.put("description", "Harmonized System code for the product (e.g., '080440' for avocados)");
        properties.put("hsCode", hsCodeParam);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"importerIso2", "originIso2", "hsCode"});
        
        return new ToolDefinition(
            getName(),
            "Look up tariff rates for importing products between countries. " +
            "Returns MFN rates, preferential rates, and applicable trade agreements. " +
            "Use when user asks about tariff rates, import duties, or customs charges.",
            parameters
        );
    }
    
    @Override
    public ToolResult execute(ToolCall toolCall) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract and validate parameters
            String importerIso2 = toolCall.getStringArgument("importerIso2");
            String originIso2 = toolCall.getStringArgument("originIso2");
            String hsCode = toolCall.getStringArgument("hsCode");
            
            // Validate required parameters
            if (importerIso2 == null || importerIso2.trim().isEmpty()) {
                return ToolResult.error(getName(), 
                    "I need to know which country is importing. Please specify the destination country using its 2-letter code (e.g., 'US' for United States, 'CA' for Canada).");
            }
            if (originIso2 == null || originIso2.trim().isEmpty()) {
                return ToolResult.error(getName(), 
                    "I need to know which country the product is coming from. Please specify the origin country using its 2-letter code (e.g., 'MX' for Mexico, 'CN' for China).");
            }
            if (hsCode == null || hsCode.trim().isEmpty()) {
                return ToolResult.error(getName(), 
                    "I need the HS code for the product. If you don't know it, ask me to 'find the HS code for [product description]' first.");
            }
            
            // Normalize parameters
            importerIso2 = importerIso2.trim().toUpperCase();
            originIso2 = originIso2.trim().toUpperCase();
            hsCode = hsCode.trim();
            
            // Validate formats
            if (importerIso2.length() != 2) {
                return ToolResult.error(getName(), 
                    String.format("The importing country code '%s' doesn't look right. Please use a 2-letter ISO code like 'US', 'CA', or 'JP'. Not sure? Ask me to list available countries.", importerIso2));
            }
            
            if (originIso2.length() != 2) {
                return ToolResult.error(getName(), 
                    String.format("The origin country code '%s' doesn't look right. Please use a 2-letter ISO code like 'MX', 'CN', or 'DE'. Not sure? Ask me to list available countries.", originIso2));
            }
            
            if (!hsCode.matches("\\d{4,10}")) {
                return ToolResult.error(getName(), 
                    String.format("The HS code '%s' doesn't look valid. HS codes should be 4-10 digits (e.g., '0804' or '080440'). If you're not sure of the code, ask me to find it for you.", hsCode));
            }
            
            logger.info("Looking up tariff rate: {} -> {} for HS code: {}", originIso2, importerIso2, hsCode);
            
            // Get tariff rate from service
            TariffRateLookupDto result = tariffRateService.getTariffRateWithAgreement(importerIso2, originIso2, hsCode);
            
            // Format result
            String formattedResult = formatTariffResult(result, importerIso2, originIso2, hsCode);
            
            ToolResult toolResult = ToolResult.success(getName(), formattedResult);
            toolResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            logger.info("Successfully completed tariff lookup in {}ms", toolResult.getExecutionTimeMs());
            
            return toolResult;
            
        } catch (Exception e) {
            logger.error("Error executing tariff lookup tool for route {} -> {}, HS code: {}", 
                    toolCall.getStringArgument("originIso2"), 
                    toolCall.getStringArgument("importerIso2"), 
                    toolCall.getStringArgument("hsCode"), e);
            
            String userMessage = "I couldn't find the tariff information you requested. ";
            
            // Provide specific guidance based on error type
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("not found")) {
                userMessage += "This could mean:\n" +
                        "• The country codes might not be in our database\n" +
                        "• The HS code might not exist for this trade route\n" +
                        "• The data might not be available yet\n\n" +
                        "Try asking me to list available countries or search for a different HS code.";
            } else {
                userMessage += "There was a problem accessing the tariff database. Please try again or rephrase your question.";
            }
            
            ToolResult errorResult = ToolResult.error(getName(), userMessage);
            errorResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return errorResult;
        }
    }
    
    /**
     * Format the tariff lookup result for LLM consumption
     */
    private String formatTariffResult(TariffRateLookupDto result, String importerIso2, String originIso2, String hsCode) {
        StringBuilder formatted = new StringBuilder();
        
        formatted.append("Tariff Rate Lookup Results:\n");
        formatted.append("Trade Route: ").append(originIso2).append(" → ").append(importerIso2).append("\n");
        formatted.append("HS Code: ").append(hsCode).append("\n\n");
        
        // MFN Rate
        if (result.tariffRateMfn() != null) {
            formatted.append("Most Favored Nation (MFN) Rate:\n");
            formatted.append("- Rate: ").append(formatRate(result.tariffRateMfn())).append("\n");
        } else {
            formatted.append("Most Favored Nation (MFN) Rate: Not available\n");
        }
        
        formatted.append("\n");
        
        // Preferential Rate
        if (result.tariffRatePref() != null) {
            formatted.append("Preferential Rate:\n");
            formatted.append("- Rate: ").append(formatRate(result.tariffRatePref())).append("\n");
            
            // Agreement information
            if (result.agreement() != null) {
                formatted.append("- Trade Agreement: ").append(result.agreement().getName()).append("\n");
                formatted.append("- Agreement Type: ").append(result.agreement().getType()).append("\n");
                if (result.agreement().getRvcThreshold() != null) {
                    formatted.append("- RVC Threshold: ").append(result.agreement().getRvcThreshold()).append("%\n");
                }
            }
        } else {
            formatted.append("Preferential Rate: Not available (no applicable trade agreement)\n");
        }
        
        return formatted.toString();
    }
    
    /**
     * Format tariff rate for display
     */
    private String formatRate(com.tariffsheriff.backend.tariff.model.TariffRate rate) {
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
            formatted.append("Free");
        }
        
        return formatted.toString();
    }
}
