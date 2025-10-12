package com.tariffsheriff.backend.chatbot.service.tools;

import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.service.AgreementService;
import com.tariffsheriff.backend.tariff.service.TariffRateService;
import com.tariffsheriff.backend.data.ExternalDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Tool for retrieving trade agreements for specific countries
 */
@Component
public class AgreementTool implements ChatbotTool {
    
    private static final Logger logger = LoggerFactory.getLogger(AgreementTool.class);
    
    private final AgreementService agreementService;
    private final TariffRateService tariffRateService;
    
    @Autowired(required = false)
    private ExternalDataService externalDataService;
    
    public AgreementTool(AgreementService agreementService, TariffRateService tariffRateService) {
        this.agreementService = agreementService;
        this.tariffRateService = tariffRateService;
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
        
        // Partner countries parameter for comparison
        Map<String, Object> partnerParam = new HashMap<>();
        partnerParam.put("type", "array");
        partnerParam.put("description", "Array of partner country ISO2 codes for agreement comparison (optional)");
        Map<String, Object> partnerItems = new HashMap<>();
        partnerItems.put("type", "string");
        partnerParam.put("items", partnerItems);
        properties.put("partnerCountries", partnerParam);
        
        // Analysis type parameter
        Map<String, Object> analysisParam = new HashMap<>();
        analysisParam.put("type", "string");
        analysisParam.put("description", "Type of analysis: 'basic' (default), 'impact', 'rules-of-origin', 'comparison', 'utilization'");
        analysisParam.put("enum", Arrays.asList("basic", "impact", "rules-of-origin", "comparison", "utilization"));
        properties.put("analysisType", analysisParam);
        
        // HS code parameter for specific product analysis
        Map<String, Object> hsCodeParam = new HashMap<>();
        hsCodeParam.put("type", "string");
        hsCodeParam.put("description", "HS code for product-specific agreement analysis (optional)");
        properties.put("hsCode", hsCodeParam);
        
        // Trade value parameter for impact analysis
        Map<String, Object> tradeValueParam = new HashMap<>();
        tradeValueParam.put("type", "number");
        tradeValueParam.put("description", "Annual trade value in USD for impact analysis (optional)");
        properties.put("tradeValue", tradeValueParam);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"countryIso2"});
        
        return new ToolDefinition(
            getName(),
            "Retrieve and analyze trade agreements between countries including FTAs, customs unions, and preferential agreements. " +
            "USE WHEN: User asks about trade agreements, FTAs, preferential rates, rules of origin, RVC requirements, or agreement benefits. " +
            "REQUIRES: Country ISO2 code (e.g., 'US', 'CA', 'JP'). Optional: partner countries for comparison, HS code for product-specific analysis. " +
            "RETURNS: Active agreements, agreement types, RVC thresholds, rules of origin requirements, potential savings, and compliance guidance. " +
            "EXAMPLES: 'What trade agreements does USA have?', 'Compare USMCA vs NAFTA benefits', 'What are the rules of origin for automotive parts?', 'Calculate potential savings from FTA utilization'. " +
            "SUPPORTS: Basic agreement listing, impact assessment, rules of origin analysis, multi-agreement comparison, and utilization rate evaluation.",
            parameters
        );
    }
    
    @Override
    public ToolResult execute(ToolCall toolCall) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract and validate parameters
            String countryIso2 = toolCall.getStringArgument("countryIso2");
            String analysisType = toolCall.getStringArgument("analysisType", "basic");
            String hsCode = toolCall.getStringArgument("hsCode");
            BigDecimal tradeValue = toolCall.getBigDecimalArgument("tradeValue");
            
            // Handle partner countries for comparison
            List<String> partnerCountries = extractPartnerCountries(toolCall);
            
            // Validate required parameter
            if (countryIso2 == null || countryIso2.trim().isEmpty()) {
                return ToolResult.error(getName(), "Missing required parameter: countryIso2");
            }
            
            // Normalize parameters
            countryIso2 = countryIso2.trim().toUpperCase();
            analysisType = analysisType.toLowerCase();
            
            // Validate ISO2 format
            if (countryIso2.length() != 2 || !countryIso2.matches("[A-Z]{2}")) {
                return ToolResult.error(getName(), "Invalid countryIso2 format. Must be 2-character ISO country code (e.g., 'US')");
            }
            
            // Validate partner countries if provided
            for (String partner : partnerCountries) {
                if (partner.length() != 2 || !partner.matches("[A-Z]{2}")) {
                    return ToolResult.error(getName(), "Invalid partner country format: " + partner);
                }
            }
            
            logger.info("Executing {} analysis for trade agreements: country={}, partners={}", 
                       analysisType, countryIso2, partnerCountries);
            
            // Execute analysis based on type with real-time enrichment
            String formattedResult = switch (analysisType) {
                case "impact" -> performImpactAnalysisWithEnrichment(countryIso2, partnerCountries, hsCode, tradeValue);
                case "rules-of-origin" -> performRulesOfOriginAnalysisWithEnrichment(countryIso2, partnerCountries, hsCode);
                case "comparison" -> performComparisonAnalysisWithEnrichment(countryIso2, partnerCountries);
                case "utilization" -> performUtilizationAnalysisWithEnrichment(countryIso2, partnerCountries, tradeValue);
                default -> performBasicAnalysisWithEnrichment(countryIso2);
            };
            
            ToolResult toolResult = ToolResult.success(getName(), formattedResult);
            toolResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            logger.info("Completed {} analysis in {}ms", analysisType, toolResult.getExecutionTimeMs());
            
            return toolResult;
            
        } catch (Exception e) {
            logger.error("Error executing enhanced agreement analysis tool", e);
            ToolResult errorResult = ToolResult.error(getName(), 
                "Failed to perform agreement analysis: " + e.getMessage());
            errorResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return errorResult;
        }
    }
    
    /**
     * Extract partner countries from tool call parameters
     */
    private List<String> extractPartnerCountries(ToolCall toolCall) {
        List<String> partnerCountries = new ArrayList<>();
        
        Object partnerCountriesParam = toolCall.getArgument("partnerCountries");
        if (partnerCountriesParam instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String country) {
                    partnerCountries.add(country.trim().toUpperCase());
                }
            }
        }
        
        return partnerCountries;
    }
    
    /**
     * Perform basic agreement analysis (legacy functionality)
     */
    private String performBasicAnalysis(String countryIso2) {
        List<Agreement> agreements = agreementService.getAgreementsByCountry(countryIso2);
        return formatAgreementResult(agreements, countryIso2);
    }
    
    /**
     * Perform agreement impact analysis
     */
    private String performImpactAnalysis(String countryIso2, List<String> partnerCountries, String hsCode, BigDecimal tradeValue) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Trade Agreement Impact Analysis\n");
        analysis.append("=====================================\n");
        analysis.append("Country: ").append(countryIso2).append("\n");
        
        if (!partnerCountries.isEmpty()) {
            analysis.append("Partner Countries: ").append(String.join(", ", partnerCountries)).append("\n");
        }
        if (hsCode != null) {
            analysis.append("HS Code: ").append(hsCode).append("\n");
        }
        if (tradeValue != null) {
            analysis.append("Annual Trade Value: $").append(tradeValue).append("\n");
        }
        analysis.append("\n");
        
        List<Agreement> agreements = agreementService.getAgreementsByCountry(countryIso2);
        
        if (agreements.isEmpty()) {
            analysis.append("No trade agreements found for ").append(countryIso2).append(".\n");
            analysis.append("Impact: Limited to MFN rates only.\n");
            return analysis.toString();
        }
        
        // Filter agreements by partner countries if specified
        if (!partnerCountries.isEmpty()) {
            agreements = agreements.stream()
                .filter(agreement -> partnerCountries.stream()
                    .anyMatch(partner -> agreement.getName().toUpperCase().contains(partner)))
                .collect(Collectors.toList());
        }
        
        analysis.append("Agreement Impact Assessment:\n\n");
        
        for (Agreement agreement : agreements) {
            analysis.append("Agreement: ").append(agreement.getName()).append("\n");
            analysis.append("- Type: ").append(agreement.getType()).append("\n");
            analysis.append("- Status: ").append(agreement.getStatus()).append("\n");
            
            // Calculate potential savings if trade value provided
            if (tradeValue != null && agreement.getRvcThreshold() != null) {
                BigDecimal potentialSavings = calculatePotentialSavings(agreement, tradeValue, hsCode, countryIso2);
                if (potentialSavings.compareTo(BigDecimal.ZERO) > 0) {
                    analysis.append("- Estimated Annual Savings: $").append(potentialSavings).append("\n");
                }
            }
            
            // Agreement maturity and stability
            if (agreement.getEnteredIntoForce() != null) {
                LocalDate entryDate = agreement.getEnteredIntoForce();
                long yearsActive = ChronoUnit.YEARS.between(entryDate, LocalDate.now());
                analysis.append("- Years Active: ").append(yearsActive).append("\n");
                
                if (yearsActive < 2) {
                    analysis.append("- Maturity: New agreement - monitor for implementation issues\n");
                } else if (yearsActive < 5) {
                    analysis.append("- Maturity: Established - good track record\n");
                } else {
                    analysis.append("- Maturity: Mature - well-established procedures\n");
                }
            }
            
            // RVC threshold impact
            if (agreement.getRvcThreshold() != null) {
                analysis.append("- RVC Requirement: ").append(agreement.getRvcThreshold()).append("%\n");
                String rvcGuidance = getRvcGuidance(agreement.getRvcThreshold());
                analysis.append("- RVC Impact: ").append(rvcGuidance).append("\n");
            }
            
            analysis.append("\n");
        }
        
        // Overall impact summary
        analysis.append("Overall Impact Summary:\n");
        if (agreements.size() > 1) {
            analysis.append("- Multiple agreements available - optimize partner selection\n");
        }
        
        long activeAgreements = agreements.stream()
            .filter(a -> "Active".equalsIgnoreCase(a.getStatus()) || "In Force".equalsIgnoreCase(a.getStatus()))
            .count();
        
        analysis.append("- Active Agreements: ").append(activeAgreements).append("/").append(agreements.size()).append("\n");
        
        if (tradeValue != null) {
            BigDecimal totalPotentialSavings = agreements.stream()
                .map(agreement -> calculatePotentialSavings(agreement, tradeValue, hsCode, countryIso2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (totalPotentialSavings.compareTo(BigDecimal.ZERO) > 0) {
                analysis.append("- Total Potential Savings: $").append(totalPotentialSavings).append("\n");
                BigDecimal savingsPercentage = totalPotentialSavings.divide(tradeValue, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                analysis.append("- Savings Rate: ").append(savingsPercentage.setScale(2, java.math.RoundingMode.HALF_UP)).append("%\n");
            }
        }
        
        return analysis.toString();
    }
    
    /**
     * Perform rules of origin analysis
     */
    private String performRulesOfOriginAnalysis(String countryIso2, List<String> partnerCountries, String hsCode) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Rules of Origin Analysis\n");
        analysis.append("=========================\n");
        analysis.append("Country: ").append(countryIso2).append("\n");
        
        if (!partnerCountries.isEmpty()) {
            analysis.append("Partner Countries: ").append(String.join(", ", partnerCountries)).append("\n");
        }
        if (hsCode != null) {
            analysis.append("HS Code: ").append(hsCode).append("\n");
        }
        analysis.append("\n");
        
        List<Agreement> agreements = agreementService.getAgreementsByCountry(countryIso2);
        
        if (agreements.isEmpty()) {
            analysis.append("No trade agreements found - only MFN rules apply.\n");
            return analysis.toString();
        }
        
        // Filter by partner countries if specified
        if (!partnerCountries.isEmpty()) {
            agreements = agreements.stream()
                .filter(agreement -> partnerCountries.stream()
                    .anyMatch(partner -> agreement.getName().toUpperCase().contains(partner)))
                .collect(Collectors.toList());
        }
        
        analysis.append("Rules of Origin Requirements:\n\n");
        
        for (Agreement agreement : agreements) {
            analysis.append("Agreement: ").append(agreement.getName()).append("\n");
            
            if (agreement.getRvcThreshold() != null) {
                analysis.append("- Regional Value Content (RVC): ").append(agreement.getRvcThreshold()).append("%\n");
                
                // Provide RVC calculation guidance
                analysis.append("- RVC Calculation Method: ");
                if (agreement.getType().contains("NAFTA") || agreement.getType().contains("USMCA")) {
                    analysis.append("Net Cost or Transaction Value method\n");
                } else {
                    analysis.append("FOB or CIF method (check specific agreement)\n");
                }
                
                // RVC compliance guidance
                String complianceGuidance = getRvcComplianceGuidance(agreement.getRvcThreshold(), hsCode);
                analysis.append("- Compliance Strategy: ").append(complianceGuidance).append("\n");
            } else {
                analysis.append("- RVC Threshold: Not specified (check agreement text)\n");
            }
            
            // Product-specific rules
            if (hsCode != null) {
                String productSpecificRules = getProductSpecificRules(hsCode, agreement);
                analysis.append("- Product-Specific Rules: ").append(productSpecificRules).append("\n");
            }
            
            // Documentation requirements
            analysis.append("- Documentation Required:\n");
            analysis.append("  • Certificate of Origin\n");
            analysis.append("  • Supporting production records\n");
            analysis.append("  • Material cost breakdowns\n");
            analysis.append("  • Supplier declarations\n");
            
            // Common compliance issues
            analysis.append("- Common Compliance Issues:\n");
            analysis.append("  • Insufficient documentation\n");
            analysis.append("  • Incorrect RVC calculations\n");
            analysis.append("  • Missing supplier certifications\n");
            analysis.append("  • Tariff shift rule violations\n");
            
            analysis.append("\n");
        }
        
        // General recommendations
        analysis.append("Recommendations:\n");
        analysis.append("- Establish robust origin tracking systems\n");
        analysis.append("- Train staff on RVC calculation methods\n");
        analysis.append("- Maintain detailed production records\n");
        analysis.append("- Regular compliance audits\n");
        analysis.append("- Consider supply chain restructuring for compliance\n");
        
        return analysis.toString();
    }
    
    /**
     * Perform agreement comparison analysis
     */
    private String performComparisonAnalysis(String countryIso2, List<String> partnerCountries) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Trade Agreement Comparison\n");
        analysis.append("============================\n");
        analysis.append("Base Country: ").append(countryIso2).append("\n");
        
        if (partnerCountries.isEmpty()) {
            analysis.append("No partner countries specified for comparison.\n");
            analysis.append("Showing all available agreements for ").append(countryIso2).append(":\n\n");
            
            List<Agreement> agreements = agreementService.getAgreementsByCountry(countryIso2);
            return formatAgreementComparison(agreements);
        }
        
        analysis.append("Comparing agreements with: ").append(String.join(", ", partnerCountries)).append("\n\n");
        
        List<Agreement> baseAgreements = agreementService.getAgreementsByCountry(countryIso2);
        Map<String, List<Agreement>> partnerAgreements = new HashMap<>();
        
        for (String partner : partnerCountries) {
            List<Agreement> agreements = agreementService.getAgreementsByCountry(partner);
            partnerAgreements.put(partner, agreements);
        }
        
        // Find common agreements
        analysis.append("Common Agreements:\n");
        Set<String> commonAgreementNames = findCommonAgreements(baseAgreements, partnerAgreements);
        
        if (commonAgreementNames.isEmpty()) {
            analysis.append("- No common multilateral agreements found\n");
        } else {
            for (String agreementName : commonAgreementNames) {
                analysis.append("- ").append(agreementName).append("\n");
            }
        }
        analysis.append("\n");
        
        // Bilateral agreements analysis
        analysis.append("Bilateral Agreement Opportunities:\n");
        for (String partner : partnerCountries) {
            boolean hasBilateral = baseAgreements.stream()
                .anyMatch(agreement -> agreement.getName().toUpperCase().contains(partner));
            
            analysis.append("- ").append(countryIso2).append(" ↔ ").append(partner).append(": ");
            if (hasBilateral) {
                Agreement bilateral = baseAgreements.stream()
                    .filter(agreement -> agreement.getName().toUpperCase().contains(partner))
                    .findFirst().orElse(null);
                
                if (bilateral != null) {
                    analysis.append("✅ Active (").append(bilateral.getName()).append(")\n");
                    if (bilateral.getRvcThreshold() != null) {
                        analysis.append("  RVC Threshold: ").append(bilateral.getRvcThreshold()).append("%\n");
                    }
                }
            } else {
                analysis.append("❌ No bilateral agreement\n");
                analysis.append("  Recommendation: Explore bilateral trade opportunities\n");
            }
        }
        analysis.append("\n");
        
        // Agreement quality comparison
        analysis.append("Agreement Quality Comparison:\n");
        for (Agreement agreement : baseAgreements) {
            String quality = assessAgreementQuality(agreement);
            analysis.append("- ").append(agreement.getName()).append(": ").append(quality).append("\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Perform utilization rate analysis
     */
    private String performUtilizationAnalysis(String countryIso2, List<String> partnerCountries, BigDecimal tradeValue) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Agreement Utilization Analysis\n");
        analysis.append("===============================\n");
        analysis.append("Country: ").append(countryIso2).append("\n");
        
        if (tradeValue != null) {
            analysis.append("Trade Value: $").append(tradeValue).append("\n");
        }
        analysis.append("\n");
        
        List<Agreement> agreements = agreementService.getAgreementsByCountry(countryIso2);
        
        if (agreements.isEmpty()) {
            analysis.append("No trade agreements available for utilization analysis.\n");
            return analysis.toString();
        }
        
        analysis.append("Utilization Assessment:\n\n");
        
        for (Agreement agreement : agreements) {
            analysis.append("Agreement: ").append(agreement.getName()).append("\n");
            
            // Estimate utilization rate (this would typically come from trade statistics)
            double estimatedUtilization = estimateUtilizationRate(agreement);
            analysis.append("- Estimated Utilization Rate: ").append(String.format("%.1f%%", estimatedUtilization)).append("\n");
            
            // Utilization barriers
            List<String> barriers = identifyUtilizationBarriers(agreement);
            if (!barriers.isEmpty()) {
                analysis.append("- Utilization Barriers:\n");
                for (String barrier : barriers) {
                    analysis.append("  • ").append(barrier).append("\n");
                }
            }
            
            // Improvement recommendations
            List<String> recommendations = getUtilizationRecommendations(agreement, estimatedUtilization);
            if (!recommendations.isEmpty()) {
                analysis.append("- Recommendations:\n");
                for (String recommendation : recommendations) {
                    analysis.append("  • ").append(recommendation).append("\n");
                }
            }
            
            // Cost-benefit analysis
            if (tradeValue != null) {
                BigDecimal potentialSavings = calculatePotentialSavings(agreement, tradeValue, null, countryIso2);
                BigDecimal actualSavings = potentialSavings.multiply(new BigDecimal(estimatedUtilization / 100));
                BigDecimal missedSavings = potentialSavings.subtract(actualSavings);
                
                analysis.append("- Potential Annual Savings: $").append(potentialSavings).append("\n");
                analysis.append("- Actual Savings (estimated): $").append(actualSavings).append("\n");
                analysis.append("- Missed Savings: $").append(missedSavings).append("\n");
            }
            
            analysis.append("\n");
        }
        
        // Overall utilization summary
        double averageUtilization = agreements.stream()
            .mapToDouble(this::estimateUtilizationRate)
            .average().orElse(0.0);
        
        analysis.append("Overall Utilization Summary:\n");
        analysis.append("- Average Utilization Rate: ").append(String.format("%.1f%%", averageUtilization)).append("\n");
        
        if (averageUtilization < 50) {
            analysis.append("- Status: Low utilization - significant improvement opportunity\n");
        } else if (averageUtilization < 75) {
            analysis.append("- Status: Moderate utilization - room for improvement\n");
        } else {
            analysis.append("- Status: Good utilization - maintain current practices\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Calculate potential savings from agreement utilization
     */
    private BigDecimal calculatePotentialSavings(Agreement agreement, BigDecimal tradeValue, String hsCode, String countryIso2) {
        // Simplified calculation - in reality would need detailed tariff data
        // Assume average 5-15% tariff savings depending on agreement type
        
        BigDecimal savingsRate = switch (agreement.getType().toUpperCase()) {
            case "FTA", "FREE TRADE AGREEMENT" -> new BigDecimal("0.12"); // 12% average savings
            case "CUSTOMS UNION" -> new BigDecimal("0.15"); // 15% average savings
            case "ECONOMIC PARTNERSHIP" -> new BigDecimal("0.08"); // 8% average savings
            case "PREFERENTIAL TRADE AGREEMENT" -> new BigDecimal("0.06"); // 6% average savings
            default -> new BigDecimal("0.05"); // 5% default
        };
        
        return tradeValue.multiply(savingsRate);
    }
    
    /**
     * Get RVC guidance based on threshold
     */
    private String getRvcGuidance(BigDecimal rvcThreshold) {
        if (rvcThreshold.compareTo(new BigDecimal("60")) >= 0) {
            return "High threshold - requires significant local content";
        } else if (rvcThreshold.compareTo(new BigDecimal("40")) >= 0) {
            return "Moderate threshold - manageable with planning";
        } else {
            return "Low threshold - relatively easy to meet";
        }
    }
    
    /**
     * Get RVC compliance guidance
     */
    private String getRvcComplianceGuidance(BigDecimal rvcThreshold, String hsCode) {
        StringBuilder guidance = new StringBuilder();
        
        if (rvcThreshold.compareTo(new BigDecimal("50")) >= 0) {
            guidance.append("Focus on local sourcing and value-added activities");
        } else {
            guidance.append("Moderate local content requirements");
        }
        
        if (hsCode != null && hsCode.startsWith("84")) {
            guidance.append("; Consider assembly operations for machinery");
        } else if (hsCode != null && hsCode.startsWith("62")) {
            guidance.append("; Textile assembly can help meet requirements");
        }
        
        return guidance.toString();
    }
    
    /**
     * Get product-specific rules
     */
    private String getProductSpecificRules(String hsCode, Agreement agreement) {
        if (hsCode.length() < 2) return "General rules apply";
        
        String chapter = hsCode.substring(0, 2);
        
        return switch (chapter) {
            case "84", "85" -> "Tariff shift + RVC or substantial transformation";
            case "87" -> "Tariff shift + specific manufacturing operations";
            case "61", "62" -> "Yarn forward rule or tariff shift + RVC";
            case "39" -> "Chemical reaction or tariff shift + RVC";
            case "72", "73" -> "Substantial transformation in steel production";
            default -> "Standard RVC or tariff shift rules";
        };
    }
    
    /**
     * Find common agreements between countries
     */
    private Set<String> findCommonAgreements(List<Agreement> baseAgreements, Map<String, List<Agreement>> partnerAgreements) {
        Set<String> commonAgreements = new HashSet<>();
        
        for (Agreement baseAgreement : baseAgreements) {
            boolean isCommon = partnerAgreements.values().stream()
                .allMatch(partnerList -> partnerList.stream()
                    .anyMatch(partnerAgreement -> 
                        partnerAgreement.getName().equals(baseAgreement.getName())));
            
            if (isCommon) {
                commonAgreements.add(baseAgreement.getName());
            }
        }
        
        return commonAgreements;
    }
    
    /**
     * Assess agreement quality
     */
    private String assessAgreementQuality(Agreement agreement) {
        int score = 0;
        
        // Status check
        if ("Active".equalsIgnoreCase(agreement.getStatus()) || "In Force".equalsIgnoreCase(agreement.getStatus())) {
            score += 3;
        }
        
        // Type check
        if (agreement.getType().toUpperCase().contains("FTA") || 
            agreement.getType().toUpperCase().contains("FREE TRADE")) {
            score += 2;
        }
        
        // RVC threshold check (lower is better for utilization)
        if (agreement.getRvcThreshold() != null) {
            if (agreement.getRvcThreshold().compareTo(new BigDecimal("50")) <= 0) {
                score += 2;
            } else {
                score += 1;
            }
        }
        
        // Maturity check
        if (agreement.getEnteredIntoForce() != null) {
            long yearsActive = ChronoUnit.YEARS.between(agreement.getEnteredIntoForce(), LocalDate.now());
            if (yearsActive >= 5) {
                score += 1;
            }
        }
        
        return switch (score) {
            case 7, 8 -> "Excellent - highly beneficial";
            case 5, 6 -> "Good - beneficial with some limitations";
            case 3, 4 -> "Fair - moderate benefits";
            default -> "Limited - minimal benefits";
        };
    }
    
    /**
     * Estimate utilization rate for an agreement
     */
    private double estimateUtilizationRate(Agreement agreement) {
        // Simplified estimation based on agreement characteristics
        double baseRate = 60.0; // Base utilization rate
        
        // Adjust based on RVC threshold
        if (agreement.getRvcThreshold() != null) {
            if (agreement.getRvcThreshold().compareTo(new BigDecimal("60")) >= 0) {
                baseRate -= 20; // High threshold reduces utilization
            } else if (agreement.getRvcThreshold().compareTo(new BigDecimal("40")) >= 0) {
                baseRate -= 10; // Moderate threshold
            }
        }
        
        // Adjust based on agreement maturity
        if (agreement.getEnteredIntoForce() != null) {
            long yearsActive = ChronoUnit.YEARS.between(agreement.getEnteredIntoForce(), LocalDate.now());
            if (yearsActive < 2) {
                baseRate -= 15; // New agreements have lower utilization
            } else if (yearsActive >= 10) {
                baseRate += 10; // Mature agreements have higher utilization
            }
        }
        
        // Adjust based on agreement type
        if (agreement.getType().toUpperCase().contains("FTA")) {
            baseRate += 15; // FTAs typically have higher utilization
        }
        
        return Math.max(10, Math.min(95, baseRate)); // Cap between 10% and 95%
    }
    
    /**
     * Identify utilization barriers
     */
    private List<String> identifyUtilizationBarriers(Agreement agreement) {
        List<String> barriers = new ArrayList<>();
        
        if (agreement.getRvcThreshold() != null && agreement.getRvcThreshold().compareTo(new BigDecimal("60")) >= 0) {
            barriers.add("High RVC threshold requirements");
        }
        
        if (agreement.getEnteredIntoForce() != null) {
            long yearsActive = ChronoUnit.YEARS.between(agreement.getEnteredIntoForce(), LocalDate.now());
            if (yearsActive < 2) {
                barriers.add("New agreement - limited implementation experience");
            }
        }
        
        // Common barriers
        barriers.add("Complex documentation requirements");
        barriers.add("Limited awareness among traders");
        barriers.add("Administrative burden");
        
        return barriers;
    }
    
    /**
     * Get utilization improvement recommendations
     */
    private List<String> getUtilizationRecommendations(Agreement agreement, double currentUtilization) {
        List<String> recommendations = new ArrayList<>();
        
        if (currentUtilization < 50) {
            recommendations.add("Conduct trader education and outreach programs");
            recommendations.add("Simplify certificate of origin procedures");
            recommendations.add("Establish help desk for origin requirements");
        }
        
        if (agreement.getRvcThreshold() != null && agreement.getRvcThreshold().compareTo(new BigDecimal("50")) >= 0) {
            recommendations.add("Develop supply chain mapping tools");
            recommendations.add("Promote regional supplier networks");
        }
        
        recommendations.add("Digitize origin certification processes");
        recommendations.add("Regular compliance training for exporters");
        recommendations.add("Monitor and address implementation issues");
        
        return recommendations;
    }
    
    /**
     * Format agreement comparison results
     */
    private String formatAgreementComparison(List<Agreement> agreements) {
        StringBuilder formatted = new StringBuilder();
        
        if (agreements.isEmpty()) {
            formatted.append("No agreements found for comparison.\n");
            return formatted.toString();
        }
        
        formatted.append("Agreement Comparison Table:\n\n");
        formatted.append(String.format("%-30s %-15s %-10s %-10s %-15s%n", 
            "Agreement Name", "Type", "Status", "RVC %", "Years Active"));
        formatted.append("-".repeat(80)).append("\n");
        
        for (Agreement agreement : agreements) {
            String yearsActive = "N/A";
            if (agreement.getEnteredIntoForce() != null) {
                long years = ChronoUnit.YEARS.between(agreement.getEnteredIntoForce(), LocalDate.now());
                yearsActive = String.valueOf(years);
            }
            
            String rvcThreshold = agreement.getRvcThreshold() != null ? 
                agreement.getRvcThreshold().toString() : "N/A";
            
            formatted.append(String.format("%-30s %-15s %-10s %-10s %-15s%n",
                truncate(agreement.getName(), 29),
                truncate(agreement.getType(), 14),
                truncate(agreement.getStatus(), 9),
                rvcThreshold,
                yearsActive));
        }
        
        return formatted.toString();
    }
    
    /**
     * Truncate string to specified length
     */
    private String truncate(String str, int length) {
        if (str == null) return "";
        return str.length() <= length ? str : str.substring(0, length - 3) + "...";
    }
    
    /**
     * Format the agreement lookup result for LLM consumption (legacy method)
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
    
    /**
     * Perform basic analysis with real-time data enrichment
     */
    private String performBasicAnalysisWithEnrichment(String countryIso2) {
        String basicResult = performBasicAnalysis(countryIso2);
        
        if (externalDataService == null) {
            return basicResult;
        }
        
        try {
            StringBuilder enrichedResult = new StringBuilder(basicResult);
            enrichedResult.append("\n").append("=".repeat(45)).append("\n");
            enrichedResult.append("REAL-TIME AGREEMENT INTELLIGENCE\n");
            enrichedResult.append("=".repeat(45)).append("\n\n");
            
            // Get recent regulatory updates
            CompletableFuture<List<ExternalDataService.RegulatoryUpdate>> regulatoryFuture = 
                externalDataService.getRegulatoryUpdates(countryIso2, "bilateral");
            
            // Get trade news
            CompletableFuture<List<ExternalDataService.NewsItem>> newsFuture = 
                externalDataService.getTradeNews("trade agreement " + countryIso2, countryIso2, 3);
            
            List<ExternalDataService.RegulatoryUpdate> updates = regulatoryFuture.get(5, TimeUnit.SECONDS);
            List<ExternalDataService.NewsItem> news = newsFuture.get(5, TimeUnit.SECONDS);
            
            // Add recent agreement updates
            if (!updates.isEmpty()) {
                enrichedResult.append("Recent Agreement Updates:\n");
                updates.stream().limit(2).forEach(update -> {
                    enrichedResult.append("• ").append(update.getTitle()).append("\n");
                    enrichedResult.append("  Type: ").append(update.getType())
                        .append(" | Effective: ").append(update.getEffectiveDate().toString().substring(0, 10)).append("\n");
                });
                enrichedResult.append("\n");
            }
            
            // Add relevant trade news
            if (!news.isEmpty()) {
                enrichedResult.append("Recent Trade Agreement News:\n");
                news.forEach(item -> {
                    enrichedResult.append("• ").append(item.getTitle()).append("\n");
                    enrichedResult.append("  Source: ").append(item.getSource())
                        .append(" (").append(item.getPublishedAt().toString().substring(0, 10)).append(")\n");
                });
                enrichedResult.append("\n");
            }
            
            // Add strategic insights
            enrichedResult.append("Strategic Insights:\n");
            enrichedResult.append("• Monitor agreement negotiations and updates regularly\n");
            enrichedResult.append("• Consider diversifying trade partnerships\n");
            enrichedResult.append("• Evaluate utilization rates of existing agreements\n");
            enrichedResult.append("• Stay informed about rules of origin changes\n");
            
            return enrichedResult.toString();
            
        } catch (Exception e) {
            logger.warn("Failed to enrich agreement analysis with real-time data: {}", e.getMessage());
            return basicResult + "\n\nNote: Real-time agreement intelligence temporarily unavailable.";
        }
    }
    
    /**
     * Enhanced methods for other analysis types (simplified implementations)
     */
    private String performImpactAnalysisWithEnrichment(String countryIso2, List<String> partnerCountries, String hsCode, BigDecimal tradeValue) {
        String basicResult = performImpactAnalysis(countryIso2, partnerCountries, hsCode, tradeValue);
        return addRealTimeContext(basicResult, "IMPACT ANALYSIS INTELLIGENCE", countryIso2);
    }
    
    private String performRulesOfOriginAnalysisWithEnrichment(String countryIso2, List<String> partnerCountries, String hsCode) {
        String basicResult = performRulesOfOriginAnalysis(countryIso2, partnerCountries, hsCode);
        return addRealTimeContext(basicResult, "RULES OF ORIGIN INTELLIGENCE", countryIso2);
    }
    
    private String performComparisonAnalysisWithEnrichment(String countryIso2, List<String> partnerCountries) {
        String basicResult = performComparisonAnalysis(countryIso2, partnerCountries);
        return addRealTimeContext(basicResult, "AGREEMENT COMPARISON INTELLIGENCE", countryIso2);
    }
    
    private String performUtilizationAnalysisWithEnrichment(String countryIso2, List<String> partnerCountries, BigDecimal tradeValue) {
        String basicResult = performUtilizationAnalysis(countryIso2, partnerCountries, tradeValue);
        return addRealTimeContext(basicResult, "UTILIZATION INTELLIGENCE", countryIso2);
    }
    
    /**
     * Add real-time context to analysis results
     */
    private String addRealTimeContext(String basicResult, String sectionTitle, String countryIso2) {
        if (externalDataService == null) {
            return basicResult;
        }
        
        try {
            StringBuilder enrichedResult = new StringBuilder(basicResult);
            enrichedResult.append("\n").append("=".repeat(40)).append("\n");
            enrichedResult.append(sectionTitle).append("\n");
            enrichedResult.append("=".repeat(40)).append("\n\n");
            
            enrichedResult.append("Real-time Considerations:\n");
            enrichedResult.append("• Check for recent agreement amendments or updates\n");
            enrichedResult.append("• Monitor trade policy changes and negotiations\n");
            enrichedResult.append("• Consider geopolitical factors affecting agreements\n");
            enrichedResult.append("• Evaluate current utilization trends and patterns\n");
            
            return enrichedResult.toString();
            
        } catch (Exception e) {
            logger.warn("Failed to add real-time context: {}", e.getMessage());
            return basicResult + "\n\nNote: Real-time context temporarily unavailable.";
        }
    }
}