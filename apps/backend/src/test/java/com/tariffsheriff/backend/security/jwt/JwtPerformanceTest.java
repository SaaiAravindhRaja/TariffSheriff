package com.tariffsheriff.backend.security.jwt;

import com.tariffsheriff.backend.config.JwtConfig;
import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.model.UserRole;
import com.tariffsheriff.backend.user.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class JwtPerformanceTest {

    @Mock
    private JwtConfig jwtConfig;

    private JwtUtil jwtUtil;
    private User testUser;

    @BeforeEach
    void setUp() {
        lenient().when(jwtConfig.getSecret()).thenReturn("mySecretKeyForTestingPurposes123456789");
        lenient().when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L); // 15 minutes
        lenient().when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800000L); // 7 days
        lenient().when(jwtConfig.getSecretKey()).thenCallRealMethod();

        jwtUtil = new JwtUtil(jwtConfig);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setRole(UserRole.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void shouldGenerateTokensWithinAcceptableTime() {
        // Given
        int iterations = 1000;
        long maxTimePerTokenMs = 10; // 10ms per token should be acceptable

        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            String token = jwtUtil.generateAccessToken(testUser);
            assertThat(token).isNotNull();
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        long averageTimePerToken = totalTime / iterations;

        // Then
        assertThat(averageTimePerToken).isLessThan(maxTimePerTokenMs);
        System.out.println("Average token generation time: " + averageTimePerToken + "ms");
        System.out.println("Total time for " + iterations + " tokens: " + totalTime + "ms");
    }

    @Test
    void shouldValidateTokensWithinAcceptableTime() {
        // Given
        int iterations = 1000;
        long maxTimePerValidationMs = 5; // 5ms per validation should be acceptable
        
        // Pre-generate tokens
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            tokens.add(jwtUtil.generateAccessToken(testUser));
        }

        // When
        long startTime = System.currentTimeMillis();
        
        for (String token : tokens) {
            boolean isValid = jwtUtil.validateToken(token, testUser);
            assertThat(isValid).isTrue();
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        long averageTimePerValidation = totalTime / iterations;

        // Then
        assertThat(averageTimePerValidation).isLessThan(maxTimePerValidationMs);
        System.out.println("Average token validation time: " + averageTimePerValidation + "ms");
        System.out.println("Total time for " + iterations + " validations: " + totalTime + "ms");
    }

    @Test
    void shouldExtractClaimsWithinAcceptableTime() {
        // Given
        int iterations = 1000;
        long maxTimePerExtractionMs = 3; // 3ms per extraction should be acceptable
        
        String token = jwtUtil.generateAccessToken(testUser);

        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            String username = jwtUtil.extractUsername(token);
            Long userId = jwtUtil.extractUserId(token);
            String roles = jwtUtil.extractRoles(token);
            
            assertThat(username).isEqualTo(testUser.getEmail());
            assertThat(userId).isEqualTo(testUser.getId());
            assertThat(roles).isEqualTo(testUser.getRole().name());
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        long averageTimePerExtraction = totalTime / iterations;

        // Then
        assertThat(averageTimePerExtraction).isLessThan(maxTimePerExtractionMs);
        System.out.println("Average claim extraction time: " + averageTimePerExtraction + "ms");
        System.out.println("Total time for " + iterations + " extractions: " + totalTime + "ms");
    }

    @Test
    void shouldHandleConcurrentTokenGeneration() throws InterruptedException {
        // Given
        int threadCount = 10;
        int tokensPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<Long>> futures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            Future<Long> future = executor.submit(() -> {
                long threadStartTime = System.currentTimeMillis();
                
                for (int j = 0; j < tokensPerThread; j++) {
                    String token = jwtUtil.generateAccessToken(testUser);
                    assertThat(token).isNotNull();
                }
                
                latch.countDown();
                return System.currentTimeMillis() - threadStartTime;
            });
            futures.add(future);
        }

        // Wait for all threads to complete
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Then
        executor.shutdown();
        
        // Verify all threads completed successfully
        for (Future<Long> future : futures) {
            try {
                Long threadTime = future.get();
                assertThat(threadTime).isLessThan(5000L); // Each thread should complete within 5 seconds
            } catch (ExecutionException e) {
                fail("Thread execution failed", e);
            }
        }

        int totalTokens = threadCount * tokensPerThread;
        long averageTimePerToken = totalTime / totalTokens;
        
        System.out.println("Concurrent token generation:");
        System.out.println("Total tokens: " + totalTokens);
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Average time per token: " + averageTimePerToken + "ms");
        System.out.println("Tokens per second: " + (totalTokens * 1000 / totalTime));
        
        // Should be able to generate at least 100 tokens per second
        assertThat(totalTokens * 1000 / totalTime).isGreaterThan(100);
    }

    @Test
    void shouldHandleConcurrentTokenValidation() throws InterruptedException {
        // Given
        int threadCount = 10;
        int validationsPerThread = 100;
        
        // Pre-generate tokens
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < threadCount * validationsPerThread; i++) {
            tokens.add(jwtUtil.generateAccessToken(testUser));
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<Long>> futures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            Future<Long> future = executor.submit(() -> {
                long threadStartTime = System.currentTimeMillis();
                
                for (int j = 0; j < validationsPerThread; j++) {
                    int tokenIndex = threadIndex * validationsPerThread + j;
                    String token = tokens.get(tokenIndex);
                    boolean isValid = jwtUtil.validateToken(token, testUser);
                    assertThat(isValid).isTrue();
                }
                
                latch.countDown();
                return System.currentTimeMillis() - threadStartTime;
            });
            futures.add(future);
        }

        // Wait for all threads to complete
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Then
        executor.shutdown();
        
        // Verify all threads completed successfully
        for (Future<Long> future : futures) {
            try {
                Long threadTime = future.get();
                assertThat(threadTime).isLessThan(3000L); // Each thread should complete within 3 seconds
            } catch (ExecutionException e) {
                fail("Thread execution failed", e);
            }
        }

        int totalValidations = threadCount * validationsPerThread;
        long averageTimePerValidation = totalTime / totalValidations;
        
        System.out.println("Concurrent token validation:");
        System.out.println("Total validations: " + totalValidations);
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Average time per validation: " + averageTimePerValidation + "ms");
        System.out.println("Validations per second: " + (totalValidations * 1000 / totalTime));
        
        // Should be able to validate at least 200 tokens per second
        assertThat(totalValidations * 1000 / totalTime).isGreaterThan(200);
    }

    @Test
    void shouldMaintainPerformanceWithLargePayloads() {
        // Given
        User userWithLargeData = new User();
        userWithLargeData.setId(1L);
        userWithLargeData.setEmail("user.with.very.long.email.address@example.com");
        userWithLargeData.setName("User With Very Long Name That Contains Many Characters");
        userWithLargeData.setRole(UserRole.ADMIN);
        userWithLargeData.setStatus(UserStatus.ACTIVE);
        userWithLargeData.setCreatedAt(LocalDateTime.now());

        int iterations = 100;
        long maxTimePerTokenMs = 15; // Allow slightly more time for larger payloads

        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            String token = jwtUtil.generateAccessToken(userWithLargeData);
            assertThat(token).isNotNull();
            
            // Verify the token can be validated
            boolean isValid = jwtUtil.validateToken(token, userWithLargeData);
            assertThat(isValid).isTrue();
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        long averageTimePerToken = totalTime / iterations;

        // Then
        assertThat(averageTimePerToken).isLessThan(maxTimePerTokenMs);
        System.out.println("Average time for large payload tokens: " + averageTimePerToken + "ms");
    }

    @Test
    void shouldHandleMemoryUsageEfficiently() {
        // Given
        int tokenCount = 10000;
        List<String> tokens = new ArrayList<>();

        // When - Generate many tokens
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        for (int i = 0; i < tokenCount; i++) {
            tokens.add(jwtUtil.generateAccessToken(testUser));
        }
        
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        long memoryPerToken = memoryUsed / tokenCount;

        // Then
        System.out.println("Memory used for " + tokenCount + " tokens: " + (memoryUsed / 1024) + " KB");
        System.out.println("Average memory per token: " + memoryPerToken + " bytes");
        
        // Each token should use less than 1KB of memory on average
        assertThat(memoryPerToken).isLessThan(1024);
        
        // Validate a sample of tokens to ensure they're still valid
        for (int i = 0; i < 100; i++) {
            String token = tokens.get(i * (tokenCount / 100));
            assertThat(jwtUtil.validateToken(token, testUser)).isTrue();
        }
    }

    @Test
    void shouldMaintainPerformanceUnderLoad() throws InterruptedException {
        // Given
        int threadCount = 20;
        int operationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<Boolean>> futures = new ArrayList<>();

        // When - Mixed operations under load
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            Future<Boolean> future = executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        // Generate token
                        String token = jwtUtil.generateAccessToken(testUser);
                        
                        // Validate token
                        boolean isValid = jwtUtil.validateToken(token, testUser);
                        if (!isValid) return false;
                        
                        // Extract claims
                        String username = jwtUtil.extractUsername(token);
                        Long userId = jwtUtil.extractUserId(token);
                        
                        if (!testUser.getEmail().equals(username) || !testUser.getId().equals(userId)) {
                            return false;
                        }
                        
                        // Check expiration
                        boolean isExpired = jwtUtil.isTokenExpired(token);
                        if (isExpired) return false;
                    }
                    return true;
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        // Wait for all threads to complete
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Then
        executor.shutdown();
        
        // Verify all operations completed successfully
        for (Future<Boolean> future : futures) {
            try {
                Boolean success = future.get();
                assertThat(success).isTrue();
            } catch (ExecutionException e) {
                fail("Thread execution failed", e);
            }
        }

        int totalOperations = threadCount * operationsPerThread * 4; // 4 operations per iteration
        long averageTimePerOperation = totalTime / totalOperations;
        
        System.out.println("Load test results:");
        System.out.println("Total operations: " + totalOperations);
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Average time per operation: " + averageTimePerOperation + "ms");
        System.out.println("Operations per second: " + (totalOperations * 1000 / totalTime));
        
        // Should maintain good performance under load
        assertThat(averageTimePerOperation).isLessThan(10); // Less than 10ms per operation
        assertThat(totalOperations * 1000 / totalTime).isGreaterThan(100); // More than 100 ops/sec
    }
}