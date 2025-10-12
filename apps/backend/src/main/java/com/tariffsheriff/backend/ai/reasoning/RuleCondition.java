package com.tariffsheriff.backend.ai.reasoning;

/**
 * Represents a condition in a reasoning rule
 */
public class RuleCondition {
    private final String predicate;
    private final String subject;
    private final String object;
    private final ConditionOperator operator;
    
    public RuleCondition(String predicate, String subject, String object, ConditionOperator operator) {
        this.predicate = predicate;
        this.subject = subject;
        this.object = object;
        this.operator = operator;
    }
    
    public RuleCondition(String predicate, String subject, String object) {
        this(predicate, subject, object, ConditionOperator.EQUALS);
    }
    
    /**
     * Check if this condition matches a fact
     */
    public boolean matches(Fact fact) {
        return switch (operator) {
            case EQUALS -> predicate.equals(fact.getPredicate()) &&
                         subject.equals(fact.getSubject()) &&
                         (object == null || object.equals(fact.getObject()));
            case NOT_EQUALS -> !predicate.equals(fact.getPredicate()) ||
                              !subject.equals(fact.getSubject()) ||
                              (object != null && !object.equals(fact.getObject()));
            case CONTAINS -> fact.getObject() != null && fact.getObject().contains(object);
            case GREATER_THAN -> compareNumeric(fact.getObject(), object) > 0;
            case LESS_THAN -> compareNumeric(fact.getObject(), object) < 0;
        };
    }
    
    /**
     * Compare numeric values
     */
    private int compareNumeric(String value1, String value2) {
        try {
            double d1 = Double.parseDouble(value1);
            double d2 = Double.parseDouble(value2);
            return Double.compare(d1, d2);
        } catch (NumberFormatException e) {
            return value1.compareTo(value2);
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s(%s, %s) %s", predicate, subject, object, operator);
    }
    
    // Getters
    public String getPredicate() { return predicate; }
    public String getSubject() { return subject; }
    public String getObject() { return object; }
    public ConditionOperator getOperator() { return operator; }
    
    /**
     * Operators for rule conditions
     */
    public enum ConditionOperator {
        EQUALS("="),
        NOT_EQUALS("!="),
        CONTAINS("contains"),
        GREATER_THAN(">"),
        LESS_THAN("<");
        
        private final String symbol;
        
        ConditionOperator(String symbol) {
            this.symbol = symbol;
        }
        
        @Override
        public String toString() {
            return symbol;
        }
    }
}