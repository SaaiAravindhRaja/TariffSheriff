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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool for comprehensive trade compliance analysis including regulatory requirements,
 * documentation needs, customs procedures, and compliance risk assessment
 */
@Component
public class ComplianceAnalysisTool implements ChatbotTool {
    
    private static final Logger logger = LoggerFactory.getLogger(ComplianceAnalysisTool.class);
    
    private final AgreementService agreementService;
    private final TariffRateService tariffRateService;
    
    public ComplianceAnalysisTool(AgreementService agreementService, TariffRateService tariffRateService) {
        this.agreementService = agreementService;
        this.tariffRateService = tariffRateService;
    }
    
    @Override
    public String getName() {
        return "analyzeTradeCompliance";
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
        originParam.put("type", "string");
        originParam.put("description", "ISO2 country code of the origin/exporting country (e.g., 'MX', 'CN', 'DE')");
        properties.put("originIso2", originParam);
        
        // Product information
        Map<String, Object> hsCodeParam = new HashMap<>();
        hsCodeParam.put("type", "string");
        hsCodeParam.put("description", "HS code of the product for compliance analysis");
        properties.put("hsCode", hsCodeParam);
        
        Map<String, Object> productParam = new HashMap<>();
        productParam.put("type", "string");
        productParam.put("description", "Product description for additional compliance context (optional)");
        properties.put("productDescription", productParam);
        
        // Analysis type parameter
        Map<String, Object> analysisParam = new HashMap<>();
        analysisParam.put("type", "string");
        analysisParam.put("description", "Type of compliance analysis: 'comprehensive' (default), 'documentation', 'procedures', 'risk-assessment', 'licensing'");
        analysisParam.put("enum", Arrays.asList("comprehensive", "documentation", "procedures", "risk-assessment", "licensing"));
        properties.put("analysisType", analysisParam);
        
        // Trade value for risk assessment
        Map<String, Object> valueParam = new HashMap<>();
        valueParam.put("type", "number");
        valueParam.put("description", "Trade value in USD for risk-based compliance analysis (optional)");
        properties.put("tradeValue", valueParam);
        
        // Shipment frequency for procedures analysis
        Map<String, Object> frequencyParam = new HashMap<>();
        frequencyParam.put("type", "string");
        frequencyParam.put("description", "Shipment frequency: 'one-time', 'monthly', 'weekly', 'daily' for procedure optimization");
        properties.put("shipmentFrequency", frequencyParam);
        
        parameters.put("properties", properties);
        parameters.put("required", new String[]{"importerIso2", "originIso2", "hsCode"});
        
        return new ToolDefinition(
            getName(),
            "Analyze trade compliance requirements including documentation, customs procedures, licenses, and regulatory obligations. " +
            "USE WHEN: User asks about compliance requirements, import/export documentation, customs procedures, licenses, permits, or regulatory obligations. " +
            "REQUIRES: Importer country (ISO2), origin country (ISO2), and HS code. Optional: product description, trade value, shipment frequency. " +
            "RETURNS: Required documentation checklist, customs clearance procedures, licensing requirements, regulatory compliance steps, risk assessment, and timeline estimates. " +
            "EXAMPLES: 'What documents do I need to import electronics from China to USA?', 'What are the customs procedures for importing food products?', 'Do I need a license to import medical devices?', 'Assess compliance risks for my trade route'. " +
            "SUPPORTS: Comprehensive compliance analysis, documentation requirements, customs procedures, risk assessment, and licensing analysis.",
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
            String productDescription = toolCall.getStringArgument("productDescription");
            String analysisType = toolCall.getStringArgument("analysisType", "comprehensive");
            String shipmentFrequency = toolCall.getStringArgument("shipmentFrequency");
            BigDecimal tradeValue = toolCall.getBigDecimalArgument("tradeValue");
            
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
            analysisType = analysisType.toLowerCase();
            
            // Validate formats
            if (importerIso2.length() != 2 || !importerIso2.matches("[A-Z]{2}")) {
                return ToolResult.error(getName(), "Invalid importerIso2 format. Must be 2-character ISO country code");
            }
            if (originIso2.length() != 2 || !originIso2.matches("[A-Z]{2}")) {
                return ToolResult.error(getName(), "Invalid originIso2 format. Must be 2-character ISO country code");
            }
            if (!hsCode.matches("\\d{4,10}")) {
                return ToolResult.error(getName(), "Invalid HS code format. Must be 4-10 digit numeric code");
            }
            
            logger.info("Executing {} compliance analysis for trade route: {} -> {} (HS: {})", 
                       analysisType, originIso2, importerIso2, hsCode);
            
            // Execute analysis based on type
            String formattedResult = switch (analysisType) {
                case "documentation" -> performDocumentationAnalysis(importerIso2, originIso2, hsCode, productDescription);
                case "procedures" -> performProceduresAnalysis(importerIso2, originIso2, hsCode, shipmentFrequency);
                case "risk-assessment" -> performRiskAssessment(importerIso2, originIso2, hsCode, tradeValue);
                case "licensing" -> performLicensingAnalysis(importerIso2, originIso2, hsCode, productDescription);
                default -> performComprehensiveAnalysis(importerIso2, originIso2, hsCode, productDescription, tradeValue, shipmentFrequency);
            };
            
            ToolResult toolResult = ToolResult.success(getName(), formattedResult);
            toolResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            logger.info("Completed {} compliance analysis in {}ms", analysisType, toolResult.getExecutionTimeMs());
            
            return toolResult;
            
        } catch (Exception e) {
            logger.error("Error executing compliance analysis tool for route {} -> {}, HS code: {}", 
                    toolCall.getStringArgument("originIso2"), 
                    toolCall.getStringArgument("importerIso2"), 
                    toolCall.getStringArgument("hsCode"), e);
            
            String userMessage = "I had trouble analyzing compliance requirements. ";
            
            // Provide helpful guidance
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("database")) {
                userMessage += "There's a problem connecting to the compliance database. Please try again in a moment.";
            } else {
                userMessage += "Please try:\n" +
                        "• Verifying the country codes are correct (2-letter codes like 'US', 'MX', 'CN')\n" +
                        "• Checking that the HS code is valid\n" +
                        "• Simplifying your compliance question\n" +
                        "• Asking about specific compliance aspects (documentation, procedures, licensing)";
            }
            
            ToolResult errorResult = ToolResult.error(getName(), userMessage);
            errorResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return errorResult;
        }
    }
    
    /**
     * Perform comprehensive compliance analysis
     */
    private String performComprehensiveAnalysis(String importerIso2, String originIso2, String hsCode, 
                                              String productDescription, BigDecimal tradeValue, String shipmentFrequency) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Comprehensive Trade Compliance Analysis\n");
        analysis.append("=========================================\n");
        analysis.append("Trade Route: ").append(originIso2).append(" → ").append(importerIso2).append("\n");
        analysis.append("HS Code: ").append(hsCode).append("\n");
        
        if (productDescription != null) {
            analysis.append("Product: ").append(productDescription).append("\n");
        }
        if (tradeValue != null) {
            analysis.append("Trade Value: $").append(tradeValue).append("\n");
        }
        if (shipmentFrequency != null) {
            analysis.append("Shipment Frequency: ").append(shipmentFrequency).append("\n");
        }
        
        analysis.append("Analysis Date: ").append(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n\n");
        
        // 1. Regulatory Overview
        analysis.append("1. REGULATORY OVERVIEW\n");
        analysis.append("========================\n");
        analysis.append(getRegulatoryOverview(importerIso2, originIso2, hsCode)).append("\n\n");
        
        // 2. Documentation Requirements
        analysis.append("2. DOCUMENTATION REQUIREMENTS\n");
        analysis.append("===============================\n");
        analysis.append(getDocumentationRequirements(importerIso2, originIso2, hsCode)).append("\n\n");
        
        // 3. Customs Procedures
        analysis.append("3. CUSTOMS PROCEDURES\n");
        analysis.append("======================\n");
        analysis.append(getCustomsProcedures(importerIso2, originIso2, hsCode, shipmentFrequency)).append("\n\n");
        
        // 4. Licensing and Permits
        analysis.append("4. LICENSING AND PERMITS\n");
        analysis.append("=========================\n");
        analysis.append(getLicensingRequirements(importerIso2, originIso2, hsCode, productDescription)).append("\n\n");
        
        // 5. Compliance Risk Assessment
        analysis.append("5. COMPLIANCE RISK ASSESSMENT\n");
        analysis.append("==============================\n");
        analysis.append(getComplianceRiskAssessment(importerIso2, originIso2, hsCode, tradeValue)).append("\n\n");
        
        // 6. Recommendations
        analysis.append("6. RECOMMENDATIONS\n");
        analysis.append("===================\n");
        analysis.append(getComplianceRecommendations(importerIso2, originIso2, hsCode, tradeValue, shipmentFrequency)).append("\n");
        
        return analysis.toString();
    }
    
    /**
     * Perform documentation-focused analysis
     */
    private String performDocumentationAnalysis(String importerIso2, String originIso2, String hsCode, String productDescription) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Trade Documentation Analysis\n");
        analysis.append("==============================\n");
        analysis.append("Trade Route: ").append(originIso2).append(" → ").append(importerIso2).append("\n");
        analysis.append("HS Code: ").append(hsCode).append("\n\n");
        
        analysis.append(getDocumentationRequirements(importerIso2, originIso2, hsCode));
        
        // Add document preparation guidance
        analysis.append("\n\nDocument Preparation Guidelines:\n");
        analysis.append("=====================================\n");
        analysis.append(getDocumentPreparationGuidance(importerIso2, hsCode));
        
        return analysis.toString();
    }
    
    /**
     * Perform procedures-focused analysis
     */
    private String performProceduresAnalysis(String importerIso2, String originIso2, String hsCode, String shipmentFrequency) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Customs Procedures Analysis\n");
        analysis.append("=============================\n");
        analysis.append("Trade Route: ").append(originIso2).append(" → ").append(importerIso2).append("\n");
        analysis.append("HS Code: ").append(hsCode).append("\n\n");
        
        analysis.append(getCustomsProcedures(importerIso2, originIso2, hsCode, shipmentFrequency));
        
        // Add timeline and process optimization
        analysis.append("\n\nProcess Optimization:\n");
        analysis.append("======================\n");
        analysis.append(getProcessOptimization(importerIso2, shipmentFrequency));
        
        return analysis.toString();
    }
    
    /**
     * Perform risk assessment analysis
     */
    private String performRiskAssessment(String importerIso2, String originIso2, String hsCode, BigDecimal tradeValue) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Compliance Risk Assessment\n");
        analysis.append("===========================\n");
        analysis.append("Trade Route: ").append(originIso2).append(" → ").append(importerIso2).append("\n");
        analysis.append("HS Code: ").append(hsCode).append("\n\n");
        
        analysis.append(getComplianceRiskAssessment(importerIso2, originIso2, hsCode, tradeValue));
        
        // Add risk mitigation strategies
        analysis.append("\n\nRisk Mitigation Strategies:\n");
        analysis.append("============================\n");
        analysis.append(getRiskMitigationStrategies(importerIso2, originIso2, hsCode));
        
        return analysis.toString();
    }
    
    /**
     * Perform licensing analysis
     */
    private String performLicensingAnalysis(String importerIso2, String originIso2, String hsCode, String productDescription) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Licensing and Permits Analysis\n");
        analysis.append("================================\n");
        analysis.append("Trade Route: ").append(originIso2).append(" → ").append(importerIso2).append("\n");
        analysis.append("HS Code: ").append(hsCode).append("\n\n");
        
        analysis.append(getLicensingRequirements(importerIso2, originIso2, hsCode, productDescription));
        
        // Add licensing process guidance
        analysis.append("\n\nLicensing Process Guidance:\n");
        analysis.append("============================\n");
        analysis.append(getLicensingProcessGuidance(importerIso2, hsCode));
        
        return analysis.toString();
    }
    
    /**
     * Get regulatory overview for the trade route
     */
    private String getRegulatoryOverview(String importerIso2, String originIso2, String hsCode) {
        StringBuilder overview = new StringBuilder();
        
        // Get applicable trade agreements
        List<Agreement> agreements = agreementService.getAgreementsByCountry(importerIso2);
        List<Agreement> applicableAgreements = agreements.stream()
            .filter(agreement -> agreement.getName().toUpperCase().contains(originIso2) ||
                               isMultilateralAgreement(agreement, originIso2))
            .collect(Collectors.toList());
        
        overview.append("Regulatory Framework:\n");
        if (applicableAgreements.isEmpty()) {
            overview.append("- Trade governed by MFN (Most Favored Nation) rules\n");
            overview.append("- No preferential trade agreements applicable\n");
            overview.append("- Standard customs procedures apply\n");
        } else {
            overview.append("- Preferential trade agreements available:\n");
            for (Agreement agreement : applicableAgreements) {
                overview.append("  • ").append(agreement.getName()).append(" (").append(agreement.getStatus()).append(")\n");
            }
        }
        
        // Product-specific regulations
        String productRegulations = getProductSpecificRegulations(importerIso2, hsCode);
        if (!productRegulations.isEmpty()) {
            overview.append("\nProduct-Specific Regulations:\n");
            overview.append(productRegulations);
        }
        
        // Import restrictions and controls
        String importControls = getImportControls(importerIso2, originIso2, hsCode);
        if (!importControls.isEmpty()) {
            overview.append("\nImport Controls:\n");
            overview.append(importControls);
        }
        
        return overview.toString();
    }
    
    /**
     * Get documentation requirements
     */
    private String getDocumentationRequirements(String importerIso2, String originIso2, String hsCode) {
        StringBuilder docs = new StringBuilder();
        
        // Standard documents
        docs.append("Required Documents:\n\n");
        docs.append("Standard Import Documents:\n");
        docs.append("✓ Commercial Invoice\n");
        docs.append("✓ Packing List\n");
        docs.append("✓ Bill of Lading/Air Waybill\n");
        docs.append("✓ Import Declaration/Entry Form\n");
        
        // Country-specific requirements
        switch (importerIso2) {
            case "US" -> {
                docs.append("✓ CBP Form 3461 (Entry/Immediate Delivery)\n");
                docs.append("✓ CBP Form 7501 (Entry Summary)\n");
                if (needsISF(hsCode)) {
                    docs.append("✓ Importer Security Filing (ISF)\n");
                }
            }
            case "CA" -> {
                docs.append("✓ CBSA Form B3 (Canada Customs Coding Form)\n");
                docs.append("✓ CFIA permits (if applicable)\n");
            }
            case "GB" -> {
                docs.append("✓ UK Import Declaration\n");
                docs.append("✓ EORI Number registration\n");
            }
            case "DE", "FR", "IT", "ES", "NL" -> {
                docs.append("✓ EU Import Declaration (SAD)\n");
                docs.append("✓ EORI Number\n");
                docs.append("✓ CE Marking (if applicable)\n");
            }
        }
        
        // Product-specific documents
        String productDocs = getProductSpecificDocuments(importerIso2, hsCode);
        if (!productDocs.isEmpty()) {
            docs.append("\nProduct-Specific Documents:\n");
            docs.append(productDocs);
        }
        
        // Preferential origin documents
        List<Agreement> agreements = agreementService.getAgreementsByCountry(importerIso2);
        boolean hasPreferentialAgreement = agreements.stream()
            .anyMatch(agreement -> agreement.getName().toUpperCase().contains(originIso2));
        
        if (hasPreferentialAgreement) {
            docs.append("\nPreferential Origin Documents:\n");
            docs.append("✓ Certificate of Origin\n");
            docs.append("✓ Supplier's Declaration\n");
            docs.append("✓ Production Records (for RVC calculation)\n");
        }
        
        return docs.toString();
    }
    
    /**
     * Get customs procedures information
     */
    private String getCustomsProcedures(String importerIso2, String originIso2, String hsCode, String shipmentFrequency) {
        StringBuilder procedures = new StringBuilder();
        
        procedures.append("Import Process Timeline:\n\n");
        
        // Pre-arrival procedures
        procedures.append("Pre-Arrival (5-15 days before):\n");
        procedures.append("1. Prepare and submit required documents\n");
        procedures.append("2. Obtain necessary licenses/permits\n");
        procedures.append("3. Arrange customs broker (if using)\n");
        
        if ("US".equals(importerIso2)) {
            procedures.append("4. Submit ISF (24 hours before loading)\n");
        }
        
        // Arrival procedures
        procedures.append("\nArrival and Clearance:\n");
        procedures.append("1. Goods arrive at port/airport\n");
        procedures.append("2. Submit import declaration\n");
        procedures.append("3. Pay duties and taxes\n");
        procedures.append("4. Physical examination (if selected)\n");
        procedures.append("5. Release of goods\n");
        
        // Timeline estimates
        procedures.append("\nEstimated Processing Times:\n");
        switch (importerIso2) {
            case "US" -> procedures.append("- Standard clearance: 1-3 days\n- Expedited (ABI): Same day\n");
            case "CA" -> procedures.append("- Standard clearance: 1-2 days\n- CBSA examination: +1-2 days\n");
            case "GB" -> procedures.append("- Standard clearance: 1-2 days\n- HMRC examination: +2-3 days\n");
            default -> procedures.append("- Standard clearance: 1-5 days\n- Customs examination: +1-3 days\n");
        }
        
        // Frequency-based optimizations
        if (shipmentFrequency != null) {
            procedures.append("\nFrequency-Based Optimizations:\n");
            switch (shipmentFrequency.toLowerCase()) {
                case "daily", "weekly" -> {
                    procedures.append("- Consider Authorized Economic Operator (AEO) status\n");
                    procedures.append("- Implement automated clearance systems\n");
                    procedures.append("- Establish trusted trader programs\n");
                }
                case "monthly" -> {
                    procedures.append("- Consider periodic entry procedures\n");
                    procedures.append("- Implement consistent documentation processes\n");
                }
                case "one-time" -> {
                    procedures.append("- Focus on accurate first-time filing\n");
                    procedures.append("- Consider using experienced customs broker\n");
                }
            }
        }
        
        return procedures.toString();
    }
    
    /**
     * Get licensing requirements
     */
    private String getLicensingRequirements(String importerIso2, String originIso2, String hsCode, String productDescription) {
        StringBuilder licensing = new StringBuilder();
        
        licensing.append("Import Licensing Assessment:\n\n");
        
        // Check for controlled products
        List<String> licenses = getRequiredLicenses(importerIso2, hsCode);
        
        if (licenses.isEmpty()) {
            licensing.append("✅ No special import licenses required\n");
            licensing.append("Standard import procedures apply\n");
        } else {
            licensing.append("⚠️ Special licenses/permits required:\n\n");
            for (String license : licenses) {
                licensing.append("• ").append(license).append("\n");
            }
        }
        
        // Country-specific licensing authorities
        licensing.append("\nLicensing Authorities:\n");
        switch (importerIso2) {
            case "US" -> {
                licensing.append("- FDA (food, drugs, medical devices)\n");
                licensing.append("- FCC (telecommunications equipment)\n");
                licensing.append("- DOT (transportation equipment)\n");
                licensing.append("- CPSC (consumer products)\n");
                licensing.append("- EPA (chemicals, pesticides)\n");
            }
            case "CA" -> {
                licensing.append("- Health Canada (health products)\n");
                licensing.append("- CFIA (food, agriculture)\n");
                licensing.append("- ISED (telecommunications)\n");
                licensing.append("- Transport Canada (transportation)\n");
            }
            case "GB" -> {
                licensing.append("- MHRA (medicines, medical devices)\n");
                licensing.append("- FSA (food safety)\n");
                licensing.append("- Ofcom (telecommunications)\n");
                licensing.append("- HSE (chemicals, workplace safety)\n");
            }
        }
        
        // Application timelines
        if (!licenses.isEmpty()) {
            licensing.append("\nTypical Processing Times:\n");
            licensing.append("- Standard permits: 2-6 weeks\n");
            licensing.append("- Complex applications: 2-6 months\n");
            licensing.append("- Expedited processing: +50-100% fees\n");
        }
        
        return licensing.toString();
    }
    
    /**
     * Get compliance risk assessment
     */
    private String getComplianceRiskAssessment(String importerIso2, String originIso2, String hsCode, BigDecimal tradeValue) {
        StringBuilder risk = new StringBuilder();
        
        risk.append("Risk Level Assessment:\n\n");
        
        // Calculate overall risk score
        int riskScore = calculateRiskScore(importerIso2, originIso2, hsCode, tradeValue);
        String riskLevel = getRiskLevel(riskScore);
        
        risk.append("Overall Risk Level: ").append(riskLevel).append(" (Score: ").append(riskScore).append("/100)\n\n");
        
        // Risk factors breakdown
        risk.append("Risk Factors Analysis:\n\n");
        
        // Country risk
        int countryRisk = getCountryRisk(originIso2);
        risk.append("1. Country Risk (").append(originIso2).append("): ").append(getRiskDescription(countryRisk)).append("\n");
        risk.append("   - Political stability, trade relations, sanctions status\n\n");
        
        // Product risk
        int productRisk = getProductRisk(hsCode);
        risk.append("2. Product Risk (HS ").append(hsCode).append("): ").append(getRiskDescription(productRisk)).append("\n");
        risk.append("   - Controlled substances, safety requirements, complexity\n\n");
        
        // Value risk
        int valueRisk = getValueRisk(tradeValue);
        risk.append("3. Value Risk: ").append(getRiskDescription(valueRisk)).append("\n");
        risk.append("   - Customs scrutiny level, bond requirements, penalties\n\n");
        
        // Compliance history risk
        risk.append("4. Compliance History: Medium (assumed)\n");
        risk.append("   - Previous violations, audit history, trusted trader status\n\n");
        
        // Risk mitigation recommendations
        risk.append("Risk Mitigation Priorities:\n");
        if (riskScore >= 70) {
            risk.append("🔴 HIGH PRIORITY:\n");
            risk.append("- Engage experienced customs broker\n");
            risk.append("- Conduct pre-import compliance review\n");
            risk.append("- Consider insurance coverage\n");
            risk.append("- Implement enhanced documentation controls\n");
        } else if (riskScore >= 40) {
            risk.append("🟡 MEDIUM PRIORITY:\n");
            risk.append("- Review documentation accuracy\n");
            risk.append("- Verify classification and valuation\n");
            risk.append("- Monitor regulatory changes\n");
        } else {
            risk.append("🟢 LOW PRIORITY:\n");
            risk.append("- Standard compliance procedures sufficient\n");
            risk.append("- Regular monitoring recommended\n");
        }
        
        return risk.toString();
    }
    
    /**
     * Get compliance recommendations
     */
    private String getComplianceRecommendations(String importerIso2, String originIso2, String hsCode, 
                                               BigDecimal tradeValue, String shipmentFrequency) {
        StringBuilder recommendations = new StringBuilder();
        
        recommendations.append("Compliance Best Practices:\n\n");
        
        // Documentation recommendations
        recommendations.append("1. Documentation Management:\n");
        recommendations.append("   - Maintain complete records for 5+ years\n");
        recommendations.append("   - Implement document version control\n");
        recommendations.append("   - Use electronic filing systems where available\n");
        recommendations.append("   - Regular document accuracy audits\n\n");
        
        // Process recommendations
        recommendations.append("2. Process Optimization:\n");
        recommendations.append("   - Establish standard operating procedures\n");
        recommendations.append("   - Train staff on compliance requirements\n");
        recommendations.append("   - Implement quality control checkpoints\n");
        recommendations.append("   - Regular compliance training updates\n\n");
        
        // Technology recommendations
        recommendations.append("3. Technology Solutions:\n");
        recommendations.append("   - Automated customs filing systems\n");
        recommendations.append("   - Trade compliance software\n");
        recommendations.append("   - Electronic document management\n");
        recommendations.append("   - Real-time regulatory monitoring\n\n");
        
        // Relationship management
        recommendations.append("4. Stakeholder Management:\n");
        recommendations.append("   - Establish customs broker relationships\n");
        recommendations.append("   - Maintain government agency contacts\n");
        recommendations.append("   - Join trade associations\n");
        recommendations.append("   - Regular compliance consultations\n\n");
        
        // Monitoring and review
        recommendations.append("5. Continuous Improvement:\n");
        recommendations.append("   - Monthly compliance reviews\n");
        recommendations.append("   - Annual compliance audits\n");
        recommendations.append("   - Regulatory change monitoring\n");
        recommendations.append("   - Performance metrics tracking\n");
        
        return recommendations.toString();
    }
    
    // Helper methods for compliance analysis
    
    private boolean isMultilateralAgreement(Agreement agreement, String originIso2) {
        String agreementName = agreement.getName().toUpperCase();
        return agreementName.contains("WTO") || 
               agreementName.contains("MULTILATERAL") ||
               agreementName.contains("REGIONAL");
    }
    
    private String getProductSpecificRegulations(String importerIso2, String hsCode) {
        if (hsCode.length() < 2) return "";
        
        String chapter = hsCode.substring(0, 2);
        StringBuilder regulations = new StringBuilder();
        
        switch (chapter) {
            case "01", "02", "03", "04", "05" -> regulations.append("- Food safety regulations apply\n- Veterinary health certificates may be required\n");
            case "30" -> regulations.append("- Pharmaceutical regulations apply\n- Drug registration may be required\n");
            case "84", "85" -> regulations.append("- Safety standards and certifications\n- Electromagnetic compatibility requirements\n");
            case "87" -> regulations.append("- Vehicle safety standards\n- Emissions compliance required\n");
            case "90" -> regulations.append("- Medical device regulations\n- FDA/CE marking requirements\n");
        }
        
        return regulations.toString();
    }
    
    private String getImportControls(String importerIso2, String originIso2, String hsCode) {
        StringBuilder controls = new StringBuilder();
        
        // Check for common import restrictions
        if (isRestrictedProduct(hsCode)) {
            controls.append("- Product subject to import restrictions\n");
        }
        
        if (isControlledCountry(originIso2)) {
            controls.append("- Origin country subject to trade controls\n");
        }
        
        if (requiresQuota(importerIso2, hsCode)) {
            controls.append("- Product subject to import quotas\n");
        }
        
        return controls.toString();
    }
    
    private boolean needsISF(String hsCode) {
        // Simplified logic - in reality would check specific product categories
        return !hsCode.startsWith("98") && !hsCode.startsWith("99"); // Exclude personal effects and special categories
    }
    
    private String getProductSpecificDocuments(String importerIso2, String hsCode) {
        if (hsCode.length() < 2) return "";
        
        String chapter = hsCode.substring(0, 2);
        StringBuilder docs = new StringBuilder();
        
        switch (chapter) {
            case "01", "02", "03", "04", "05" -> docs.append("✓ Health Certificate\n✓ Veterinary Certificate\n");
            case "30" -> docs.append("✓ Drug Registration Certificate\n✓ Good Manufacturing Practice Certificate\n");
            case "84", "85" -> docs.append("✓ CE Marking Certificate\n✓ Safety Test Reports\n");
            case "87" -> docs.append("✓ Vehicle Conformity Certificate\n✓ Emissions Test Certificate\n");
            case "90" -> docs.append("✓ Medical Device Registration\n✓ Quality System Certificate\n");
        }
        
        return docs.toString();
    }
    
    private String getDocumentPreparationGuidance(String importerIso2, String hsCode) {
        StringBuilder guidance = new StringBuilder();
        
        guidance.append("Document Accuracy Requirements:\n");
        guidance.append("- All documents must be consistent and accurate\n");
        guidance.append("- Use official letterhead and signatures\n");
        guidance.append("- Include all required data elements\n");
        guidance.append("- Ensure proper translation if required\n\n");
        
        guidance.append("Common Documentation Errors:\n");
        guidance.append("- Inconsistent product descriptions\n");
        guidance.append("- Incorrect HS code classification\n");
        guidance.append("- Missing or invalid certificates\n");
        guidance.append("- Incomplete commercial invoice details\n");
        
        return guidance.toString();
    }
    
    private String getProcessOptimization(String importerIso2, String shipmentFrequency) {
        StringBuilder optimization = new StringBuilder();
        
        if ("daily".equals(shipmentFrequency) || "weekly".equals(shipmentFrequency)) {
            optimization.append("High-Frequency Shipment Optimizations:\n");
            optimization.append("- Apply for Authorized Economic Operator status\n");
            optimization.append("- Use automated clearance systems\n");
            optimization.append("- Establish dedicated customs lanes\n");
            optimization.append("- Implement predictive analytics\n");
        } else {
            optimization.append("Standard Process Optimizations:\n");
            optimization.append("- Use electronic filing systems\n");
            optimization.append("- Prepare documents in advance\n");
            optimization.append("- Maintain consistent procedures\n");
            optimization.append("- Monitor processing times\n");
        }
        
        return optimization.toString();
    }
    
    private String getRiskMitigationStrategies(String importerIso2, String originIso2, String hsCode) {
        StringBuilder strategies = new StringBuilder();
        
        strategies.append("Specific Risk Mitigation Actions:\n\n");
        
        strategies.append("1. Pre-Import Planning:\n");
        strategies.append("   - Conduct compliance gap analysis\n");
        strategies.append("   - Verify all regulatory requirements\n");
        strategies.append("   - Prepare contingency plans\n\n");
        
        strategies.append("2. Documentation Controls:\n");
        strategies.append("   - Implement document review process\n");
        strategies.append("   - Use compliance checklists\n");
        strategies.append("   - Maintain audit trails\n\n");
        
        strategies.append("3. Monitoring and Response:\n");
        strategies.append("   - Track regulatory changes\n");
        strategies.append("   - Monitor compliance metrics\n");
        strategies.append("   - Establish incident response procedures\n");
        
        return strategies.toString();
    }
    
    private String getLicensingProcessGuidance(String importerIso2, String hsCode) {
        StringBuilder guidance = new StringBuilder();
        
        guidance.append("License Application Process:\n\n");
        guidance.append("1. Determine Requirements:\n");
        guidance.append("   - Identify applicable regulations\n");
        guidance.append("   - Contact relevant agencies\n");
        guidance.append("   - Review application requirements\n\n");
        
        guidance.append("2. Prepare Application:\n");
        guidance.append("   - Complete all required forms\n");
        guidance.append("   - Gather supporting documentation\n");
        guidance.append("   - Pay applicable fees\n\n");
        
        guidance.append("3. Submit and Track:\n");
        guidance.append("   - Submit through official channels\n");
        guidance.append("   - Track application status\n");
        guidance.append("   - Respond to agency requests promptly\n");
        
        return guidance.toString();
    }
    
    private List<String> getRequiredLicenses(String importerIso2, String hsCode) {
        List<String> licenses = new ArrayList<>();
        
        if (hsCode.length() < 2) return licenses;
        
        String chapter = hsCode.substring(0, 2);
        
        switch (chapter) {
            case "30" -> licenses.add("Drug Import License");
            case "93" -> licenses.add("Firearms Import Permit");
            case "90" -> {
                if (hsCode.startsWith("9018") || hsCode.startsWith("9019")) {
                    licenses.add("Medical Device Registration");
                }
            }
            case "85" -> {
                if (hsCode.startsWith("8517")) {
                    licenses.add("Telecommunications Equipment Approval");
                }
            }
        }
        
        return licenses;
    }
    
    private int calculateRiskScore(String importerIso2, String originIso2, String hsCode, BigDecimal tradeValue) {
        int score = 0;
        
        score += getCountryRisk(originIso2);
        score += getProductRisk(hsCode);
        score += getValueRisk(tradeValue);
        score += 20; // Base compliance risk
        
        return Math.min(100, score);
    }
    
    private int getCountryRisk(String originIso2) {
        // Simplified country risk assessment
        return switch (originIso2) {
            case "CN", "RU", "IR", "KP" -> 30; // Higher risk countries
            case "MX", "CA", "GB", "DE", "JP" -> 5; // Lower risk countries
            default -> 15; // Medium risk
        };
    }
    
    private int getProductRisk(String hsCode) {
        if (hsCode.length() < 2) return 10;
        
        String chapter = hsCode.substring(0, 2);
        return switch (chapter) {
            case "30", "93" -> 25; // High risk (drugs, weapons)
            case "84", "85", "90" -> 15; // Medium risk (machinery, electronics, instruments)
            case "61", "62", "64" -> 5; // Low risk (textiles, footwear)
            default -> 10; // Standard risk
        };
    }
    
    private int getValueRisk(BigDecimal tradeValue) {
        if (tradeValue == null) return 10;
        
        if (tradeValue.compareTo(new BigDecimal("100000")) >= 0) {
            return 20; // High value
        } else if (tradeValue.compareTo(new BigDecimal("10000")) >= 0) {
            return 10; // Medium value
        } else {
            return 5; // Low value
        }
    }
    
    private String getRiskLevel(int score) {
        if (score >= 70) return "HIGH";
        if (score >= 40) return "MEDIUM";
        return "LOW";
    }
    
    private String getRiskDescription(int score) {
        if (score >= 25) return "High";
        if (score >= 15) return "Medium";
        if (score >= 10) return "Low";
        return "Very Low";
    }
    
    private boolean isRestrictedProduct(String hsCode) {
        // Simplified check for restricted products
        return hsCode.startsWith("93") || // Weapons
               hsCode.startsWith("30") || // Pharmaceuticals
               hsCode.startsWith("2208"); // Alcohol
    }
    
    private boolean isControlledCountry(String originIso2) {
        // Simplified check for countries with trade controls
        return Arrays.asList("IR", "KP", "SY", "CU").contains(originIso2);
    }
    
    private boolean requiresQuota(String importerIso2, String hsCode) {
        // Simplified quota check
        return hsCode.startsWith("61") || hsCode.startsWith("62") || // Textiles
               hsCode.startsWith("17"); // Sugar
    }
}