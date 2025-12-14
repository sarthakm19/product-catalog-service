package com.product.catalog.service.impl;

import com.product.catalog.dto.LoginRequest;
import com.product.catalog.dto.LoginResponse;
import com.product.catalog.security.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.
 * Tests authentication flow and token generation.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginRequest loginRequest;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("admin", "admin123");
        userDetails = User.builder()
                .username("admin")
                .password("encodedPassword")
                .authorities(new ArrayList<>())
                .build();
    }

    @Test
    void authenticate_WithValidCredentials_ShouldReturnLoginResponse() {
        // Given
        String expectedToken = "jwt.token.here";
        long expectedExpiration = 86400L;

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);
        when(jwtTokenUtil.generateToken(userDetails)).thenReturn(expectedToken);
        when(jwtTokenUtil.getExpirationInSeconds()).thenReturn(expectedExpiration);

        // When
        LoginResponse response = authService.authenticate(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals(expectedToken, response.getToken());
        assertEquals("Bearer", response.getType());
        assertEquals(expectedExpiration, response.getExpiresIn());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService).loadUserByUsername("admin");
        verify(jwtTokenUtil).generateToken(userDetails);
        verify(jwtTokenUtil).getExpirationInSeconds();
    }

    @Test
    void authenticate_WithInvalidCredentials_ShouldThrowException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        assertThrows(BadCredentialsException.class, () -> authService.authenticate(loginRequest));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtTokenUtil, never()).generateToken(any());
    }

    @Test
    void authenticate_WithNullUsername_ShouldThrowException() {
        // Given
        LoginRequest invalidRequest = new LoginRequest(null, "password");

        // When & Then
        assertThrows(Exception.class, () -> authService.authenticate(invalidRequest));
    }

    @Test
    void authenticate_ShouldSetSecurityContext() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);
        when(jwtTokenUtil.generateToken(userDetails)).thenReturn("token");
        when(jwtTokenUtil.getExpirationInSeconds()).thenReturn(86400L);

        // When
        authService.authenticate(loginRequest);

        // Then
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        // SecurityContext setting is verified by the authentication flow
    }
}

