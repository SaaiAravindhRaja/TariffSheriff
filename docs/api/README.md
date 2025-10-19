# Tariff Sheriff AI Assistant API Documentation

## Overview

The Tariff Sheriff AI Assistant provides a conversational interface for querying tariff and trade data. The API uses OpenAI's GPT models to understand natural language queries and automatically selects appropriate tools to fetch data from the database.

**Base URL**: `http://localhost:8080/api/chatbot`

**Authentication**: All endpoints require JWT authentication with `USER` or `ADMIN` role.

---

## Endpoints

### 1. Process Chat Query

Process a natural language query and get an AI-generated response.

**Endpoint**: `POST /api/chatbot/query`

**Authentication**: Required (Bearer token)

**Request Body**:
```json
{
  "query": "What's the tariff rate for importing steel from China to USA?",
  "conversationId": "optional-conversation-id"
}
```

**Request Fields**:
- `query` (string, required): The user's natural language question (max 2000 characters)
- `conversationId` (string, optional): ID to continue an existing conversation. If omitted, a new conversation is created.

**Success Response** (200 OK):
```json
{
  "response": "The tariff rate for importing steel from China to the USA depends on the specific HS code...",
  "conversationId": "conv_abc123xyz",
  "timestamp": "2025-10-19T14:30:00",
  "toolsUsed": ["getTariffRateLookup"],
  "processingTimeMs": 2340,
  "success": true,
  "cached": false,
  "degraded": false,
  "confidence": null
}
```

**Response Fields**:
- `response` (string): The AI-generated conversational response
- `conversationId` (string): ID for this conversation (use in subsequent queries to maintain context)
- `timestamp` (datetime): When the response was generated
- `toolsUsed` (array): List of tools the AI used to fetch data
- `processingTimeMs` (number): Time taken to process the query in milliseconds
- `success` (boolean): Whether the query was processed successfully
- `cached` (boolean): Whether the response was served from cache
- `degraded` (boolean): Whether the response used fallback mechanisms
- `confidence` (number, nullable): Confidence score of the response (0-1)

**Error Responses**:

**400 Bad Request** - Invalid query:
```json
{
  "error": "InvalidQueryException",
  "message": "Query cannot be empty",
  "suggestion": "Please provide a valid question about tariffs or trade data.",
  "timestamp": "2025-10-19T14:30:00",
  "conversationId": "conv_abc123xyz"
}
```

**429 Too Many Requests** - Rate limit exceeded:
```json
{
  "error": "RateLimitExceededException",
  "message": "You have exceeded your rate limit. Please try again in 45 seconds.",
  "suggestion": "Rate limits: 10 requests per minute, 100 requests per hour.",
  "timestamp": "2025-10-19T14:30:00",
  "conversationId": "conv_abc123xyz"
}
```

**503 Service Unavailable** - LLM service error:
```json
{
  "error": "LlmServiceException",
  "message": "I'm having trouble connecting to my AI service. Please try again in a moment.",
  "suggestion": "If the problem persists, please contact support.",
  "timestamp": "2025-10-19T14:30:00",
  "conversationId": "conv_abc123xyz"
}
```

**500 Internal Server Error** - Tool execution error:
```json
{
  "error": "ToolExecutionException",
  "message": "I couldn't fetch that information. The database query failed.",
  "suggestion": "Please try rephrasing your question or try again later.",
  "timestamp": "2025-10-19T14:30:00",
  "conversationId": "conv_abc123xyz"
}
```

---

### 2. Get Service Health

Check the health status of the chatbot service.

**Endpoint**: `GET /api/chatbot/health`

**Authentication**: Required (Bearer token)

**Success Response** (200 OK):
```json
{
  "healthy": true,
  "message": "Chatbot service is healthy",
  "availableTools": 9,
  "trackedUsers": 42
}
```

**Response Fields**:
- `healthy` (boolean): Whether the service is operational
- `message` (string): Health status message
- `availableTools` (number): Number of tools available to the AI
- `trackedUsers` (number): Number of users currently tracked for rate limiting

**Error Response** (503 Service Unavailable):
```json
{
  "healthy": false,
  "message": "Chatbot service is not available",
  "availableTools": 0,
  "trackedUsers": 0
}
```

---

### 3. Get Rate Limit Status

Get the current rate limit status for the authenticated user.

**Endpoint**: `GET /api/chatbot/rate-limit-status`

**Authentication**: Required (Bearer token)

**Success Response** (200 OK):
```json
{
  "requestsInLastMinute": 3,
  "requestsInLastHour": 15,
  "maxRequestsPerMinute": 10,
  "maxRequestsPerHour": 100,
  "minuteResetTime": "2025-10-19T14:31:00",
  "hourResetTime": "2025-10-19T15:00:00"
}
```

**Response Fields**:
- `requestsInLastMinute` (number): Number of requests made in the last minute
- `requestsInLastHour` (number): Number of requests made in the last hour
- `maxRequestsPerMinute` (number): Maximum allowed requests per minute
- `maxRequestsPerHour` (number): Maximum allowed requests per hour
- `minuteResetTime` (datetime): When the minute counter resets
- `hourResetTime` (datetime): When the hour counter resets

