package com.tariffsheriff.backend.chatbot.service.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.service.TariffRateService;
import com.tariffsheriff.backend.data.ExternalDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Tool for looking up tariff rates between countries for specific HS codes
 */
@Component
public class TariffLookupTool implements ChatbotTool {
    
    private static final Logger logger = LoggerFactory.getLogger(TariffLookupTool.class);
    
    private final TariffRateService tariffRateService;
    private final ObjectMapper objectMapper;
    
    @Autowired(required = false)
    private ExternalDataService externalDataService;
    
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
        
        // Origin countries parameter (enhanced for multi-route analysis)
        Map<String, Object> originParam = new HashMap<>();
        originParam.put("type", "array");
        originParam.put("description", "Array of ISO2 country codes for origin/exporting countries (e.g., ['MX', 'CN', 'DE']) for multi-route comparison, or single string for single route");
        Map<String, Object> originItems = new HashMap<>();
        originItems.put("type", "string");
        originParam.put("items", originItems);
        properties.put("originCountries", originParam);
        
        // Legacy single origin support
        Map<String, Object> singleOriginParam = new HashMap<>();
        singleOriginParam.put("type", "string");
        singleOriginParam.put("description", "ISO2 country code of the origin/exporting country (e.g., 'MX', 'CN', 'DE') - legacy support");
        properties.put("originIso2", singleOriginParam);
        
        // HS Code parameter
        Map<String, Object> hsCodeParam = new HashMap<>();
        hsCodeParam.put("type", "string");
        hsCodeParam.put("description", "Harmonized System code for the product (e.g., '080440' for avocados)");
        properties.put("hsCode", hsCodeParam);
        
        // Value parameter for total landed cost calculation
        Map<String, Object> valueParam = new HashMap<>();
        valueParam.put("type", "number");
        valueParam.put("description", "FOB value of the goods in USD for total landed cost calculation (optional)");
        properties.put("fobValue", valueParam);
        
        // Quantity parameter
        Map<String, Object> quantityParam = new HashMap<>();
        quantityParam.put("type", "number");
        quantityParam.put("description", "Quantity of goods for cost analysis (optional)");
        properties.put("quantity", quantityParam);
        
        // Weight parameter for shipping cost estimation
        Map<String, Object> weightParam = new HashMap<>();
        weightParam.put("type", "number");
        weightParam.put("description", "Weight in kg for shipping cost estimation (optional)");
        properties.put("weightKg", weightParam);
        
