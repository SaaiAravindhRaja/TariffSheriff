package com.tariffsheriff.backend.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Secure key management service for encryption keys and API keys
 */
@Service
@Slf4j
public class SecureKeyManagementService {

    @Value("${app.security.key-rotation.enabled:true}")
    private boolean keyRotationEnabled;

    @Value("${app.security.key-rotation.interval-hours:24}")
    private int keyRotationIntervalHours;

    // In-memory key store (in production, use a proper key management system)
    private final Map<String, ManagedKey> keyStore = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a new encryption key
     */
    public String generateEncryptionKey(String keyId, KeyType keyType) {
        try {
            SecretKey secretKey;
            
            switch (keyType) {
                case AES_256:
                    KeyGenerator aesGenerator = KeyGenerator.getInstance("AES");
                    aesGenerator.init(256);
                    secretKey = aesGenerator.generateKey();
                    break;
                    
                case AES_128:
                    KeyGenerator aes128Generator = KeyGenerator.getInstance("AES");
                    aes128Generator.init(128);
                    secretKey = aes128Generator.generateKey();
                    break;
                    
                case HMAC_SHA256:
                    KeyGenerator hmacGenerator = KeyGenerator.getInstance("HmacSHA256");
                    secretKey = hmacGenerator.generateKey();
                    break;
                    
                default:
                    throw new IllegalArgumentException("Unsupported key type: " + keyType);
            }
            
            String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
            
            ManagedKey managedKey = new ManagedKey();
            managedKey.setKeyId(keyId);
            managedKey.setKeyType(keyType);
            managedKey.setEncodedKey(encodedKey);
            managedKey.setCreatedAt(LocalDateTime.now());
            managedKey.setLastUsed(LocalDateTime.now());
            managedKey.setActive(true);
            
            keyStore.put(keyId, managedKey);
            
            log.info("Generated new {} key with ID: {}", keyType, keyId);
            return encodedKey;
            
        } catch (Exception e) {
            log.error("Failed to generate encryption key: {}", keyId, e);
            throw new RuntimeException("Key generation failed", e);
        }
    }

    /**
     * Retrieves an encryption key
     */
    public String getEncryptionKey(String keyId) {
        ManagedKey managedKey = keyStore.get(keyId);
        if (managedKey == null) {
            log.warn("Encryption key not found: {}", keyId);
            return null;
        }
        
        if (!managedKey.isActive()) {
            log.warn("Encryption key is inactive: {}", keyId);
            return null;
        }
        
        // Check if key needs rotation
        if (keyRotationEnabled && needsRotation(managedKey)) {
            log.info("Key {} needs rotation", keyId);
            rotateKey(keyId);
            managedKey = keyStore.get(keyId); // Get the new key
        }
        
        managedKey.setLastUsed(LocalDateTime.now());
        return managedKey.getEncodedKey();
    }

    /**
     * Generates a secure API key
     */
    public String generateApiKey(String keyId, int length) {
        byte[] keyBytes = new byte[length];
        secureRandom.nextBytes(keyBytes);
        String apiKey = Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
        
        ManagedKey managedKey = new ManagedKey();
        managedKey.setKeyId(keyId);
        managedKey.setKeyType(KeyType.API_KEY);
        managedKey.setEncodedKey(apiKey);
        managedKey.setCreatedAt(LocalDateTime.now());
        managedKey.setLastUsed(LocalDateTime.now());
        managedKey.setActive(true);
        
        keyStore.put(keyId, managedKey);
        
        log.info("Generated new API key with ID: {}", keyId);
        return apiKey;
    }

    /**
     * Validates an API key
     */
    public boolean validateApiKey(String keyId, String providedKey) {
        ManagedKey managedKey = keyStore.get(keyId);
        if (managedKey == null || !managedKey.isActive()) {
            return false;
        }
        
        boolean isValid = managedKey.getEncodedKey().equals(providedKey);
        if (isValid) {
            managedKey.setLastUsed(LocalDateTime.now());
        }
        
        return isValid;
    }

    /**
     * Rotates an encryption key
     */
    public String rotateKey(String keyId) {
        ManagedKey currentKey = keyStore.get(keyId);
        if (currentKey == null) {
            log.warn("Cannot rotate non-existent key: {}", keyId);
            return null;
        }
        
        // Generate new key
        String newKey = generateEncryptionKey(keyId, currentKey.getKeyType());
        
        // Mark old key as rotated but keep it for a grace period
        currentKey.setActive(false);
        currentKey.setRotatedAt(LocalDateTime.now());
        
        // Store old key with rotated suffix for grace period
        String oldKeyId = keyId + "_rotated_" + System.currentTimeMillis();
        keyStore.put(oldKeyId, currentKey);
        
        log.info("Rotated key: {} -> {}", keyId, oldKeyId);
        return newKey;
    }

    /**
     * Revokes a key
     */
    public void revokeKey(String keyId) {
        ManagedKey managedKey = keyStore.get(keyId);
        if (managedKey != null) {
            managedKey.setActive(false);
            managedKey.setRevokedAt(LocalDateTime.now());
            log.info("Revoked key: {}", keyId);
        }
    }

    /**
     * Gets key metadata
     */
    public KeyMetadata getKeyMetadata(String keyId) {
        ManagedKey managedKey = keyStore.get(keyId);
        if (managedKey == null) {
            return null;
        }
        
        KeyMetadata metadata = new KeyMetadata();
        metadata.setKeyId(keyId);
        metadata.setKeyType(managedKey.getKeyType());
        metadata.setCreatedAt(managedKey.getCreatedAt());
        metadata.setLastUsed(managedKey.getLastUsed());
        metadata.setActive(managedKey.isActive());
        metadata.setRotatedAt(managedKey.getRotatedAt());
        metadata.setRevokedAt(managedKey.getRevokedAt());
        
        return metadata;
    }

