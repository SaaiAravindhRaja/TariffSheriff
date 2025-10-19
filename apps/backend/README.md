# Backend

Spring Boot application for TariffSheriff with AI-powered chatbot assistant.

## Database & Migrations

- Uses PostgreSQL with Flyway. The core schema lives in `src/main/resources/db/migration`.
- `V1__schema.sql` creates the canonical tariff tables (countries, HS products, tariff rates, VAT, etc.).
- `V2__seed_mock.sql` inserts a small EV-focused dataset (EU importer, Korea preference) for smoke testing.
- MFN rates always exist with `origin_id` `NULL` (applies to any origin). Preferential rates require an `agreement_id` and only replace MFN when RoO + certificate checks are satisfied in the UI/business logic.
- All rates are valid-date bounded (`valid_from`, optional `valid_to`) and VAT is stored separately in the `vat` table.

### Running locally

1. **Provision PostgreSQL** and create a database (default JDBC URI `jdbc:postgresql://localhost:5432/tariffsheriff`).

2. **Configure Environment Variables**: Create a `.env` file in `apps/backend/` with the following:

```bash
# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/tariffsheriff
DATABASE_USERNAME=your_db_user
DATABASE_PASSWORD=your_db_password

# OpenAI Configuration (Required for AI Assistant)
OPENAI_API_KEY=your-openai-api-key-here
OPENAI_MODEL=gpt-4o-mini
OPENAI_TEMPERATURE=0.7
OPENAI_MAX_TOKENS=1000
OPENAI_TIMEOUT_MS=30000
OPENAI_MAX_RETRIES=2

# JWT Configuration
JWT_SECRET=your-jwt-secret-here

# Server Configuration
SERVER_PORT=8080
```

3. **Get OpenAI API Key**: 
   - Sign up at https://platform.openai.com/
   - Create an API key in your account settings
   - Add the key to your `.env` file

4. **Start the application**: 
```bash
cd apps/backend
./mvnw spring-boot:run
```

On startup Flyway runs the migrations automatically. You can verify the seed data with the contract queries shared in the design doc (resolve HS product, fetch MFN, fetch preferential, retrieve VAT).

```sql
-- Resolve HS product for EU importer, HS 870380 (version 2022)
SELECT id FROM hs_product
WHERE destination_id = 1 AND hs_version = '2022' AND hs_code = '870380';

-- MFN lookup on 2024-03-01
SELECT * FROM tariff_rate
WHERE importer_id = 1 AND hs_product_id = 1 AND basis = 'MFN'
  AND origin_id IS NULL AND valid_from <= DATE '2024-03-01'
  AND (valid_to IS NULL OR valid_to >= DATE '2024-03-01')
ORDER BY valid_from DESC
LIMIT 1;

-- Preferential lookup for Korea origin
SELECT * FROM tariff_rate
WHERE importer_id = 1 AND origin_id = 2 AND hs_product_id = 1 AND basis = 'PREF'
  AND valid_from <= DATE '2024-03-01'
  AND (valid_to IS NULL OR valid_to >= DATE '2024-03-01')
ORDER BY valid_from DESC
LIMIT 1;

-- VAT for EU importer
SELECT standard_rate FROM vat WHERE importer_id = 1;
```

### Running backend tests

These tests use Testcontainers to launch PostgreSQL automatically. Ensure Docker is running (Docker Desktop or Colima).

```bash
# if you are using Colima, expose the socket for JVM-based tools
export DOCKER_HOST="$(docker context inspect --format '{{.Endpoints.docker.Host}}')"

cd apps/backend
mvn test
```

The Maven build disables Ryuk cleanup (via `TESTCONTAINERS_RYUK_DISABLED=true`) so Testcontainers works with rootless Docker setups such as Colima. Containers are still stopped at the end of the test run.

## AI Assistant

The backend includes an AI-powered chatbot assistant that can answer natural language queries about tariffs, trade agreements, HS codes, and more.

