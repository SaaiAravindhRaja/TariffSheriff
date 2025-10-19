package com.tariffsheriff.backend.chatbot.service.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.tariff.model.Country;
import com.tariffsheriff.backend.tariff.repository.CountryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool for querying country information
 */
@Component
public class CountryInfoTool implements ChatbotTool {
    
    private static final Logger logger = LoggerFactory.getLogger(CountryInfoTool.class);
    
    private final CountryRepository countryRepository;
    private final ObjectMapper objectMapper;
    
    public CountryInfoTool(CountryRepository countryRepository, ObjectMapper objectMapper) {
        this.countryRepository = countryRepository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "getCountryInfo";
    }
    
    @Override
    public ToolDefinition getDefinition() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // Action parameter
        Map<String, Object> actionParam = new HashMap<>();
        actionParam.put("type", "string");
        actionParam.put("description", "Action to perform: 'list' (list all countries), 'get' (get country by ISO2 code), or 'search' (search countries by name)");
        actionParam.put("enum", new String[]{"list", "get", "search"});
        properties.put("action", actionParam);
        
        // ISO2 code parameter (for 'get' action)
        Map<String, Object> iso2Param = new HashMap<>();
        iso2Param.put("type", "string");
        iso2Param.put("description", "ISO2 country code (e.g., 'US', 'CA') - required for 'get' action");
        properties.put("iso2", iso2Param);
        
        // Search query parameter (for 'search' action)
        Map<String, Object> queryParam = new HashMap<>();
        queryParam.put("type", "string");
        queryParam.put("description", "Search query for country name - required for 'search' action");
        properties.put("query", queryParam);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"action"});
        
        return new ToolDefinition(
            getName(),
            "Get information about countries in the system. " +
            "Use 'list' to see all available countries, 'get' to retrieve details for a specific country by ISO2 code, " +
            "or 'search' to find countries by name. " +
            "Use when user asks about available countries, country codes, or wants to find a country.",
            parameters
        );
    }
    
    @Override
    public ToolResult execute(ToolCall toolCall) {
        long startTime = System.currentTimeMillis();
        
        try {
            String action = toolCall.getStringArgument("action");
            
            if (action == null || action.trim().isEmpty()) {
                return ToolResult.error(getName(), 
                    "I need to know what you want to do. You can ask me to:\n" +
                    "• List all available countries\n" +
                    "• Get details for a specific country (provide the 2-letter code)\n" +
                    "• Search for countries by name");
            }
            
            action = action.trim().toLowerCase();
            
            String result;
            switch (action) {
                case "list":
                    result = listCountries();
                    break;
                case "get":
                    String iso2 = toolCall.getStringArgument("iso2");
                    if (iso2 == null || iso2.trim().isEmpty()) {
                        return ToolResult.error(getName(), 
                            "I need the country code to look up. Please provide a 2-letter ISO code (e.g., 'US', 'CA', 'JP').");
                    }
                    result = getCountryByCode(iso2.trim().toUpperCase());
                    break;
                case "search":
                    String query = toolCall.getStringArgument("query");
                    if (query == null || query.trim().isEmpty()) {
                        return ToolResult.error(getName(), 
                            "I need a search term to find countries. Please tell me what country name you're looking for.");
                    }
                    result = searchCountries(query.trim());
                    break;
                default:
                    return ToolResult.error(getName(), 
                        String.format("I don't understand the action '%s'. You can ask me to 'list', 'get', or 'search' for countries.", action));
            }
            
            ToolResult toolResult = ToolResult.success(getName(), result);
            toolResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            logger.info("Successfully completed country info query (action: {}) in {}ms", action, toolResult.getExecutionTimeMs());
            
            return toolResult;
            
        } catch (Exception e) {
            logger.error("Error executing country info tool (action: {})", 
                    toolCall.getStringArgument("action"), e);
            
            String userMessage = "I had trouble getting country information. ";
            
            // Provide helpful guidance
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("database")) {
                userMessage += "There's a problem connecting to the country database. Please try again in a moment.";
            } else {
                userMessage += "Please try:\n" +
                        "• Asking me to 'list all countries'\n" +
                        "• Searching with a different country name\n" +
                        "• Using the full country name instead of abbreviations\n" +
                        "• Checking your spelling";
            }
            
            ToolResult errorResult = ToolResult.error(getName(), userMessage);
            errorResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return errorResult;
        }
    }
    
    /**
     * List all available countries
     */
    private String listCountries() {
        logger.info("Listing all countries");
        
        List<Country> countries = countryRepository.findAll();
        
        if (countries.isEmpty()) {
            return "No countries found in the system.";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Available Countries (").append(countries.size()).append(" total):\n\n");
        
        // Sort by name for better readability
        countries.sort(Comparator.comparing(Country::getName));
        
        for (Country country : countries) {
            result.append("- ").append(country.getName())
                  .append(" (").append(country.getIso2()).append(")");
            
            if (country.getIso3() != null && !country.getIso3().isEmpty()) {
                result.append(" [ISO3: ").append(country.getIso3()).append("]");
            }
            
            result.append("\n");
        }
        
        return result.toString();
    }
    
    /**
     * Get country details by ISO2 code
     */
    private String getCountryByCode(String iso2) {
        logger.info("Getting country by ISO2 code: {}", iso2);
        
        Optional<Country> countryOpt = countryRepository.findByIso2IgnoreCase(iso2);
        
        if (countryOpt.isEmpty()) {
            return String.format("Country not found with ISO2 code: %s. " +
                "Please check the code or use the 'list' action to see available countries.", iso2);
        }
        
        Country country = countryOpt.get();
        
        StringBuilder result = new StringBuilder();
        result.append("Country Details:\n\n");
        result.append("Name: ").append(country.getName()).append("\n");
        result.append("ISO2 Code: ").append(country.getIso2()).append("\n");
        
        if (country.getIso3() != null && !country.getIso3().isEmpty()) {
            result.append("ISO3 Code: ").append(country.getIso3()).append("\n");
        }
        
        result.append("\nThis country is available in the Tariff Sheriff system for tariff lookups and trade agreement queries.");
        
        return result.toString();
    }
    
    /**
     * Search countries by name
     */
    private String searchCountries(String query) {
        logger.info("Searching countries with query: {}", query);
        
        // Get up to 20 results
        Page<Country> results = countryRepository.findAllByNameContainingIgnoreCase(query, PageRequest.of(0, 20));
        
        if (results.isEmpty()) {
            return String.format("No countries found matching '%s'. " +
                "Try a different search term or use the 'list' action to see all available countries.", query);
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Countries matching '").append(query).append("' (")
              .append(results.getTotalElements()).append(" found):\n\n");
        
        for (Country country : results.getContent()) {
            result.append("- ").append(country.getName())
                  .append(" (").append(country.getIso2()).append(")");
            
            if (country.getIso3() != null && !country.getIso3().isEmpty()) {
                result.append(" [ISO3: ").append(country.getIso3()).append("]");
            }
            
            result.append("\n");
        }
        
        if (results.getTotalElements() > results.getNumberOfElements()) {
            result.append("\n(Showing first ").append(results.getNumberOfElements())
                  .append(" of ").append(results.getTotalElements()).append(" results)");
        }
        
        return result.toString();
    }
}