    /**
     * Lists all active keys
     */
    public Map<String, KeyMetadata> listActiveKeys() {
        Map<String, KeyMetadata> activeKeys = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, ManagedKey> entry : keyStore.entrySet()) {
            ManagedKey key = entry.getValue();
            if (key.isActive()) {
                KeyMetadata metadata = getKeyMetadata(entry.getKey());
                activeKeys.put(entry.getKey(), metadata);
            }
        }
        
        return activeKeys;
    }

    /**
     * Cleans up old rotated keys
     */
    public void cleanupOldKeys() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30); // Keep rotated keys for 30 days
        
        keyStore.entrySet().removeIf(entry -> {
            ManagedKey key = entry.getValue();
            return !key.isActive() && 
                   key.getRotatedAt() != null && 
                   key.getRotatedAt().isBefore(cutoff);
        });
        
        log.info("Cleaned up old rotated keys");
    }

    /**
     * Encrypts a key for storage
     */
    public String encryptKeyForStorage(String key, String masterKey) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                Base64.getDecoder().decode(masterKey), "AES");
            
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            
            // Generate random IV
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);
            
            javax.crypto.spec.GCMParameterSpec parameterSpec = 
                new javax.crypto.spec.GCMParameterSpec(128, iv);
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, parameterSpec);
            
            byte[] encryptedKey = cipher.doFinal(key.getBytes());
            
            // Combine IV and encrypted key
            byte[] result = new byte[iv.length + encryptedKey.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encryptedKey, 0, result, iv.length, encryptedKey.length);
            
            return Base64.getEncoder().encodeToString(result);
            
        } catch (Exception e) {
            log.error("Failed to encrypt key for storage", e);
            throw new RuntimeException("Key encryption failed", e);
        }
    }

    /**
     * Decrypts a key from storage
     */
    public String decryptKeyFromStorage(String encryptedKey, String masterKey) {
        try {
            byte[] encryptedData = Base64.getDecoder().decode(encryptedKey);
            
            // Extract IV and encrypted key
            byte[] iv = new byte[12];
            byte[] encrypted = new byte[encryptedData.length - 12];
            System.arraycopy(encryptedData, 0, iv, 0, 12);
            System.arraycopy(encryptedData, 12, encrypted, 0, encrypted.length);
            
            SecretKeySpec keySpec = new SecretKeySpec(
                Base64.getDecoder().decode(masterKey), "AES");
            
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            javax.crypto.spec.GCMParameterSpec parameterSpec = 
                new javax.crypto.spec.GCMParameterSpec(128, iv);
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, parameterSpec);
            
            byte[] decryptedKey = cipher.doFinal(encrypted);
            
            return new String(decryptedKey);
            
        } catch (Exception e) {
            log.error("Failed to decrypt key from storage", e);
            throw new RuntimeException("Key decryption failed", e);
        }
    }

    /**
     * Checks if a key needs rotation
     */
    private boolean needsRotation(ManagedKey key) {
        if (!keyRotationEnabled) {
            return false;
        }
        
        LocalDateTime rotationThreshold = LocalDateTime.now().minusHours(keyRotationIntervalHours);
        return key.getCreatedAt().isBefore(rotationThreshold);
    }

    /**
     * Scheduled key rotation and cleanup
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void performScheduledMaintenance() {
        if (keyRotationEnabled) {
            // Check for keys that need rotation
            for (Map.Entry<String, ManagedKey> entry : keyStore.entrySet()) {
                ManagedKey key = entry.getValue();
                if (key.isActive() && needsRotation(key)) {
                    rotateKey(entry.getKey());
                }
            }
        }
        
        // Cleanup old keys
        cleanupOldKeys();
    }

    // Enums and data classes
    
    public enum KeyType {
        AES_256,
        AES_128,
        HMAC_SHA256,
        API_KEY
    }

    private static class ManagedKey {
        private String keyId;
        private KeyType keyType;
        private String encodedKey;
        private LocalDateTime createdAt;
        private LocalDateTime lastUsed;
        private LocalDateTime rotatedAt;
        private LocalDateTime revokedAt;
        private boolean active;

        // Getters and setters
        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
        public KeyType getKeyType() { return keyType; }
        public void setKeyType(KeyType keyType) { this.keyType = keyType; }
        public String getEncodedKey() { return encodedKey; }
        public void setEncodedKey(String encodedKey) { this.encodedKey = encodedKey; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getLastUsed() { return lastUsed; }
        public void setLastUsed(LocalDateTime lastUsed) { this.lastUsed = lastUsed; }
        public LocalDateTime getRotatedAt() { return rotatedAt; }
        public void setRotatedAt(LocalDateTime rotatedAt) { this.rotatedAt = rotatedAt; }
        public LocalDateTime getRevokedAt() { return revokedAt; }
        public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public static class KeyMetadata {
        private String keyId;
        private KeyType keyType;
        private LocalDateTime createdAt;
        private LocalDateTime lastUsed;
        private LocalDateTime rotatedAt;
        private LocalDateTime revokedAt;
        private boolean active;

        // Getters and setters
        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
        public KeyType getKeyType() { return keyType; }
        public void setKeyType(KeyType keyType) { this.keyType = keyType; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getLastUsed() { return lastUsed; }
        public void setLastUsed(LocalDateTime lastUsed) { this.lastUsed = lastUsed; }
        public LocalDateTime getRotatedAt() { return rotatedAt; }
        public void setRotatedAt(LocalDateTime rotatedAt) { this.rotatedAt = rotatedAt; }
        public LocalDateTime getRevokedAt() { return revokedAt; }
        public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}