---

### 4. Get User Conversations

Get a list of all conversations for the authenticated user.

**Endpoint**: `GET /api/chatbot/conversations`

**Authentication**: Required (Bearer token)

**Success Response** (200 OK):
```json
[
  {
    "conversationId": "conv_abc123xyz",
    "userId": "user@example.com",
    "messageCount": 5,
    "createdAt": "2025-10-19T14:00:00",
    "updatedAt": "2025-10-19T14:30:00",
    "firstMessage": "What's the tariff rate for importing steel?"
  },
  {
    "conversationId": "conv_def456uvw",
    "userId": "user@example.com",
    "messageCount": 3,
    "createdAt": "2025-10-18T10:00:00",
    "updatedAt": "2025-10-18T10:15:00",
    "firstMessage": "Find HS code for coffee beans"
  }
]
```

---

### 5. Get Conversation Details

Get detailed information about a specific conversation including full message history.

**Endpoint**: `GET /api/chatbot/conversations/{conversationId}`

**Authentication**: Required (Bearer token)

**Path Parameters**:
- `conversationId` (string): The ID of the conversation to retrieve

**Success Response** (200 OK):
```json
{
  "conversationId": "conv_abc123xyz",
  "userId": "user@example.com",
  "messages": [
    {
      "role": "user",
      "content": "What's the tariff rate for importing steel from China to USA?",
      "toolsUsed": [],
      "timestamp": "2025-10-19T14:00:00"
    },
    {
      "role": "assistant",
      "content": "The tariff rate for importing steel from China to the USA...",
      "toolsUsed": ["getTariffRateLookup"],
      "timestamp": "2025-10-19T14:00:03"
    }
  ],
  "createdAt": "2025-10-19T14:00:00",
  "updatedAt": "2025-10-19T14:00:03"
}
```

**Error Response** (404 Not Found):
```json
{
  "error": "NotFound",
  "message": "Conversation not found"
}
```

---

### 6. Delete Conversation

Delete a specific conversation and its message history.

**Endpoint**: `DELETE /api/chatbot/conversations/{conversationId}`

**Authentication**: Required (Bearer token)

**Path Parameters**:
- `conversationId` (string): The ID of the conversation to delete

**Success Response** (200 OK):
```json
{}
```

**Error Response** (404 Not Found):
```json
{
  "error": "NotFound",
  "message": "Conversation not found"
}
```

---

## Available Tools

The AI Assistant can automatically select and use the following tools to answer queries:

### 1. Tariff Lookup Tool
**Tool Name**: `getTariffRateLookup`

**Purpose**: Get MFN and preferential tariff rates for specific trade routes

**Example Queries**:
- "What's the tariff rate for importing steel from China to USA?"
- "Show me the duty rate for HS code 7208.10 from Mexico to USA"
- "What's the import tax on electronics from Japan?"

### 2. HS Code Finder Tool
**Tool Name**: `findHsCodeForProduct`

**Purpose**: Find HS codes based on product descriptions

**Example Queries**:
- "Find HS code for coffee beans"
- "What's the HS code for smartphones?"
- "Search HS codes for automotive parts"

### 3. Agreement Tool
**Tool Name**: `getAgreementsByCountry`

**Purpose**: Get trade agreements for specific countries

**Example Queries**:
- "What trade agreements does the US have?"
- "Show me agreements between USA and Mexico"
- "Does Canada have a trade agreement with Japan?"

### 4. Country Info Tool
**Tool Name**: `getCountryInfo`

**Purpose**: Get information about countries and search available countries

**Example Queries**:
- "List all available countries"
- "Tell me about Canada"
- "Search for countries in Asia"

### 5. Tariff Comparison Tool
**Tool Name**: `compareTariffRates`

**Purpose**: Compare tariff rates for the same product from different origin countries

**Example Queries**:
- "Compare tariff rates for steel from China vs Mexico to USA"
- "Which country has the lowest tariff for electronics to USA?"
- "Compare import duties for coffee from Brazil, Colombia, and Vietnam"

### 6. Compliance Analysis Tool
**Tool Name**: `analyzeCompliance`

**Purpose**: Analyze compliance requirements for trade routes

**Example Queries**:
- "What are the compliance requirements for importing from China?"
- "Check compliance for HS code 8471.30"

### 7. Market Intelligence Tool
**Tool Name**: `getMarketIntelligence`

**Purpose**: Get market intelligence and trade statistics

**Example Queries**:
- "What are the trade trends for electronics?"
- "Show me market data for steel imports"

### 8. Risk Assessment Tool
**Tool Name**: `assessRisk`

**Purpose**: Assess risks for trade routes and products

**Example Queries**:
- "What are the risks of importing from China?"
- "Assess trade risks for automotive parts"

---

## Example Usage

### Example 1: Simple Tariff Query

**Request**:
```bash
curl -X POST http://localhost:8080/api/chatbot/query \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is the tariff rate for importing coffee from Brazil to USA?"
  }'
```