### Features

- **Natural Language Processing**: Ask questions in plain English
- **Automatic Tool Selection**: AI automatically selects the right data source
- **Conversation Context**: Maintains context across multiple messages
- **9 Specialized Tools**: Tariff lookup, HS code search, trade agreements, country info, and more
- **Rate Limiting**: 10 requests/minute, 100 requests/hour per user
- **Error Handling**: User-friendly error messages with helpful suggestions

### Quick Start

1. **Configure OpenAI**: Add your OpenAI API key to `.env` (see setup instructions above)

2. **Start the backend**: The AI Assistant is automatically available at `/api/chatbot`

3. **Test the API**: Use curl or Postman to send queries:

```bash
# Login first to get JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password"}'

# Query the AI Assistant
curl -X POST http://localhost:8080/api/chatbot/query \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query": "What is the tariff rate for importing steel from China to USA?"}'
```

4. **Check health**: Verify the AI Assistant is running:

```bash
curl http://localhost:8080/api/chatbot/health \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Available Endpoints

- `POST /api/chatbot/query` - Process a natural language query
- `GET /api/chatbot/health` - Check service health
- `GET /api/chatbot/rate-limit-status` - Check your rate limit status
- `GET /api/chatbot/conversations` - Get conversation history
- `GET /api/chatbot/conversations/{id}` - Get specific conversation
- `DELETE /api/chatbot/conversations/{id}` - Delete conversation

### Example Queries

The AI Assistant can answer questions like:

- "What's the tariff rate for importing steel from China to USA?"
- "Find HS code for coffee beans"
- "What trade agreements does the US have?"
- "Compare tariff rates for electronics from China vs Mexico"
- "List all available countries"
- "What are the compliance requirements for importing from Japan?"

### Documentation

- **API Documentation**: See [docs/api/README.md](../../docs/api/README.md) for complete API reference
- **Tools Documentation**: See [docs/api/TOOLS.md](../../docs/api/TOOLS.md) for details on available tools
- **OpenAPI/Swagger**: Visit http://localhost:8080/swagger-ui.html when running

### Configuration

The AI Assistant can be configured in `application.properties`:

```properties
# OpenAI Configuration
openai.api-key=${OPENAI_API_KEY}
openai.model=${OPENAI_MODEL:gpt-4o-mini}
openai.temperature=${OPENAI_TEMPERATURE:0.7}
openai.max-tokens=${OPENAI_MAX_TOKENS:1000}
openai.timeout-ms=${OPENAI_TIMEOUT_MS:30000}
openai.max-retries=${OPENAI_MAX_RETRIES:2}

# Tool Configuration
chatbot.tools.default-timeout-ms=10000
chatbot.tools.max-concurrent-executions=5
chatbot.tools.enable-health-checks=false
```

### Architecture

The AI Assistant uses a simple 3-phase flow:

1. **Understand**: LLM analyzes query and selects appropriate tool
2. **Execute**: Selected tool fetches data from database
3. **Respond**: LLM generates conversational response

**Key Components**:
- `ChatbotService` - Main orchestrator
- `LlmClient` - OpenAI integration
- `ToolRegistry` - Tool management and discovery
- `ConversationService` - Conversation history
- `RateLimitService` - Rate limiting
- `Tools` - Data access layer (9 specialized tools)

### Troubleshooting

**"LLM service error"**:
- Check that `OPENAI_API_KEY` is set correctly
- Verify your OpenAI account has credits
- Check network connectivity to OpenAI API

**"Tool execution error"**:
- Verify database is running and accessible
- Check database migrations have run successfully
- Review logs for specific error details

**"Rate limit exceeded"**:
- Wait for the rate limit window to reset
- Check your current status at `/api/chatbot/rate-limit-status`

**No response or timeout**:
- Increase `OPENAI_TIMEOUT_MS` in configuration
- Check OpenAI API status at https://status.openai.com/
- Review application logs for errors
