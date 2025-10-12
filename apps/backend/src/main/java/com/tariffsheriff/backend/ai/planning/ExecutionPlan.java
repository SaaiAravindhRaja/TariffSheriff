package com.tariffsheriff.backend.ai.planning;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Execution plan for multi-agent query processing
 */
public class ExecutionPlan {
    private final String id;
    private final String query;
    private final List<ExecutionStep> steps;
    private final List<StepDependency> dependencies;
    private final int estimatedDuration;
    private final List<ResourceRequirement> requiredResources;
    
    public ExecutionPlan(String id, String query, List<ExecutionStep> steps,
                        List<StepDependency> dependencies, int estimatedDuration,
                        List<ResourceRequirement> requiredResources) {
        this.id = id;
        this.query = query;
        this.steps = new ArrayList<>(steps);
        this.dependencies = new ArrayList<>(dependencies);
        this.estimatedDuration = estimatedDuration;
        this.requiredResources = new ArrayList<>(requiredResources);
    }
    
    /**
     * Get steps in execution order
     */
    public List<ExecutionStep> getStepsInOrder() {
        return steps.stream()
                .sorted((s1, s2) -> Integer.compare(s1.getOrder(), s2.getOrder()))
                .toList();
    }
    
    /**
     * Get steps that can run in parallel
     */
    public List<List<ExecutionStep>> getParallelSteps() {
        // For now, return sequential execution
        // This would be enhanced with dependency analysis
        List<List<ExecutionStep>> parallelGroups = new ArrayList<>();
        for (ExecutionStep step : getStepsInOrder()) {
            parallelGroups.add(List.of(step));
        }
        return parallelGroups;
    }
    
    /**
     * Get required steps only
     */
    public List<ExecutionStep> getRequiredSteps() {
        return steps.stream()
                .filter(ExecutionStep::isRequired)
                .toList();
    }
    
    /**
     * Get optional steps
     */
    public List<ExecutionStep> getOptionalSteps() {
        return steps.stream()
                .filter(step -> !step.isRequired())
                .toList();
    }
    
    // Getters
    public String getId() { return id; }
    public String getQuery() { return query; }
    public List<ExecutionStep> getSteps() { return new ArrayList<>(steps); }
    public List<StepDependency> getDependencies() { return new ArrayList<>(dependencies); }
    public int getEstimatedDuration() { return estimatedDuration; }
    public List<ResourceRequirement> getRequiredResources() { return new ArrayList<>(requiredResources); }
    
    /**
     * Execution step within a plan
     */
    public static class ExecutionStep {
        private final String stepId;
        private final int order;
        private final AgentType agentType;
        private final String description;
        private final boolean required;
        private final int estimatedDuration;
        private final Map<String, Object> parameters;
        
        public ExecutionStep(String stepId, int order, AgentType agentType, String description,
                           boolean required, int estimatedDuration, Map<String, Object> parameters) {
            this.stepId = stepId;
            this.order = order;
            this.agentType = agentType;
            this.description = description;
            this.required = required;
            this.estimatedDuration = estimatedDuration;
            this.parameters = Map.copyOf(parameters);
        }
        
        // Getters
        public String getStepId() { return stepId; }
        public int getOrder() { return order; }
        public AgentType getAgentType() { return agentType; }
        public String getDescription() { return description; }
        public boolean isRequired() { return required; }
        public int getEstimatedDuration() { return estimatedDuration; }
        public Map<String, Object> getParameters() { return Map.copyOf(parameters); }
    }
    
    /**
     * Dependency between execution steps
     */
    public static class StepDependency {
        private final String dependentStepId;
        private final String prerequisiteStepId;
        private final DependencyType type;
        
        public StepDependency(String dependentStepId, String prerequisiteStepId, DependencyType type) {
            this.dependentStepId = dependentStepId;
            this.prerequisiteStepId = prerequisiteStepId;
            this.type = type;
        }
        
        // Getters
        public String getDependentStepId() { return dependentStepId; }
        public String getPrerequisiteStepId() { return prerequisiteStepId; }
        public DependencyType getType() { return type; }
    }
    
    /**
     * Resource requirement for execution
     */
    public static class ResourceRequirement {
        private final String resourceType;
        private final int quantity;
        private final int durationSeconds;
        
        public ResourceRequirement(String resourceType, int quantity, int durationSeconds) {
            this.resourceType = resourceType;
            this.quantity = quantity;
            this.durationSeconds = durationSeconds;
        }
        
        // Getters
        public String getResourceType() { return resourceType; }
        public int getQuantity() { return quantity; }
        public int getDurationSeconds() { return durationSeconds; }
    }
    
    /**
     * Types of dependencies between steps
     */
    public enum DependencyType {
        SEQUENTIAL("Step must complete before next step starts"),
        DATA_DEPENDENCY("Step requires data from previous step"),
        RESOURCE_DEPENDENCY("Step requires resources from previous step");
        
        private final String description;
        
        DependencyType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}