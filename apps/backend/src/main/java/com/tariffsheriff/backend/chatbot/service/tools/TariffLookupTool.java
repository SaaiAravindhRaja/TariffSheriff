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
        importerParam.put("description", "ISO3 country code of the importing country (e.g., 'USA', 'CAN', 'JPN')");
        properties.put("importerIso3", importerParam);
        
        // Origin country parameter
        Map<String, Object> originParam = new HashMap<>();
        originParam.put("type", "string");
        originParam.put("description", "ISO3 country code of the origin/exporting country (e.g., 'MEX', 'CHN', 'DEU')");
        properties.put("originIso3", originParam);
        
        // HS Code parameter
        Map<String, Object> hsCodeParam = new HashMap<>();
        hsCodeParam.put("type", "string");
        hsCodeParam.put("description", "Harmonized System code for the product (e.g., '080440' for avocados)");
        properties.put("hsCode", hsCodeParam);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"importerIso3", "originIso3", "hsCode"});
        
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
            String importerIso3 = toolCall.getStringArgument("importerIso3");
            String originIso3 = toolCall.getStringArgument("originIso3");
            String hsCode = toolCall.getStringArgument("hsCode");
            
            // Validate required parameters
            if (importerIso3 == null || importerIso3.trim().isEmpty()) {
                return ToolResult.error(getName(), 
                    "I need to know which country is importing. Please specify the destination country using its 3-letter ISO code (e.g., 'USA' for United States, 'CAN' for Canada).");
            }
            if (originIso3 == null || originIso3.trim().isEmpty()) {
                return ToolResult.error(getName(), 
                    "I need to know which country the product is coming from. Please specify the origin country using its 3-letter ISO code (e.g., 'MEX' for Mexico, 'CHN' for China).");
            }
            if (hsCode == null || hsCode.trim().isEmpty()) {
                return ToolResult.error(getName(), 
                    "I need the HS code for the product. If you don't know it, ask me to 'find the HS code for [product description]' first.");
            }
            
            // Normalize parameters
            importerIso3 = importerIso3.trim().toUpperCase();
            originIso3 = originIso3.trim().toUpperCase();
            hsCode = hsCode.trim();
            
            // Validate formats
            if (importerIso3.length() != 3) {
                return ToolResult.error(getName(), 
                    String.format("The importing country code '%s' doesn't look right. Please use a 3-letter ISO code like 'USA', 'CAN', or 'JPN'. Not sure? Ask me to list available countries.", importerIso3));
            }
            
            if (originIso3.length() != 3) {
                return ToolResult.error(getName(), 
                    String.format("The origin country code '%s' doesn't look right. Please use a 3-letter ISO code like 'MEX', 'CHN', or 'DEU'. Not sure? Ask me to list available countries.", originIso3));
            }
            
            if (!hsCode.matches("\\d{4,10}")) {
                return ToolResult.error(getName(), 
                    String.format("The HS code '%s' doesn't look valid. HS codes should be 4-10 digits (e.g., '0804' or '080440'). If you're not sure of the code, ask me to find it for you.", hsCode));
            }
            
            logger.info("Looking up tariff rate: {} -> {} for HS code: {}", originIso3, importerIso3, hsCode);
            
            // Get tariff rate from service
            TariffRateLookupDto result = tariffRateService.getTariffRateWithAgreement(importerIso3, originIso3, hsCode);
            
            // Format result
            String formattedResult = formatTariffResult(result, importerIso3, originIso3, hsCode);
            
            ToolResult toolResult = ToolResult.success(getName(), formattedResult);
            toolResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            logger.info("Successfully completed tariff lookup in {}ms", toolResult.getExecutionTimeMs());
            
            return toolResult;
            
        } catch (Exception e) {
            logger.error("Error executing tariff lookup tool for route {} -> {}, HS code: {}", 
                    toolCall.getStringArgument("originIso3"), 
                    toolCall.getStringArgument("importerIso3"), 
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
    private String formatTariffResult(TariffRateLookupDto result, String importerIso3, String originIso3, String hsCode) {
        StringBuilder formatted = new StringBuilder();
        
        formatted.append("Tariff Rate Lookup Results:\n");
        formatted.append("Trade Route: ").append(originIso3).append(" → ").append(importerIso3).append("\n");
        formatted.append("HS Code: ").append(hsCode).append("\n\n");
        
        if (result.rates() == null || result.rates().isEmpty()) {
            formatted.append("No tariff rates found for this trade route and HS code.\n");
            return formatted.toString();
        }
        
        // Separate MFN and preferential rates
        var mfnRates = result.rates().stream()
                .filter(rate -> "MFN".equalsIgnoreCase(rate.basis()))
                .toList();
        var prefRates = result.rates().stream()
                .filter(rate -> !"MFN".equalsIgnoreCase(rate.basis()))
                .toList();
        
        // MFN Rates
        if (!mfnRates.isEmpty()) {
            formatted.append("Most Favored Nation (MFN) Rate:\n");
            for (var rate : mfnRates) {
                formatted.append("- Rate: ").append(formatRateOption(rate)).append("\n");
            }
        } else {
            formatted.append("Most Favored Nation (MFN) Rate: Not available\n");
        }
        
        formatted.append("\n");
        
        // Preferential Rates
        if (!prefRates.isEmpty()) {
            formatted.append("Preferential Rate(s):\n");
            for (var rate : prefRates) {
                formatted.append("- Rate: ").append(formatRateOption(rate)).append("\n");
                if (rate.agreementName() != null) {
                    formatted.append("  Trade Agreement: ").append(rate.agreementName()).append("\n");
                }
                if (rate.rvcThreshold() != null) {
                    formatted.append("  RVC Threshold: ").append(rate.rvcThreshold()).append("%\n");
                }
            }
        } else {
            formatted.append("Preferential Rate: Not available (no applicable trade agreement)\n");
        }
        
        return formatted.toString();
    }
    
    /**
     * Format tariff rate option for display
     */
    private String formatRateOption(com.tariffsheriff.backend.tariff.dto.TariffRateOptionDto rate) {
        StringBuilder formatted = new StringBuilder();
        
        if (rate.adValoremRate() != null) {
            formatted.append(rate.adValoremRate()).append("%");
        }
        
        if (rate.nonAdValorem()) {
            if (formatted.length() > 0) formatted.append(" + ");
            formatted.append(rate.nonAdValoremText() != null ? rate.nonAdValoremText() : "Special tariff note");
        }
        
        if (formatted.length() == 0) {
            formatted.append("Free");
        }
        
        return formatted.toString();
    }
}
