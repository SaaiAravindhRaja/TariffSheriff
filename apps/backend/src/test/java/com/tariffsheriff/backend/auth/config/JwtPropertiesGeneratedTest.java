package com.tariffsheriff.backend.auth.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtPropertiesGeneratedTest {

    @Test
    void testSettersAndGetters() {
        // Arrange
        JwtProperties properties = new JwtProperties();

        // Act
        properties.setSecret("myTestSecret");
        properties.setExpiration(3600L);

        // Assert
        assertEquals("myTestSecret", properties.getSecret());
        assertEquals(3600L, properties.getExpiration());
    }
}