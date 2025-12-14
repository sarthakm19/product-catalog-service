package com.product.catalog.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Custom UserDetailsService implementation
 * For production, this should load users from a database
 * Currently uses in-memory users for demonstration
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final PasswordEncoder passwordEncoder;

    public CustomUserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        // TODO: In production, load user from database
        // This is a simple in-memory implementation for demonstration

        if ("admin".equals(username)) {
            return User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .authorities(new ArrayList<>())
                    .build();
        } else if ("user".equals(username)) {
            return User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user123"))
                    .authorities(new ArrayList<>())
                    .build();
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }
}
