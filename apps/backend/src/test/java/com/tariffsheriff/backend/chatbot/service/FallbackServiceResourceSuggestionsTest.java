package com.tariffsheriff.backend.chatbot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for FallbackService resource suggestions and enhanced error messages
 */
@ExtendWith(MockitoExtension.class)
class FallbackServiceResourceSuggestionsTest {

    @Mock
    private ChatCacheService cacheService;

    @Mock
    private ConversationService conversationService;

    private FallbackService fallbackService;

    @BeforeEach
    void setUp() {
        fallbackService = new FallbackService(cacheService, conversationService);
    }

    @Test
    void testSuggestOfficialResources_TariffQuery() {
        // Test tariff-related resource suggestions
        String query = "What is the tariff rate for importing steel?";
        List<FallbackService.ResourceSuggestion> suggestions = 
                fallbackService.suggestOfficialResources(query, "TARIFF");

        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.size() >= 2);
        
        // Verify WTO resource is included
        boolean hasWTO = suggestions.stream()
                .anyMatch(s -> s.getName().contains("WTO"));
        assertTrue(hasWTO, "Should include WTO resource");
        
        // Verify all suggestions have required fields
        for (FallbackService.ResourceSuggestion suggestion : suggestions) {
            assertNotNull(suggestion.getName());
            assertNotNull(suggestion.getUrl());
            assertNotNull(suggestion.getDescription());
            assertNotNull(suggestion.getRelevance());
            assertTrue(suggestion.getUrl().startsWith("http"));
        }
    }

    @Test
    void testSuggestOfficialResources_HsCodeQuery() {
        // Test HS code resource suggestions
        String query = "Find HS code for laptops";
        List<FallbackService.ResourceSuggestion> suggestions = 
                fallbackService.suggestOfficialResources(query, "HS_CODE");

        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        
        // Verify USITC HTS resource is included
        boolean hasUSITC = suggestions.stream()
                .anyMatch(s -> s.getName().contains("USITC") || s.getUrl().contains("hts.usitc.gov"));
        assertTrue(hasUSITC, "Should include USITC HTS resource");
    }

    @Test
    void testSuggestOfficialResources_AgreementQuery() {
        // Test trade agreement resource suggestions
        String query = "What trade agreements does Canada have?";
        List<FallbackService.ResourceSuggestion> suggestions = 
                fallbackService.suggestOfficialResources(query, "AGREEMENT");

        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        
        // Verify trade agreement resources are included
        boolean hasTradeAgreementResource = suggestions.stream()
                .anyMatch(s -> s.getName().toLowerCase().contains("agreement") || 
                              s.getName().toLowerCase().contains("fta"));
        assertTrue(hasTradeAgreementResource, "Should include trade agreement resources");
    }

    @Test
    void testSuggestOfficialResources_CountryQuery() {
        // Test country-specific resource suggestions
        String query = "Tell me about trade with Germany";
        List<FallbackService.ResourceSuggestion> suggestions = 
                fallbackService.suggestOfficialResources(query, "COUNTRY");

        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        
        // Verify country profile resources are included
        boolean hasCountryResource = suggestions.stream()
                .anyMatch(s -> s.getName().toLowerCase().contains("country") || 
                              s.getName().toLowerCase().contains("profile"));
        assertTrue(hasCountryResource, "Should include country profile resources");
    }

    @Test
    void testFormatResourceSuggestions() {
        // Test formatting of resource suggestions
        String query = "tariff rates";
        List<FallbackService.ResourceSuggestion> suggestions = 
                fallbackService.suggestOfficialResources(query, "TARIFF");
        
        String formatted = fallbackService.formatResourceSuggestions(suggestions);
        
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());
        assertTrue(formatted.contains("Official Resources"));
        assertTrue(formatted.contains("🔗"));
        assertTrue(formatted.contains("http"));
    }

    @Test
    void testFormatResourceSuggestions_EmptyList() {
        // Test formatting with empty list
        String formatted = fallbackService.formatResourceSuggestions(List.of());
        
        assertNotNull(formatted);
        assertTrue(formatted.isEmpty());
    }

    @Test
    void testGenerateDataNotFoundResponse_Tariff() {
        // Test data not found response for tariff queries
        String query = "What is the tariff for importing unicorns from Narnia?";
        String response = fallbackService.generateDataNotFoundResponse(
                query, "tariff", "Product or country not found in database");

        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.contains("couldn't find"));
        assertTrue(response.contains("What this means"));
        assertTrue(response.contains("What you can try"));
        assertTrue(response.contains("Official Resources"));
        assertTrue(response.contains("http"));
    }

    @Test
    void testGenerateDataNotFoundResponse_HsCode() {
        // Test data not found response for HS code queries
        String query = "Find HS code for magical wands";
        String response = fallbackService.generateDataNotFoundResponse(
                query, "HS code", "Product not found");

        assertNotNull(response);
        assertTrue(response.contains("couldn't find"));
        assertTrue(response.contains("material composition"));
        assertTrue(response.contains("synonyms"));
    }

    @Test
    void testGenerateDataNotFoundResponse_Agreement() {
        // Test data not found response for agreement queries
        String query = "Trade agreement between Atlantis and Wakanda";
        String response = fallbackService.generateDataNotFoundResponse(
                query, "trade agreement", "Countries not found");

        assertNotNull(response);
        assertTrue(response.contains("couldn't find"));
        assertTrue(response.contains("country names"));
        assertTrue(response.contains("spelled correctly"));
    }

    @Test
    void testGenerateDataNotFoundResponse_WithoutDataType() {
        // Test data not found response without specific data type
        String query = "Some random query";
        String response = fallbackService.generateDataNotFoundResponse(
                query, null, null);

        assertNotNull(response);
        assertTrue(response.contains("couldn't find"));
        assertTrue(response.contains("What you can try"));
    }

    @Test
    void testGenerateDataNotFoundResponse_IncludesNextSteps() {
        // Verify response includes actionable next steps
        String query = "tariff for steel";
        String response = fallbackService.generateDataNotFoundResponse(
                query, "tariff", "Not found");

        assertTrue(response.contains("Try"));
        assertTrue(response.contains("Calculator") || response.contains("Database"));
        assertTrue(response.contains("Tip"));
    }
}
