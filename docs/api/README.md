# TariffSheriff API Documentation

## Overview

Simple REST API for tariff lookup and calculation with JWT-based authentication.

Base URL: `http://localhost:8080`

Authentication: Most endpoints require JWT Bearer token.

**Key Features**:
- RESTful design with JSON request/response format
- JWT-based stateless authentication
- Comprehensive error handling with consistent error format
- OpenAPI/Swagger documentation for interactive exploration
- Support for both local and cloud-hosted PostgreSQL databases
- Rate limiting and CORS configuration for production deployments

---

## Endpoints

### Auth

- POST `/api/auth/register`
  - Body: `{ name, email, password }`
  - 200: `{ token, id, name, email, role, isAdmin }`
  - Notes: Server assigns `role=USER` and `isAdmin=false`.

- POST `/api/auth/login`
  - Body: `{ email, password }`
  - 200: same response as register

- POST `/api/auth/validate`
  - Header: `Authorization: Bearer <token>`
  - 200: same response as register

### Reference

- GET `/api/countries?q=&page=&size=`
  - 200: list of countries for selectors

### Tariff

- GET `/api/tariff-rate/lookup?importerIso2=&originIso2=&hsCode=`
  - 200: `{ mfn: {...}, pref: {...}|null, agreement: {...}|null }`
  - Notes: Simple selection (no date window logic yet).
- GET `/api/tariff-rate/subcategories?importerIso3=&originIso3=&hsCode=&limit=`
  - 200: `[ { importerIso3, originIso3, hsCode, rates: [...] }, ... ]`
  - Notes: Returns every HS subcode (default limit 200) matching the prefix plus MFN/PREF options for each.

- POST `/api/tariff-rate/calculate`
  - Body: `{ totalValue, mfnRate, prefRate, rvcThreshold, materialCost, labourCost, overheadCost, profit, otherCosts, fob }`
  - 200: `{ basis, appliedRate, totalDuty, rvc, rvcThreshold }`
  - Logic: `RVC = (material+labour+overhead+profit+other)/fob*100`; if `RVC >= rvcThreshold` and `prefRate` present then `basis=PREF`, else `basis=MFN`.

---

## Usage Examples

```bash
# Register
curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alice","email":"alice@example.com","password":"secret123"}'

# Login
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"secret123"}'

# Countries
curl -s http://localhost:8080/api/countries \
  -H "Authorization: Bearer YOUR_JWT"

# Lookup (seeded example)
curl -s "http://localhost:8080/api/tariff-rate/lookup?importerIso2=EU&originIso2=KR&hsCode=870380" \
  -H "Authorization: Bearer YOUR_JWT"

# Calculator
curl -s -X POST http://localhost:8080/api/tariff-rate/calculate \
  -H 'Content-Type: application/json' -H "Authorization: Bearer YOUR_JWT" \
  -d '{"mfnRate":0.10,"prefRate":0.00,"rvcThreshold":40,"totalValue":1000,"materialCost":20,"labourCost":10,"overheadCost":10,"profit":5,"otherCosts":5,"fob":100}'

# Response example
{"basis":"PREF","appliedRate":0.00,"totalDuty":0.00,"rvc":50.00,"rvcThreshold":40}
```

---

### 5. List Conversations

Retrieve every conversation for the authenticated user ordered by most recent activity.

**Endpoint**: `GET /api/chatbot/conversations`

**Authentication**: Required (Bearer token)

**Success Response** (200 OK):
```json
[
  {
    "conversationId": "b879d6c1-1b4a-4f39-a2d9-1f4debc22a9a",
    "createdAt": "2025-10-19T14:00:00",
    "updatedAt": "2025-10-19T14:05:12"
  },
  {
    "conversationId": "af2c1b91-45e3-4f63-8b4a-9c78e6b5a210",
    "createdAt": "2025-10-18T09:12:44",
    "updatedAt": "2025-10-18T09:30:10"
  }
]
```

---

### 6. Get Conversation Details

Get detailed information about a specific conversation including full message history.

**Endpoint**: `GET /api/chatbot/conversations/{conversationId}`

**Authentication**: Required (Bearer token)

**Path Parameters**:
- `conversationId` (string): The ID of the conversation to retrieve

**Success Response** (200 OK):
```json
{
  "conversationId": "b879d6c1-1b4a-4f39-a2d9-1f4debc22a9a",
  "createdAt": "2025-10-19T14:00:00",
  "updatedAt": "2025-10-19T14:05:12",
  "messages": [
    {
      "role": "user",
      "content": "What's the tariff rate for importing steel from China to USA?",
      "createdAt": "2025-10-19T14:00:00"
    },
    {
      "role": "assistant",
      "content": "The MFN rate for HS 7208.10 into the USA is 0%...",
      "createdAt": "2025-10-19T14:00:02"
    }
  ]
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

### 7. Delete Conversation

Delete a specific conversation and its message history.

**Endpoint**: `DELETE /api/chatbot/conversations/{conversationId}`

**Authentication**: Required (Bearer token)

**Path Parameters**:
- `conversationId` (string): The ID of the conversation to delete

**Success Response** (204 No Content)

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

### 9. Tariff Subcategory Tool
**Tool Name**: `TariffSubcategoryFunction`

**Purpose**: Given an importer, optional origin, and HS prefix, return every more specific HS subcode plus its MFN/PREF options.

**Example Queries**:
- "Show all HS8 subcategories under 850760 for USA vs KOR"
- "List detailed HS codes beneath 870380 for Canada"
- "Break down 854430 subcodes for USA imports"

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

## Recent Improvements

- Enhanced tariff calculator API with improved validation and detailed error messages
- Optimized database queries for both local and cloud-hosted PostgreSQL environments
- Improved authentication flow with better error handling and JWT validation
- Better connection pooling for serverless and cloud database providers
- Optional AI Assistant endpoints with specialized tools for tariff analysis
- OpenAI integration for intelligent query processing and recommendations
- Conversation history management for contextual assistance
- Rate limiting capabilities for production deployments
- Comprehensive API documentation with OpenAPI/Swagger specifications
