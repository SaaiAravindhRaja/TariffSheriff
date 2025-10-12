package com.tariffsheriff.backend.ai.planning;

import com.tariffsheriff.backend.ai.context.QueryContext;
import com.tariffsheriff.backend.ai.context.ContextualEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Advanced query analyzer for complex query understanding and intent recognition
 */
@Service
public class QueryAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryAnalyzer.class);
    
    // Enhanced patterns for query analysis
    private static final Pattern COMPARISON_PATTERN = Pattern.compile(
        "\\b(compare|vs|versus|difference|better|cheaper|more expensive|against|between)\\b", 
        Pattern.CASE_INSENSITIVE);
    
    private static final Pattern COST_ANALYSIS_PATTERN = Pattern.compile(
        "\\b(cost|price|expensive|cheap|total|landed|duty|tariff|fee|charge|calculate)\\b", 
        Pattern.CASE_INSENSITIVE);
    
    private static final Pattern COMPLIANCE_PATTERN = Pattern.compile(
        "\\b(compliance|regulation|legal|requirement|documentation|permit|license|certificate|customs)\\b", 
        Pattern.CASE_INSENSITIVE);
    
    private static final Pattern RISK_PATTERN = Pattern.compile(
        "\\b(risk|danger|problem|issue|disruption|volatility|threat|vulnerability|impact)\\b", 
        Pattern.CASE_INSENSITIVE);
    
    private static final Pattern MARKET_PATTERN = Pattern.compile(
        "\\b(market|trend|opportunity|demand|supply|forecast|analysis|intelligence|data)\\b", 
        Pattern.CASE_INSENSITIVE);
    
    private static final Pattern OPTIMIZATION_PATTERN = Pattern.compile(
        "\\b(optimize|best|efficient|improve|reduce|minimize|maximize|strategy|recommend)\\b", 
        Pattern.CASE_INSENSITIVE);
    
    private static final Pattern SCENARIO_PATTERN = Pattern.compile(
        "\\b(scenario|what if|suppose|assume|consider|alternative|option)\\b", 
        Pattern.CASE_INSENSITIVE);
    
    private static final Pattern CONTEXTUAL_REFERENCE_PATTERN = Pattern.compile(
        "\\b(the|that|this|previous|last|earlier|mentioned|discussed|above)\\s+(option|choice|country|product|analysis|calculation|result)\\b", 
        Pattern.CASE_INSENSITIVE);
    
    // Entity extraction patterns
    private static final Pattern COUNTRY_PATTERN = Pattern.compile(
        "\\b(united states|usa|us|america|china|germany|japan|canada|mexico|united kingdom|uk|britain|france|italy|spain|netherlands|belgium|australia|brazil|india|south korea|korea|singapore|taiwan|switzerland|sweden|norway|denmark|finland|poland|czech republic|hungary|romania|bulgaria|greece|portugal|ireland|austria|luxembourg)\\b", 
        Pattern.CASE_INSENSITIVE);
    
    private static final Pattern PRODUCT_PATTERN = Pattern.compile(
        "\\b(vehicles?|cars?|automobiles?|electronics?|computers?|smartphones?|textiles?|clothing|machinery|equipment|steel|aluminum|plastic|chemicals?|pharmaceuticals?|medical devices?|food|beverages?|furniture|toys|books|paper|wood|lumber|oil|gas|coal|minerals?)\\b", 
        Pattern.CASE_INSENSITIVE);
    
    private static final Pattern HS_CODE_PATTERN = Pattern.compile(
        "\\b(hs\\s*code|harmonized\\s*system|classification)\\s*:?\\s*(\\d{4,10})\\b|\\b(\\d{4}\\.\\d{2}\\.\\d{2})\\b", 
        Pattern.CASE_INSENSITIVE);
    
    private static final Pattern MONETARY_PATTERN = Pattern.compile(
        "\\$([\\d,]+(?:\\.\\d{2})?)|([\\d,]+(?:\\.\\d{2})?)\\s*(dollars?|usd|euros?|eur|pounds?|gbp|yen|jpy)", 
        Pattern.CASE_INSENSITIVE);
    
    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile(
        "(\\d+(?:\\.\\d+)?)\\s*%", 
        Pattern.CASE_INSENSITIVE);
    
    private static final Pattern QUANTITY_PATTERN = Pattern.compile(
        "(\\d+(?:,\\d{3})*(?:\\.\\d+)?)\\s*(units?|pieces?|tons?|kg|pounds?|lbs|containers?|shipments?)", 
        Pattern.CASE_INSENSITIVE);
    
    /**
     * Perform comprehensive query analysis with advanced understanding
     */
    public QueryAnalysis performAdvancedAnalysis(String query, QueryContext context) {
        try {
            logger.debug("Performing advanced analysis for query: {}", query);
            
            // Normalize query
            String normalizedQuery = normalizeQuery(query);
            
            // Extract entities with confidence scoring
            List<QueryEntity> entities = extractEntitiesWithConfidence(normalizedQuery, context);
            
            // Determine intent hierarchy
            QueryIntent primaryIntent = determinePrimaryIntent(normalizedQuery, entities);
            List<QueryIntent> secondaryIntents = identifySecondaryIntents(normalizedQuery, entities);
            
            // Assess complexity with multiple factors
            ComplexityLevel complexity = assessAdvancedComplexity(normalizedQuery, entities, context);
            
            // Identify required agents with dependency analysis
            List<AgentType> requiredAgents = identifyRequiredAgentsAdvanced(
                primaryIntent, secondaryIntents, normalizedQuery, entities);
            
            // Check for contextual references
            boolean hasContextualReferences = detectContextualReferences(normalizedQuery, context);
            
            // Decompose multi-part queries
            List<String> queryParts = decomposeMutliPartQuery(normalizedQuery);
            
            // Validate query completeness
            QueryValidationResult validation = validateQueryCompleteness(normalizedQuery, entities);
            
            // Create enhanced analysis
            EnhancedQueryAnalysis analysis = new EnhancedQueryAnalysis(
                query,
                normalizedQuery,
                primaryIntent,
                secondaryIntents,
                complexity,
                requiredAgents,
                entities,
                hasContextualReferences,
                queryParts,
                validation
            );
            
            logger.debug("Advanced analysis complete: intent={}, complexity={}, agents={}, entities={}", 
                    primaryIntent, complexity, requiredAgents.size(), entities.size());
            
            return analysis;
            
        } catch (Exception e) {
            logger.error("Error in advanced query analysis", e);
            
            // Return basic analysis on error
            return createFallbackAnalysis(query);
        }
    }
    
    /**
     * Normalize query text for better analysis
     */
    private String normalizeQuery(String query) {
        if (query == null) return "";
        
        // Remove extra whitespace
        String normalized = query.trim().replaceAll("\\s+", " ");
        
        // Expand common abbreviations
        normalized = normalized.replaceAll("\\bUS\\b", "United States");
        normalized = normalized.replaceAll("\\bUK\\b", "United Kingdom");
        normalized = normalized.replaceAll("\\bEU\\b", "European Union");
        normalized = normalized.replaceAll("\\bFTA\\b", "Free Trade Agreement");
        
        // Standardize comparison terms
        normalized = normalized.replaceAll("\\bvs\\b", "versus");
        
        return normalized;
    }
    
    /**
     * Extract entities with confidence scoring
     */
    private List<QueryEntity> extractEntitiesWithConfidence(String query, QueryContext context) {
        List<QueryEntity> entities = new ArrayList<>();
        
        try {
            // Extract countries
            entities.addAll(extractCountriesWithConfidence(query));
            
            // Extract products
            entities.addAll(extractProductsWithConfidence(query));
            
            // Extract HS codes
            entities.addAll(extractHsCodesWithConfidence(query));
            
            // Extract monetary amounts
            entities.addAll(extractMonetaryAmountsWithConfidence(query));
            
            // Extract percentages (tariff rates)
            entities.addAll(extractPercentagesWithConfidence(query));
            
            // Extract quantities
            entities.addAll(extractQuantitiesWithConfidence(query));
            
            // Add entities from context
            if (context != null) {
                entities.addAll(extractContextualEntities(context));
            }
            
            // Remove duplicates and sort by confidence
            entities = entities.stream()
                    .collect(Collectors.toMap(
                        e -> e.getType() + ":" + e.getValue().toLowerCase(),
                        e -> e,
                        (e1, e2) -> e1.getConfidence() > e2.getConfidence() ? e1 : e2))
                    .values()
                    .stream()
                    .sorted((e1, e2) -> Double.compare(e2.getConfidence(), e1.getConfidence()))
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.warn("Error extracting entities", e);
        }
        
        return entities;
    }
    
    /**
     * Extract countries with confidence scoring
     */
    private List<QueryEntity> extractCountriesWithConfidence(String query) {
        List<QueryEntity> countries = new ArrayList<>();
        
        Matcher matcher = COUNTRY_PATTERN.matcher(query);
        while (matcher.find()) {
            String country = matcher.group().trim();
            double confidence = calculateCountryConfidence(country, query);
            countries.add(new QueryEntity("COUNTRY", country, confidence));
        }
        
        return countries;
    }
    
    /**
     * Extract products with confidence scoring
     */
    private List<QueryEntity> extractProductsWithConfidence(String query) {
        List<QueryEntity> products = new ArrayList<>();
        
        Matcher matcher = PRODUCT_PATTERN.matcher(query);
        while (matcher.find()) {
            String product = matcher.group().trim();
            double confidence = calculateProductConfidence(product, query);
            products.add(new QueryEntity("PRODUCT", product, confidence));
        }
        
        return products;
    }
    
    /**
     * Extract HS codes with confidence scoring
     */
    private List<QueryEntity> extractHsCodesWithConfidence(String query) {
        List<QueryEntity> hsCodes = new ArrayList<>();
        
        Matcher matcher = HS_CODE_PATTERN.matcher(query);
        while (matcher.find()) {
            String hsCode = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            if (hsCode != null) {
                double confidence = 0.95; // HS codes are very specific
                hsCodes.add(new QueryEntity("HS_CODE", hsCode, confidence));
            }
        }
        
        return hsCodes;
    }
    
    /**
     * Extract monetary amounts with confidence scoring
     */
    private List<QueryEntity> extractMonetaryAmountsWithConfidence(String query) {
        List<QueryEntity> amounts = new ArrayList<>();
        
        Matcher matcher = MONETARY_PATTERN.matcher(query);
        while (matcher.find()) {
            String amount = matcher.group();
            double confidence = 0.9;
            amounts.add(new QueryEntity("MONETARY_AMOUNT", amount, confidence));
        }
        
        return amounts;
    }
    
    /**
     * Extract percentages with confidence scoring
     */
    private List<QueryEntity> extractPercentagesWithConfidence(String query) {
        List<QueryEntity> percentages = new ArrayList<>();
        
        Matcher matcher = PERCENTAGE_PATTERN.matcher(query);
        while (matcher.find()) {
            String percentage = matcher.group();
            double confidence = 0.85;
            percentages.add(new QueryEntity("PERCENTAGE", percentage, confidence));
        }
        
        return percentages;
    }
    
    /**
     * Extract quantities with confidence scoring
     */
    private List<QueryEntity> extractQuantitiesWithConfidence(String query) {
        List<QueryEntity> quantities = new ArrayList<>();
        
        Matcher matcher = QUANTITY_PATTERN.matcher(query);
        while (matcher.find()) {
            String quantity = matcher.group();
            double confidence = 0.8;
            quantities.add(new QueryEntity("QUANTITY", quantity, confidence));
        }
        
        return quantities;
    }
    
    /**
     * Extract entities from context
     */
    private List<QueryEntity> extractContextualEntities(QueryContext context) {
        List<QueryEntity> entities = new ArrayList<>();
        
        if (context.getReferencedEntities() != null) {
            for (ContextualEntity contextEntity : context.getReferencedEntities()) {
                entities.add(new QueryEntity(
                    contextEntity.getType(),
                    contextEntity.getValue(),
                    0.7 // Context entities have moderate confidence
                ));
            }
        }
        
        return entities;
    }
    
    /**
     * Calculate confidence for country extraction
     */
    private double calculateCountryConfidence(String country, String query) {
        double confidence = 0.8; // Base confidence
        
        // Increase confidence if country is mentioned with trade-related terms
        String lowerQuery = query.toLowerCase();
        if (lowerQuery.contains("import from " + country.toLowerCase()) ||
            lowerQuery.contains("export to " + country.toLowerCase()) ||
            lowerQuery.contains("trade with " + country.toLowerCase())) {
            confidence += 0.15;
        }
        
        // Increase confidence for full country names
        if (country.length() > 5) {
            confidence += 0.05;
        }
        
        return Math.min(confidence, 1.0);
    }
    
    /**
     * Calculate confidence for product extraction
     */
    private double calculateProductConfidence(String product, String query) {
        double confidence = 0.7; // Base confidence
        
        // Increase confidence if product is mentioned with trade-related terms
        String lowerQuery = query.toLowerCase();
        if (lowerQuery.contains("tariff on " + product.toLowerCase()) ||
            lowerQuery.contains("import " + product.toLowerCase()) ||
            lowerQuery.contains("export " + product.toLowerCase())) {
            confidence += 0.2;
        }
        
        return Math.min(confidence, 1.0);
    }
    
    /**
     * Determine primary intent with enhanced logic
     */
    private QueryIntent determinePrimaryIntent(String query, List<QueryEntity> entities) {
        Map<QueryIntent, Double> intentScores = new HashMap<>();
        
        // Score each intent based on patterns and entities
        intentScores.put(QueryIntent.COMPARISON, scoreComparisonIntent(query, entities));
        intentScores.put(QueryIntent.COST_ANALYSIS, scoreCostAnalysisIntent(query, entities));
        intentScores.put(QueryIntent.COMPLIANCE_CHECK, scoreComplianceIntent(query, entities));
        intentScores.put(QueryIntent.RISK_ASSESSMENT, scoreRiskIntent(query, entities));
        intentScores.put(QueryIntent.MARKET_ANALYSIS, scoreMarketIntent(query, entities));
        intentScores.put(QueryIntent.OPTIMIZATION, scoreOptimizationIntent(query, entities));
        intentScores.put(QueryIntent.TARIFF_LOOKUP, scoreTariffIntent(query, entities));
        intentScores.put(QueryIntent.PRODUCT_CLASSIFICATION, scoreClassificationIntent(query, entities));
        
        // Return intent with highest score
        return intentScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(QueryIntent.GENERAL_INQUIRY);
    }
    
    /**
     * Score comparison intent
     */
    private double scoreComparisonIntent(String query, List<QueryEntity> entities) {
        double score = 0.0;
        
        if (COMPARISON_PATTERN.matcher(query).find()) score += 0.8;
        
        // Multiple countries or products suggest comparison
        long countryCount = entities.stream().filter(e -> "COUNTRY".equals(e.getType())).count();
        long productCount = entities.stream().filter(e -> "PRODUCT".equals(e.getType())).count();
        
        if (countryCount > 1) score += 0.6;
        if (productCount > 1) score += 0.4;
        
        return score;
    }
    
    /**
     * Score cost analysis intent
     */
    private double scoreCostAnalysisIntent(String query, List<QueryEntity> entities) {
        double score = 0.0;
        
        if (COST_ANALYSIS_PATTERN.matcher(query).find()) score += 0.7;
        
        // Monetary amounts suggest cost analysis
        if (entities.stream().anyMatch(e -> "MONETARY_AMOUNT".equals(e.getType()))) {
            score += 0.5;
        }
        
        return score;
    }
    
    /**
     * Score compliance intent
     */
    private double scoreComplianceIntent(String query, List<QueryEntity> entities) {
        double score = 0.0;
        
        if (COMPLIANCE_PATTERN.matcher(query).find()) score += 0.8;
        
        return score;
    }
    
    /**
     * Score risk assessment intent
     */
    private double scoreRiskIntent(String query, List<QueryEntity> entities) {
        double score = 0.0;
        
        if (RISK_PATTERN.matcher(query).find()) score += 0.8;
        
        return score;
    }
    
    /**
     * Score market analysis intent
     */
    private double scoreMarketIntent(String query, List<QueryEntity> entities) {
        double score = 0.0;
        
        if (MARKET_PATTERN.matcher(query).find()) score += 0.8;
        
        return score;
    }
    
    /**
     * Score optimization intent
     */
    private double scoreOptimizationIntent(String query, List<QueryEntity> entities) {
        double score = 0.0;
        
        if (OPTIMIZATION_PATTERN.matcher(query).find()) score += 0.8;
        
        return score;
    }
    
    /**
     * Score tariff lookup intent
     */
    private double scoreTariffIntent(String query, List<QueryEntity> entities) {
        double score = 0.0;
        
        String lowerQuery = query.toLowerCase();
        if (lowerQuery.contains("tariff") || lowerQuery.contains("duty")) score += 0.7;
        
        return score;
    }
    
    /**
     * Score product classification intent
     */
    private double scoreClassificationIntent(String query, List<QueryEntity> entities) {
        double score = 0.0;
        
        String lowerQuery = query.toLowerCase();
        if (lowerQuery.contains("hs code") || lowerQuery.contains("classification")) score += 0.8;
        
        if (entities.stream().anyMatch(e -> "HS_CODE".equals(e.getType()))) {
            score += 0.6;
        }
        
        return score;
    }
    
    /**
     * Identify secondary intents
     */
    private List<QueryIntent> identifySecondaryIntents(String query, List<QueryEntity> entities) {
        List<QueryIntent> secondaryIntents = new ArrayList<>();
        
        Map<QueryIntent, Double> intentScores = new HashMap<>();
        intentScores.put(QueryIntent.COST_ANALYSIS, scoreCostAnalysisIntent(query, entities));
        intentScores.put(QueryIntent.COMPLIANCE_CHECK, scoreComplianceIntent(query, entities));
        intentScores.put(QueryIntent.RISK_ASSESSMENT, scoreRiskIntent(query, entities));
        intentScores.put(QueryIntent.MARKET_ANALYSIS, scoreMarketIntent(query, entities));
        
        // Add intents with score > 0.3 as secondary intents
        intentScores.entrySet().stream()
                .filter(entry -> entry.getValue() > 0.3)
                .map(Map.Entry::getKey)
                .forEach(secondaryIntents::add);
        
        return secondaryIntents;
    }
    
    /**
     * Assess complexity with multiple factors
     */
    private ComplexityLevel assessAdvancedComplexity(String query, List<QueryEntity> entities, QueryContext context) {
        int complexityScore = 0;
        
        // Length factor
        if (query.length() > 300) complexityScore += 3;
        else if (query.length() > 150) complexityScore += 2;
        else if (query.length() > 75) complexityScore += 1;
        
        // Entity count factor
        complexityScore += Math.min(entities.size() / 2, 3);
        
        // Pattern complexity
        if (COMPARISON_PATTERN.matcher(query).find()) complexityScore += 2;
        if (OPTIMIZATION_PATTERN.matcher(query).find()) complexityScore += 3;
        if (SCENARIO_PATTERN.matcher(query).find()) complexityScore += 2;
        
        // Multiple countries or products
        long countryCount = entities.stream().filter(e -> "COUNTRY".equals(e.getType())).count();
        long productCount = entities.stream().filter(e -> "PRODUCT".equals(e.getType())).count();
        
        if (countryCount > 2) complexityScore += 2;
        if (productCount > 2) complexityScore += 2;
        
        // Context references add complexity
        if (context != null && !context.getReferencedEntities().isEmpty()) {
            complexityScore += 1;
        }
        
        // Multi-part queries
        if (query.split("\\band\\b|\\bor\\b").length > 2) complexityScore += 2;
        
        if (complexityScore >= 8) return ComplexityLevel.HIGH;
        else if (complexityScore >= 4) return ComplexityLevel.MEDIUM;
        else return ComplexityLevel.LOW;
    }
    
    /**
     * Identify required agents with advanced logic
     */
    private List<AgentType> identifyRequiredAgentsAdvanced(QueryIntent primaryIntent, 
                                                          List<QueryIntent> secondaryIntents, 
                                                          String query, List<QueryEntity> entities) {
        Set<AgentType> agents = new HashSet<>();
        
        // Add agent for primary intent
        agents.add(getAgentForIntent(primaryIntent));
        
        // Add agents for secondary intents
        for (QueryIntent intent : secondaryIntents) {
            agents.add(getAgentForIntent(intent));
        }
        
        // Always include tariff analysis for trade queries
        if (isTradeRelatedQuery(query)) {
            agents.add(AgentType.TARIFF_ANALYSIS);
        }
        
        // Add optimization agent for complex multi-entity queries
        if (entities.size() > 4 && (
            entities.stream().filter(e -> "COUNTRY".equals(e.getType())).count() > 1 ||
            entities.stream().filter(e -> "PRODUCT".equals(e.getType())).count() > 1)) {
            agents.add(AgentType.OPTIMIZATION);
        }
        
        return new ArrayList<>(agents);
    }
    
    /**
     * Get appropriate agent for intent
     */
    private AgentType getAgentForIntent(QueryIntent intent) {
        return switch (intent) {
            case TARIFF_LOOKUP, COST_ANALYSIS -> AgentType.TARIFF_ANALYSIS;
            case COMPLIANCE_CHECK -> AgentType.COMPLIANCE;
            case RISK_ASSESSMENT -> AgentType.RISK_ASSESSMENT;
            case MARKET_ANALYSIS -> AgentType.MARKET_INTELLIGENCE;
            case OPTIMIZATION, COMPARISON -> AgentType.OPTIMIZATION;
            case PRODUCT_CLASSIFICATION -> AgentType.TARIFF_ANALYSIS;
            default -> AgentType.TARIFF_ANALYSIS;
        };
    }
    
    /**
     * Check if query is trade-related
     */
    private boolean isTradeRelatedQuery(String query) {
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("trade") || lowerQuery.contains("import") || 
               lowerQuery.contains("export") || lowerQuery.contains("tariff") ||
               lowerQuery.contains("customs") || lowerQuery.contains("duty");
    }
    
    /**
     * Detect contextual references in query
     */
    private boolean detectContextualReferences(String query, QueryContext context) {
        if (CONTEXTUAL_REFERENCE_PATTERN.matcher(query).find()) {
            return true;
        }
        
        // Check for pronouns that might refer to previous context
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("it") || lowerQuery.contains("that") || 
               lowerQuery.contains("this") || lowerQuery.contains("them");
    }
    
    /**
     * Decompose multi-part queries
     */
    private List<String> decomposeMutliPartQuery(String query) {
        List<String> parts = new ArrayList<>();
        
        // Split on common conjunctions
        String[] conjunctions = {"and", "or", "also", "additionally", "furthermore", "moreover"};
        
        String[] segments = query.split("\\b(?:" + String.join("|", conjunctions) + ")\\b", -1);
        
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (!trimmed.isEmpty() && trimmed.length() > 10) {
                parts.add(trimmed);
            }
        }
        
        // If no meaningful decomposition, return original query
        if (parts.size() <= 1) {
            parts.clear();
            parts.add(query);
        }
        
        return parts;
    }
    
    /**
     * Validate query completeness and suggest improvements
     */
    private QueryValidationResult validateQueryCompleteness(String query, List<QueryEntity> entities) {
        List<String> missingElements = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        
        // Check for essential trade elements
        boolean hasCountry = entities.stream().anyMatch(e -> "COUNTRY".equals(e.getType()));
        boolean hasProduct = entities.stream().anyMatch(e -> "PRODUCT".equals(e.getType()));
        
        String lowerQuery = query.toLowerCase();
        boolean isTariffQuery = lowerQuery.contains("tariff") || lowerQuery.contains("duty");
        boolean isComparisonQuery = COMPARISON_PATTERN.matcher(query).find();
        
        if (isTariffQuery && !hasCountry) {
            missingElements.add("country");
            suggestions.add("Specify the country you're importing from or exporting to");
        }
        
        if (isTariffQuery && !hasProduct) {
            missingElements.add("product");
            suggestions.add("Specify the product or HS code you're interested in");
        }
        
        if (isComparisonQuery && entities.stream().filter(e -> "COUNTRY".equals(e.getType())).count() < 2) {
            missingElements.add("comparison_targets");
            suggestions.add("Specify at least two countries or options to compare");
        }
        
        boolean isComplete = missingElements.isEmpty();
        double completenessScore = calculateCompletenessScore(query, entities, missingElements);
        
        return new QueryValidationResult(isComplete, completenessScore, missingElements, suggestions);
    }
    
    /**
     * Calculate completeness score
     */
    private double calculateCompletenessScore(String query, List<QueryEntity> entities, List<String> missingElements) {
        double score = 1.0;
        
        // Deduct for missing elements
        score -= missingElements.size() * 0.2;
        
        // Bonus for having entities
        score += Math.min(entities.size() * 0.1, 0.3);
        
        // Bonus for query length (more detail)
        if (query.length() > 50) score += 0.1;
        if (query.length() > 100) score += 0.1;
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Create fallback analysis for error cases
     */
    private QueryAnalysis createFallbackAnalysis(String query) {
        return new QueryAnalysis(
            query,
            QueryIntent.GENERAL_INQUIRY,
            Collections.emptyList(),
            ComplexityLevel.LOW,
            List.of(AgentType.TARIFF_ANALYSIS),
            Collections.emptyList()
        );
    }
    
    /**
     * Enhanced query analysis result
     */
    public static class EnhancedQueryAnalysis extends QueryAnalysis {
        private final String normalizedQuery;
        private final boolean hasContextualReferences;
        private final List<String> queryParts;
        private final QueryValidationResult validation;
        
        public EnhancedQueryAnalysis(String query, String normalizedQuery, QueryIntent primaryIntent, 
                                   List<QueryIntent> secondaryIntents, ComplexityLevel complexity,
                                   List<AgentType> requiredAgents, List<QueryEntity> entities,
                                   boolean hasContextualReferences, List<String> queryParts,
                                   QueryValidationResult validation) {
            super(query, primaryIntent, secondaryIntents, complexity, requiredAgents, entities);
            this.normalizedQuery = normalizedQuery;
            this.hasContextualReferences = hasContextualReferences;
            this.queryParts = new ArrayList<>(queryParts);
            this.validation = validation;
        }
        
        // Additional getters
        public String getNormalizedQuery() { return normalizedQuery; }
        public boolean hasContextualReferences() { return hasContextualReferences; }
        public List<String> getQueryParts() { return new ArrayList<>(queryParts); }
        public QueryValidationResult getValidation() { return validation; }
        
        public boolean isMultiPart() { return queryParts.size() > 1; }
        public boolean isComplete() { return validation.isComplete(); }
        public List<String> getSuggestions() { return validation.getSuggestions(); }
    }
    
    /**
     * Query validation result
     */
    public static class QueryValidationResult {
        private final boolean complete;
        private final double completenessScore;
        private final List<String> missingElements;
        private final List<String> suggestions;
        
        public QueryValidationResult(boolean complete, double completenessScore, 
                                   List<String> missingElements, List<String> suggestions) {
            this.complete = complete;
            this.completenessScore = completenessScore;
            this.missingElements = new ArrayList<>(missingElements);
            this.suggestions = new ArrayList<>(suggestions);
        }
        
        // Getters
        public boolean isComplete() { return complete; }
        public double getCompletenessScore() { return completenessScore; }
        public List<String> getMissingElements() { return new ArrayList<>(missingElements); }
        public List<String> getSuggestions() { return new ArrayList<>(suggestions); }
    }
}