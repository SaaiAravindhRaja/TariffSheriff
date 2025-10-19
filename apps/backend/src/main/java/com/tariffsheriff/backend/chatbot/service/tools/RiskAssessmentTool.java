package com.tariffsheriff.backend.chatbot.service.tools;

import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.service.AgreementService;
import com.tariffsheriff.backend.tariff.service.TariffRateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool for comprehensive trade risk assessment including political and economic risk,
 * supply chain vulnerability analysis, currency and financial risk evaluation,
 * trade dispute impact analysis, and force majeure planning
 */
@Component
public class RiskAssessmentTool implements ChatbotTool {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskAssessmentTool.class);
    
    private final AgreementService agreementService;
    private final TariffRateService tariffRateService;
    
    public RiskAssessmentTool(AgreementService agreementService, TariffRateService tariffRateService) {
        this.agreementService = agreementService;
        this.tariffRateService = tariffRateService;
    }
    
    @Override
    public String getName() {
        return "assessTradeRisk";
    }
    
    @Override
    public ToolDefinition getDefinition() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // Trade route parameters
        Map<String, Object> importerParam = new HashMap<>();
        importerParam.put("type", "string");
        importerParam.put("description", "ISO2 country code of the importing country (e.g., 'US', 'CA', 'JP')");
        properties.put("importerIso2", importerParam);
        
        Map<String, Object> originParam = new HashMap<>();
        originParam.put("type", "array");
        originParam.put("description", "Array of origin country ISO2 codes for risk assessment (e.g., ['CN', 'MX', 'DE'])");
        Map<String, Object> originItems = new HashMap<>();
        originItems.put("type", "string");
        originParam.put("items", originItems);
        properties.put("originCountries", originParam);
        
        // Product and trade information
        Map<String, Object> hsCodeParam = new HashMap<>();
        hsCodeParam.put("type", "string");
        hsCodeParam.put("description", "HS code for product-specific risk analysis (optional)");
        properties.put("hsCode", hsCodeParam);
        
        Map<String, Object> productParam = new HashMap<>();
        productParam.put("type", "string");
        productParam.put("description", "Product description for risk context (optional)");
        properties.put("productDescription", productParam);
        
        // Risk assessment parameters
        Map<String, Object> analysisParam = new HashMap<>();
        analysisParam.put("type", "string");
        analysisParam.put("description", "Type of risk analysis: 'comprehensive' (default), 'political', 'economic', 'supply-chain', 'financial', 'force-majeure'");
        analysisParam.put("enum", Arrays.asList("comprehensive", "political", "economic", "supply-chain", "financial", "force-majeure"));
        properties.put("riskType", analysisParam);
        
        // Trade value and volume for impact assessment
        Map<String, Object> valueParam = new HashMap<>();
        valueParam.put("type", "number");
        valueParam.put("description", "Annual trade value in USD for risk impact calculation (optional)");
        properties.put("tradeValue", valueParam);
        
        Map<String, Object> volumeParam = new HashMap<>();
        volumeParam.put("type", "number");
        volumeParam.put("description", "Annual trade volume in units for supply chain risk assessment (optional)");
        properties.put("tradeVolume", volumeParam);
        
        // Time horizon for risk assessment
        Map<String, Object> horizonParam = new HashMap<>();
        horizonParam.put("type", "string");
        horizonParam.put("description", "Risk assessment time horizon: 'short-term' (1 year), 'medium-term' (3 years), 'long-term' (5+ years)");
        horizonParam.put("enum", Arrays.asList("short-term", "medium-term", "long-term"));
        properties.put("timeHorizon", horizonParam);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"importerIso2", "originCountries"});
        
        return new ToolDefinition(
            getName(),
            "Assess trade risks including political, economic, supply chain, financial, and force majeure risks with mitigation strategies. " +
            "USE WHEN: User asks about trade risks, supply chain vulnerabilities, political risks, economic stability, currency risks, or contingency planning. " +
            "REQUIRES: Importer country (ISO2) and array of origin countries (ISO2). Optional: HS code, product description, trade value, trade volume, time horizon. " +
            "RETURNS: Risk scores by category (political, economic, supply chain, financial), vulnerability assessment, impact analysis, mitigation strategies, and contingency recommendations. " +
            "EXAMPLES: 'What are the risks of importing from China?', 'Assess supply chain vulnerabilities for my trade route', 'Analyze political risks for long-term trade partnership', 'What are currency risks for importing from Europe?'. " +
            "SUPPORTS: Comprehensive risk assessment, political risk analysis, economic risk evaluation, supply chain vulnerability analysis, financial risk assessment, and force majeure planning.",
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
            String productDescription = toolCall.getStringArgument("productDescription");
            String riskType = toolCall.getStringArgument("riskType", "comprehensive");
            String timeHorizon = toolCall.getStringArgument("timeHorizon", "medium-term");
            BigDecimal tradeValue = toolCall.getBigDecimalArgument("tradeValue");
            BigDecimal tradeVolume = toolCall.getBigDecimalArgument("tradeVolume");
            
            // Handle origin countries
            List<String> originCountries = extractOriginCountries(toolCall);
            
            // Validate required parameters
            if (importerIso2 == null || importerIso2.trim().isEmpty()) {
                return ToolResult.error(getName(), "Missing required parameter: importerIso2");
            }
            if (originCountries.isEmpty()) {
                return ToolResult.error(getName(), "Missing required parameter: originCountries");
            }
            
            // Normalize parameters
            importerIso2 = importerIso2.trim().toUpperCase();
            riskType = riskType.toLowerCase();
            timeHorizon = timeHorizon.toLowerCase();
            
            // Validate formats
            if (importerIso2.length() != 2 || !importerIso2.matches("[A-Z]{2}")) {
                return ToolResult.error(getName(), "Invalid importerIso2 format. Must be 2-character ISO country code");
            }
            
            for (String origin : originCountries) {
                if (origin.length() != 2 || !origin.matches("[A-Z]{2}")) {
                    return ToolResult.error(getName(), "Invalid origin country format: " + origin);
                }
            }
            
            logger.info("Executing {} risk assessment for trade routes: {} -> {}, horizon: {}", 
                       riskType, originCountries, importerIso2, timeHorizon);
            
            // Execute risk assessment based on type
            String formattedResult = switch (riskType) {
                case "political" -> performPoliticalRiskAssessment(importerIso2, originCountries, timeHorizon, tradeValue);
                case "economic" -> performEconomicRiskAssessment(importerIso2, originCountries, timeHorizon, tradeValue);
                case "supply-chain" -> performSupplyChainRiskAssessment(importerIso2, originCountries, hsCode, tradeVolume);
                case "financial" -> performFinancialRiskAssessment(importerIso2, originCountries, timeHorizon, tradeValue);
                case "force-majeure" -> performForceMajeureAssessment(importerIso2, originCountries, hsCode, productDescription);
                default -> performComprehensiveRiskAssessment(importerIso2, originCountries, hsCode, productDescription, 
                                                            timeHorizon, tradeValue, tradeVolume);
            };
            
            ToolResult toolResult = ToolResult.success(getName(), formattedResult);
            toolResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            logger.info("Completed {} risk assessment in {}ms", riskType, toolResult.getExecutionTimeMs());
            
            return toolResult;
            
        } catch (Exception e) {
            logger.error("Error executing trade risk assessment tool for importer: {}, origins: {}", 
                    toolCall.getStringArgument("importerIso2"), 
                    toolCall.getArgument("originCountries"), e);
            
            String userMessage = "I had trouble assessing trade risks. ";
            
            // Provide helpful guidance
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("database")) {
                userMessage += "There's a problem connecting to the risk assessment data. Please try again in a moment.";
            } else {
                userMessage += "Please try:\n" +
                        "• Verifying all country codes are correct (2-letter codes like 'US', 'CN', 'MX')\n" +
                        "• Providing at least one origin country\n" +
                        "• Simplifying your risk assessment question\n" +
                        "• Asking about specific risk types (political, economic, supply-chain, financial)";
            }
            
            ToolResult errorResult = ToolResult.error(getName(), userMessage);
            errorResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return errorResult;
        }
    }
    
    /**
     * Extract origin countries from tool call parameters
     */
    private List<String> extractOriginCountries(ToolCall toolCall) {
        List<String> originCountries = new ArrayList<>();
        
        Object originCountriesParam = toolCall.getArgument("originCountries");
        if (originCountriesParam instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String country) {
                    originCountries.add(country.trim().toUpperCase());
                }
            }
        }
        
        return originCountries;
    }
    
    /**
     * Perform comprehensive risk assessment
     */
    private String performComprehensiveRiskAssessment(String importerIso2, List<String> originCountries, 
                                                    String hsCode, String productDescription, String timeHorizon, 
                                                    BigDecimal tradeValue, BigDecimal tradeVolume) {
        StringBuilder assessment = new StringBuilder();
        assessment.append("Comprehensive Trade Risk Assessment\n");
        assessment.append("====================================\n");
        assessment.append("Destination: ").append(getCountryName(importerIso2)).append(" (").append(importerIso2).append(")\n");
        assessment.append("Origin Countries: ").append(originCountries.stream()
            .map(this::getCountryName).collect(Collectors.joining(", "))).append("\n");
        
        if (hsCode != null) {
            assessment.append("HS Code: ").append(hsCode).append("\n");
        }
        if (productDescription != null) {
            assessment.append("Product: ").append(productDescription).append("\n");
        }
        if (tradeValue != null) {
            assessment.append("Annual Trade Value: $").append(tradeValue).append("\n");
        }
        if (tradeVolume != null) {
            assessment.append("Annual Trade Volume: ").append(tradeVolume).append(" units\n");
        }
        
        assessment.append("Time Horizon: ").append(getTimeHorizonDescription(timeHorizon)).append("\n");
        assessment.append("Assessment Date: ").append(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n\n");
        
        // Overall risk summary
        assessment.append("EXECUTIVE SUMMARY\n");
        assessment.append("==================\n");
        RiskSummary riskSummary = calculateOverallRisk(importerIso2, originCountries, hsCode, timeHorizon, tradeValue);
        assessment.append("Overall Risk Level: ").append(riskSummary.overallRisk).append(" (").append(riskSummary.riskScore).append("/100)\n");
        assessment.append("Primary Risk Factors: ").append(String.join(", ", riskSummary.primaryRisks)).append("\n");
        assessment.append("Risk Trend: ").append(riskSummary.riskTrend).append("\n\n");
        
        // Detailed risk analysis by category
        assessment.append("DETAILED RISK ANALYSIS\n");
        assessment.append("=======================\n\n");
        
        // 1. Political Risk
        assessment.append("1. POLITICAL RISK\n");
        assessment.append("------------------\n");
        assessment.append(analyzePoliticalRisk(importerIso2, originCountries, timeHorizon)).append("\n\n");
        
        // 2. Economic Risk
        assessment.append("2. ECONOMIC RISK\n");
        assessment.append("-----------------\n");
        assessment.append(analyzeEconomicRisk(importerIso2, originCountries, timeHorizon)).append("\n\n");
        
        // 3. Supply Chain Risk
        assessment.append("3. SUPPLY CHAIN RISK\n");
        assessment.append("---------------------\n");
        assessment.append(analyzeSupplyChainRisk(importerIso2, originCountries, hsCode, tradeVolume)).append("\n\n");
        
        // 4. Financial Risk
        assessment.append("4. FINANCIAL RISK\n");
        assessment.append("------------------\n");
        assessment.append(analyzeFinancialRisk(importerIso2, originCountries, timeHorizon, tradeValue)).append("\n\n");
        
        // 5. Force Majeure Risk
        assessment.append("5. FORCE MAJEURE RISK\n");
        assessment.append("----------------------\n");
        assessment.append(analyzeForceMajeureRisk(importerIso2, originCountries, hsCode)).append("\n\n");
        
        // Risk mitigation strategies
        assessment.append("RISK MITIGATION STRATEGIES\n");
        assessment.append("===========================\n");
        assessment.append(getRiskMitigationStrategies(riskSummary, importerIso2, originCountries, tradeValue)).append("\n\n");
        
        // Contingency planning
        assessment.append("CONTINGENCY PLANNING\n");
        assessment.append("=====================\n");
        assessment.append(getContingencyPlanning(riskSummary, importerIso2, originCountries, hsCode)).append("\n\n");
        
        // Monitoring and review
        assessment.append("MONITORING AND REVIEW\n");
        assessment.append("======================\n");
        assessment.append(getMonitoringRecommendations(riskSummary, timeHorizon)).append("\n");
        
        return assessment.toString();
    }
    
    /**
     * Perform political risk assessment
     */
    private String performPoliticalRiskAssessment(String importerIso2, List<String> originCountries, 
                                                String timeHorizon, BigDecimal tradeValue) {
        StringBuilder assessment = new StringBuilder();
        assessment.append("Political Risk Assessment\n");
        assessment.append("==========================\n");
        assessment.append("Trade Route: ").append(String.join(", ", originCountries)).append(" → ").append(importerIso2).append("\n");
        assessment.append("Time Horizon: ").append(getTimeHorizonDescription(timeHorizon)).append("\n\n");
        
        assessment.append(analyzePoliticalRisk(importerIso2, originCountries, timeHorizon));
        
        // Add political risk mitigation
        assessment.append("\n\nPolitical Risk Mitigation:\n");
        assessment.append("===========================\n");
        assessment.append(getPoliticalRiskMitigation(importerIso2, originCountries));
        
        return assessment.toString();
    }
    
    /**
     * Perform economic risk assessment
     */
    private String performEconomicRiskAssessment(String importerIso2, List<String> originCountries, 
                                                String timeHorizon, BigDecimal tradeValue) {
        StringBuilder assessment = new StringBuilder();
        assessment.append("Economic Risk Assessment\n");
        assessment.append("=========================\n");
        assessment.append("Trade Route: ").append(String.join(", ", originCountries)).append(" → ").append(importerIso2).append("\n");
        assessment.append("Time Horizon: ").append(getTimeHorizonDescription(timeHorizon)).append("\n\n");
        
        assessment.append(analyzeEconomicRisk(importerIso2, originCountries, timeHorizon));
        
        // Add economic risk mitigation
        assessment.append("\n\nEconomic Risk Mitigation:\n");
        assessment.append("==========================\n");
        assessment.append(getEconomicRiskMitigation(importerIso2, originCountries, tradeValue));
        
        return assessment.toString();
    }
    
    /**
     * Perform supply chain risk assessment
     */
    private String performSupplyChainRiskAssessment(String importerIso2, List<String> originCountries, 
                                                  String hsCode, BigDecimal tradeVolume) {
        StringBuilder assessment = new StringBuilder();
        assessment.append("Supply Chain Risk Assessment\n");
        assessment.append("=============================\n");
        assessment.append("Trade Route: ").append(String.join(", ", originCountries)).append(" → ").append(importerIso2).append("\n");
        if (hsCode != null) {
            assessment.append("HS Code: ").append(hsCode).append("\n");
        }
        assessment.append("\n");
        
        assessment.append(analyzeSupplyChainRisk(importerIso2, originCountries, hsCode, tradeVolume));
        
        // Add supply chain risk mitigation
        assessment.append("\n\nSupply Chain Risk Mitigation:\n");
        assessment.append("==============================\n");
        assessment.append(getSupplyChainRiskMitigation(importerIso2, originCountries, hsCode));
        
        return assessment.toString();
    }
    
    /**
     * Perform financial risk assessment
     */
    private String performFinancialRiskAssessment(String importerIso2, List<String> originCountries, 
                                                String timeHorizon, BigDecimal tradeValue) {
        StringBuilder assessment = new StringBuilder();
        assessment.append("Financial Risk Assessment\n");
        assessment.append("==========================\n");
        assessment.append("Trade Route: ").append(String.join(", ", originCountries)).append(" → ").append(importerIso2).append("\n");
        assessment.append("Time Horizon: ").append(getTimeHorizonDescription(timeHorizon)).append("\n\n");
        
        assessment.append(analyzeFinancialRisk(importerIso2, originCountries, timeHorizon, tradeValue));
        
        // Add financial risk mitigation
        assessment.append("\n\nFinancial Risk Mitigation:\n");
        assessment.append("===========================\n");
        assessment.append(getFinancialRiskMitigation(importerIso2, originCountries, tradeValue));
        
        return assessment.toString();
    }
    
    /**
     * Perform force majeure assessment
     */
    private String performForceMajeureAssessment(String importerIso2, List<String> originCountries, 
                                                String hsCode, String productDescription) {
        StringBuilder assessment = new StringBuilder();
        assessment.append("Force Majeure Risk Assessment\n");
        assessment.append("==============================\n");
        assessment.append("Trade Route: ").append(String.join(", ", originCountries)).append(" → ").append(importerIso2).append("\n");
        if (productDescription != null) {
            assessment.append("Product: ").append(productDescription).append("\n");
        }
        assessment.append("\n");
        
        assessment.append(analyzeForceMajeureRisk(importerIso2, originCountries, hsCode));
        
        // Add force majeure planning
        assessment.append("\n\nForce Majeure Planning:\n");
        assessment.append("========================\n");
        assessment.append(getForceMajeurePlanning(importerIso2, originCountries, hsCode));
        
        return assessment.toString();
    }
    
    // Risk analysis methods
    
    /**
     * Calculate overall risk summary
     */
    private RiskSummary calculateOverallRisk(String importerIso2, List<String> originCountries, 
                                           String hsCode, String timeHorizon, BigDecimal tradeValue) {
        int totalRiskScore = 0;
        List<String> primaryRisks = new ArrayList<>();
        
        // Calculate individual risk scores
        int politicalRisk = calculatePoliticalRiskScore(importerIso2, originCountries);
        int economicRisk = calculateEconomicRiskScore(importerIso2, originCountries);
        int supplyChainRisk = calculateSupplyChainRiskScore(importerIso2, originCountries, hsCode);
        int financialRisk = calculateFinancialRiskScore(importerIso2, originCountries, tradeValue);
        int forceMajeureRisk = calculateForceMajeureRiskScore(importerIso2, originCountries);
        
        // Weight the risks (political and economic have higher weights)
        totalRiskScore = (int) (politicalRisk * 0.25 + economicRisk * 0.25 + supplyChainRisk * 0.2 + 
                               financialRisk * 0.15 + forceMajeureRisk * 0.15);
        
        // Identify primary risks (scores above 60)
        if (politicalRisk >= 60) primaryRisks.add("Political instability");
        if (economicRisk >= 60) primaryRisks.add("Economic volatility");
        if (supplyChainRisk >= 60) primaryRisks.add("Supply chain disruption");
        if (financialRisk >= 60) primaryRisks.add("Financial/currency risk");
        if (forceMajeureRisk >= 60) primaryRisks.add("Force majeure events");
        
        if (primaryRisks.isEmpty()) {
            primaryRisks.add("Moderate operational risks");
        }
        
        String overallRisk = getRiskLevel(totalRiskScore);
        String riskTrend = getRiskTrend(timeHorizon, totalRiskScore);
        
        return new RiskSummary(overallRisk, totalRiskScore, primaryRisks, riskTrend);
    }
    
    /**
     * Analyze political risk
     */
    private String analyzePoliticalRisk(String importerIso2, List<String> originCountries, String timeHorizon) {
        StringBuilder analysis = new StringBuilder();
        
        analysis.append("Political Risk Factors:\n\n");
        
        for (String origin : originCountries) {
            PoliticalRiskProfile profile = getPoliticalRiskProfile(origin);
            analysis.append("Country: ").append(getCountryName(origin)).append(" (").append(origin).append(")\n");
            analysis.append("- Political Stability: ").append(profile.stability).append("\n");
            analysis.append("- Government Effectiveness: ").append(profile.effectiveness).append("\n");
            analysis.append("- Regulatory Quality: ").append(profile.regulatoryQuality).append("\n");
            analysis.append("- Trade Relations with ").append(importerIso2).append(": ").append(profile.tradeRelations).append("\n");
            analysis.append("- Risk Level: ").append(profile.riskLevel).append("\n\n");
        }
        
        // Bilateral relationship analysis
        analysis.append("Bilateral Trade Relationship Analysis:\n");
        for (String origin : originCountries) {
            String relationship = analyzeBilateralRelationship(importerIso2, origin);
            analysis.append("- ").append(importerIso2).append(" ↔ ").append(origin).append(": ").append(relationship).append("\n");
        }
        analysis.append("\n");
        
        // Trade agreement stability
        analysis.append("Trade Agreement Stability:\n");
        List<Agreement> agreements = agreementService.getAgreementsByCountry(importerIso2);
        for (String origin : originCountries) {
            boolean hasAgreement = agreements.stream()
                .anyMatch(agreement -> agreement.getName().toUpperCase().contains(origin));
            
            if (hasAgreement) {
                analysis.append("- ").append(origin).append(": Protected by trade agreement\n");
            } else {
                analysis.append("- ").append(origin).append(": Vulnerable to policy changes\n");
            }
        }
        
        return analysis.toString();
    }
    
    /**
     * Analyze economic risk
     */
    private String analyzeEconomicRisk(String importerIso2, List<String> originCountries, String timeHorizon) {
        StringBuilder analysis = new StringBuilder();
        
        analysis.append("Economic Risk Assessment:\n\n");
        
        // Destination market economic risk
        EconomicRiskProfile importerProfile = getEconomicRiskProfile(importerIso2);
        analysis.append("Destination Market (").append(getCountryName(importerIso2)).append("):\n");
        analysis.append("- GDP Growth Stability: ").append(importerProfile.gdpStability).append("\n");
        analysis.append("- Inflation Risk: ").append(importerProfile.inflationRisk).append("\n");
        analysis.append("- Unemployment Trend: ").append(importerProfile.unemploymentTrend).append("\n");
        analysis.append("- Economic Outlook: ").append(importerProfile.economicOutlook).append("\n\n");
        
        // Origin country economic risks
        analysis.append("Origin Country Economic Risks:\n");
        for (String origin : originCountries) {
            EconomicRiskProfile profile = getEconomicRiskProfile(origin);
            analysis.append("- ").append(getCountryName(origin)).append(":\n");
            analysis.append("  • Economic Growth: ").append(profile.gdpStability).append("\n");
            analysis.append("  • Currency Stability: ").append(profile.currencyStability).append("\n");
            analysis.append("  • Export Capacity: ").append(profile.exportCapacity).append("\n");
            analysis.append("  • Risk Level: ").append(profile.riskLevel).append("\n\n");
        }
        
        // Economic interdependence
        analysis.append("Economic Interdependence Analysis:\n");
        for (String origin : originCountries) {
            double tradeIntensity = calculateTradeIntensity(importerIso2, origin);
            analysis.append("- ").append(origin).append(" trade intensity: ").append(String.format("%.1f%%", tradeIntensity)).append("\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Analyze supply chain risk
     */
    private String analyzeSupplyChainRisk(String importerIso2, List<String> originCountries, 
                                        String hsCode, BigDecimal tradeVolume) {
        StringBuilder analysis = new StringBuilder();
        
        analysis.append("Supply Chain Vulnerability Assessment:\n\n");
        
        // Supplier concentration risk
        analysis.append("1. Supplier Concentration Risk:\n");
        if (originCountries.size() == 1) {
            analysis.append("   ⚠️ HIGH RISK: Single source dependency\n");
            analysis.append("   Recommendation: Diversify supplier base\n");
        } else if (originCountries.size() <= 3) {
            analysis.append("   🟡 MEDIUM RISK: Limited supplier diversity\n");
            analysis.append("   Recommendation: Consider additional suppliers\n");
        } else {
            analysis.append("   ✅ LOW RISK: Good supplier diversification\n");
        }
        analysis.append("\n");
        
        // Geographic risk concentration
        analysis.append("2. Geographic Risk Concentration:\n");
        Map<String, List<String>> regionGroups = groupCountriesByRegion(originCountries);
        for (Map.Entry<String, List<String>> entry : regionGroups.entrySet()) {
            analysis.append("   - ").append(entry.getKey()).append(": ").append(entry.getValue().size()).append(" suppliers\n");
        }
        
        if (regionGroups.size() == 1) {
            analysis.append("   ⚠️ Regional concentration risk identified\n");
        }
        analysis.append("\n");
        
        // Infrastructure and logistics risk
        analysis.append("3. Infrastructure and Logistics Risk:\n");
        for (String origin : originCountries) {
            InfrastructureRisk infraRisk = assessInfrastructureRisk(origin, importerIso2);
            analysis.append("   - ").append(getCountryName(origin)).append(":\n");
            analysis.append("     • Port/Airport Capacity: ").append(infraRisk.transportCapacity).append("\n");
            analysis.append("     • Logistics Reliability: ").append(infraRisk.logisticsReliability).append("\n");
            analysis.append("     • Transit Time Variability: ").append(infraRisk.transitVariability).append("\n");
            analysis.append("     • Infrastructure Risk: ").append(infraRisk.overallRisk).append("\n\n");
        }
        
        // Product-specific supply risks
        if (hsCode != null) {
            analysis.append("4. Product-Specific Supply Risks:\n");
            ProductSupplyRisk productRisk = assessProductSupplyRisk(hsCode);
            analysis.append("   - Raw Material Availability: ").append(productRisk.rawMaterialRisk).append("\n");
            analysis.append("   - Manufacturing Complexity: ").append(productRisk.manufacturingComplexity).append("\n");
            analysis.append("   - Quality Control Requirements: ").append(productRisk.qualityControlRisk).append("\n");
            analysis.append("   - Seasonal Variations: ").append(productRisk.seasonalRisk).append("\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Analyze financial risk
     */
    private String analyzeFinancialRisk(String importerIso2, List<String> originCountries, 
                                      String timeHorizon, BigDecimal tradeValue) {
        StringBuilder analysis = new StringBuilder();
        
        analysis.append("Financial Risk Analysis:\n\n");
        
        // Currency risk assessment
        analysis.append("1. Currency Exchange Risk:\n");
        for (String origin : originCountries) {
            CurrencyRisk currencyRisk = assessCurrencyRisk(importerIso2, origin, timeHorizon);
            analysis.append("   - ").append(getCountryName(origin)).append(" (").append(getCurrencyCode(origin)).append("):\n");
            analysis.append("     • Volatility: ").append(currencyRisk.volatility).append("\n");
            analysis.append("     • Trend: ").append(currencyRisk.trend).append("\n");
            analysis.append("     • Hedging Availability: ").append(currencyRisk.hedgingAvailability).append("\n");
            analysis.append("     • Risk Level: ").append(currencyRisk.riskLevel).append("\n\n");
        }
        
        // Payment and credit risk
        analysis.append("2. Payment and Credit Risk:\n");
        for (String origin : originCountries) {
            PaymentRisk paymentRisk = assessPaymentRisk(origin);
            analysis.append("   - ").append(getCountryName(origin)).append(":\n");
            analysis.append("     • Country Credit Rating: ").append(paymentRisk.creditRating).append("\n");
            analysis.append("     • Banking System Stability: ").append(paymentRisk.bankingStability).append("\n");
            analysis.append("     • Payment Terms Risk: ").append(paymentRisk.paymentTermsRisk).append("\n");
            analysis.append("     • Default Risk: ").append(paymentRisk.defaultRisk).append("\n\n");
        }
        
        // Trade finance availability
        analysis.append("3. Trade Finance Risk:\n");
        TradeFinanceRisk financeRisk = assessTradeFinanceRisk(importerIso2, originCountries, tradeValue);
        analysis.append("   - Letter of Credit Availability: ").append(financeRisk.lcAvailability).append("\n");
        analysis.append("   - Export Credit Insurance: ").append(financeRisk.creditInsurance).append("\n");
        analysis.append("   - Financing Costs: ").append(financeRisk.financingCosts).append("\n");
        analysis.append("   - Liquidity Risk: ").append(financeRisk.liquidityRisk).append("\n");
        
        return analysis.toString();
    }
    
    /**
     * Analyze force majeure risk
     */
    private String analyzeForceMajeureRisk(String importerIso2, List<String> originCountries, String hsCode) {
        StringBuilder analysis = new StringBuilder();
        
        analysis.append("Force Majeure Risk Assessment:\n\n");
        
        // Natural disaster risk
        analysis.append("1. Natural Disaster Risk:\n");
        for (String origin : originCountries) {
            NaturalDisasterRisk disasterRisk = assessNaturalDisasterRisk(origin);
            analysis.append("   - ").append(getCountryName(origin)).append(":\n");
            analysis.append("     • Earthquake Risk: ").append(disasterRisk.earthquakeRisk).append("\n");
            analysis.append("     • Flood Risk: ").append(disasterRisk.floodRisk).append("\n");
            analysis.append("     • Hurricane/Typhoon Risk: ").append(disasterRisk.stormRisk).append("\n");
            analysis.append("     • Overall Natural Disaster Risk: ").append(disasterRisk.overallRisk).append("\n\n");
        }
        
        // Pandemic and health crisis risk
        analysis.append("2. Pandemic and Health Crisis Risk:\n");
        PandemicRisk pandemicRisk = assessPandemicRisk(originCountries);
        analysis.append("   - Healthcare System Capacity: ").append(pandemicRisk.healthcareCapacity).append("\n");
        analysis.append("   - Border Closure Risk: ").append(pandemicRisk.borderClosureRisk).append("\n");
        analysis.append("   - Supply Chain Disruption: ").append(pandemicRisk.supplyChainImpact).append("\n");
        analysis.append("   - Recovery Capability: ").append(pandemicRisk.recoveryCapability).append("\n\n");
        
        // Cyber security and technology risk
        analysis.append("3. Cyber Security Risk:\n");
        CyberSecurityRisk cyberRisk = assessCyberSecurityRisk(originCountries);
        analysis.append("   - Critical Infrastructure Vulnerability: ").append(cyberRisk.infrastructureVulnerability).append("\n");
        analysis.append("   - Supply Chain Cyber Risk: ").append(cyberRisk.supplyChainRisk).append("\n");
        analysis.append("   - Data Security Risk: ").append(cyberRisk.dataSecurityRisk).append("\n");
        analysis.append("   - Recovery Preparedness: ").append(cyberRisk.recoveryPreparedness).append("\n\n");
        
        // Climate change impact
        analysis.append("4. Climate Change Impact:\n");
        ClimateRisk climateRisk = assessClimateRisk(originCountries, hsCode);
        analysis.append("   - Physical Climate Risk: ").append(climateRisk.physicalRisk).append("\n");
        analysis.append("   - Transition Risk: ").append(climateRisk.transitionRisk).append("\n");
        analysis.append("   - Regulatory Impact: ").append(climateRisk.regulatoryImpact).append("\n");
        analysis.append("   - Adaptation Capability: ").append(climateRisk.adaptationCapability).append("\n");
        
        return analysis.toString();
    }
    
    // Risk mitigation and planning methods
    
    private String getRiskMitigationStrategies(RiskSummary riskSummary, String importerIso2, 
                                             List<String> originCountries, BigDecimal tradeValue) {
        StringBuilder strategies = new StringBuilder();
        
        strategies.append("Priority Risk Mitigation Strategies:\n\n");
        
        // High-priority strategies based on primary risks
        int priority = 1;
        for (String primaryRisk : riskSummary.primaryRisks) {
            strategies.append(priority).append(". ").append(primaryRisk).append(" Mitigation:\n");
            
            switch (primaryRisk.toLowerCase()) {
                case "political instability" -> {
                    strategies.append("   • Diversify supplier base across stable countries\n");
                    strategies.append("   • Obtain political risk insurance\n");
                    strategies.append("   • Monitor political developments closely\n");
                    strategies.append("   • Establish government relations programs\n");
                }
                case "economic volatility" -> {
                    strategies.append("   • Implement currency hedging strategies\n");
                    strategies.append("   • Use flexible pricing mechanisms\n");
                    strategies.append("   • Monitor economic indicators\n");
                    strategies.append("   • Diversify across economic cycles\n");
                }
                case "supply chain disruption" -> {
                    strategies.append("   • Build supplier redundancy\n");
                    strategies.append("   • Maintain strategic inventory buffers\n");
                    strategies.append("   • Develop alternative logistics routes\n");
                    strategies.append("   • Implement supply chain visibility systems\n");
                }
                case "financial/currency risk" -> {
                    strategies.append("   • Use forward contracts and options\n");
                    strategies.append("   • Implement natural hedging strategies\n");
                    strategies.append("   • Secure trade finance facilities\n");
                    strategies.append("   • Monitor credit exposures\n");
                }
                case "force majeure events" -> {
                    strategies.append("   • Develop comprehensive business continuity plans\n");
                    strategies.append("   • Obtain force majeure insurance coverage\n");
                    strategies.append("   • Create emergency response procedures\n");
                    strategies.append("   • Establish alternative supply sources\n");
                }
                default -> {
                    strategies.append("   • Implement comprehensive risk monitoring\n");
                    strategies.append("   • Develop contingency plans\n");
                    strategies.append("   • Regular risk assessment reviews\n");
                }
            }
            strategies.append("\n");
            priority++;
        }
        
        // General risk management framework
        strategies.append("General Risk Management Framework:\n");
        strategies.append("• Establish risk governance structure\n");
        strategies.append("• Implement regular risk monitoring and reporting\n");
        strategies.append("• Develop key risk indicators (KRIs)\n");
        strategies.append("• Create risk escalation procedures\n");
        strategies.append("• Regular stress testing and scenario planning\n");
        
        return strategies.toString();
    }
    
    private String getContingencyPlanning(RiskSummary riskSummary, String importerIso2, 
                                        List<String> originCountries, String hsCode) {
        StringBuilder planning = new StringBuilder();
        
        planning.append("Contingency Planning Framework:\n\n");
        
        // Scenario-based contingency plans
        planning.append("1. Scenario-Based Response Plans:\n\n");
        
        planning.append("   Scenario A: Major Supplier Disruption\n");
        planning.append("   • Activate backup suppliers within 48 hours\n");
        planning.append("   • Implement emergency procurement procedures\n");
        planning.append("   • Communicate with customers about potential delays\n");
        planning.append("   • Assess alternative product sources\n\n");
        
        planning.append("   Scenario B: Trade Policy Changes\n");
        planning.append("   • Review tariff implications immediately\n");
        planning.append("   • Evaluate alternative trade routes\n");
        planning.append("   • Assess pricing and contract adjustments\n");
        planning.append("   • Engage trade policy experts\n\n");
        
        planning.append("   Scenario C: Currency Crisis\n");
        planning.append("   • Activate currency hedging strategies\n");
        planning.append("   • Review payment terms and conditions\n");
        planning.append("   • Assess pricing adjustments\n");
        planning.append("   • Consider alternative currencies\n\n");
        
        planning.append("   Scenario D: Force Majeure Event\n");
        planning.append("   • Activate business continuity plan\n");
        planning.append("   • Assess supply chain impact\n");
        planning.append("   • Implement alternative logistics routes\n");
        planning.append("   • Communicate with all stakeholders\n\n");
        
        // Emergency response procedures
        planning.append("2. Emergency Response Procedures:\n");
        planning.append("   • 24/7 crisis management hotline\n");
        planning.append("   • Rapid response team activation\n");
        planning.append("   • Stakeholder communication protocols\n");
        planning.append("   • Decision-making authority matrix\n");
        planning.append("   • Regular drill and testing schedule\n\n");
        
        // Recovery planning
        planning.append("3. Recovery Planning:\n");
        planning.append("   • Business impact assessment procedures\n");
        planning.append("   • Recovery time objectives (RTO)\n");
        planning.append("   • Recovery point objectives (RPO)\n");
        planning.append("   • Resource allocation for recovery\n");
        planning.append("   • Lessons learned documentation\n");
        
        return planning.toString();
    }
    
    private String getMonitoringRecommendations(RiskSummary riskSummary, String timeHorizon) {
        StringBuilder monitoring = new StringBuilder();
        
        monitoring.append("Risk Monitoring and Review Framework:\n\n");
        
        // Monitoring frequency based on risk level
        String frequency = switch (riskSummary.overallRisk) {
            case "HIGH", "CRITICAL" -> "Weekly";
            case "MEDIUM" -> "Monthly";
            default -> "Quarterly";
        };
        
        monitoring.append("Recommended Monitoring Frequency: ").append(frequency).append("\n\n");
        
        // Key risk indicators
        monitoring.append("Key Risk Indicators (KRIs) to Monitor:\n");
        monitoring.append("• Political stability indices\n");
        monitoring.append("• Economic indicators (GDP, inflation, unemployment)\n");
        monitoring.append("• Currency exchange rate volatility\n");
        monitoring.append("• Supply chain performance metrics\n");
        monitoring.append("• Trade policy and regulatory changes\n");
        monitoring.append("• Natural disaster and climate events\n");
        monitoring.append("• Cyber security threat levels\n\n");
        
        // Review and update schedule
        monitoring.append("Risk Assessment Review Schedule:\n");
        monitoring.append("• Comprehensive review: Annually\n");
        monitoring.append("• Targeted reviews: Quarterly\n");
        monitoring.append("• Ad-hoc reviews: As triggered by events\n");
        monitoring.append("• Stress testing: Semi-annually\n\n");
        
        // Reporting and escalation
        monitoring.append("Reporting and Escalation:\n");
        monitoring.append("• Monthly risk dashboard\n");
        monitoring.append("• Quarterly risk committee reports\n");
        monitoring.append("• Immediate escalation for critical risks\n");
        monitoring.append("• Annual risk strategy review\n");
        
        return monitoring.toString();
    }
    
    // Helper methods and data classes
    
    private String getCountryName(String iso2) {
        Map<String, String> countryNames = new HashMap<>();
        countryNames.put("US", "United States");
        countryNames.put("CA", "Canada");
        countryNames.put("MX", "Mexico");
        countryNames.put("GB", "United Kingdom");
        countryNames.put("DE", "Germany");
        countryNames.put("FR", "France");
        countryNames.put("JP", "Japan");
        countryNames.put("CN", "China");
        countryNames.put("KR", "South Korea");
        countryNames.put("IN", "India");
        countryNames.put("BR", "Brazil");
        countryNames.put("AU", "Australia");
        return countryNames.getOrDefault(iso2, iso2);
    }
    
    private String getTimeHorizonDescription(String timeHorizon) {
        return switch (timeHorizon) {
            case "short-term" -> "1 year";
            case "medium-term" -> "3 years";
            case "long-term" -> "5+ years";
            default -> "3 years";
        };
    }
    
    private String getRiskLevel(int score) {
        if (score >= 80) return "CRITICAL";
        if (score >= 60) return "HIGH";
        if (score >= 40) return "MEDIUM";
        if (score >= 20) return "LOW";
        return "MINIMAL";
    }
    
    private String getRiskTrend(String timeHorizon, int currentScore) {
        // Simplified trend analysis
        if ("long-term".equals(timeHorizon) && currentScore > 50) {
            return "Increasing (climate and geopolitical factors)";
        } else if ("short-term".equals(timeHorizon) && currentScore < 40) {
            return "Stable";
        } else {
            return "Moderate increase expected";
        }
    }
    
    // Risk calculation methods (simplified for demonstration)
    
    private int calculatePoliticalRiskScore(String importerIso2, List<String> originCountries) {
        int totalScore = 0;
        for (String origin : originCountries) {
            totalScore += getPoliticalRiskScore(origin);
        }
        return totalScore / originCountries.size();
    }
    
    private int calculateEconomicRiskScore(String importerIso2, List<String> originCountries) {
        int totalScore = 0;
        for (String origin : originCountries) {
            totalScore += getEconomicRiskScore(origin);
        }
        return totalScore / originCountries.size();
    }
    
    private int calculateSupplyChainRiskScore(String importerIso2, List<String> originCountries, String hsCode) {
        int baseScore = 30; // Base supply chain risk
        
        // Single source penalty
        if (originCountries.size() == 1) {
            baseScore += 20;
        }
        
        // Geographic concentration penalty
        Map<String, List<String>> regions = groupCountriesByRegion(originCountries);
        if (regions.size() == 1) {
            baseScore += 15;
        }
        
        return Math.min(100, baseScore);
    }
    
    private int calculateFinancialRiskScore(String importerIso2, List<String> originCountries, BigDecimal tradeValue) {
        int totalScore = 0;
        for (String origin : originCountries) {
            totalScore += getFinancialRiskScore(origin);
        }
        
        // High value trade increases risk
        if (tradeValue != null && tradeValue.compareTo(new BigDecimal("1000000")) >= 0) {
            totalScore += 10;
        }
        
        return Math.min(100, totalScore / originCountries.size());
    }
    
    private int calculateForceMajeureRiskScore(String importerIso2, List<String> originCountries) {
        int totalScore = 0;
        for (String origin : originCountries) {
            totalScore += getForceMajeureRiskScore(origin);
        }
        return totalScore / originCountries.size();
    }
    
    // Individual country risk scores (simplified)
    
    private int getPoliticalRiskScore(String countryIso2) {
        return switch (countryIso2) {
            case "CN", "RU", "IR", "VE" -> 70; // High political risk
            case "TR", "BR", "IN", "ZA" -> 50; // Medium political risk
            case "US", "CA", "GB", "DE", "JP", "AU" -> 20; // Low political risk
            default -> 40; // Default medium risk
        };
    }
    
    private int getEconomicRiskScore(String countryIso2) {
        return switch (countryIso2) {
            case "VE", "AR", "TR" -> 80; // High economic risk
            case "BR", "IN", "ZA", "RU" -> 60; // Medium-high economic risk
            case "CN", "MX", "KR" -> 40; // Medium economic risk
            case "US", "CA", "GB", "DE", "JP", "AU" -> 25; // Low economic risk
            default -> 45; // Default medium risk
        };
    }
    
    private int getFinancialRiskScore(String countryIso2) {
        return switch (countryIso2) {
            case "VE", "AR", "IR" -> 85; // Very high financial risk
            case "TR", "BR", "RU" -> 65; // High financial risk
            case "IN", "ZA", "MX" -> 45; // Medium financial risk
            case "CN", "KR" -> 35; // Medium-low financial risk
            case "US", "CA", "GB", "DE", "JP", "AU" -> 20; // Low financial risk
            default -> 50; // Default medium risk
        };
    }
    
    private int getForceMajeureRiskScore(String countryIso2) {
        return switch (countryIso2) {
            case "JP", "PH", "ID" -> 70; // High natural disaster risk
            case "CN", "IN", "US" -> 50; // Medium natural disaster risk
            case "CA", "AU", "BR" -> 40; // Medium-low natural disaster risk
            case "GB", "DE", "FR" -> 30; // Low natural disaster risk
            default -> 45; // Default medium risk
        };
    }
    
    // Data classes for risk assessment
    
    private static class RiskSummary {
        String overallRisk;
        int riskScore;
        List<String> primaryRisks;
        String riskTrend;
        
        RiskSummary(String overallRisk, int riskScore, List<String> primaryRisks, String riskTrend) {
            this.overallRisk = overallRisk;
            this.riskScore = riskScore;
            this.primaryRisks = primaryRisks;
            this.riskTrend = riskTrend;
        }
    }
    
    // Additional data classes and helper methods would be implemented here
    // (PoliticalRiskProfile, EconomicRiskProfile, etc.)
    
    // Simplified implementations for demonstration
    
    private PoliticalRiskProfile getPoliticalRiskProfile(String countryIso2) {
        return new PoliticalRiskProfile("Medium", "Good", "High", "Stable", "Medium");
    }
    
    private EconomicRiskProfile getEconomicRiskProfile(String countryIso2) {
        return new EconomicRiskProfile("Stable", "Low", "Stable", "Positive", "Stable", "Good", "Medium");
    }
    
    private String analyzeBilateralRelationship(String country1, String country2) {
        return "Stable trade relationship with regular diplomatic engagement";
    }
    
    private double calculateTradeIntensity(String importer, String exporter) {
        return 15.5; // Simplified calculation
    }
    
    private Map<String, List<String>> groupCountriesByRegion(List<String> countries) {
        Map<String, List<String>> regions = new HashMap<>();
        for (String country : countries) {
            String region = getRegion(country);
            regions.computeIfAbsent(region, k -> new ArrayList<>()).add(country);
        }
        return regions;
    }
    
    private String getRegion(String countryIso2) {
        return switch (countryIso2) {
            case "US", "CA", "MX" -> "North America";
            case "CN", "JP", "KR", "IN" -> "Asia";
            case "GB", "DE", "FR", "IT" -> "Europe";
            case "BR", "AR", "CL" -> "South America";
            default -> "Other";
        };
    }
    
    private InfrastructureRisk assessInfrastructureRisk(String origin, String destination) {
        return new InfrastructureRisk("Good", "High", "Low", "Medium");
    }
    
    private ProductSupplyRisk assessProductSupplyRisk(String hsCode) {
        return new ProductSupplyRisk("Medium", "Medium", "High", "Low");
    }
    
    private CurrencyRisk assessCurrencyRisk(String importer, String exporter, String timeHorizon) {
        return new CurrencyRisk("Medium", "Stable", "Good", "Medium");
    }
    
    private String getCurrencyCode(String countryIso2) {
        return switch (countryIso2) {
            case "US" -> "USD";
            case "CA" -> "CAD";
            case "GB" -> "GBP";
            case "JP" -> "JPY";
            case "CN" -> "CNY";
            default -> "Local Currency";
        };
    }
    
    private PaymentRisk assessPaymentRisk(String countryIso2) {
        return new PaymentRisk("A", "Stable", "Low", "Low");
    }
    
    private TradeFinanceRisk assessTradeFinanceRisk(String importer, List<String> exporters, BigDecimal tradeValue) {
        return new TradeFinanceRisk("Good", "Available", "Medium", "Low");
    }
    
    private NaturalDisasterRisk assessNaturalDisasterRisk(String countryIso2) {
        return new NaturalDisasterRisk("Medium", "Low", "Medium", "Medium");
    }
    
    private PandemicRisk assessPandemicRisk(List<String> countries) {
        return new PandemicRisk("Good", "Medium", "Medium", "Good");
    }
    
    private CyberSecurityRisk assessCyberSecurityRisk(List<String> countries) {
        return new CyberSecurityRisk("Medium", "Medium", "Medium", "Good");
    }
    
    private ClimateRisk assessClimateRisk(List<String> countries, String hsCode) {
        return new ClimateRisk("Medium", "Medium", "Medium", "Good");
    }
    
    // Additional mitigation methods
    
    private String getPoliticalRiskMitigation(String importer, List<String> exporters) {
        return "Implement political risk insurance, diversify supplier base, monitor political developments";
    }
    
    private String getEconomicRiskMitigation(String importer, List<String> exporters, BigDecimal tradeValue) {
        return "Use currency hedging, implement flexible pricing, monitor economic indicators";
    }
    
    private String getSupplyChainRiskMitigation(String importer, List<String> exporters, String hsCode) {
        return "Build supplier redundancy, maintain inventory buffers, develop alternative logistics";
    }
    
    private String getFinancialRiskMitigation(String importer, List<String> exporters, BigDecimal tradeValue) {
        return "Use forward contracts, implement natural hedging, secure trade finance facilities";
    }
    
    private String getForceMajeurePlanning(String importer, List<String> exporters, String hsCode) {
        return "Develop business continuity plans, obtain insurance coverage, create emergency procedures";
    }
    
    // Simple data classes for risk profiles
    
    private record PoliticalRiskProfile(String stability, String effectiveness, String regulatoryQuality, 
                                       String tradeRelations, String riskLevel) {}
    
    private record EconomicRiskProfile(String gdpStability, String inflationRisk, String unemploymentTrend, 
                                      String economicOutlook, String currencyStability, String exportCapacity, 
                                      String riskLevel) {}
    
    private record InfrastructureRisk(String transportCapacity, String logisticsReliability, 
                                     String transitVariability, String overallRisk) {}
    
    private record ProductSupplyRisk(String rawMaterialRisk, String manufacturingComplexity, 
                                    String qualityControlRisk, String seasonalRisk) {}
    
    private record CurrencyRisk(String volatility, String trend, String hedgingAvailability, String riskLevel) {}
    
    private record PaymentRisk(String creditRating, String bankingStability, String paymentTermsRisk, String defaultRisk) {}
    
    private record TradeFinanceRisk(String lcAvailability, String creditInsurance, String financingCosts, String liquidityRisk) {}
    
    private record NaturalDisasterRisk(String earthquakeRisk, String floodRisk, String stormRisk, String overallRisk) {}
    
    private record PandemicRisk(String healthcareCapacity, String borderClosureRisk, String supplyChainImpact, String recoveryCapability) {}
    
    private record CyberSecurityRisk(String infrastructureVulnerability, String supplyChainRisk, String dataSecurityRisk, String recoveryPreparedness) {}
    
    private record ClimateRisk(String physicalRisk, String transitionRisk, String regulatoryImpact, String adaptationCapability) {}
}