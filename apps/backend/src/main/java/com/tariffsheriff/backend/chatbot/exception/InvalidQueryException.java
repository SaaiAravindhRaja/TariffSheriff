package com.tariffsheriff.backend.chatbot.exception;

/**
 * Exception thrown when user query is invalid or cannot be processed with helpful guidance
 */
public class InvalidQueryException extends ChatbotException {
    
    public InvalidQueryException(String message) {
        super(message, makeUserFriendly(message), generateHelpfulSuggestion(message), null);
    }
    
    public InvalidQueryException(String message, String suggestion) {
        super(message, makeUserFriendly(message), suggestion, null);
    }
    
    public InvalidQueryException(String message, Throwable cause) {
        super(message, makeUserFriendly(message), generateHelpfulSuggestion(message), cause);
    }
    
    public InvalidQueryException(String message, String suggestion, Throwable cause) {
        super(message, makeUserFriendly(message), suggestion, cause);
    }
    
    // ... rest of the class is unchanged ...
    /**
     * Convert technical validation errors to user-friendly messages
     */
    private static String makeUserFriendly(String technicalMessage) {
        if (technicalMessage == null) {
            return "I had trouble understanding your question.";
        }
        
        String lower = technicalMessage.toLowerCase();
        
        if (lower.contains("empty") || lower.contains("blank") || lower.contains("null")) {
            return "I didn't receive a question. Please type your question and try again.";
        }
        
        if (lower.contains("too long") || lower.contains("length") || lower.contains("exceeds")) {
            return "Your question is too long. Please try breaking it into smaller, more specific questions.";
        }
        
        if (lower.contains("special character") || lower.contains("invalid character")) {
            return "Your question contains characters I can't process. Please use standard text.";
        }
        
        if (lower.contains("format") || lower.contains("structure")) {
            return "I had trouble understanding the format of your question.";
        }
        
        return "I had trouble understanding your question. Could you rephrase it?";
    }
    
    /**
     * Generate helpful suggestions based on the validation error
     */
    private static String generateHelpfulSuggestion(String message) {
        if (message == null) {
            return generateDefaultSuggestion();
        }
        
        String lower = message.toLowerCase();
        
        if (lower.contains("empty") || lower.contains("blank")) {
            return "\n\n**Try asking questions like:**\n" +
                   "• \"What's the tariff for importing coffee from Brazil to the US?\"\n" +
                   "• \"Find the HS code for leather handbags\"\n" +
                   "• \"What trade agreements does Canada have?\"\n" +
                   "• \"Help\" - to see what I can do";
        }
        
        if (lower.contains("too long")) {
            return "\n\n**Tips for better questions:**\n" +
                   "• Focus on one specific topic at a time\n" +
                   "• Break complex questions into smaller parts\n" +
                   "• Ask follow-up questions for additional details\n" +
                   "• Keep questions under 500 characters";
        }
        
        return generateDefaultSuggestion();
    }
    
    /**
     * Generate default helpful suggestion
     */
    private static String generateDefaultSuggestion() {
        return "\n\n**How to ask better questions:**\n" +
               "• Be specific about countries and products\n" +
               "• Include relevant details (HS codes, materials, etc.)\n" +
               "• Ask one question at a time\n" +
               "• Use clear, simple language\n\n" +
               "**Example questions:**\n" +
               "• \"What's the tariff for [product] from [country] to [country]?\"\n" +
               "• \"Find HS code for [product description]\"\n" +
               "• \"Does [country] have a trade agreement with [country]?\"\n\n" +
               "**Need help?** Type \"help\" to see all my capabilities.";
    }
}