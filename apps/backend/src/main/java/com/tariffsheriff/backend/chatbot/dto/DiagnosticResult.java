package com.tariffsheriff.backend.chatbot.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Diagnostic result for Gemini API integration testing.
 * Contains detailed information about API calls including timing, request/response data, and errors.
 */
public class DiagnosticResult {
    private boolean success;
    private long timingMs;
    private String phase;
    private Object rawRequest;
    private Object rawResponse;
    private String error;
    private Map<String, Object> metadata;

    // Private constructor for builder pattern
    private DiagnosticResult() {
        this.metadata = new HashMap<>();
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public long getTimingMs() {
        return timingMs;
    }

    public String getPhase() {
        return phase;
    }

    public Object getRawRequest() {
        return rawRequest;
    }

    public Object getRawResponse() {
        return rawResponse;
    }

    public String getError() {
        return error;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    // Setters
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setTimingMs(long timingMs) {
        this.timingMs = timingMs;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public void setRawRequest(Object rawRequest) {
        this.rawRequest = rawRequest;
    }

    public void setRawResponse(Object rawResponse) {
        this.rawResponse = rawResponse;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Builder for constructing DiagnosticResult instances.
     */
    public static class Builder {
        private final DiagnosticResult result;

        public Builder() {
            this.result = new DiagnosticResult();
        }

        public Builder success(boolean success) {
            result.success = success;
            return this;
        }

        public Builder timingMs(long timingMs) {
            result.timingMs = timingMs;
            return this;
        }

        public Builder phase(String phase) {
            result.phase = phase;
            return this;
        }

        public Builder rawRequest(Object rawRequest) {
            result.rawRequest = rawRequest;
            return this;
        }

        public Builder rawResponse(Object rawResponse) {
            result.rawResponse = rawResponse;
            return this;
        }

        public Builder error(String error) {
            result.error = error;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            result.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            result.metadata.put(key, value);
            return this;
        }

        public DiagnosticResult build() {
            return result;
        }
    }

    /**
     * Creates a new builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "DiagnosticResult{" +
                "success=" + success +
                ", timingMs=" + timingMs +
                ", phase='" + phase + '\'' +
                ", error='" + error + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}
