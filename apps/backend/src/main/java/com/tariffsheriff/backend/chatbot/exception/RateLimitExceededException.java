package com.tariffsheriff.backend.chatbot.exception;

/**
 * Exception thrown when user exceeds rate limits with clear guidance on alternatives
 */
public class RateLimitExceededException extends ChatbotException {
    
    private final long requestsInLastMinute;
    private final long requestsInLastHour;
    private final int maxRequestsPerMinute;
    private final int maxRequestsPerHour;
    
    public RateLimitExceededException(long requestsInLastMinute, long requestsInLastHour,
                                   int maxRequestsPerMinute, int maxRequestsPerHour) {
        super(buildUserFriendlyMessage(requestsInLastMinute, requestsInLastHour, maxRequestsPerMinute, maxRequestsPerHour),
              buildHelpfulSuggestion(requestsInLastMinute, requestsInLastHour, maxRequestsPerMinute, maxRequestsPerHour));
        
        this.requestsInLastMinute = requestsInLastMinute;
        this.requestsInLastHour = requestsInLastHour;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.maxRequestsPerHour = maxRequestsPerHour;
    }
    
    /**
     * Build user-friendly rate limit message
     */
    private static String buildUserFriendlyMessage(long requestsInLastMinute, long requestsInLastHour,
                                                  int maxRequestsPerMinute, int maxRequestsPerHour) {
        if (requestsInLastMinute >= maxRequestsPerMinute) {
            return String.format("You're asking questions too quickly! You've made %d requests in the last minute. " +
                               "Please take a short break (limit: %d per minute).",
                    requestsInLastMinute, maxRequestsPerMinute);
        } else {
            long remainingRequests = maxRequestsPerHour - requestsInLastHour;
            if (remainingRequests <= 0) {
                return String.format("You've reached your hourly limit of %d questions. " +
                                   "This helps ensure fair access for all users.",
                        maxRequestsPerHour);
            } else {
                return String.format("You're approaching your hourly limit. You've made %d requests " +
                                   "(limit: %d per hour). You have %d requests remaining.",
                        requestsInLastHour, maxRequestsPerHour, remainingRequests);
            }
        }
    }
    
    /**
     * Build helpful suggestions with alternatives
     */
    private static String buildHelpfulSuggestion(long requestsInLastMinute, long requestsInLastHour,
                                               int maxRequestsPerMinute, int maxRequestsPerHour) {
        StringBuilder suggestion = new StringBuilder();
        
        if (requestsInLastMinute >= maxRequestsPerMinute) {
            suggestion.append("\n\n**Please wait 30-60 seconds before your next question.**\n\n");
            suggestion.append("**While you wait, you can:**\n");
        } else {
            long remainingRequests = maxRequestsPerHour - requestsInLastHour;
            if (remainingRequests <= 0) {
                suggestion.append("\n\n**Your limit will reset in the next hour.**\n\n");
                suggestion.append("**In the meantime, you can:**\n");
            } else {
                suggestion.append(String.format("\n\n**You have %d questions remaining this hour.**\n\n", remainingRequests));
                suggestion.append("**To make the most of your remaining questions:**\n");
                suggestion.append("• Combine related questions into one\n");
                suggestion.append("• Be specific to get complete answers\n");
                suggestion.append("• Use the manual tools below\n\n");
                suggestion.append("**Or use these alternatives:**\n");
            }
        }
        
        // Add alternative options
        suggestion.append("• Use the **Calculator** page for tariff lookups (no limits)\n");
        suggestion.append("• Browse the **Database** to explore trade data (no limits)\n");
        suggestion.append("• Check the **Analytics** page for insights (no limits)\n");
        suggestion.append("• Review your conversation history for previous answers\n\n");
        
        // Add official resources
        suggestion.append("**Official Resources (no limits):**\n");
        suggestion.append("• WTO Database: https://www.wto.org/\n");
        suggestion.append("• Trade.gov: https://www.trade.gov/\n");
        suggestion.append("• USITC HTS: https://hts.usitc.gov/\n\n");
        
        suggestion.append("💡 **Tip:** Rate limits help ensure fast responses for everyone. ");
        suggestion.append("The manual tools have no limits and are great for detailed research!");
        
        return suggestion.toString();
    }
    
    // Getters
    public long getRequestsInLastMinute() { return requestsInLastMinute; }
    public long getRequestsInLastHour() { return requestsInLastHour; }
    public int getMaxRequestsPerMinute() { return maxRequestsPerMinute; }
    public int getMaxRequestsPerHour() { return maxRequestsPerHour; }
}