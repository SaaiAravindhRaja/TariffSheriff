package com.tariffsheriff.backend.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Service for real-time notifications and alerts
 * Features:
 * - Alert system for tariff rate changes and trade policy updates
 * - User subscription management for personalized notifications
 * - Notification delivery mechanisms (email, in-app, webhook)
 * - Notification history and analytics
 */
@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    // Configuration
    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;
    
    @Value("${notification.webhook.enabled:true}")
    private boolean webhookEnabled;
    
    @Value("${notification.in-app.enabled:true}")
    private boolean inAppEnabled;
    
    @Value("${notification.rate-limit.per-user:100}")
    private int rateLimitPerUser;
    
    @Value("${notification.batch.size:50}")
    private int batchSize;
    
    @Value("${notification.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    // Dependencies
    private final JavaMailSender mailSender; // Can be null if email is not configured
    private final RestTemplate restTemplate;
    
    // Subscription management
    private final Map<String, UserSubscription> userSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, List<NotificationTemplate>> notificationTemplates = new ConcurrentHashMap<>();
    
    // Notification queues
    private final BlockingQueue<NotificationRequest> emailQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<NotificationRequest> webhookQueue = new LinkedBlockingQueue<>();
    private final Map<String, BlockingQueue<InAppNotification>> inAppQueues = new ConcurrentHashMap<>();
    
    // Notification history
    private final Map<String, List<NotificationHistory>> notificationHistory = new ConcurrentHashMap<>();
    
    // Rate limiting
    private final Map<String, RateLimiter> userRateLimiters = new ConcurrentHashMap<>();
    
    // Statistics
    private final AtomicLong totalNotifications = new AtomicLong(0);
    private final AtomicLong successfulDeliveries = new AtomicLong(0);
    private final AtomicLong failedDeliveries = new AtomicLong(0);
    
    // Executors
    private final ExecutorService emailExecutor = Executors.newFixedThreadPool(3);
    private final ExecutorService webhookExecutor = Executors.newFixedThreadPool(2);
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    public NotificationService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
        this.restTemplate = new RestTemplate();
        
        initializeNotificationTemplates();
        startNotificationProcessors();
        
        // Schedule cleanup every hour
        cleanupExecutor.scheduleAtFixedRate(this::cleanupOldNotifications, 1, 1, TimeUnit.HOURS);
    }
    
    /**
     * Initialize notification templates
     */
    private void initializeNotificationTemplates() {
        // Tariff rate change templates
        List<NotificationTemplate> tariffTemplates = Arrays.asList(
            new NotificationTemplate("tariff_rate_increase", "Tariff Rate Increase Alert",
                "The tariff rate for {productCode} from {originCountry} to {destinationCountry} has increased from {oldRate}% to {newRate}%."),
            new NotificationTemplate("tariff_rate_decrease", "Tariff Rate Decrease Alert",
                "Good news! The tariff rate for {productCode} from {originCountry} to {destinationCountry} has decreased from {oldRate}% to {newRate}%."),
            new NotificationTemplate("new_tariff_schedule", "New Tariff Schedule Available",
                "A new tariff schedule is now available for {country}. Review the changes that may affect your trade operations.")
        );
        notificationTemplates.put("tariff", tariffTemplates);
        
        // Trade policy templates
        List<NotificationTemplate> policyTemplates = Arrays.asList(
            new NotificationTemplate("trade_agreement_update", "Trade Agreement Update",
                "The trade agreement between {country1} and {country2} has been updated. New provisions may affect your trade operations."),
            new NotificationTemplate("regulatory_change", "Regulatory Change Alert",
                "New trade regulations have been announced for {country}. Effective date: {effectiveDate}."),
            new NotificationTemplate("compliance_deadline", "Compliance Deadline Reminder",
                "Reminder: Compliance deadline for {requirement} is approaching on {deadline}.")
        );
        notificationTemplates.put("policy", policyTemplates);
        
        // Market intelligence templates
        List<NotificationTemplate> marketTemplates = Arrays.asList(
            new NotificationTemplate("market_opportunity", "Market Opportunity Alert",
                "A new market opportunity has been identified for {productCode} in {market}. Potential savings: {savings}."),
            new NotificationTemplate("price_volatility", "Price Volatility Alert",
                "High price volatility detected for {productCode}. Current price: {price}, Change: {change}%."),
            new NotificationTemplate("supply_chain_disruption", "Supply Chain Disruption Alert",
                "Potential supply chain disruption detected for route {originCountry} to {destinationCountry}.")
        );
        notificationTemplates.put("market", marketTemplates);
    }
    
    /**
     * Start notification processors
     */
    private void startNotificationProcessors() {
        // Email processor
        emailExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    NotificationRequest request = emailQueue.take();
                    processEmailNotification(request);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error processing email notification", e);
                }
            }
        });
        
        // Webhook processor
        webhookExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    NotificationRequest request = webhookQueue.take();
                    processWebhookNotification(request);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error processing webhook notification", e);
                }
            }
        });
    }
    
    /**
     * Subscribe user to notifications
     */
    public void subscribeUser(String userId, String email, List<String> categories, 
                            List<NotificationChannel> channels, Map<String, Object> preferences) {
        UserSubscription subscription = new UserSubscription(userId, email, categories, channels, preferences);
        userSubscriptions.put(userId, subscription);
        
        // Initialize in-app notification queue for user
        inAppQueues.put(userId, new LinkedBlockingQueue<>());
        
        // Initialize rate limiter for user
        userRateLimiters.put(userId, new RateLimiter(rateLimitPerUser, Duration.ofHours(1)));
        
        logger.info("User {} subscribed to notifications with categories: {}", userId, categories);
    }
    
    /**
     * Unsubscribe user from notifications
     */
    public void unsubscribeUser(String userId) {
        userSubscriptions.remove(userId);
        inAppQueues.remove(userId);
        userRateLimiters.remove(userId);
        
        logger.info("User {} unsubscribed from notifications", userId);
    }
    
    /**
     * Update user subscription preferences
     */
    public void updateSubscription(String userId, List<String> categories, 
                                 List<NotificationChannel> channels, Map<String, Object> preferences) {
        UserSubscription subscription = userSubscriptions.get(userId);
        if (subscription != null) {
            subscription.updatePreferences(categories, channels, preferences);
            logger.info("Updated subscription preferences for user {}", userId);
        }
    }
    
    /**
     * Send notification to specific user
     */
    public CompletableFuture<NotificationResult> sendNotification(String userId, String category, 
                                                                String templateId, Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserSubscription subscription = userSubscriptions.get(userId);
                if (subscription == null || !subscription.isSubscribedTo(category)) {
                    return new NotificationResult(false, "User not subscribed to category: " + category);
                }
                
                // Check rate limit
                RateLimiter rateLimiter = userRateLimiters.get(userId);
                if (rateLimiter != null && !rateLimiter.tryAcquire()) {
                    return new NotificationResult(false, "Rate limit exceeded for user: " + userId);
                }
                
                NotificationTemplate template = findTemplate(category, templateId);
                if (template == null) {
                    return new NotificationResult(false, "Template not found: " + templateId);
                }
                
                String message = template.formatMessage(parameters);
                
                // Send to all subscribed channels
                List<CompletableFuture<Boolean>> deliveryFutures = new ArrayList<>();
                
                for (NotificationChannel channel : subscription.getChannels()) {
                    switch (channel) {
                        case EMAIL:
                            if (emailEnabled) {
                                deliveryFutures.add(sendEmailNotification(userId, subscription.getEmail(), 
                                    template.getSubject(), message));
                            }
                            break;
                        case WEBHOOK:
                            if (webhookEnabled && subscription.getWebhookUrl() != null) {
                                deliveryFutures.add(sendWebhookNotification(userId, subscription.getWebhookUrl(), 
                                    category, templateId, parameters));
                            }
                            break;
                        case IN_APP:
                            if (inAppEnabled) {
                                deliveryFutures.add(sendInAppNotification(userId, template.getSubject(), 
                                    message, category));
                            }
                            break;
                    }
                }
                
                // Wait for all deliveries to complete
                CompletableFuture.allOf(deliveryFutures.toArray(new CompletableFuture[0])).join();
                
                boolean allSuccessful = deliveryFutures.stream()
                    .allMatch(future -> future.join());
                
                // Record notification history
                recordNotificationHistory(userId, category, templateId, message, allSuccessful);
                
                totalNotifications.incrementAndGet();
                if (allSuccessful) {
                    successfulDeliveries.incrementAndGet();
                } else {
                    failedDeliveries.incrementAndGet();
                }
                
                return new NotificationResult(allSuccessful, 
                    allSuccessful ? "Notification sent successfully" : "Some deliveries failed");
                
            } catch (Exception e) {
                logger.error("Error sending notification to user {}", userId, e);
                failedDeliveries.incrementAndGet();
                return new NotificationResult(false, "Error sending notification: " + e.getMessage());
            }
        });
    }
    
    /**
     * Send broadcast notification to all subscribed users
     */
    public CompletableFuture<BroadcastResult> sendBroadcastNotification(String category, String templateId, 
                                                                      Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> subscribedUsers = userSubscriptions.entrySet().stream()
                .filter(entry -> entry.getValue().isSubscribedTo(category))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            if (subscribedUsers.isEmpty()) {
                return new BroadcastResult(0, 0, "No users subscribed to category: " + category);
            }
            
            List<CompletableFuture<NotificationResult>> futures = subscribedUsers.stream()
                .map(userId -> sendNotification(userId, category, templateId, parameters))
                .collect(Collectors.toList());
            
            // Wait for all notifications to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            long successful = futures.stream()
                .mapToLong(future -> future.join().isSuccess() ? 1 : 0)
                .sum();
            
            return new BroadcastResult((int) successful, subscribedUsers.size() - (int) successful, 
                String.format("Broadcast sent to %d users, %d successful", subscribedUsers.size(), successful));
        });
    }  
  /**
     * Send email notification
     */
    private CompletableFuture<Boolean> sendEmailNotification(String userId, String email, String subject, String message) {
        NotificationRequest request = new NotificationRequest(userId, email, subject, message, Instant.now());
        
        try {
            emailQueue.offer(request, 5, TimeUnit.SECONDS);
            return CompletableFuture.completedFuture(true);
        } catch (InterruptedException e) {
            logger.error("Failed to queue email notification for user {}", userId, e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Send webhook notification
     */
    private CompletableFuture<Boolean> sendWebhookNotification(String userId, String webhookUrl, String category, 
                                                             String templateId, Map<String, Object> parameters) {
        NotificationRequest request = new NotificationRequest(userId, webhookUrl, category, 
            templateId, parameters, Instant.now());
        
        try {
            webhookQueue.offer(request, 5, TimeUnit.SECONDS);
            return CompletableFuture.completedFuture(true);
        } catch (InterruptedException e) {
            logger.error("Failed to queue webhook notification for user {}", userId, e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Send in-app notification
     */
    private CompletableFuture<Boolean> sendInAppNotification(String userId, String title, String message, String category) {
        BlockingQueue<InAppNotification> userQueue = inAppQueues.get(userId);
        if (userQueue == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        InAppNotification notification = new InAppNotification(
            UUID.randomUUID().toString(), title, message, category, Instant.now(), false);
        
        try {
            userQueue.offer(notification, 1, TimeUnit.SECONDS);
            return CompletableFuture.completedFuture(true);
        } catch (InterruptedException e) {
            logger.error("Failed to queue in-app notification for user {}", userId, e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Process email notification
     */
    private void processEmailNotification(NotificationRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.getRecipient());
            message.setSubject(request.getSubject());
            message.setText(request.getMessage());
            message.setFrom("noreply@tariffsheriff.com");
            
            mailSender.send(message);
            
            logger.debug("Email notification sent to {}", request.getRecipient());
            
        } catch (Exception e) {
            logger.error("Failed to send email notification to {}", request.getRecipient(), e);
            
            // Retry logic
            if (request.getRetryCount() < maxRetryAttempts) {
                request.incrementRetryCount();
                try {
                    emailQueue.offer(request, 1, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    logger.error("Failed to requeue email notification", ie);
                }
            }
        }
    }
    
    /**
     * Process webhook notification
     */
    private void processWebhookNotification(NotificationRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", request.getUserId());
            payload.put("category", request.getCategory());
            payload.put("templateId", request.getTemplateId());
            payload.put("parameters", request.getParameters());
            payload.put("timestamp", request.getTimestamp().toString());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                request.getRecipient(), HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.debug("Webhook notification sent to {}", request.getRecipient());
            } else {
                throw new RuntimeException("Webhook returned status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Failed to send webhook notification to {}", request.getRecipient(), e);
            
            // Retry logic
            if (request.getRetryCount() < maxRetryAttempts) {
                request.incrementRetryCount();
                try {
                    webhookQueue.offer(request, 1, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    logger.error("Failed to requeue webhook notification", ie);
                }
            }
        }
    }
    
    /**
     * Get in-app notifications for user
     */
    public List<InAppNotification> getInAppNotifications(String userId, boolean unreadOnly) {
        BlockingQueue<InAppNotification> userQueue = inAppQueues.get(userId);
        if (userQueue == null) {
            return Collections.emptyList();
        }
        
        return userQueue.stream()
            .filter(notification -> !unreadOnly || !notification.isRead())
            .sorted((n1, n2) -> n2.getTimestamp().compareTo(n1.getTimestamp()))
            .collect(Collectors.toList());
    }
    
    /**
     * Mark in-app notification as read
     */
    public boolean markNotificationAsRead(String userId, String notificationId) {
        BlockingQueue<InAppNotification> userQueue = inAppQueues.get(userId);
        if (userQueue == null) {
            return false;
        }
        
        return userQueue.stream()
            .filter(notification -> notification.getId().equals(notificationId))
            .findFirst()
            .map(notification -> {
                notification.markAsRead();
                return true;
            })
            .orElse(false);
    }
    
    /**
     * Clear all in-app notifications for user
     */
    public void clearInAppNotifications(String userId) {
        BlockingQueue<InAppNotification> userQueue = inAppQueues.get(userId);
        if (userQueue != null) {
            userQueue.clear();
            logger.info("Cleared in-app notifications for user {}", userId);
        }
    }
    
    /**
     * Find notification template
     */
    private NotificationTemplate findTemplate(String category, String templateId) {
        List<NotificationTemplate> templates = notificationTemplates.get(category);
        if (templates == null) {
            return null;
        }
        
        return templates.stream()
            .filter(template -> template.getId().equals(templateId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Record notification history
     */
    private void recordNotificationHistory(String userId, String category, String templateId, 
                                         String message, boolean successful) {
        NotificationHistory history = new NotificationHistory(userId, category, templateId, 
            message, successful, Instant.now());
        
        notificationHistory.computeIfAbsent(userId, k -> new ArrayList<>()).add(history);
        
        // Keep only recent history (last 1000 notifications per user)
        List<NotificationHistory> userHistory = notificationHistory.get(userId);
        if (userHistory.size() > 1000) {
            userHistory.subList(0, userHistory.size() - 1000).clear();
        }
    }
    
    /**
     * Get notification history for user
     */
    public List<NotificationHistory> getNotificationHistory(String userId, int limit) {
        List<NotificationHistory> history = notificationHistory.get(userId);
        if (history == null) {
            return Collections.emptyList();
        }
        
        return history.stream()
            .sorted((h1, h2) -> h2.getTimestamp().compareTo(h1.getTimestamp()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Clean up old notifications and history
     */
    private void cleanupOldNotifications() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(30));
        
        // Clean up in-app notifications older than 30 days
        inAppQueues.values().forEach(queue -> 
            queue.removeIf(notification -> notification.getTimestamp().isBefore(cutoff)));
        
        // Clean up notification history older than 30 days
        notificationHistory.values().forEach(history -> 
            history.removeIf(record -> record.getTimestamp().isBefore(cutoff)));
        
        logger.debug("Cleaned up old notifications and history");
    }
    
    /**
     * Get notification statistics
     */
    public NotificationStats getNotificationStats() {
        Map<String, Integer> subscriptionsByCategory = new HashMap<>();
        Map<String, Integer> subscriptionsByChannel = new HashMap<>();
        
        userSubscriptions.values().forEach(subscription -> {
            subscription.getCategories().forEach(category -> 
                subscriptionsByCategory.merge(category, 1, Integer::sum));
            
            subscription.getChannels().forEach(channel -> 
                subscriptionsByChannel.merge(channel.name(), 1, Integer::sum));
        });
        
        return new NotificationStats(
            userSubscriptions.size(),
            totalNotifications.get(),
            successfulDeliveries.get(),
            failedDeliveries.get(),
            subscriptionsByCategory,
            subscriptionsByChannel
        );
    }
    
    /**
     * Get user subscription
     */
    public UserSubscription getUserSubscription(String userId) {
        return userSubscriptions.get(userId);
    }
    
    /**
     * Rate limiter implementation
     */
    private static class RateLimiter {
        private final int limit;
        private final Duration window;
        private final Queue<Instant> requests = new LinkedList<>();
        
        public RateLimiter(int limit, Duration window) {
            this.limit = limit;
            this.window = window;
        }
        
        public synchronized boolean tryAcquire() {
            Instant now = Instant.now();
            Instant windowStart = now.minus(window);
            
            // Remove old requests
            requests.removeIf(timestamp -> timestamp.isBefore(windowStart));
            
            if (requests.size() < limit) {
                requests.offer(now);
                return true;
            }
            
            return false;
        }
    }
    
    /**
     * Notification channels
     */
    public enum NotificationChannel {
        EMAIL, WEBHOOK, IN_APP
    }
    
    /**
     * User subscription
     */
    public static class UserSubscription {
        private final String userId;
        private final String email;
        private List<String> categories;
        private List<NotificationChannel> channels;
        private Map<String, Object> preferences;
        private String webhookUrl;
        private Instant createdAt;
        private Instant updatedAt;
        
        public UserSubscription(String userId, String email, List<String> categories, 
                              List<NotificationChannel> channels, Map<String, Object> preferences) {
            this.userId = userId;
            this.email = email;
            this.categories = new ArrayList<>(categories);
            this.channels = new ArrayList<>(channels);
            this.preferences = new HashMap<>(preferences);
            this.webhookUrl = (String) preferences.get("webhookUrl");
            this.createdAt = Instant.now();
            this.updatedAt = Instant.now();
        }
        
        public void updatePreferences(List<String> categories, List<NotificationChannel> channels, 
                                    Map<String, Object> preferences) {
            this.categories = new ArrayList<>(categories);
            this.channels = new ArrayList<>(channels);
            this.preferences = new HashMap<>(preferences);
            this.webhookUrl = (String) preferences.get("webhookUrl");
            this.updatedAt = Instant.now();
        }
        
        public boolean isSubscribedTo(String category) {
            return categories.contains(category);
        }
        
        // Getters
        public String getUserId() { return userId; }
        public String getEmail() { return email; }
        public List<String> getCategories() { return categories; }
        public List<NotificationChannel> getChannels() { return channels; }
        public Map<String, Object> getPreferences() { return preferences; }
        public String getWebhookUrl() { return webhookUrl; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getUpdatedAt() { return updatedAt; }
    }
    
    /**
     * Notification template
     */
    public static class NotificationTemplate {
        private final String id;
        private final String subject;
        private final String messageTemplate;
        
        public NotificationTemplate(String id, String subject, String messageTemplate) {
            this.id = id;
            this.subject = subject;
            this.messageTemplate = messageTemplate;
        }
        
        public String formatMessage(Map<String, Object> parameters) {
            String message = messageTemplate;
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", 
                    entry.getValue() != null ? entry.getValue().toString() : "");
            }
            return message;
        }
        
        public String getId() { return id; }
        public String getSubject() { return subject; }
        public String getMessageTemplate() { return messageTemplate; }
    }
    
    /**
     * Notification request
     */
    private static class NotificationRequest {
        private final String userId;
        private final String recipient;
        private final String subject;
        private final String message;
        private final String category;
        private final String templateId;
        private final Map<String, Object> parameters;
        private final Instant timestamp;
        private int retryCount = 0;
        
        // Email constructor
        public NotificationRequest(String userId, String recipient, String subject, String message, Instant timestamp) {
            this.userId = userId;
            this.recipient = recipient;
            this.subject = subject;
            this.message = message;
            this.category = null;
            this.templateId = null;
            this.parameters = null;
            this.timestamp = timestamp;
        }
        
        // Webhook constructor
        public NotificationRequest(String userId, String recipient, String category, String templateId, 
                                 Map<String, Object> parameters, Instant timestamp) {
            this.userId = userId;
            this.recipient = recipient;
            this.subject = null;
            this.message = null;
            this.category = category;
            this.templateId = templateId;
            this.parameters = parameters;
            this.timestamp = timestamp;
        }
        
        public void incrementRetryCount() { retryCount++; }
        
        // Getters
        public String getUserId() { return userId; }
        public String getRecipient() { return recipient; }
        public String getSubject() { return subject; }
        public String getMessage() { return message; }
        public String getCategory() { return category; }
        public String getTemplateId() { return templateId; }
        public Map<String, Object> getParameters() { return parameters; }
        public Instant getTimestamp() { return timestamp; }
        public int getRetryCount() { return retryCount; }
    }
    
    /**
     * In-app notification
     */
    public static class InAppNotification {
        private final String id;
        private final String title;
        private final String message;
        private final String category;
        private final Instant timestamp;
        private boolean read;
        
        public InAppNotification(String id, String title, String message, String category, Instant timestamp, boolean read) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.category = category;
            this.timestamp = timestamp;
            this.read = read;
        }
        
        public void markAsRead() { this.read = true; }
        
        // Getters
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getCategory() { return category; }
        public Instant getTimestamp() { return timestamp; }
        public boolean isRead() { return read; }
    }
    
    /**
     * Notification history
     */
    public static class NotificationHistory {
        private final String userId;
        private final String category;
        private final String templateId;
        private final String message;
        private final boolean successful;
        private final Instant timestamp;
        
        public NotificationHistory(String userId, String category, String templateId, 
                                 String message, boolean successful, Instant timestamp) {
            this.userId = userId;
            this.category = category;
            this.templateId = templateId;
            this.message = message;
            this.successful = successful;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getUserId() { return userId; }
        public String getCategory() { return category; }
        public String getTemplateId() { return templateId; }
        public String getMessage() { return message; }
        public boolean isSuccessful() { return successful; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    /**
     * Notification result
     */
    public static class NotificationResult {
        private final boolean success;
        private final String message;
        
        public NotificationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    /**
     * Broadcast result
     */
    public static class BroadcastResult {
        private final int successful;
        private final int failed;
        private final String message;
        
        public BroadcastResult(int successful, int failed, String message) {
            this.successful = successful;
            this.failed = failed;
            this.message = message;
        }
        
        public int getSuccessful() { return successful; }
        public int getFailed() { return failed; }
        public String getMessage() { return message; }
        public int getTotal() { return successful + failed; }
    }
    
    /**
     * Notification statistics
     */
    public static class NotificationStats {
        private final int totalSubscriptions;
        private final long totalNotifications;
        private final long successfulDeliveries;
        private final long failedDeliveries;
        private final Map<String, Integer> subscriptionsByCategory;
        private final Map<String, Integer> subscriptionsByChannel;
        
        public NotificationStats(int totalSubscriptions, long totalNotifications, 
                               long successfulDeliveries, long failedDeliveries,
                               Map<String, Integer> subscriptionsByCategory, 
                               Map<String, Integer> subscriptionsByChannel) {
            this.totalSubscriptions = totalSubscriptions;
            this.totalNotifications = totalNotifications;
            this.successfulDeliveries = successfulDeliveries;
            this.failedDeliveries = failedDeliveries;
            this.subscriptionsByCategory = subscriptionsByCategory;
            this.subscriptionsByChannel = subscriptionsByChannel;
        }
        
        public int getTotalSubscriptions() { return totalSubscriptions; }
        public long getTotalNotifications() { return totalNotifications; }
        public long getSuccessfulDeliveries() { return successfulDeliveries; }
        public long getFailedDeliveries() { return failedDeliveries; }
        public Map<String, Integer> getSubscriptionsByCategory() { return subscriptionsByCategory; }
        public Map<String, Integer> getSubscriptionsByChannel() { return subscriptionsByChannel; }
        public double getSuccessRate() { 
            return totalNotifications > 0 ? (double) successfulDeliveries / totalNotifications : 1.0; 
        }
    }
}