**Response**:
```json
{
  "response": "To provide you with the exact tariff rate for importing coffee from Brazil to the USA, I need the specific HS code for the type of coffee you're importing. Coffee products have different HS codes depending on whether they are roasted, decaffeinated, or in bean form. Could you specify the type of coffee, or would you like me to search for relevant HS codes?",
  "conversationId": "conv_abc123xyz",
  "timestamp": "2025-10-19T14:30:00",
  "toolsUsed": [],
  "processingTimeMs": 1200,
  "success": true
}
```

### Example 2: HS Code Search

**Request**:
```bash
curl -X POST http://localhost:8080/api/chatbot/query \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Find HS code for roasted coffee beans",
    "conversationId": "conv_abc123xyz"
  }'
```

**Response**:
```json
{
  "response": "Here are the relevant HS codes for roasted coffee beans:\n\n1. **0901.21** - Coffee, roasted, not decaffeinated\n2. **0901.22** - Coffee, roasted, decaffeinated\n\nThese codes are used for coffee that has been roasted but not ground. If you need the tariff rate for importing these from Brazil to the USA, I can look that up for you.",
  "conversationId": "conv_abc123xyz",
  "timestamp": "2025-10-19T14:30:15",
  "toolsUsed": ["findHsCodeForProduct"],
  "processingTimeMs": 1800,
  "success": true
}
```

### Example 3: Continuing Conversation

**Request**:
```bash
curl -X POST http://localhost:8080/api/chatbot/query \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Yes, please look up the tariff rate for 0901.21",
    "conversationId": "conv_abc123xyz"
  }'
```

**Response**:
```json
{
  "response": "For importing roasted coffee (HS code 0901.21) from Brazil to the USA:\n\n- **MFN Rate**: 0% (duty-free)\n- **Preferential Rate**: 0% (duty-free)\n\nGood news! Coffee imports from Brazil to the USA are duty-free under both the Most Favored Nation (MFN) rate and any applicable trade agreements. This means you won't pay any tariffs on this product.",
  "conversationId": "conv_abc123xyz",
  "timestamp": "2025-10-19T14:30:30",
  "toolsUsed": ["getTariffRateLookup"],
  "processingTimeMs": 2100,
  "success": true
}
```

---

## Rate Limiting

The API implements rate limiting to ensure fair usage:

- **Per Minute**: 10 requests
- **Per Hour**: 100 requests

When rate limits are exceeded, the API returns a `429 Too Many Requests` error with information about when you can retry.

---

## Error Handling

All errors follow a consistent format:

```json
{
  "error": "ErrorType",
  "message": "User-friendly error message",
  "suggestion": "Helpful suggestion for resolving the error",
  "timestamp": "2025-10-19T14:30:00",
  "conversationId": "conv_abc123xyz"
}
```

### Common Error Types

| Error Type | HTTP Status | Description |
|------------|-------------|-------------|
| `InvalidQueryException` | 400 | Query is empty, too long, or malformed |
| `RateLimitExceededException` | 429 | User has exceeded rate limits |
| `LlmServiceException` | 503 | OpenAI API is unavailable or returned an error |
| `ToolExecutionException` | 500 | A tool failed to execute (e.g., database error) |
| `ChatbotException` | 500 | General chatbot error |
| `InternalServerError` | 500 | Unexpected server error |

---

## Authentication

All endpoints require JWT authentication. Include the JWT token in the `Authorization` header:

```
Authorization: Bearer YOUR_JWT_TOKEN
```

To obtain a JWT token, use the authentication endpoints:
- `POST /api/auth/login` - Login with email and password
- `POST /api/auth/register` - Register a new user account

---

## Configuration

The AI Assistant can be configured using environment variables:

### OpenAI Configuration
- `OPENAI_API_KEY` - Your OpenAI API key (required)
- `OPENAI_MODEL` - Model to use (default: `gpt-4o-mini`)
- `OPENAI_TEMPERATURE` - Response creativity (default: `0.7`)
- `OPENAI_MAX_TOKENS` - Maximum response length (default: `1000`)
- `OPENAI_TIMEOUT_MS` - API timeout in milliseconds (default: `30000`)
- `OPENAI_MAX_RETRIES` - Number of retries on failure (default: `2`)

### Rate Limiting Configuration
- `RATE_LIMIT_PER_MINUTE` - Requests per minute (default: `10`)
- `RATE_LIMIT_PER_HOUR` - Requests per hour (default: `100`)

---

## Best Practices

1. **Maintain Conversation Context**: Use the same `conversationId` for related queries to maintain context
2. **Be Specific**: More specific queries yield better results (e.g., include HS codes when known)
3. **Handle Rate Limits**: Implement exponential backoff when receiving 429 errors
4. **Check Health**: Monitor the `/health` endpoint to ensure service availability
5. **Error Handling**: Always handle errors gracefully and display user-friendly messages

---

## Support

For issues or questions about the API:
- Check the logs for detailed error information
- Review the OpenAPI/Swagger documentation at `/swagger-ui.html`
- Contact the development team

---

## Changelog

### Version 1.0 (Current)
- Initial release with 9 available tools
- OpenAI GPT-4o-mini integration
- Conversation history management
- Rate limiting
- Comprehensive error handling