        // Analysis type parameter
        Map<String, Object> analysisParam = new HashMap<>();
        analysisParam.put("type", "string");
        analysisParam.put("description", "Type of analysis: 'basic' (default), 'multi-route', 'total-cost', 'seasonal', 'duty-drawback'");
        analysisParam.put("enum", Arrays.asList("basic", "multi-route", "total-cost", "seasonal", "duty-drawback"));
        properties.put("analysisType", analysisParam);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"importerIso2", "hsCode"});
        
        return new ToolDefinition(
            getName(),
            "Look up tariff rates and duties for importing products between countries. " +
            "USE WHEN: User asks about tariff rates, import duties, customs charges, or total landed costs. " +
            "REQUIRES: Importer country (ISO2), origin country (ISO2), and HS code. " +
            "RETURNS: MFN rates, preferential rates, applicable trade agreements, duty calculations, and cost breakdowns. " +
            "EXAMPLES: 'What's the tariff for importing steel from China to USA?', 'Calculate total landed cost for HS 080440 from Mexico to Canada', 'Compare tariff rates from multiple countries'. " +
            "SUPPORTS: Single route analysis, multi-route comparison, total cost calculation, seasonal analysis, and duty drawback assessment.",
            parameters
        );
    }
    
    @Override
    public ToolResult execute(ToolCall toolCall) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract and validate parameters
            String importerIso2 = toolCall.getStringArgument("importerIso2");
            String hsCode = toolCall.getStringArgument("hsCode");
            String analysisType = toolCall.getStringArgument("analysisType", "basic");
            
            // Handle both legacy single origin and new multi-origin parameters
            List<String> originCountries = extractOriginCountries(toolCall);
            
            // Optional parameters for advanced analysis
            BigDecimal fobValue = toolCall.getBigDecimalArgument("fobValue");
            BigDecimal quantity = toolCall.getBigDecimalArgument("quantity");
            BigDecimal weightKg = toolCall.getBigDecimalArgument("weightKg");
            
            // Validate required parameters
            if (importerIso2 == null || importerIso2.trim().isEmpty()) {
                return ToolResult.error(getName(), "Missing required parameter: importerIso2");
            }
            if (hsCode == null || hsCode.trim().isEmpty()) {
                return ToolResult.error(getName(), "Missing required parameter: hsCode");
            }
            if (originCountries.isEmpty()) {
                return ToolResult.error(getName(), "Missing origin country information. Provide either originIso2 or originCountries");
            }
            
            // Normalize parameters
            importerIso2 = importerIso2.trim().toUpperCase();
            hsCode = hsCode.trim();
            originCountries = originCountries.stream()
                .map(country -> country.trim().toUpperCase())
                .collect(Collectors.toList());
            
            // Validate formats
            if (importerIso2.length() != 2) {
                return ToolResult.error(getName(), "Invalid importerIso2 format. Must be 2-character ISO country code (e.g., 'US')");
            }
            
            for (String origin : originCountries) {
                if (origin.length() != 2) {
                    return ToolResult.error(getName(), "Invalid origin country format: " + origin + ". Must be 2-character ISO country code");
                }
            }
            
            if (!hsCode.matches("\\d{4,10}")) {
                return ToolResult.error(getName(), "Invalid HS code format. Must be 4-10 digit numeric code");
            }
            
            logger.info("Executing {} analysis for trade routes: {} -> {} for HS code: {}", 
                       analysisType, originCountries, importerIso2, hsCode);
            
            // Execute analysis based on type with real-time data enrichment
            String formattedResult = switch (analysisType.toLowerCase()) {
                case "multi-route" -> performMultiRouteAnalysisWithEnrichment(importerIso2, originCountries, hsCode, fobValue, quantity);
                case "total-cost" -> performTotalCostAnalysisWithEnrichment(importerIso2, originCountries.get(0), hsCode, fobValue, quantity, weightKg);
                case "seasonal" -> performSeasonalAnalysisWithEnrichment(importerIso2, originCountries.get(0), hsCode);
                case "duty-drawback" -> performDutyDrawbackAnalysisWithEnrichment(importerIso2, originCountries.get(0), hsCode, fobValue);
                default -> performBasicAnalysisWithEnrichment(importerIso2, originCountries.get(0), hsCode);
            };
            
            ToolResult toolResult = ToolResult.success(getName(), formattedResult);
            toolResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            logger.info("Successfully completed {} analysis in {}ms", analysisType, toolResult.getExecutionTimeMs());
            
            return toolResult;
            
        } catch (Exception e) {
            logger.error("Error executing enhanced tariff lookup tool", e);
            ToolResult errorResult = ToolResult.error(getName(), 
                "Failed to perform tariff analysis: " + e.getMessage());
            errorResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return errorResult;
        }
    }
    
    /**
     * Extract origin countries from tool call parameters (supports both legacy and new formats)
     */
    private List<String> extractOriginCountries(ToolCall toolCall) {
        List<String> originCountries = new ArrayList<>();
        
        // Try new array format first
        Object originCountriesParam = toolCall.getArgument("originCountries");
        if (originCountriesParam instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String country) {
                    originCountries.add(country);
                }
            }
        }
        
        // Fall back to legacy single origin format
        if (originCountries.isEmpty()) {
            String singleOrigin = toolCall.getStringArgument("originIso2");
            if (singleOrigin != null && !singleOrigin.trim().isEmpty()) {
                originCountries.add(singleOrigin);
            }
        }
        
        return originCountries;
    }
    
    /**
     * Perform basic tariff analysis (legacy functionality)
     */
    private String performBasicAnalysis(String importerIso2, String originIso2, String hsCode) {
        TariffRateLookupDto result = tariffRateService.getTariffRateWithAgreement(importerIso2, originIso2, hsCode);
        return formatTariffResult(result, importerIso2, originIso2, hsCode);
    }
    
    /**
     * Perform basic tariff analysis with real-time data enrichment
     */
    private String performBasicAnalysisWithEnrichment(String importerIso2, String originIso2, String hsCode) {
        TariffRateLookupDto result = tariffRateService.getTariffRateWithAgreement(importerIso2, originIso2, hsCode);
        String basicResult = formatTariffResult(result, importerIso2, originIso2, hsCode);
        
        // Add real-time data enrichment
        if (externalDataService != null) {
            try {
                StringBuilder enrichedResult = new StringBuilder(basicResult);
                enrichedResult.append("\n").append("=".repeat(50)).append("\n");
                enrichedResult.append("REAL-TIME MARKET INTELLIGENCE\n");
                enrichedResult.append("=".repeat(50)).append("\n\n");
                
                // Get trade intelligence
                CompletableFuture<ExternalDataService.TradeIntelligence> intelligenceFuture = 
                    externalDataService.getTradeIntelligence(hsCode, originIso2, importerIso2);
                
                ExternalDataService.TradeIntelligence intelligence = intelligenceFuture.get(5, TimeUnit.SECONDS);
                
                // Add news context
                if (!intelligence.getNews().isEmpty()) {
                    enrichedResult.append("Recent Trade News:\n");
                    intelligence.getNews().stream().limit(3).forEach(news -> {
                        enrichedResult.append("• ").append(news.getTitle()).append("\n");
                        enrichedResult.append("  Source: ").append(news.getSource())
                            .append(" (").append(news.getPublishedAt().toString().substring(0, 10)).append(")\n");
                    });
                    enrichedResult.append("\n");
                }
                
                // Add market data context
                if (intelligence.getMarketData() != null && intelligence.getMarketData().getPrice() > 0) {
                    enrichedResult.append("Market Data:\n");
                    enrichedResult.append("• Current Price: $").append(intelligence.getMarketData().getPrice()).append("\n");
                    enrichedResult.append("• Price Change: ").append(intelligence.getMarketData().getChange()).append("%\n");
                    enrichedResult.append("• Volume: ").append(intelligence.getMarketData().getVolume()).append("\n\n");
                }
                
                // Add regulatory updates
                if (!intelligence.getRegulatoryUpdates().isEmpty()) {
                    enrichedResult.append("Recent Regulatory Updates:\n");
                    intelligence.getRegulatoryUpdates().stream().limit(2).forEach(update -> {
                        enrichedResult.append("• ").append(update.getTitle()).append("\n");
                        enrichedResult.append("  Country: ").append(update.getCountry())
                            .append(" | Effective: ").append(update.getEffectiveDate().toString().substring(0, 10)).append("\n");
                    });
                    enrichedResult.append("\n");
                }
                
                enrichedResult.append("Note: Real-time data provided for enhanced trade intelligence.\n");
                return enrichedResult.toString();
                
            } catch (Exception e) {
                logger.warn("Failed to enrich tariff analysis with real-time data: {}", e.getMessage());
                return basicResult + "\n\nNote: Real-time data enrichment temporarily unavailable.";
            }
        }
        
        return basicResult;
    }
    
    /**
     * Perform multi-route comparison analysis
     */
    private String performMultiRouteAnalysis(String importerIso2, List<String> originCountries, String hsCode, 
                                           BigDecimal fobValue, BigDecimal quantity) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Multi-Route Tariff Analysis\n");
        analysis.append("=================================\n");
        analysis.append("Destination: ").append(importerIso2).append("\n");
        analysis.append("HS Code: ").append(hsCode).append("\n");
        if (fobValue != null) {
            analysis.append("FOB Value: $").append(fobValue).append("\n");
        }
        if (quantity != null) {
            analysis.append("Quantity: ").append(quantity).append("\n");
        }
        analysis.append("\n");
        
        List<RouteAnalysis> routes = new ArrayList<>();
        
        for (String originIso2 : originCountries) {
            try {
                TariffRateLookupDto result = tariffRateService.getTariffRateWithAgreement(importerIso2, originIso2, hsCode);
                RouteAnalysis route = analyzeRoute(originIso2, result, fobValue, quantity);
                routes.add(route);
            } catch (Exception e) {
                logger.warn("Failed to analyze route {} -> {}: {}", originIso2, importerIso2, e.getMessage());
                routes.add(new RouteAnalysis(originIso2, null, null, "Data not available: " + e.getMessage()));
            }
        }
        
        // Sort routes by total cost (if available)
        routes.sort((r1, r2) -> {
            if (r1.totalCost == null && r2.totalCost == null) return 0;
            if (r1.totalCost == null) return 1;
            if (r2.totalCost == null) return -1;
            return r1.totalCost.compareTo(r2.totalCost);
        });
        
        // Format results
        analysis.append("Route Comparison (sorted by total cost):\n\n");
        for (int i = 0; i < routes.size(); i++) {
            RouteAnalysis route = routes.get(i);
            analysis.append(String.format("%d. %s → %s\n", i + 1, route.originIso2, importerIso2));
            
            if (route.error != null) {
                analysis.append("   Status: ").append(route.error).append("\n\n");
                continue;
            }
            
            if (route.result.tariffRateMfn() != null) {
                analysis.append("   MFN Rate: ").append(formatRate(route.result.tariffRateMfn())).append("\n");
            }
            
            if (route.result.tariffRatePref() != null) {
                analysis.append("   Preferential Rate: ").append(formatRate(route.result.tariffRatePref()));
                if (route.result.agreement() != null) {
                    analysis.append(" (").append(route.result.agreement().getName()).append(")");
                }
                analysis.append("\n");
            }
            
            if (route.totalCost != null) {
                analysis.append("   Total Cost: $").append(route.totalCost).append("\n");
                BigDecimal savings = calculateSavings(routes.get(routes.size() - 1).totalCost, route.totalCost);
                if (savings.compareTo(BigDecimal.ZERO) > 0 && i > 0) {
                    analysis.append("   Savings vs. most expensive: $").append(savings).append("\n");
                }
            }
            
            analysis.append("\n");
        }
        
        // Add recommendations
        if (routes.size() > 1 && routes.get(0).totalCost != null) {
            analysis.append("Recommendations:\n");
            analysis.append("- Best route: ").append(routes.get(0).originIso2).append(" → ").append(importerIso2);
            if (routes.get(0).result.tariffRatePref() != null) {
                analysis.append(" (preferential rate available)");
            }
            analysis.append("\n");
            
            if (routes.size() > 1 && routes.get(routes.size() - 1).totalCost != null) {
                BigDecimal totalSavings = calculateSavings(routes.get(routes.size() - 1).totalCost, routes.get(0).totalCost);
                analysis.append("- Potential savings: $").append(totalSavings).append(" vs. most expensive route\n");
            }
        }
        
        return analysis.toString();
    }
    
    /**
     * Perform multi-route comparison analysis with real-time data enrichment
     */
    private String performMultiRouteAnalysisWithEnrichment(String importerIso2, List<String> originCountries, String hsCode, 
                                                         BigDecimal fobValue, BigDecimal quantity) {
        String basicAnalysis = performMultiRouteAnalysis(importerIso2, originCountries, hsCode, fobValue, quantity);
        
        if (externalDataService == null) {
            return basicAnalysis;
        }
        
        try {
            StringBuilder enrichedAnalysis = new StringBuilder(basicAnalysis);
            enrichedAnalysis.append("\n").append("=".repeat(50)).append("\n");
            enrichedAnalysis.append("REAL-TIME ROUTE INTELLIGENCE\n");
            enrichedAnalysis.append("=".repeat(50)).append("\n\n");
            
            // Get market data for each origin country
            List<CompletableFuture<ExternalDataService.MarketData>> marketFutures = originCountries.stream()
                .map(origin -> externalDataService.getMarketData(hsCode, origin))
                .collect(Collectors.toList());
            
            // Get news for the trade route
            CompletableFuture<List<ExternalDataService.NewsItem>> newsFuture = 
                externalDataService.getTradeNews("tariff " + hsCode, importerIso2, 5);
            
            // Wait for all data
            CompletableFuture.allOf(marketFutures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
            List<ExternalDataService.NewsItem> news = newsFuture.get(5, TimeUnit.SECONDS);
            
            // Add market intelligence by route
            enrichedAnalysis.append("Market Intelligence by Route:\n");
            for (int i = 0; i < originCountries.size() && i < marketFutures.size(); i++) {
                String origin = originCountries.get(i);
                ExternalDataService.MarketData marketData = marketFutures.get(i).get();
                
                enrichedAnalysis.append(String.format("%s → %s:\n", origin, importerIso2));
                if (marketData.getPrice() > 0) {
                    enrichedAnalysis.append("  • Market Price: $").append(marketData.getPrice()).append("\n");
                    enrichedAnalysis.append("  • Price Trend: ").append(marketData.getChange() > 0 ? "↑" : "↓")
                        .append(" ").append(marketData.getChange()).append("%\n");
                } else {
                    enrichedAnalysis.append("  • Market data not available\n");
                }
            }
            enrichedAnalysis.append("\n");
            
            // Add relevant trade news
            if (!news.isEmpty()) {
                enrichedAnalysis.append("Recent Trade Developments:\n");
                news.stream().limit(3).forEach(item -> {
                    enrichedAnalysis.append("• ").append(item.getTitle()).append("\n");
                    enrichedAnalysis.append("  Impact: Potential effects on trade costs and timing\n");
                });
                enrichedAnalysis.append("\n");
            }
            
            // Add strategic recommendations based on real-time data
            enrichedAnalysis.append("Strategic Recommendations:\n");
            enrichedAnalysis.append("• Monitor market price volatility for optimal timing\n");
            enrichedAnalysis.append("• Consider supply chain diversification across multiple routes\n");
            enrichedAnalysis.append("• Stay informed about regulatory changes affecting trade routes\n");
            
            return enrichedAnalysis.toString();
            
        } catch (Exception e) {
            logger.warn("Failed to enrich multi-route analysis with real-time data: {}", e.getMessage());
            return basicAnalysis + "\n\nNote: Real-time route intelligence temporarily unavailable.";
        }
    }
    
    /**
     * Perform total landed cost analysis
     */
    private String performTotalCostAnalysis(String importerIso2, String originIso2, String hsCode, 
                                          BigDecimal fobValue, BigDecimal quantity, BigDecimal weightKg) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Total Landed Cost Analysis\n");
        analysis.append("============================\n");
        analysis.append("Trade Route: ").append(originIso2).append(" → ").append(importerIso2).append("\n");
        analysis.append("HS Code: ").append(hsCode).append("\n\n");
        
        if (fobValue == null) {
            analysis.append("Note: FOB value not provided. Cost calculations will be shown as percentages.\n\n");
        }
        
        TariffRateLookupDto result = tariffRateService.getTariffRateWithAgreement(importerIso2, originIso2, hsCode);
        
        // Base tariff information
        analysis.append("Tariff Information:\n");
        if (result.tariffRateMfn() != null) {
            analysis.append("- MFN Rate: ").append(formatRate(result.tariffRateMfn())).append("\n");
        }
        if (result.tariffRatePref() != null) {
            analysis.append("- Preferential Rate: ").append(formatRate(result.tariffRatePref()));
            if (result.agreement() != null) {
                analysis.append(" (").append(result.agreement().getName()).append(")");
            }
            analysis.append("\n");
        }
        analysis.append("\n");
        
        // Cost breakdown
        analysis.append("Total Landed Cost Breakdown:\n");
        
        if (fobValue != null) {
            analysis.append("1. FOB Value: $").append(fobValue).append("\n");
            
            // Estimate shipping costs
            BigDecimal shippingCost = estimateShippingCost(originIso2, importerIso2, weightKg, fobValue);
            analysis.append("2. Estimated Shipping: $").append(shippingCost).append("\n");
            
            // Calculate insurance (typically 0.1-0.5% of FOB + shipping)
            BigDecimal insuranceCost = fobValue.add(shippingCost).multiply(new BigDecimal("0.003"));
            analysis.append("3. Estimated Insurance: $").append(insuranceCost.setScale(2, RoundingMode.HALF_UP)).append("\n");
            
            // CIF value
            BigDecimal cifValue = fobValue.add(shippingCost).add(insuranceCost);
            analysis.append("4. CIF Value: $").append(cifValue.setScale(2, RoundingMode.HALF_UP)).append("\n");
            
            // Duty calculation
            BigDecimal dutyRate = getEffectiveRate(result);
            BigDecimal dutyAmount = cifValue.multiply(dutyRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
            analysis.append("5. Duty (").append(dutyRate).append("%): $").append(dutyAmount.setScale(2, RoundingMode.HALF_UP)).append("\n");
            
            // Handling and broker fees (estimate 2-5% of CIF)
            BigDecimal handlingFees = cifValue.multiply(new BigDecimal("0.03"));
            analysis.append("6. Handling/Broker Fees: $").append(handlingFees.setScale(2, RoundingMode.HALF_UP)).append("\n");
            
            // Total landed cost
            BigDecimal totalLandedCost = cifValue.add(dutyAmount).add(handlingFees);
            analysis.append("\nTotal Landed Cost: $").append(totalLandedCost.setScale(2, RoundingMode.HALF_UP)).append("\n");
            
            // Cost per unit if quantity provided
            if (quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal costPerUnit = totalLandedCost.divide(quantity, 2, RoundingMode.HALF_UP);
                analysis.append("Cost per Unit: $").append(costPerUnit).append("\n");
            }
            
            // Markup analysis
            BigDecimal markupFromFOB = totalLandedCost.subtract(fobValue).divide(fobValue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            analysis.append("Total Markup from FOB: ").append(markupFromFOB.setScale(1, RoundingMode.HALF_UP)).append("%\n");
            
        } else {
            analysis.append("Provide FOB value for detailed cost calculations.\n");
            analysis.append("Estimated cost components as % of FOB:\n");
            analysis.append("- Shipping: 5-15% (varies by route and mode)\n");
            analysis.append("- Insurance: 0.1-0.5%\n");
            analysis.append("- Duty: ").append(getEffectiveRate(result)).append("%\n");
            analysis.append("- Handling/Fees: 2-5%\n");
            analysis.append("- Total markup: 10-25% typically\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Perform seasonal rate analysis
     */
    private String performSeasonalAnalysis(String importerIso2, String originIso2, String hsCode) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Seasonal Tariff Rate Analysis\n");
        analysis.append("===============================\n");
        analysis.append("Trade Route: ").append(originIso2).append(" → ").append(importerIso2).append("\n");
        analysis.append("HS Code: ").append(hsCode).append("\n\n");
        
        TariffRateLookupDto result = tariffRateService.getTariffRateWithAgreement(importerIso2, originIso2, hsCode);
        
        // Current rates
        analysis.append("Current Tariff Rates:\n");
        if (result.tariffRateMfn() != null) {
            analysis.append("- MFN Rate: ").append(formatRate(result.tariffRateMfn())).append("\n");
        }
        if (result.tariffRatePref() != null) {
            analysis.append("- Preferential Rate: ").append(formatRate(result.tariffRatePref())).append("\n");
        }
        analysis.append("\n");
        
        // Seasonal considerations (this would typically come from a database or external service)
        analysis.append("Seasonal Considerations:\n");
        analysis.append("Note: Seasonal rate variations are product-specific. Common patterns include:\n\n");
        
        // Analyze HS code for seasonal products
        String productCategory = analyzeProductCategory(hsCode);
        analysis.append("Product Category: ").append(productCategory).append("\n");
        
        switch (productCategory.toLowerCase()) {
            case "agricultural" -> {
                analysis.append("- Agricultural products often have seasonal quotas and rate variations\n");
                analysis.append("- Peak season: Rates may increase during local harvest seasons\n");
                analysis.append("- Off-season: Lower rates to encourage imports when domestic supply is low\n");
                analysis.append("- Recommendation: Import during off-peak seasons for better rates\n");
            }
            case "textiles" -> {
                analysis.append("- Textile imports may have seasonal quota allocations\n");
                analysis.append("- Fashion seasons affect demand and potentially rates\n");
                analysis.append("- End-of-year quota availability may offer opportunities\n");
            }
            case "industrial" -> {
                analysis.append("- Industrial goods typically have stable year-round rates\n");
                analysis.append("- Minimal seasonal variation expected\n");
                analysis.append("- Focus on trade agreement utilization for savings\n");
            }
            default -> {
                analysis.append("- Seasonal patterns vary by specific product\n");
                analysis.append("- Monitor for quota periods and special rate programs\n");
                analysis.append("- Consider timing imports around trade agreement anniversaries\n");
            }
        }
        
        // Timing recommendations
        analysis.append("\nOptimal Timing Recommendations:\n");
        LocalDate currentDate = LocalDate.now();
        Month currentMonth = currentDate.getMonth();
        
        analysis.append("- Current period: ").append(currentMonth).append(" ").append(currentDate.getYear()).append("\n");
        analysis.append("- Quarter-end considerations: Review quota utilization\n");
        analysis.append("- Year-end considerations: Quota availability and rollover policies\n");
        
        if (result.agreement() != null) {
            analysis.append("- Trade agreement: Monitor for anniversary dates and rate changes\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Perform duty drawback analysis
     */
    private String performDutyDrawbackAnalysis(String importerIso2, String originIso2, String hsCode, BigDecimal fobValue) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Duty Drawback Analysis\n");
        analysis.append("========================\n");
        analysis.append("Trade Route: ").append(originIso2).append(" → ").append(importerIso2).append("\n");
        analysis.append("HS Code: ").append(hsCode).append("\n\n");
        
        TariffRateLookupDto result = tariffRateService.getTariffRateWithAgreement(importerIso2, originIso2, hsCode);
        
        // Current duty information
        BigDecimal effectiveRate = getEffectiveRate(result);
        analysis.append("Current Duty Information:\n");
        analysis.append("- Effective Rate: ").append(effectiveRate).append("%\n");
        
        if (fobValue != null) {
            BigDecimal dutyAmount = fobValue.multiply(effectiveRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
            analysis.append("- Duty Amount: $").append(dutyAmount.setScale(2, RoundingMode.HALF_UP)).append("\n");
        }
        analysis.append("\n");
        
        // Drawback eligibility analysis
        analysis.append("Duty Drawback Eligibility:\n");
        
        if (importerIso2.equals("US")) {
            analysis.append("US Duty Drawback Programs:\n");
            analysis.append("1. Manufacturing Drawback (19 USC 1313(a)):\n");
            analysis.append("   - Up to 99% of duties paid on imported materials\n");
            analysis.append("   - Used in manufacturing exported products\n");
            analysis.append("   - 5-year claim period\n\n");
            
            analysis.append("2. Same Condition Drawback (19 USC 1313(j)):\n");
            analysis.append("   - Up to 99% of duties paid\n");
            analysis.append("   - Imported goods exported in same condition\n");
            analysis.append("   - 5-year claim period\n\n");
            
            analysis.append("3. Rejected Merchandise Drawback (19 USC 1313(c)):\n");
            analysis.append("   - Up to 99% of duties paid\n");
            analysis.append("   - Goods rejected and exported within 3 years\n\n");
            
            if (fobValue != null) {
                BigDecimal potentialDrawback = fobValue.multiply(effectiveRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
                    .multiply(new BigDecimal("0.99"));
                analysis.append("Potential Drawback Amount: $").append(potentialDrawback.setScale(2, RoundingMode.HALF_UP)).append("\n");
            }
        } else {
            analysis.append("Drawback programs vary by country. Common features:\n");
            analysis.append("- Export processing zones may offer duty exemptions\n");
            analysis.append("- Manufacturing for export programs\n");
            analysis.append("- Temporary importation procedures\n");
            analysis.append("- Consult local customs authorities for specific programs\n");
        }
        
        analysis.append("\nRecommendations:\n");
        if (effectiveRate.compareTo(new BigDecimal("5")) > 0) {
            analysis.append("- High duty rate makes drawback programs attractive\n");
            analysis.append("- Consider manufacturing or re-export opportunities\n");
        } else {
            analysis.append("- Low duty rate may not justify drawback complexity\n");
            analysis.append("- Focus on preferential rate utilization instead\n");
        }
        
        analysis.append("- Maintain detailed records for drawback claims\n");
        analysis.append("- Consider bonded warehouse storage options\n");
        analysis.append("- Evaluate foreign trade zone benefits\n");
        
        return analysis.toString();
    }
    
    /**
     * Helper class for route analysis
     */
    private static class RouteAnalysis {
        final String originIso2;
        final TariffRateLookupDto result;
        final BigDecimal totalCost;
        final String error;
        
        RouteAnalysis(String originIso2, TariffRateLookupDto result, BigDecimal totalCost, String error) {
            this.originIso2 = originIso2;
            this.result = result;
            this.totalCost = totalCost;
            this.error = error;
        }
    }
    
    /**
     * Analyze a single route
     */
    private RouteAnalysis analyzeRoute(String originIso2, TariffRateLookupDto result, BigDecimal fobValue, BigDecimal quantity) {
        if (fobValue == null) {
            return new RouteAnalysis(originIso2, result, null, null);
        }
        
        try {
            // Estimate total cost
            BigDecimal shippingCost = estimateShippingCost(originIso2, "US", null, fobValue); // Default to US for estimation
            BigDecimal insuranceCost = fobValue.multiply(new BigDecimal("0.003"));
            BigDecimal cifValue = fobValue.add(shippingCost).add(insuranceCost);
            BigDecimal dutyRate = getEffectiveRate(result);
            BigDecimal dutyAmount = cifValue.multiply(dutyRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
            BigDecimal handlingFees = cifValue.multiply(new BigDecimal("0.03"));
            BigDecimal totalCost = cifValue.add(dutyAmount).add(handlingFees);
            
            return new RouteAnalysis(originIso2, result, totalCost, null);
        } catch (Exception e) {
            return new RouteAnalysis(originIso2, result, null, "Cost calculation failed: " + e.getMessage());
        }
    }
    
    /**
     * Get effective tariff rate (preferential if available, otherwise MFN)
     */
    private BigDecimal getEffectiveRate(TariffRateLookupDto result) {
        if (result.tariffRatePref() != null && result.tariffRatePref().getAdValoremRate() != null) {
            return result.tariffRatePref().getAdValoremRate();
        } else if (result.tariffRateMfn() != null && result.tariffRateMfn().getAdValoremRate() != null) {
            return result.tariffRateMfn().getAdValoremRate();
        }
        return BigDecimal.ZERO;
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
    
    /**
     * Estimate shipping cost based on route and value
     */
    private BigDecimal estimateShippingCost(String originIso2, String destinationIso2, BigDecimal weightKg, BigDecimal fobValue) {
        // Simplified shipping cost estimation
        // In a real implementation, this would integrate with shipping APIs
        
        Map<String, BigDecimal> shippingMultipliers = Map.of(
            "CN", new BigDecimal("0.08"), // China
            "MX", new BigDecimal("0.05"), // Mexico
            "CA", new BigDecimal("0.03"), // Canada
            "DE", new BigDecimal("0.10"), // Germany
            "JP", new BigDecimal("0.12"), // Japan
            "KR", new BigDecimal("0.10"), // South Korea
            "IN", new BigDecimal("0.09"), // India
            "VN", new BigDecimal("0.07")  // Vietnam
        );
        
        BigDecimal multiplier = shippingMultipliers.getOrDefault(originIso2, new BigDecimal("0.08"));
        BigDecimal baseCost = fobValue.multiply(multiplier);
        
        // Minimum shipping cost
        BigDecimal minimumCost = new BigDecimal("100");
        return baseCost.max(minimumCost);
    }
    
    /**
     * Calculate savings between two costs
     */
    private BigDecimal calculateSavings(BigDecimal higherCost, BigDecimal lowerCost) {
        if (higherCost == null || lowerCost == null) {
            return BigDecimal.ZERO;
        }
        return higherCost.subtract(lowerCost).max(BigDecimal.ZERO);
    }
    
    /**
     * Analyze product category based on HS code
     */
    private String analyzeProductCategory(String hsCode) {
        if (hsCode.length() < 2) return "Unknown";
        
        String chapter = hsCode.substring(0, 2);
        return switch (chapter) {
            case "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15" -> "Agricultural";
            case "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63" -> "Textiles";
            case "84", "85" -> "Machinery";
            case "87" -> "Vehicles";
            case "72", "73", "74", "75", "76", "78", "79", "80", "81", "82", "83" -> "Metals";
            case "39", "40" -> "Plastics/Rubber";
            default -> "Industrial";
        };
    }
    
    /**
     * Format the tariff lookup result for LLM consumption (legacy method)
     */
    private String formatTariffResult(TariffRateLookupDto result, String importerIso2, String originIso2, String hsCode) {
        StringBuilder formatted = new StringBuilder();
        
        formatted.append("Tariff Rate Lookup Results:\n");
        formatted.append("Trade Route: ").append(originIso2).append(" → ").append(importerIso2).append("\n");
        formatted.append("HS Code: ").append(hsCode).append("\n\n");
        
        // MFN Rate
        if (result.tariffRateMfn() != null) {
            formatted.append("Most Favored Nation (MFN) Rate:\n");
            formatted.append("- Rate Type: ").append(result.tariffRateMfn().getRateType()).append("\n");
            formatted.append("- Basis: ").append(result.tariffRateMfn().getBasis()).append("\n");
            
            if (result.tariffRateMfn().getAdValoremRate() != null) {
                formatted.append("- Ad Valorem Rate: ").append(result.tariffRateMfn().getAdValoremRate()).append("%\n");
            }
            
            if (result.tariffRateMfn().getSpecificAmount() != null) {
                formatted.append("- Specific Amount: ").append(result.tariffRateMfn().getSpecificAmount());
                if (result.tariffRateMfn().getSpecificUnit() != null) {
                    formatted.append(" ").append(result.tariffRateMfn().getSpecificUnit());
                }
                formatted.append("\n");
            }
        } else {
            formatted.append("Most Favored Nation (MFN) Rate: Not available\n");
        }
        
        formatted.append("\n");
        
        // Preferential Rate
        if (result.tariffRatePref() != null) {
            formatted.append("Preferential Rate:\n");
            formatted.append("- Rate Type: ").append(result.tariffRatePref().getRateType()).append("\n");
            formatted.append("- Basis: ").append(result.tariffRatePref().getBasis()).append("\n");
            
            if (result.tariffRatePref().getAdValoremRate() != null) {
                formatted.append("- Ad Valorem Rate: ").append(result.tariffRatePref().getAdValoremRate()).append("%\n");
            }
            
            if (result.tariffRatePref().getSpecificAmount() != null) {
                formatted.append("- Specific Amount: ").append(result.tariffRatePref().getSpecificAmount());
                if (result.tariffRatePref().getSpecificUnit() != null) {
                    formatted.append(" ").append(result.tariffRatePref().getSpecificUnit());
                }
                formatted.append("\n");
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
    
    /**
     * Perform total landed cost analysis with real-time data enrichment
     */
    private String performTotalCostAnalysisWithEnrichment(String importerIso2, String originIso2, String hsCode, 
                                                        BigDecimal fobValue, BigDecimal quantity, BigDecimal weightKg) {
        String basicAnalysis = performTotalCostAnalysis(importerIso2, originIso2, hsCode, fobValue, quantity, weightKg);
        
        if (externalDataService == null) {
            return basicAnalysis;
        }
        
        try {
            StringBuilder enrichedAnalysis = new StringBuilder(basicAnalysis);
            enrichedAnalysis.append("\n").append("=".repeat(40)).append("\n");
            enrichedAnalysis.append("REAL-TIME COST INTELLIGENCE\n");
            enrichedAnalysis.append("=".repeat(40)).append("\n\n");
            
            // Get market data for cost context
            CompletableFuture<ExternalDataService.MarketData> marketFuture = 
                externalDataService.getMarketData(hsCode, originIso2);
            
            ExternalDataService.MarketData marketData = marketFuture.get(5, TimeUnit.SECONDS);
            
            if (marketData.getPrice() > 0) {
                enrichedAnalysis.append("Market Price Context:\n");
                enrichedAnalysis.append("• Current Market Price: $").append(marketData.getPrice()).append("\n");
                enrichedAnalysis.append("• Price Volatility: ").append(Math.abs(marketData.getChange())).append("% recent change\n");
                
                if (fobValue != null) {
                    double priceRatio = fobValue.doubleValue() / marketData.getPrice();
                    if (priceRatio < 0.9) {
                        enrichedAnalysis.append("• Price Advantage: Your FOB price is below market average\n");
                    } else if (priceRatio > 1.1) {
                        enrichedAnalysis.append("• Price Alert: Your FOB price is above market average\n");
                    }
                }
                enrichedAnalysis.append("\n");
            }
            
            // Add cost optimization recommendations
            enrichedAnalysis.append("Cost Optimization Recommendations:\n");
            enrichedAnalysis.append("• Monitor market price trends for procurement timing\n");
            enrichedAnalysis.append("• Consider forward contracts to hedge against price volatility\n");
            enrichedAnalysis.append("• Evaluate bulk shipping discounts for larger quantities\n");
            
            return enrichedAnalysis.toString();
            
        } catch (Exception e) {
            logger.warn("Failed to enrich cost analysis with real-time data: {}", e.getMessage());
            return basicAnalysis + "\n\nNote: Real-time cost intelligence temporarily unavailable.";
        }
    }
    
    /**
     * Perform seasonal analysis with real-time data enrichment
     */
    private String performSeasonalAnalysisWithEnrichment(String importerIso2, String originIso2, String hsCode) {
        String basicAnalysis = performSeasonalAnalysis(importerIso2, originIso2, hsCode);
        
        if (externalDataService == null) {
            return basicAnalysis;
        }
        
        try {
            StringBuilder enrichedAnalysis = new StringBuilder(basicAnalysis);
            enrichedAnalysis.append("\n").append("=".repeat(40)).append("\n");
            enrichedAnalysis.append("REAL-TIME SEASONAL INTELLIGENCE\n");
            enrichedAnalysis.append("=".repeat(40)).append("\n\n");
            
            // Get recent regulatory updates
            CompletableFuture<List<ExternalDataService.RegulatoryUpdate>> regulatoryFuture = 
                externalDataService.getRegulatoryUpdates(importerIso2, "seasonal");
            
            List<ExternalDataService.RegulatoryUpdate> updates = regulatoryFuture.get(5, TimeUnit.SECONDS);
            
            if (!updates.isEmpty()) {
                enrichedAnalysis.append("Recent Seasonal Regulatory Changes:\n");
                updates.stream().limit(2).forEach(update -> {
                    enrichedAnalysis.append("• ").append(update.getTitle()).append("\n");
                    enrichedAnalysis.append("  Effective: ").append(update.getEffectiveDate().toString().substring(0, 10)).append("\n");
                });
                enrichedAnalysis.append("\n");
            }
            
            // Add current market timing advice
            enrichedAnalysis.append("Current Market Timing:\n");
            enrichedAnalysis.append("• Monitor quota utilization rates for optimal timing\n");
            enrichedAnalysis.append("• Consider seasonal demand patterns in destination market\n");
            enrichedAnalysis.append("• Track competitor import patterns for strategic advantage\n");
            
            return enrichedAnalysis.toString();
            
        } catch (Exception e) {
            logger.warn("Failed to enrich seasonal analysis with real-time data: {}", e.getMessage());
            return basicAnalysis + "\n\nNote: Real-time seasonal intelligence temporarily unavailable.";
        }
    }
    
    /**
     * Perform duty drawback analysis with real-time data enrichment
     */
    private String performDutyDrawbackAnalysisWithEnrichment(String importerIso2, String originIso2, String hsCode, BigDecimal fobValue) {
        String basicAnalysis = performDutyDrawbackAnalysis(importerIso2, originIso2, hsCode, fobValue);
        
        if (externalDataService == null) {
            return basicAnalysis;
        }
        
        try {
            StringBuilder enrichedAnalysis = new StringBuilder(basicAnalysis);
            enrichedAnalysis.append("\n").append("=".repeat(40)).append("\n");
            enrichedAnalysis.append("REAL-TIME DRAWBACK INTELLIGENCE\n");
            enrichedAnalysis.append("=".repeat(40)).append("\n\n");
            
            // Get regulatory updates related to drawback programs
            CompletableFuture<List<ExternalDataService.RegulatoryUpdate>> regulatoryFuture = 
                externalDataService.getRegulatoryUpdates(importerIso2, "drawback");
            
            List<ExternalDataService.RegulatoryUpdate> updates = regulatoryFuture.get(5, TimeUnit.SECONDS);
            
            if (!updates.isEmpty()) {
                enrichedAnalysis.append("Recent Drawback Program Updates:\n");
                updates.stream().limit(2).forEach(update -> {
                    enrichedAnalysis.append("• ").append(update.getTitle()).append("\n");
                    enrichedAnalysis.append("  Status: ").append(update.getType()).append("\n");
                });
                enrichedAnalysis.append("\n");
            }
            
            // Add current program status and recommendations
            enrichedAnalysis.append("Current Program Intelligence:\n");
            enrichedAnalysis.append("• Verify current drawback program eligibility requirements\n");
            enrichedAnalysis.append("• Monitor processing times for drawback claims\n");
            enrichedAnalysis.append("• Consider automated drawback filing systems for efficiency\n");
            
            return enrichedAnalysis.toString();
            
        } catch (Exception e) {
            logger.warn("Failed to enrich drawback analysis with real-time data: {}", e.getMessage());
            return basicAnalysis + "\n\nNote: Real-time drawback intelligence temporarily unavailable.";
        }
    }
}