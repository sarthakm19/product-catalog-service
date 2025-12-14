package com.product.catalog.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenUtil.
 * Tests JWT token generation, validation, and claim extraction.
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenUtilTest {

    private JwtTokenUtil jwtTokenUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtTokenUtil = new JwtTokenUtil();
        ReflectionTestUtils.setField(jwtTokenUtil, "secret",
                "mySecretKeyForJWTTokenGenerationAndValidationThatIsLongEnoughToMeetRequirements");
        ReflectionTestUtils.setField(jwtTokenUtil, "jwtExpirationMs", 86400000L);

        userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(new ArrayList<>())
                .build();
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        // When
        String token = jwtTokenUtil.generateToken(userDetails);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void extractUsername_FromValidToken_ShouldReturnUsername() {
        // Given
        String token = jwtTokenUtil.generateToken(userDetails);

        // When
        String username = jwtTokenUtil.extractUsername(token);

        // Then
        assertEquals("testuser", username);
    }

    @Test
    void extractExpiration_FromValidToken_ShouldReturnExpirationDate() {
        // Given
        String token = jwtTokenUtil.generateToken(userDetails);

        // When
        var expiration = jwtTokenUtil.extractExpiration(token);

        // Then
        assertNotNull(expiration);
        assertTrue(expiration.getTime() > System.currentTimeMillis());
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Given
        String token = jwtTokenUtil.generateToken(userDetails);

        // When
        boolean isValid = jwtTokenUtil.validateToken(token, userDetails);

        // Then
        assertTrue(isValid);
    }

    @Test
    void validateToken_WithWrongUsername_ShouldReturnFalse() {
        // Given
        String token = jwtTokenUtil.generateToken(userDetails);
        UserDetails wrongUser = User.builder()
                .username("wronguser")
                .password("password")
                .authorities(new ArrayList<>())
                .build();

        // When
        boolean isValid = jwtTokenUtil.validateToken(token, wrongUser);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_WithExpiredToken_ShouldReturnFalse() {
        // Given - create util with very short expiration
        JwtTokenUtil shortExpirationUtil = new JwtTokenUtil();
        ReflectionTestUtils.setField(shortExpirationUtil, "secret",
                "mySecretKeyForJWTTokenGenerationAndValidationThatIsLongEnoughToMeetRequirements");
        ReflectionTestUtils.setField(shortExpirationUtil, "jwtExpirationMs", 1L);

        String token = shortExpirationUtil.generateToken(userDetails);

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        boolean isValid = shortExpirationUtil.validateToken(token, userDetails);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtTokenUtil.validateToken(invalidToken, userDetails);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_WithMalformedToken_ShouldReturnFalse() {
        // Given
        String malformedToken = "malformed-token";

        // When
        boolean isValid = jwtTokenUtil.validateToken(malformedToken, userDetails);

        // Then
        assertFalse(isValid);
    }

    @Test
    void getExpirationInSeconds_ShouldReturnCorrectValue() {
        // When
        long expirationInSeconds = jwtTokenUtil.getExpirationInSeconds();

        // Then
        assertEquals(86400L, expirationInSeconds);
    }

    @Test
    void validateToken_WithTokenFromDifferentSecret_ShouldReturnFalse() {
        // Given - create token with one secret
        String token = jwtTokenUtil.generateToken(userDetails);

        // Change secret
        ReflectionTestUtils.setField(jwtTokenUtil, "secret",
                "differentSecretKeyForJWTTokenGenerationAndValidationThatIsLongEnoughToMeet");

        // When
        boolean isValid = jwtTokenUtil.validateToken(token, userDetails);

        // Then
        assertFalse(isValid);
    }

    @Test
    void extractUsername_WithNullToken_ShouldThrowException() {
        // When & Then
        assertThrows(Exception.class, () -> jwtTokenUtil.extractUsername(null));
    }

    @Test
    void generateToken_MultipleCallsShouldGenerateDifferentTokens() {
        // When
        String token1 = jwtTokenUtil.generateToken(userDetails);

        // Small delay to ensure different issuedAt
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String token2 = jwtTokenUtil.generateToken(userDetails);

        // Then
        assertNotEquals(token1, token2);
    }
}

