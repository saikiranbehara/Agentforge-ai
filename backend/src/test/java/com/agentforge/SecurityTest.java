package com.agentforge;

import com.agentforge.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "MyVerySecureAgentForgeSuperSecretKeyForJwtSigning1234567890",
                3600000L
        );
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = tokenProvider.generateToken("developer@agentforge.ai", 42L, "ROLE_USER");

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals("developer@agentforge.ai", tokenProvider.getUsernameFromToken(token));
        assertEquals(42L, tokenProvider.getUserIdFromToken(token));
    }

    @Test
    void testValidateToken_InvalidToken_ReturnsFalse() {
        assertFalse(tokenProvider.validateToken("invalid.jwt.token"));
    }
}
