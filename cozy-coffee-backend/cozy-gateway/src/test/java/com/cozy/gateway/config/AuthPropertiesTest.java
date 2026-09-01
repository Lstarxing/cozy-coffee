package com.cozy.gateway.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AuthPropertiesTest {
    @Test
    void devLoginEnabled_defaultsToFalse() {
        assertFalse(new AuthProperties().isDevLoginEnabled());
    }
}
