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

import java.util.HashMap;
import java.util.Map;

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
        return "getTariffRateLookup";
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
            "Get MFN and preferential tariff rates for specific trade routes. Returns both Most Favored Nation rates and any applicable preferential rates from trade agreements.",
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
                return ToolResult.error(getName(), "Missing required parameter: importerIso2");
            }
            if (originIso2 == null || originIso2.trim().isEmpty()) {
                return ToolResult.error(getName(), "Missing required parameter: originIso2");
            }
            if (hsCode == null || hsCode.trim().isEmpty()) {
                return ToolResult.error(getName(), "Missing required parameter: hsCode");
            }
            
            // Normalize parameters
            importerIso2 = importerIso2.trim().toUpperCase();
            originIso2 = originIso2.trim().toUpperCase();
            hsCode = hsCode.trim();
            
            // Validate ISO2 format (2 characters)
            if (importerIso2.length() != 2) {
                return ToolResult.error(getName(), "Invalid importerIso2 format. Must be 2-character ISO country code (e.g., 'US')");
            }
            if (originIso2.length() != 2) {
                return ToolResult.error(getName(), "Invalid originIso2 format. Must be 2-character ISO country code (e.g., 'MX')");
            }
            
            // Validate HS code format (should be numeric and reasonable length)
            if (!hsCode.matches("\\d{4,10}")) {
                return ToolResult.error(getName(), "Invalid HS code format. Must be 4-10 digit numeric code");
            }
            
            logger.info("Looking up tariff rates for trade route: {} -> {} for HS code: {}", 
                       originIso2, importerIso2, hsCode);
            
            // Call the service
            TariffRateLookupDto result = tariffRateService.getTariffRateWithAgreement(
                importerIso2, originIso2, hsCode);
            
            // Format result for LLM consumption
            String formattedResult = formatTariffResult(result, importerIso2, originIso2, hsCode);
            
            ToolResult toolResult = ToolResult.success(getName(), formattedResult);
            toolResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            logger.info("Successfully retrieved tariff rates for {}->{} HS:{} in {}ms", 
                       originIso2, importerIso2, hsCode, toolResult.getExecutionTimeMs());
            
            return toolResult;
            
        } catch (Exception e) {
            logger.error("Error executing tariff lookup tool", e);
            ToolResult errorResult = ToolResult.error(getName(), 
                "Failed to retrieve tariff rates: " + e.getMessage());
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
            formatted.append("- Rate: ").append(result.tariffRateMfn().getRate()).append("%\n");
            formatted.append("- Basis: ").append(result.tariffRateMfn().getBasis()).append("\n");
            if (result.tariffRateMfn().getUnit() != null) {
                formatted.append("- Unit: ").append(result.tariffRateMfn().getUnit()).append("\n");
            }
        } else {
            formatted.append("Most Favored Nation (MFN) Rate: Not available\n");
        }
        
        formatted.append("\n");
        
        // Preferential Rate
        if (result.tariffRatePref() != null) {
            formatted.append("Preferential Rate:\n");
            formatted.append("- Rate: ").append(result.tariffRatePref().getRate()).append("%\n");
            formatted.append("- Basis: ").append(result.tariffRatePref().getBasis()).append("\n");
            if (result.tariffRatePref().getUnit() != null) {
                formatted.append("- Unit: ").append(result.tariffRatePref().getUnit()).append("\n");
            }
            
            // Agreement information
            if (result.agreement() != null) {
                formatted.append("- Trade Agreement: ").append(result.agreement().getName()).append("\n");
                formatted.append("- Agreement Type: ").append(result.agreement().getType()).append("\n");
                formatted.append("- Status: ").append(result.agreement().getStatus()).append("\n");
                if (result.agreement().getRvcThreshold() != null) {
                    formatted.append("- RVC Threshold: ").append(result.agreement().getRvcThreshold()).append("%\n");
                }
            }
        } else {
            formatted.append("Preferential Rate: Not available (no applicable trade agreement)\n");
        }
        
        return formatted.toString();
    }
}