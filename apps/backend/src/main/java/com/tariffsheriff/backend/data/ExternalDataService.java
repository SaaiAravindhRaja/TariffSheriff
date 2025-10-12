package com.tariffsheriff.backend.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for integrating real-time data from external sources
 * Features:
 * - News API integration for trade-related news and events
 * - Market data API connections for pricing and volume information
 * - Regulatory API integration for policy and rule updates
 * - API rate limiting and quota management system
 */
@Service
public class ExternalDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalDataService.class);
    
    // Configuration
    @Value("${external.data.news.api.key:}")
    private String newsApiKey;
    
    @Value("${external.data.news.api.url:https://newsapi.org/v2}")
    private String newsApiUrl;
    
    @Value("${external.data.market.api.key:}")
    private String marketApiKey;
    
    @Value("${external.data.market.api.url:https://api.marketdata.com}")
    private String marketApiUrl;
    
    @Value("${external.data.regulatory.api.key:}")
    private String regulatoryApiKey;
    
    @Value("${external.data.regulatory.api.url:https://api.trade.gov}")
    private String regulatoryApiUrl;
    
    @Value("${external.data.cache.ttl.minutes:30}")
    private long cacheTtlMinutes;
    
    @Value("${external.data.timeout.seconds:10}")
    private int timeoutSeconds;
    
    // Dependencies
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // Rate limiting
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> quotaCounters = new ConcurrentHashMap<>();
    
    // Caching
    private final Map<String, CachedData> dataCache = new ConcurrentHashMap<>();
    
    // Executors
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(5);
    
    public ExternalDataService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        
        initializeRateLimiters();
        
        // Schedule cache cleanup every 15 minutes
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredCache, 15, 15, TimeUnit.MINUTES);
    }
    
    /**
     * Initialize rate limiters for different APIs
     */
    private void initializeRateLimiters() {
        // News API: 1000 requests per hour
        rateLimiters.put("news", new RateLimiter(1000, Duration.ofHours(1)));
        
        // Market Data API: 500 requests per hour
        rateLimiters.put("market", new RateLimiter(500, Duration.ofHours(1)));
        
        // Regulatory API: 200 requests per hour
        rateLimiters.put("regulatory", new RateLimiter(200, Duration.ofHours(1)));
    }
    
    /**
     * Get trade-related news for specific topics
     */
    public CompletableFuture<List<NewsItem>> getTradeNews(String query, String country, int maxResults) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!rateLimiters.get("news").tryAcquire()) {
                    logger.warn("News API rate limit exceeded");
                    return getCachedNews(query, country);
                }
                
                String cacheKey = String.format("news_%s_%s_%d", query, country, maxResults);
                CachedData cached = dataCache.get(cacheKey);
                
                if (cached != null && !cached.isExpired()) {
                    return (List<NewsItem>) cached.data;
                }
                
                List<NewsItem> news = fetchNewsFromApi(query, country, maxResults);
                dataCache.put(cacheKey, new CachedData(news, Instant.now()));
                
                return news;
                
            } catch (Exception e) {
                logger.error("Error fetching trade news", e);
                return getCachedNews(query, country);
            }
        }, asyncExecutor);
    }
    
    /**
     * Get market data for specific products or commodities
     */
    public CompletableFuture<MarketData> getMarketData(String productCode, String market) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!rateLimiters.get("market").tryAcquire()) {
                    logger.warn("Market API rate limit exceeded");
                    return getCachedMarketData(productCode, market);
                }
                
                String cacheKey = String.format("market_%s_%s", productCode, market);
                CachedData cached = dataCache.get(cacheKey);
                
                if (cached != null && !cached.isExpired()) {
                    return (MarketData) cached.data;
                }
                
                MarketData marketData = fetchMarketDataFromApi(productCode, market);
                dataCache.put(cacheKey, new CachedData(marketData, Instant.now()));
                
                return marketData;
                
            } catch (Exception e) {
                logger.error("Error fetching market data", e);
                return getCachedMarketData(productCode, market);
            }
        }, asyncExecutor);
    }
    
    /**
     * Get regulatory updates for specific countries or trade agreements
     */
    public CompletableFuture<List<RegulatoryUpdate>> getRegulatoryUpdates(String country, String agreementType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!rateLimiters.get("regulatory").tryAcquire()) {
                    logger.warn("Regulatory API rate limit exceeded");
                    return getCachedRegulatoryUpdates(country, agreementType);
                }
                
                String cacheKey = String.format("regulatory_%s_%s", country, agreementType);
                CachedData cached = dataCache.get(cacheKey);
                
                if (cached != null && !cached.isExpired()) {
                    return (List<RegulatoryUpdate>) cached.data;
                }
                
                List<RegulatoryUpdate> updates = fetchRegulatoryUpdatesFromApi(country, agreementType);
                dataCache.put(cacheKey, new CachedData(updates, Instant.now()));
                
                return updates;
                
            } catch (Exception e) {
                logger.error("Error fetching regulatory updates", e);
                return getCachedRegulatoryUpdates(country, agreementType);
            }
        }, asyncExecutor);
    }
    
    /**
     * Get comprehensive trade intelligence combining multiple data sources
     */
    public CompletableFuture<TradeIntelligence> getTradeIntelligence(String productCode, String originCountry, String destinationCountry) {
        CompletableFuture<List<NewsItem>> newsFuture = getTradeNews(
            String.format("%s trade %s %s", productCode, originCountry, destinationCountry), 
            originCountry, 5);
        
        CompletableFuture<MarketData> marketFuture = getMarketData(productCode, originCountry);
        
        CompletableFuture<List<RegulatoryUpdate>> regulatoryFuture = getRegulatoryUpdates(
            destinationCountry, "bilateral");
        
        return CompletableFuture.allOf(newsFuture, marketFuture, regulatoryFuture)
            .thenApply(v -> new TradeIntelligence(
                newsFuture.join(),
                marketFuture.join(),
                regulatoryFuture.join(),
                Instant.now()
            ));
    } 
   /**
     * Fetch news from external news API
     */
    private List<NewsItem> fetchNewsFromApi(String query, String country, int maxResults) {
        try {
            String url = String.format("%s/everything?q=%s+trade+%s&sortBy=publishedAt&pageSize=%d&apiKey=%s",
                newsApiUrl, query, country, maxResults, newsApiKey);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return parseNewsResponse(response.getBody());
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error fetching news: {}", e.getStatusCode());
        } catch (ResourceAccessException e) {
            logger.error("Network error fetching news", e);
        } catch (Exception e) {
            logger.error("Unexpected error fetching news", e);
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Fetch market data from external market API
     */
    private MarketData fetchMarketDataFromApi(String productCode, String market) {
        try {
            String url = String.format("%s/v1/commodities/%s?market=%s&apiKey=%s",
                marketApiUrl, productCode, market, marketApiKey);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return parseMarketDataResponse(response.getBody());
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error fetching market data: {}", e.getStatusCode());
        } catch (ResourceAccessException e) {
            logger.error("Network error fetching market data", e);
        } catch (Exception e) {
            logger.error("Unexpected error fetching market data", e);
        }
        
        return new MarketData(productCode, market, 0.0, 0.0, 0L, Instant.now());
    }
    
    /**
     * Fetch regulatory updates from external regulatory API
     */
    private List<RegulatoryUpdate> fetchRegulatoryUpdatesFromApi(String country, String agreementType) {
        try {
            String url = String.format("%s/v1/trade_agreements?country=%s&type=%s&apiKey=%s",
                regulatoryApiUrl, country, agreementType, regulatoryApiKey);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return parseRegulatoryResponse(response.getBody());
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error fetching regulatory updates: {}", e.getStatusCode());
        } catch (ResourceAccessException e) {
            logger.error("Network error fetching regulatory updates", e);
        } catch (Exception e) {
            logger.error("Unexpected error fetching regulatory updates", e);
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Parse news API response
     */
    private List<NewsItem> parseNewsResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode articles = root.get("articles");
            
            List<NewsItem> newsItems = new ArrayList<>();
            if (articles != null && articles.isArray()) {
                for (JsonNode article : articles) {
                    NewsItem item = new NewsItem(
                        article.get("title").asText(),
                        article.get("description").asText(),
                        article.get("url").asText(),
                        article.get("source").get("name").asText(),
                        Instant.parse(article.get("publishedAt").asText())
                    );
                    newsItems.add(item);
                }
            }
            
            return newsItems;
            
        } catch (Exception e) {
            logger.error("Error parsing news response", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Parse market data API response
     */
    private MarketData parseMarketDataResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            
            return new MarketData(
                root.get("symbol").asText(),
                root.get("market").asText(),
                root.get("price").asDouble(),
                root.get("change").asDouble(),
                root.get("volume").asLong(),
                Instant.now()
            );
            
        } catch (Exception e) {
            logger.error("Error parsing market data response", e);
            return new MarketData("", "", 0.0, 0.0, 0L, Instant.now());
        }
    }
    
    /**
     * Parse regulatory updates API response
     */
    private List<RegulatoryUpdate> parseRegulatoryResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode updates = root.get("updates");
            
            List<RegulatoryUpdate> regulatoryUpdates = new ArrayList<>();
            if (updates != null && updates.isArray()) {
                for (JsonNode update : updates) {
                    RegulatoryUpdate item = new RegulatoryUpdate(
                        update.get("title").asText(),
                        update.get("description").asText(),
                        update.get("country").asText(),
                        update.get("type").asText(),
                        Instant.parse(update.get("effectiveDate").asText()),
                        update.get("url").asText()
                    );
                    regulatoryUpdates.add(item);
                }
            }
            
            return regulatoryUpdates;
            
        } catch (Exception e) {
            logger.error("Error parsing regulatory response", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get cached news items
     */
    private List<NewsItem> getCachedNews(String query, String country) {
        String cacheKey = String.format("news_%s_%s", query, country);
        CachedData cached = dataCache.get(cacheKey);
        
        if (cached != null) {
            return (List<NewsItem>) cached.data;
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Get cached market data
     */
    private MarketData getCachedMarketData(String productCode, String market) {
        String cacheKey = String.format("market_%s_%s", productCode, market);
        CachedData cached = dataCache.get(cacheKey);
        
        if (cached != null) {
            return (MarketData) cached.data;
        }
        
        return new MarketData(productCode, market, 0.0, 0.0, 0L, Instant.now());
    }
    
    /**
     * Get cached regulatory updates
     */
    private List<RegulatoryUpdate> getCachedRegulatoryUpdates(String country, String agreementType) {
        String cacheKey = String.format("regulatory_%s_%s", country, agreementType);
        CachedData cached = dataCache.get(cacheKey);
        
        if (cached != null) {
            return (List<RegulatoryUpdate>) cached.data;
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Clean up expired cache entries
     */
    private void cleanupExpiredCache() {
        Instant now = Instant.now();
        
        List<String> keysToRemove = dataCache.entrySet().stream()
            .filter(entry -> entry.getValue().isExpired(now))
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
        
        keysToRemove.forEach(dataCache::remove);
        
        if (!keysToRemove.isEmpty()) {
            logger.debug("Cleaned up {} expired external data cache entries", keysToRemove.size());
        }
    }
    
    /**
     * Get API usage statistics
     */
    public ExternalDataStats getUsageStats() {
        Map<String, Integer> quotas = new HashMap<>();
        Map<String, Integer> remaining = new HashMap<>();
        
        rateLimiters.forEach((api, limiter) -> {
            quotas.put(api, limiter.getLimit());
            remaining.put(api, limiter.getRemaining());
        });
        
        return new ExternalDataStats(quotas, remaining, dataCache.size());
    }
    
    /**
     * Clear all cached data
     */
    public void clearCache() {
        dataCache.clear();
        logger.info("External data cache cleared");
    }
    
    /**
     * Rate limiter implementation
     */
    private static class RateLimiter {
        private final int limit;
        private final Duration window;
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile Instant windowStart = Instant.now();
        
        public RateLimiter(int limit, Duration window) {
            this.limit = limit;
            this.window = window;
        }
        
        public synchronized boolean tryAcquire() {
            Instant now = Instant.now();
            
            // Reset window if expired
            if (now.isAfter(windowStart.plus(window))) {
                windowStart = now;
                count.set(0);
            }
            
            if (count.get() < limit) {
                count.incrementAndGet();
                return true;
            }
            
            return false;
        }
        
        public int getLimit() {
            return limit;
        }
        
        public int getRemaining() {
            return Math.max(0, limit - count.get());
        }
    }
    
    /**
     * Cached data wrapper
     */
    private class CachedData {
        final Object data;
        final Instant timestamp;
        
        CachedData(Object data, Instant timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
        
        boolean isExpired() {
            return isExpired(Instant.now());
        }
        
        boolean isExpired(Instant now) {
            return now.isAfter(timestamp.plus(Duration.ofMinutes(cacheTtlMinutes)));
        }
    }
    
    /**
     * News item data structure
     */
    public static class NewsItem {
        private final String title;
        private final String description;
        private final String url;
        private final String source;
        private final Instant publishedAt;
        
        public NewsItem(String title, String description, String url, String source, Instant publishedAt) {
            this.title = title;
            this.description = description;
            this.url = url;
            this.source = source;
            this.publishedAt = publishedAt;
        }
        
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getUrl() { return url; }
        public String getSource() { return source; }
        public Instant getPublishedAt() { return publishedAt; }
    }
    
    /**
     * Market data structure
     */
    public static class MarketData {
        private final String symbol;
        private final String market;
        private final double price;
        private final double change;
        private final long volume;
        private final Instant timestamp;
        
        public MarketData(String symbol, String market, double price, double change, long volume, Instant timestamp) {
            this.symbol = symbol;
            this.market = market;
            this.price = price;
            this.change = change;
            this.volume = volume;
            this.timestamp = timestamp;
        }
        
        public String getSymbol() { return symbol; }
        public String getMarket() { return market; }
        public double getPrice() { return price; }
        public double getChange() { return change; }
        public long getVolume() { return volume; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    /**
     * Regulatory update data structure
     */
    public static class RegulatoryUpdate {
        private final String title;
        private final String description;
        private final String country;
        private final String type;
        private final Instant effectiveDate;
        private final String url;
        
        public RegulatoryUpdate(String title, String description, String country, String type, Instant effectiveDate, String url) {
            this.title = title;
            this.description = description;
            this.country = country;
            this.type = type;
            this.effectiveDate = effectiveDate;
            this.url = url;
        }
        
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getCountry() { return country; }
        public String getType() { return type; }
        public Instant getEffectiveDate() { return effectiveDate; }
        public String getUrl() { return url; }
    }
    
    /**
     * Trade intelligence combining multiple data sources
     */
    public static class TradeIntelligence {
        private final List<NewsItem> news;
        private final MarketData marketData;
        private final List<RegulatoryUpdate> regulatoryUpdates;
        private final Instant timestamp;
        
        public TradeIntelligence(List<NewsItem> news, MarketData marketData, List<RegulatoryUpdate> regulatoryUpdates, Instant timestamp) {
            this.news = news;
            this.marketData = marketData;
            this.regulatoryUpdates = regulatoryUpdates;
            this.timestamp = timestamp;
        }
        
        public List<NewsItem> getNews() { return news; }
        public MarketData getMarketData() { return marketData; }
        public List<RegulatoryUpdate> getRegulatoryUpdates() { return regulatoryUpdates; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    /**
     * External data usage statistics
     */
    public static class ExternalDataStats {
        private final Map<String, Integer> quotas;
        private final Map<String, Integer> remaining;
        private final int cacheSize;
        
        public ExternalDataStats(Map<String, Integer> quotas, Map<String, Integer> remaining, int cacheSize) {
            this.quotas = quotas;
            this.remaining = remaining;
            this.cacheSize = cacheSize;
        }
        
        public Map<String, Integer> getQuotas() { return quotas; }
        public Map<String, Integer> getRemaining() { return remaining; }
        public int getCacheSize() { return cacheSize; }
    }
}