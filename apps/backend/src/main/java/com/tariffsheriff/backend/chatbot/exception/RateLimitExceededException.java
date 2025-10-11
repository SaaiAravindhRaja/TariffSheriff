package com.tariffsheriff.backend.chatbot.exception;

/**
 * Exception thrown when user exceeds rate limits for chatbot queries
 */
public class RateLimitExceededException extends ChatbotException {
    
    private final long requestsInLastMinute;
    private final long requestsInLastHour;
    private final int maxRequestsPerMinute;
    private final int maxRequestsPerHour;
    
    public RateLimitExceededException(long requestsInLastMinute, long requestsInLastHour,
                                   int maxRequestsPerMinute, int maxRequestsPerHour) {
        super(buildMessage(requestsInLastMinute, requestsInLastHour, maxRequestsPerMinute, maxRequestsPerHour),
              buildSuggestion(requestsInLastMinute, requestsInLastHour, maxRequestsPerMinute, maxRequestsPerHour));
        
        this.requestsInLastMinute = requestsInLastMinute;
        this.requestsInLastHour = requestsInLastHour;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.maxRequestsPerHour = maxRequestsPerHour;
    }
    
    private static String buildMessage(long requestsInLastMinute, long requestsInLastHour,
                                     int maxRequestsPerMinute, int maxRequestsPerHour) {
        if (requestsInLastMinute >= maxRequestsPerMinute) {
            return String.format("Too many requests. You've made %d requests in the last minute (limit: %d).",
                    requestsInLastMinute, maxRequestsPerMinute);
        } else {
            return String.format("Too many requests. You've made %d requests in the last hour (limit: %d).",
                    requestsInLastHour, maxRequestsPerHour);
        }
    }
    
    private static String buildSuggestion(long requestsInLastMinute, long requestsInLastHour,
                                        int maxRequestsPerMinute, int maxRequestsPerHour) {
        if (requestsInLastMinute >= maxRequestsPerMinute) {
            return "Please wait a moment before making another request.";
        } else {
            long remainingRequests = maxRequestsPerHour - requestsInLastHour;
            if (remainingRequests > 0) {
                return String.format("You have %d requests remaining this hour. Please pace your queries.", 
                        remainingRequests);
            } else {
                return "Please wait for the next hour to make more requests.";
            }
        }
    }
    
    // Getters
    public long getRequestsInLastMinute() { return requestsInLastMinute; }
    public long getRequestsInLastHour() { return requestsInLastHour; }
    public int getMaxRequestsPerMinute() { return maxRequestsPerMinute; }
    public int getMaxRequestsPerHour() { return maxRequestsPerHour; }
}