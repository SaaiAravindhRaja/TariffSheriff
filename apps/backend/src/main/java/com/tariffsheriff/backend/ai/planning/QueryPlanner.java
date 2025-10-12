package com.tariffsheriff.backend.ai.planning;

import com.tariffsheriff.backend.ai.context.QueryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Analyzes queries and creates execution plans for multi-agent processing
 */
@Service
public class QueryPlanner {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryPlanner.class);
    
    // Patterns for query analysis
    private static final Pattern COMPARISON_PATTERN = Pattern.compile("\\b(compare|vs|versus|difference|better|cheaper)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern COST_PATTERN = Pattern.compile("\\b(cost|price|expensive|cheap|total|landed)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPLIANCE_PATTERN = Pattern.compile("\\b(compliance|regulation|legal|requirement|documentation|permit)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern RISK_PATTERN = Pattern.compile("\\b(risk|danger|problem|issue|disruption|volatility)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MARKET_PATTERN = Pattern.compile("\\b(market|trend|opportunity|demand|supply|forecast)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPTIMIZATION_PATTERN = Pattern.compile("\\b(optimize|best|efficient|improve|reduce|minimize)\\b", Pattern.CASE_INSENSITIVE);
    
    /**
     * Analyze query and determine intent, complexity, and required agents
     */
    public QueryAnalysis analyzeQuery(String query, QueryContext context) {
        try {
            logger.debug("Analyzing query: {}", query);
            
            // Determine primary intent
            QueryIntent primaryIntent = determinePrimaryIntent(query);
            
            // Identify secondary intents
            List<QueryIntent> secondaryIntents = identifySecondaryIntents(query);
            
            // Assess complexity
            ComplexityLevel complexity = assessComplexity(query, context);
            
            // Identify required agents
            List<AgentType> requiredAgents = identifyRequiredAgents(primaryIntent, secondaryIntents, query);
            
            // Extract entities
            List<QueryEntity> entities = extractQueryEntities(query, context);
            
            QueryAnalysis analysis = new QueryAnalysis(
                query,
                primaryIntent,
                secondaryIntents,
                complexity,
                requiredAgents,
                entities
            );
            
            logger.debug("Query analysis complete: intent={}, complexity={}, agents={}", 
                    primaryIntent, complexity, requiredAgents.size());
            
            return analysis;
            
        } catch (Exception e) {
            logger.error("Error analyzing query", e);
            
            // Return basic analysis on error
            return new QueryAnalysis(
                query,
                QueryIntent.GENERAL_INQUIRY,
                Collections.emptyList(),
                ComplexityLevel.LOW,
                List.of(AgentType.TARIFF_ANALYSIS),
                Collections.emptyList()
            );
        }
    }
    
    /**
     * Create execution plan based on query analysis
     */
    public ExecutionPlan createExecutionPlan(String query, QueryContext context) {
        try {
            QueryAnalysis analysis = analyzeQuery(query, context);
            
            String planId = UUID.randomUUID().toString();
            List<ExecutionPlan.ExecutionStep> steps = new ArrayList<>();
            List<ExecutionPlan.StepDependency> dependencies = new ArrayList<>();
            
            // Create steps based on required agents
            int stepOrder = 1;
            for (AgentType agentType : analysis.getRequiredAgents()) {
                ExecutionPlan.ExecutionStep step = createExecutionStep(
                    stepOrder++, agentType, analysis, context);
                steps.add(step);
            }
            
            // Add dependencies between steps
            dependencies.addAll(createStepDependencies(steps, analysis));
            
            // Estimate duration
            int estimatedDuration = estimateExecutionDuration(steps, analysis.getComplexity());
            
            // Determine resource requirements
            List<ExecutionPlan.ResourceRequirement> resources = determineResourceRequirements(steps);
            
            ExecutionPlan plan = new ExecutionPlan(
                planId,
                query,
                steps,
                dependencies,
                estimatedDuration,
                resources
            );
            
            // Optimize plan
            optimizePlan(plan);
            
            logger.debug("Created execution plan {} with {} steps", planId, steps.size());
            
            return plan;
            
        } catch (Exception e) {
            logger.error("Error creating execution plan", e);
            
            // Return minimal plan on error
            return createMinimalPlan(query);
        }
    }
    
    /**
     * Determine primary intent from query
     */
    private QueryIntent determinePrimaryIntent(String query) {
        String lowerQuery = query.toLowerCase();
        
        if (COMPARISON_PATTERN.matcher(query).find()) {
            return QueryIntent.COMPARISON;
        } else if (COST_PATTERN.matcher(query).find()) {
            return QueryIntent.COST_ANALYSIS;
        } else if (COMPLIANCE_PATTERN.matcher(query).find()) {
            return QueryIntent.COMPLIANCE_CHECK;
        } else if (RISK_PATTERN.matcher(query).find()) {
            return QueryIntent.RISK_ASSESSMENT;
        } else if (MARKET_PATTERN.matcher(query).find()) {
            return QueryIntent.MARKET_ANALYSIS;
        } else if (OPTIMIZATION_PATTERN.matcher(query).find()) {
            return QueryIntent.OPTIMIZATION;
        } else if (lowerQuery.contains("tariff") || lowerQuery.contains("duty")) {
            return QueryIntent.TARIFF_LOOKUP;
        } else if (lowerQuery.contains("hs code") || lowerQuery.contains("classification")) {
            return QueryIntent.PRODUCT_CLASSIFICATION;
        } else {
            return QueryIntent.GENERAL_INQUIRY;
        }
    }
    
    /**
     * Identify secondary intents
     */
    private List<QueryIntent> identifySecondaryIntents(String query) {
        List<QueryIntent> secondaryIntents = new ArrayList<>();
        
        // Check for multiple patterns in the same query
        if (COST_PATTERN.matcher(query).find()) {
            secondaryIntents.add(QueryIntent.COST_ANALYSIS);
        }
        if (COMPLIANCE_PATTERN.matcher(query).find()) {
            secondaryIntents.add(QueryIntent.COMPLIANCE_CHECK);
        }
        if (RISK_PATTERN.matcher(query).find()) {
            secondaryIntents.add(QueryIntent.RISK_ASSESSMENT);
        }
        if (MARKET_PATTERN.matcher(query).find()) {
            secondaryIntents.add(QueryIntent.MARKET_ANALYSIS);
        }
        
        return secondaryIntents;
    }
    
    /**
     * Assess query complexity
     */
    private ComplexityLevel assessComplexity(String query, QueryContext context) {
        int complexityScore = 0;
        
        // Length factor
        if (query.length() > 200) complexityScore += 2;
        else if (query.length() > 100) complexityScore += 1;
        
        // Multiple countries/products
        if (countEntities(query, "country") > 1) complexityScore += 2;
        if (countEntities(query, "product") > 1) complexityScore += 2;
        
        // Complex operations
        if (COMPARISON_PATTERN.matcher(query).find()) complexityScore += 2;
        if (OPTIMIZATION_PATTERN.matcher(query).find()) complexityScore += 3;
        
        // Multiple intents
        List<QueryIntent> secondaryIntents = identifySecondaryIntents(query);
        complexityScore += secondaryIntents.size();
        
        // Context references
        if (context.getReferencedEntities().size() > 0) complexityScore += 1;
        
        if (complexityScore >= 6) return ComplexityLevel.HIGH;
        else if (complexityScore >= 3) return ComplexityLevel.MEDIUM;
        else return ComplexityLevel.LOW;
    }
    
    /**
     * Identify required agents based on intent and query content
     */
    private List<AgentType> identifyRequiredAgents(QueryIntent primaryIntent, 
                                                  List<QueryIntent> secondaryIntents, String query) {
        Set<AgentType> agents = new HashSet<>();
        
        // Add agent for primary intent
        agents.add(getAgentForIntent(primaryIntent));
        
        // Add agents for secondary intents
        for (QueryIntent intent : secondaryIntents) {
            agents.add(getAgentForIntent(intent));
        }
        
        // Always include tariff analysis for trade queries
        if (query.toLowerCase().contains("trade") || query.toLowerCase().contains("import") || 
            query.toLowerCase().contains("export")) {
            agents.add(AgentType.TARIFF_ANALYSIS);
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
            case PRODUCT_CLASSIFICATION -> AgentType.TARIFF_ANALYSIS; // Uses existing tools
            default -> AgentType.TARIFF_ANALYSIS;
        };
    }
    
    /**
     * Extract entities from query
     */
    private List<QueryEntity> extractQueryEntities(String query, QueryContext context) {
        List<QueryEntity> entities = new ArrayList<>();
        
        // Simple entity extraction - would be enhanced with NLP
        String lowerQuery = query.toLowerCase();
        
        // Countries
        String[] countries = {"germany", "japan", "usa", "us", "china", "canada", "mexico", "uk", "france"};
        for (String country : countries) {
            if (lowerQuery.contains(country)) {
                entities.add(new QueryEntity("country", country, 0.9));
            }
        }
        
        // Products
        String[] products = {"vehicles", "cars", "electronics", "textiles", "machinery", "steel", "aluminum"};
        for (String product : products) {
            if (lowerQuery.contains(product)) {
                entities.add(new QueryEntity("product", product, 0.8));
            }
        }
        
        // Add entities from context
        for (var contextEntity : context.getReferencedEntities()) {
            entities.add(new QueryEntity(contextEntity.getType(), contextEntity.getValue(), 
                                       contextEntity.getConfidence()));
        }
        
        return entities;
    }
    
    /**
     * Create execution step for agent
     */
    private ExecutionPlan.ExecutionStep createExecutionStep(int order, AgentType agentType, 
                                                           QueryAnalysis analysis, QueryContext context) {
        String stepId = "step_" + order;
        String description = generateStepDescription(agentType, analysis);
        boolean required = isStepRequired(agentType, analysis);
        int estimatedDuration = estimateStepDuration(agentType, analysis.getComplexity());
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("query", analysis.getQuery());
        parameters.put("entities", analysis.getEntities());
        parameters.put("complexity", analysis.getComplexity());
        
        return new ExecutionPlan.ExecutionStep(
            stepId,
            order,
            agentType,
            description,
            required,
            estimatedDuration,
            parameters
        );
    }
    
    /**
     * Create dependencies between steps
     */
    private List<ExecutionPlan.StepDependency> createStepDependencies(List<ExecutionPlan.ExecutionStep> steps, 
                                                                     QueryAnalysis analysis) {
        List<ExecutionPlan.StepDependency> dependencies = new ArrayList<>();
        
        // For now, simple sequential dependencies
        for (int i = 1; i < steps.size(); i++) {
            dependencies.add(new ExecutionPlan.StepDependency(
                steps.get(i).getStepId(),
                steps.get(i-1).getStepId(),
                ExecutionPlan.DependencyType.SEQUENTIAL
            ));
        }
        
        return dependencies;
    }
    
    /**
     * Optimize execution plan
     */
    private void optimizePlan(ExecutionPlan plan) {
        // Identify steps that can run in parallel
        // Reorder steps for efficiency
        // This would be enhanced with more sophisticated optimization
        logger.debug("Optimized execution plan {}", plan.getId());
    }
    
    /**
     * Helper methods
     */
    private int countEntities(String query, String entityType) {
        // Simple count - would be enhanced with proper NLP
        String lowerQuery = query.toLowerCase();
        if ("country".equals(entityType)) {
            String[] countries = {"germany", "japan", "usa", "us", "china", "canada", "mexico"};
            return (int) Arrays.stream(countries).filter(lowerQuery::contains).count();
        }
        return 0;
    }
    
    private String generateStepDescription(AgentType agentType, QueryAnalysis analysis) {
        return switch (agentType) {
            case TARIFF_ANALYSIS -> "Analyze tariff rates and trade costs";
            case COMPLIANCE -> "Check regulatory compliance requirements";
            case RISK_ASSESSMENT -> "Assess trade risks and vulnerabilities";
            case MARKET_INTELLIGENCE -> "Analyze market trends and opportunities";
            case OPTIMIZATION -> "Optimize trade strategy and recommendations";
        };
    }
    
    private boolean isStepRequired(AgentType agentType, QueryAnalysis analysis) {
        // Core agents are always required
        return agentType == AgentType.TARIFF_ANALYSIS || 
               analysis.getPrimaryIntent() == getIntentForAgent(agentType);
    }
    
    private QueryIntent getIntentForAgent(AgentType agentType) {
        return switch (agentType) {
            case TARIFF_ANALYSIS -> QueryIntent.TARIFF_LOOKUP;
            case COMPLIANCE -> QueryIntent.COMPLIANCE_CHECK;
            case RISK_ASSESSMENT -> QueryIntent.RISK_ASSESSMENT;
            case MARKET_INTELLIGENCE -> QueryIntent.MARKET_ANALYSIS;
            case OPTIMIZATION -> QueryIntent.OPTIMIZATION;
        };
    }
    
    private int estimateStepDuration(AgentType agentType, ComplexityLevel complexity) {
        int baseDuration = switch (agentType) {
            case TARIFF_ANALYSIS -> 5;
            case COMPLIANCE -> 8;
            case RISK_ASSESSMENT -> 10;
            case MARKET_INTELLIGENCE -> 12;
            case OPTIMIZATION -> 15;
        };
        
        return switch (complexity) {
            case LOW -> baseDuration;
            case MEDIUM -> baseDuration * 2;
            case HIGH -> baseDuration * 3;
        };
    }
    
    private int estimateExecutionDuration(List<ExecutionPlan.ExecutionStep> steps, ComplexityLevel complexity) {
        return steps.stream().mapToInt(ExecutionPlan.ExecutionStep::getEstimatedDuration).sum();
    }
    
    private List<ExecutionPlan.ResourceRequirement> determineResourceRequirements(List<ExecutionPlan.ExecutionStep> steps) {
        List<ExecutionPlan.ResourceRequirement> requirements = new ArrayList<>();
        
        for (ExecutionPlan.ExecutionStep step : steps) {
            requirements.add(new ExecutionPlan.ResourceRequirement(
                "cpu",
                1,
                step.getEstimatedDuration()
            ));
        }
        
        return requirements;
    }
    
    private ExecutionPlan createMinimalPlan(String query) {
        String planId = UUID.randomUUID().toString();
        
        ExecutionPlan.ExecutionStep step = new ExecutionPlan.ExecutionStep(
            "step_1",
            1,
            AgentType.TARIFF_ANALYSIS,
            "Basic tariff lookup",
            true,
            10,
            Map.of("query", query)
        );
        
        return new ExecutionPlan(
            planId,
            query,
            List.of(step),
            Collections.emptyList(),
            10,
            List.of(new ExecutionPlan.ResourceRequirement("cpu", 1, 10))
        );
    }
}