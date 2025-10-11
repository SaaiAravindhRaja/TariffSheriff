package com.tariffsheriff.backend.chatbot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response model for Gemini generateContent API
 */
public class GeminiResponse {
    
    private List<Candidate> candidates;
    
    @JsonProperty("usageMetadata")
    private UsageMetadata usageMetadata;
    
    public GeminiResponse() {}
    
    public List<Candidate> getCandidates() {
        return candidates;
    }
    
    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }
    
    public UsageMetadata getUsageMetadata() {
        return usageMetadata;
    }
    
    public void setUsageMetadata(UsageMetadata usageMetadata) {
        this.usageMetadata = usageMetadata;
    }
    
    /**
     * Response candidate
     */
    public static class Candidate {
        private Content content;
        
        @JsonProperty("finishReason")
        private String finishReason;
        
        private int index;
        
        @JsonProperty("safetyRatings")
        private List<SafetyRating> safetyRatings;
        
        public Candidate() {}
        
        public Content getContent() {
            return content;
        }
        
        public void setContent(Content content) {
            this.content = content;
        }
        
        public String getFinishReason() {
            return finishReason;
        }
        
        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
        
        public int getIndex() {
            return index;
        }
        
        public void setIndex(int index) {
            this.index = index;
        }
        
        public List<SafetyRating> getSafetyRatings() {
            return safetyRatings;
        }
        
        public void setSafetyRatings(List<SafetyRating> safetyRatings) {
            this.safetyRatings = safetyRatings;
        }
    }
    
    /**
     * Content in the response
     */
    public static class Content {
        private List<Part> parts;
        private String role;
        
        public Content() {}
        
        public List<Part> getParts() {
            return parts;
        }
        
        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
    }
    
    /**
     * Part of the content
     */
    public static class Part {
        private String text;
        
        @JsonProperty("functionCall")
        private FunctionCall functionCall;
        
        public Part() {}
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public FunctionCall getFunctionCall() {
            return functionCall;
        }
        
        public void setFunctionCall(FunctionCall functionCall) {
            this.functionCall = functionCall;
        }
    }
    
    /**
     * Function call in the response
     */
    public static class FunctionCall {
        private String name;
        private Object args;
        
        public FunctionCall() {}
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public Object getArgs() {
            return args;
        }
        
        public void setArgs(Object args) {
            this.args = args;
        }
    }
    
    /**
     * Safety rating
     */
    public static class SafetyRating {
        private String category;
        private String probability;
        
        public SafetyRating() {}
        
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
        
        public String getProbability() {
            return probability;
        }
        
        public void setProbability(String probability) {
            this.probability = probability;
        }
    }
    
    /**
     * Usage metadata
     */
    public static class UsageMetadata {
        @JsonProperty("promptTokenCount")
        private int promptTokenCount;
        
        @JsonProperty("candidatesTokenCount")
        private int candidatesTokenCount;
        
        @JsonProperty("totalTokenCount")
        private int totalTokenCount;
        
        public UsageMetadata() {}
        
        public int getPromptTokenCount() {
            return promptTokenCount;
        }
        
        public void setPromptTokenCount(int promptTokenCount) {
            this.promptTokenCount = promptTokenCount;
        }
        
        public int getCandidatesTokenCount() {
            return candidatesTokenCount;
        }
        
        public void setCandidatesTokenCount(int candidatesTokenCount) {
            this.candidatesTokenCount = candidatesTokenCount;
        }
        
        public int getTotalTokenCount() {
            return totalTokenCount;
        }
        
        public void setTotalTokenCount(int totalTokenCount) {
            this.totalTokenCount = totalTokenCount;
        }
    }
}