package com.tariffsheriff.backend.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

/**
 * Data protection service for encryption, data masking, and secure key management
 */
@Service
@Slf4j
public class DataProtectionService {

    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int AES_KEY_LENGTH = 256;

    @Value("${app.security.encryption.master-key:}")
    private String masterKeyBase64;

    private SecretKey masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    // PII patterns for data masking
    private static final Map<String, Pattern> PII_PATTERNS = new HashMap<>();
    static {
        PII_PATTERNS.put("EMAIL", Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"));
        PII_PATTERNS.put("SSN", Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"));
        PII_PATTERNS.put("CREDIT_CARD", Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"));
        PII_PATTERNS.put("PHONE", Pattern.compile("\\b\\d{3}[\\s-]?\\d{3}[\\s-]?\\d{4}\\b"));
        PII_PATTERNS.put("ADDRESS", Pattern.compile("\\b\\d{1,5}\\s+[A-Za-z\\s]+\\s+(Street|St|Avenue|Ave|Road|Rd|Drive|Dr|Lane|Ln|Boulevard|Blvd)\\b"));
        PII_PATTERNS.put("IP_ADDRESS", Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"));
    }

    /**
     * Initializes the master key for encryption
     */
    private void initializeMasterKey() {
        if (masterKey != null) return;
        
        try {
            if (masterKeyBase64 != null && !masterKeyBase64.trim().isEmpty()) {
                // Use provided master key
                byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);
                masterKey = new SecretKeySpec(keyBytes, ENCRYPTION_ALGORITHM);
                log.info("Master key loaded from configuration");
            } else {
                // Generate new master key (for development/testing)
                KeyGenerator keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
                keyGenerator.init(AES_KEY_LENGTH);
                masterKey = keyGenerator.generateKey();
                log.warn("Generated new master key - this should not happen in production!");
                log.info("Generated master key (base64): {}", Base64.getEncoder().encodeToString(masterKey.getEncoded()));
            }
        } catch (Exception e) {
            log.error("Failed to initialize master key", e);
            throw new RuntimeException("Failed to initialize encryption key", e);
        }
    }

    /**
     * Encrypts sensitive data
     */
    public String encryptData(String plaintext) {
        if (plaintext == null || plaintext.trim().isEmpty()) {
            return plaintext;
        }

        try {
            initializeMasterKey();
            
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, parameterSpec);
            
            byte[] encryptedData = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and encrypted data
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);
            
            return Base64.getEncoder().encodeToString(encryptedWithIv);
            
        } catch (Exception e) {
            log.error("Failed to encrypt data", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts sensitive data
     */
    public String decryptData(String encryptedData) {
        if (encryptedData == null || encryptedData.trim().isEmpty()) {
            return encryptedData;
        }

        try {
            initializeMasterKey();
            
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedData);
            
            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, parameterSpec);
            
            byte[] decryptedData = cipher.doFinal(encrypted);
            
            return new String(decryptedData, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("Failed to decrypt data", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Masks PII data in text
     */
    public String maskPiiData(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String maskedText = text;
        
        for (Map.Entry<String, Pattern> entry : PII_PATTERNS.entrySet()) {
            String piiType = entry.getKey();
            Pattern pattern = entry.getValue();
            
            maskedText = pattern.matcher(maskedText).replaceAll(match -> {
                String matched = match.group();
                return generateMask(matched, piiType);
            });
        }
        
        return maskedText;
    }

    /**
     * Masks PII data in text with custom patterns
     */
    public String maskPiiData(String text, List<String> additionalPatterns) {
        String maskedText = maskPiiData(text);
        
        // Apply additional custom patterns
        for (String patternStr : additionalPatterns) {
            try {
                Pattern pattern = Pattern.compile(patternStr);
                maskedText = pattern.matcher(maskedText).replaceAll("[REDACTED]");
            } catch (Exception e) {
                log.warn("Invalid regex pattern: {}", patternStr, e);
            }
        }
        
        return maskedText;
    }

    /**
     * Checks if text contains PII data
     */
    public boolean containsPii(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        for (Pattern pattern : PII_PATTERNS.values()) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Gets PII detection results with details
     */
    public PiiDetectionResult detectPii(String text) {
        PiiDetectionResult result = new PiiDetectionResult();
        result.setText(text);
        result.setContainsPii(false);
        
        if (text == null || text.trim().isEmpty()) {
            return result;
        }

        Map<String, Integer> piiCounts = new HashMap<>();
        
        for (Map.Entry<String, Pattern> entry : PII_PATTERNS.entrySet()) {
            String piiType = entry.getKey();
            Pattern pattern = entry.getValue();
            
            long count = pattern.matcher(text).results().count();
            if (count > 0) {
                piiCounts.put(piiType, (int) count);
                result.setContainsPii(true);
            }
        }
        
        result.setPiiTypes(piiCounts);
        return result;
    }

    /**
     * Securely hashes sensitive data for comparison purposes
     */
    public String hashSensitiveData(String data) {
        if (data == null || data.trim().isEmpty()) {
            return data;
        }

        try {
            // Use SHA-256 with salt for hashing
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            
            // Add salt to prevent rainbow table attacks
            String saltedData = data + getSalt();
            byte[] hash = digest.digest(saltedData.getBytes(StandardCharsets.UTF_8));
            
            return Base64.getEncoder().encodeToString(hash);
            
        } catch (Exception e) {
            log.error("Failed to hash sensitive data", e);
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /**
     * Generates a secure random token
     */
    public String generateSecureToken(int length) {
        byte[] tokenBytes = new byte[length];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Validates data retention requirements
     */
    public DataRetentionResult validateRetention(String dataType, java.time.LocalDateTime createdAt) {
        DataRetentionResult result = new DataRetentionResult();
        result.setDataType(dataType);
        result.setCreatedAt(createdAt);
        
        // Define retention periods based on data type
        Map<String, Integer> retentionPeriods = getRetentionPeriods();
        
        Integer retentionYears = retentionPeriods.get(dataType.toUpperCase());
        if (retentionYears == null) {
            retentionYears = 7; // Default retention period
        }
        
        java.time.LocalDateTime expiryDate = createdAt.plusYears(retentionYears);
        result.setExpiryDate(expiryDate);
        result.setExpired(java.time.LocalDateTime.now().isAfter(expiryDate));
        result.setRetentionPeriodYears(retentionYears);
        
        return result;
    }

    /**
     * Securely deletes data (overwrite with random data)
     */
    public void secureDelete(String[] dataToDelete) {
        if (dataToDelete == null) return;
        
        // Overwrite strings with random data
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < dataToDelete.length; i++) {
            if (dataToDelete[i] != null) {
                char[] chars = dataToDelete[i].toCharArray();
                for (int j = 0; j < chars.length; j++) {
                    chars[j] = (char) random.nextInt(Character.MAX_VALUE);
                }
                dataToDelete[i] = null;
            }
        }
    }

    /**
     * Generates appropriate mask for PII type
     */
    private String generateMask(String original, String piiType) {
        switch (piiType) {
            case "EMAIL":
                int atIndex = original.indexOf('@');
                if (atIndex > 0) {
                    return original.charAt(0) + "***@" + original.substring(atIndex + 1);
                }
                return "[EMAIL_REDACTED]";
                
            case "SSN":
                return "***-**-" + original.substring(original.length() - 4);
                
            case "CREDIT_CARD":
                String digits = original.replaceAll("[^\\d]", "");
                if (digits.length() >= 4) {
                    return "****-****-****-" + digits.substring(digits.length() - 4);
                }
                return "[CARD_REDACTED]";
                
            case "PHONE":
                String phoneDigits = original.replaceAll("[^\\d]", "");
                if (phoneDigits.length() >= 4) {
                    return "***-***-" + phoneDigits.substring(phoneDigits.length() - 4);
                }
                return "[PHONE_REDACTED]";
                
            case "ADDRESS":
                return "[ADDRESS_REDACTED]";
                
            case "IP_ADDRESS":
                String[] parts = original.split("\\.");
                if (parts.length == 4) {
                    return parts[0] + ".***.***.***";
                }
                return "[IP_REDACTED]";
                
            default:
                return "[" + piiType + "_REDACTED]";
        }
    }

    /**
     * Gets salt for hashing
     */
    private String getSalt() {
        // In production, this should be a configurable salt
        return "TariffSheriff_Salt_2024";
    }

    /**
     * Gets retention periods for different data types
     */
    private Map<String, Integer> getRetentionPeriods() {
        Map<String, Integer> periods = new HashMap<>();
        
        // Compliance-based retention periods
        periods.put("AUDIT_LOG", 7);           // 7 years for audit logs
        periods.put("FINANCIAL_DATA", 7);      // 7 years for financial records
        periods.put("USER_DATA", 5);           // 5 years for user data
        periods.put("CONVERSATION", 3);        // 3 years for conversations
        periods.put("SECURITY_LOG", 7);        // 7 years for security logs
        periods.put("TRADE_DATA", 10);         // 10 years for trade records
        periods.put("COMPLIANCE_DATA", 7);     // 7 years for compliance data
        periods.put("TEMPORARY", 1);           // 1 year for temporary data
        
        return periods;
    }

    // Result classes
    
    public static class PiiDetectionResult {
        private String text;
        private boolean containsPii;
        private Map<String, Integer> piiTypes = new HashMap<>();

        // Getters and setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public boolean isContainsPii() { return containsPii; }
        public void setContainsPii(boolean containsPii) { this.containsPii = containsPii; }
        public Map<String, Integer> getPiiTypes() { return piiTypes; }
        public void setPiiTypes(Map<String, Integer> piiTypes) { this.piiTypes = piiTypes; }
    }

    public static class DataRetentionResult {
        private String dataType;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime expiryDate;
        private boolean expired;
        private int retentionPeriodYears;

        // Getters and setters
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
        public java.time.LocalDateTime getExpiryDate() { return expiryDate; }
        public void setExpiryDate(java.time.LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
        public boolean isExpired() { return expired; }
        public void setExpired(boolean expired) { this.expired = expired; }
        public int getRetentionPeriodYears() { return retentionPeriodYears; }
        public void setRetentionPeriodYears(int retentionPeriodYears) { this.retentionPeriodYears = retentionPeriodYears; }
    }
}