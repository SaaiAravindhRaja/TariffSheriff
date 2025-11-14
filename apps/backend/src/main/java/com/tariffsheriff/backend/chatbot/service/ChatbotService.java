package com.tariffsheriff.backend.chatbot.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.tariffsheriff.backend.chatbot.dto.ChatConversationDetailDto;
import com.tariffsheriff.backend.chatbot.dto.ChatConversationSummaryDto;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryRequest;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryResponse;
import com.tariffsheriff.backend.chatbot.exception.ChatbotException;
import com.tariffsheriff.backend.chatbot.model.ChatConversation;
import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.dto.TariffRateOptionDto;
import com.tariffsheriff.backend.tariff.exception.TariffRateNotFoundException;
import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.model.HsProduct;
import com.tariffsheriff.backend.tariff.service.AgreementService;
import com.tariffsheriff.backend.tariff.service.HsProductService;
import com.tariffsheriff.backend.tariff.service.TariffRateService;

/**
 * Lightweight chatbot orchestrator: loads stored history, prepends the engineered
 * system prompt, calls the LLM, and persists the result.
 */
@Service
public class ChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);

    private static final String SYSTEM_PROMPT = (
        """
        You are TariffSheriff, the in-app teammate who knows how TariffSheriff’s data and workflows fit together. Keep every response grounded in TariffSheriff APIs—never invent numbers.

        ## How to Think
        1. Clarify the user's intent (HS exploration, one-off tariff lookup, calculator support, agreements, comparisons, news recap).
        2. Note which inputs you already have (importer/origin ISO3, HS code, calculator values) and gather only what’s missing.
        3. Choose the tool—or no tool—that best fits the intent:
           • Free-text product → `HsSearchFunction`.
           • Need a more precise suffix for a known HS6 or a lookup failed → `TariffSubcategoryFunction`.
           • Have importer/origin + **8/10 digit HS code** and need rates → `TariffLookupFunction`.
           • Need country agreements/RVC thresholds → `AgreementLookupFunction`.
           • Calculator help → stay in conversation; walk through the required inputs in order before computing.
        4. If a tool can't find data (e.g., HS not stored for that importer), say so plainly (“TariffSheriff doesn’t store USA 850760 yet…”) and outline the recovery path (HS search → subcategories → retry lookup).
        5. Summarize results clearly (MFN vs PREF, RVC thresholds, agreements, non-ad-valorem notes, assumptions) and end with a practical next action.

        ## Data Basics
        - HS codes: treat 6 digits as canonical. Only cite HS8/HS10 if you retrieved them via the subcategory tool.
        - Not every importer has every HS line. Explain gaps and propose next steps.
        - MFN is baseline; preferential applies only when an agreement covers the origin AND rules of origin (e.g., RVC) are met.
        - Non-ad-valorem duties appear as text; include them beside percentages.
        - Use the latest valid tariff entry unless the user specifies a date.

        ## Calculator Guidance
        - RVC = (material + labour + overhead + profit + otherCosts) / FOB × 100.
        - Preferential applies only if `prefRate` exists AND RVC ≥ threshold; otherwise MFN.
        - Applied duty = appliedRate × totalValue.
        - Ask for inputs logically (total value → cost breakdown → FOB → MFN/PREF → RVC threshold). Don’t assume values.

        ## Tool Recap
        - `HsSearchFunction(description, limit?)`: use for free-text products or when lookups fail.
        - `TariffSubcategoryFunction(importerIso3, originIso3?, hsCode, limit?)`: drill from HS6 to HS8/HS10 before re-running tariff lookups.
        - `TariffLookupFunction(importerIso3, originIso3?, hsCode)`: fetch MFN/PREF options and agreements once the code is confirmed.
        - `AgreementLookupFunction(countryIso3)`: list agreements and RVC thresholds for a country.

        ## Tone & Recovery
        - Be concise; use bullet lists or tables for clarity.
        - When data is missing, explain why and give concrete next steps (run HS search, try a nearby HS6, consult HTS.gov if TariffSheriff truly lacks it).
        - Always end with an actionable suggestion tailored to the user’s goal. No legal advice.
        """
    );

    private final LlmClient llmClient;
    private final ConversationService conversationService;
    private final TariffRateService tariffRateService;
    private final HsProductService hsProductService;
    private final AgreementService agreementService;

    public ChatbotService(LlmClient llmClient,
                          ConversationService conversationService,
                          TariffRateService tariffRateService,
                          HsProductService hsProductService,
                          AgreementService agreementService) {
        this.llmClient = llmClient;
        this.conversationService = conversationService;
        this.tariffRateService = tariffRateService;
        this.hsProductService = hsProductService;
        this.agreementService = agreementService;
    }

    public ChatQueryResponse processQuery(ChatQueryRequest request, String userEmail) {
        long startedAt = System.currentTimeMillis();
        validateQuery(request.getQuery());

        ChatConversation conversation = conversationService
                .ensureConversation(request.getConversationId(), userEmail);
        List<ChatQueryRequest.ChatMessage> history = conversationService.loadHistory(conversation);

        ChatCompletionCreateParams.Builder builder = llmClient.newChatBuilder()
                .addSystemMessage(SYSTEM_PROMPT)
                .addTool(TariffLookupFunction.class)
                .addTool(HsSearchFunction.class)
                .addTool(AgreementLookupFunction.class)
                .addTool(TariffSubcategoryFunction.class);

        for (ChatQueryRequest.ChatMessage msg : history) {
            if (msg == null || !StringUtils.hasText(msg.getContent())) {
                continue;
            }
            if ("assistant".equalsIgnoreCase(msg.getRole())) {
                builder.addAssistantMessage(msg.getContent());
            } else {
                builder.addUserMessage(msg.getContent());
            }
        }

        builder.addUserMessage(request.getQuery());

        List<String> toolsUsed = new ArrayList<>();
        ChatCompletion completion;

        while (true) {
            completion = llmClient.createCompletion(builder.build());
            completion.choices().stream()
                    .map(choice -> choice.message())
                    .forEach(builder::addMessage);

            if (!handleToolCalls(completion, builder, toolsUsed)) {
                break;
            }
        }

        String assistantReply = llmClient.extractContent(completion);

        conversationService.appendExchange(conversation, request.getQuery(), assistantReply);

        ChatQueryResponse response = new ChatQueryResponse(assistantReply, conversation.getPublicId().toString());
        response.setProcessingTimeMs(System.currentTimeMillis() - startedAt);
        response.setToolsUsed(toolsUsed.isEmpty() ? List.of("openai") : toolsUsed);
        return response;
    }

    private boolean handleToolCalls(ChatCompletion completion,
                                    ChatCompletionCreateParams.Builder builder,
                                    List<String> toolsUsed) {
        var toolCallsOpt = completion.choices().get(0).message().toolCalls();
        if (toolCallsOpt.isEmpty() || toolCallsOpt.get().isEmpty()) {
            return false;
        }

        for (ChatCompletionMessageToolCall toolCall : toolCallsOpt.get()) {
            ChatCompletionMessageFunctionToolCall functionCall = toolCall.asFunction();
            Object payload = callFunction(functionCall.function(), toolsUsed);
            builder.addMessage(ChatCompletionToolMessageParam.builder()
                    .toolCallId(functionCall.id())
                    .contentAsJson(payload)
                    .build());
        }
        return true;
    }

    public boolean isHealthy() {
        return llmClient.isEnabled();
    }

    public List<String> getCapabilities() {
        return List.of("chat", "history");
    }

    public List<ChatConversationSummaryDto> listConversations(String userEmail) {
        return conversationService.listSummaries(userEmail);
    }

    public ChatConversationDetailDto getConversationDetail(String conversationId, String userEmail) {
        return conversationService.getConversationDetail(conversationId, userEmail);
    }

    public void deleteConversation(String conversationId, String userEmail) {
        conversationService.deleteConversation(conversationId, userEmail);
    }

    private void validateQuery(String query) {
        if (!StringUtils.hasText(query)) {
            throw new ChatbotException("Please enter a question before sending it.");
        }
        if (query.length() > 4000) {
            throw new ChatbotException("Your question is too long.", "Try shortening it and resubmit.");
        }
    }

    private Object callFunction(ChatCompletionMessageFunctionToolCall.Function function, List<String> toolsUsed) {
        String name = function.name();
        toolsUsed.add(name);
        try {
            return switch (name) {
                case "TariffLookupFunction" -> handleTariffLookup(function.arguments(TariffLookupFunction.class));
                case "HsSearchFunction" -> handleHsSearch(function.arguments(HsSearchFunction.class));
                case "AgreementLookupFunction" -> handleAgreementLookup(function.arguments(AgreementLookupFunction.class));
                case "TariffSubcategoryFunction" -> handleTariffSubcategories(function.arguments(TariffSubcategoryFunction.class));
                default -> throw new ChatbotException(
                        "Unknown tool invoked: " + name,
                        "Please try a different request or contact support.");
            };
        } catch (IllegalArgumentException ex) {
            throw new ChatbotException("Tool arguments were invalid.", ex.getMessage(), ex);
        } catch (TariffRateNotFoundException ex) {
            String message = ex.getMessage() != null ? ex.getMessage()
                    : "I couldn't find tariff data for that HS code and country.";
            String suggestion = """

Try one of these steps:
• Run the HS Search tool with a short product description (e.g., \"lithium battery pack\") to confirm the available codes for the importer.
• Provide a nearby HS6 prefix that exists in TariffSheriff (you can ask me for suggestions if unsure).
• If you already know the precise HS8/HS10, share it so I can attempt a more specific lookup.""";
            throw new ChatbotException(message, suggestion, ex);
        }
    }

    private TariffLookupResult handleTariffLookup(TariffLookupFunction args) {
        if (!StringUtils.hasText(args.importerIso3) || !StringUtils.hasText(args.hsCode)) {
            throw new ChatbotException("Importer ISO3 and HS code are required for tariff lookup.");
        }
        String importer = normalizeIso(args.importerIso3);
        String origin = StringUtils.hasText(args.originIso3) ? normalizeIso(args.originIso3) : null;
        String hsCode = sanitizeHsCode(args.hsCode);
        try {
            TariffRateLookupDto dto = tariffRateService.getTariffRateWithAgreement(importer, origin, hsCode);

            TariffLookupResult result = new TariffLookupResult();
            result.importerIso3 = dto.importerIso3();
            result.originIso3 = dto.originIso3();
            result.hsCode = dto.hsCode();
            result.rates = dto.rates().stream()
                    .map(rate -> {
                        TariffRateSummary summary = new TariffRateSummary();
                        summary.basis = rate.basis();
                        summary.adValoremRate = rate.adValoremRate();
                        summary.nonAdValorem = rate.nonAdValorem();
                        summary.nonAdValoremText = rate.nonAdValoremText();
                        summary.agreementName = rate.agreementName();
                        summary.rvcThreshold = rate.rvcThreshold();
                        return summary;
                    })
                    .toList();
            return result;
        } catch (TariffRateNotFoundException ex) {
            logger.info("Tariff lookup missing for importer {} origin {} hs {}", importer, origin, hsCode);
            TariffLookupResult fallback = new TariffLookupResult();
            fallback.importerIso3 = importer;
            fallback.originIso3 = origin;
            fallback.hsCode = hsCode;
            fallback.rates = java.util.List.of();
            fallback.note = "TariffSheriff does not store HS " + hsCode + " for "
                    + importer + (origin != null ? " <- " + origin : "")
                    + ". Run HsSearchFunction to confirm the HS6 prefix, then use TariffSubcategoryFunction before retrying.";
            return fallback;
        }
    }

    private List<HsSearchResult> handleHsSearch(HsSearchFunction args) {
        if (!StringUtils.hasText(args.description)) {
            throw new ChatbotException("Description is required for HS search.");
        }
        int limit = args.limit != null ? Math.max(1, Math.min(args.limit, 10)) : 5;
        List<HsProduct> matches = hsProductService.searchByDescription(args.description, limit);
        return matches.stream().map(product -> {
            HsSearchResult res = new HsSearchResult();
            res.hsCode = product.getHsCode();
            res.label = product.getHsLabel();
            res.destinationIso3 = product.getDestinationIso3();
            return res;
        }).toList();
    }

    private List<AgreementSummary> handleAgreementLookup(AgreementLookupFunction args) {
        if (!StringUtils.hasText(args.countryIso3)) {
            throw new ChatbotException("Country ISO3 is required for agreement lookup.");
        }
        String iso = normalizeIso(args.countryIso3);
        List<Agreement> agreements = agreementService.getAgreementsByCountry(iso);
        return agreements.stream().map(agreement -> {
            AgreementSummary summary = new AgreementSummary();
            summary.name = agreement.getName();
            summary.rvcThreshold = agreement.getRvcThreshold();
            return summary;
        }).toList();
    }

    private List<SubcategoryResult> handleTariffSubcategories(TariffSubcategoryFunction args) {
        if (!StringUtils.hasText(args.importerIso3) || !StringUtils.hasText(args.hsCode)) {
            throw new ChatbotException("Importer ISO3 and HS code prefix are required for subcategory lookup.");
        }
        String importer = normalizeIso(args.importerIso3);
        String origin = StringUtils.hasText(args.originIso3) ? normalizeIso(args.originIso3) : null;
        String prefix = sanitizeHsCode(args.hsCode);
        int limit = args.limit != null ? Math.max(1, Math.min(args.limit, 500)) : 200;
        List<TariffRateLookupDto> lookups = tariffRateService.getSubcategories(importer, origin, prefix, limit);
        return lookups.stream().map(dto -> {
            SubcategoryResult result = new SubcategoryResult();
            result.importerIso3 = dto.importerIso3();
            result.originIso3 = dto.originIso3();
            result.hsCode = dto.hsCode();
            result.rates = dto.rates().stream()
                    .map(rate -> {
                        TariffRateSummary summary = new TariffRateSummary();
                        summary.basis = rate.basis();
                        summary.adValoremRate = rate.adValoremRate();
                        summary.nonAdValorem = rate.nonAdValorem();
                        summary.nonAdValoremText = rate.nonAdValoremText();
                        summary.agreementName = rate.agreementName();
                        summary.rvcThreshold = rate.rvcThreshold();
                        return summary;
                    })
                    .toList();
            return result;
        }).toList();
    }

    private String normalizeIso(String iso) {
        return iso == null ? null : iso.trim().toUpperCase();
    }

    private String sanitizeHsCode(String code) {
        if (code == null) return null;
        String digits = code.replaceAll("[^0-9]", "");
        if (digits.length() < 4) {
            throw new IllegalArgumentException("HS code must contain digits");
        }
        return digits;
    }

    @JsonClassDescription("Look up tariff rates for an importer/origin HS code combination.")
    static class TariffLookupFunction {
        @JsonPropertyDescription("Importer/destination ISO3 code (e.g., USA)")
        public String importerIso3;
        @JsonPropertyDescription("Origin ISO3 code (e.g., KOR) - optional")
        public String originIso3;
        @JsonPropertyDescription("HS code (digits only)")
        public String hsCode;
    }

    static class TariffLookupResult {
        public String importerIso3;
        public String originIso3;
        public String hsCode;
        public List<TariffRateSummary> rates;
        public String note;
    }

    static class TariffRateSummary {
        public String basis;
        public java.math.BigDecimal adValoremRate;
        public boolean nonAdValorem;
        public String nonAdValoremText;
        public String agreementName;
        public java.math.BigDecimal rvcThreshold;
    }

    @JsonClassDescription("Search HS codes by description.")
    static class HsSearchFunction {
        @JsonPropertyDescription("Product description to search for")
        public String description;
        @JsonPropertyDescription("Maximum number of results (1-10)")
        public Integer limit;
    }

    static class HsSearchResult {
        public String hsCode;
        public String label;
        public String destinationIso3;
    }

    @JsonClassDescription("List trade agreements for a country.")
    static class AgreementLookupFunction {
        @JsonPropertyDescription("Country ISO3 code")
        public String countryIso3;
    }

    static class AgreementSummary {
        public String name;
        public java.math.BigDecimal rvcThreshold;
    }

    @JsonClassDescription("List all more-specific HS subcategories for an importer/origin pair.")
    static class TariffSubcategoryFunction {
        @JsonPropertyDescription("Importer/destination ISO3 code (e.g., USA)")
        public String importerIso3;
        @JsonPropertyDescription("Origin ISO3 code (optional)")
        public String originIso3;
        @JsonPropertyDescription("HS code prefix (at least 4 digits, ideally 6)")
        public String hsCode;
        @JsonPropertyDescription("Maximum number of subcategories to return (default 200, max 500)")
        public Integer limit;
    }

    static class SubcategoryResult {
        public String importerIso3;
        public String originIso3;
        public String hsCode;
        public List<TariffRateSummary> rates;
    }
}
