package com.cozy.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProdConfigGuardTest {

    private Environment env(String... profiles) {
        Environment e = mock(Environment.class);
        when(e.getActiveProfiles()).thenReturn(profiles);
        return e;
    }

    @Test
    void prod_withWildcardCors_fails() {
        assertThrows(IllegalStateException.class, () ->
                new ProdConfigGuard(env("prod"), "*", "x".repeat(40)).validate());
    }

    @Test
    void prod_withHttpsWildcardCors_fails() {
        assertThrows(IllegalStateException.class, () ->
                new ProdConfigGuard(env("prod"), "https://*", "x".repeat(40)).validate());
    }

    @Test
    void prod_withDevLoginEnabled_fails() {
        AuthProperties auth = new AuthProperties();
        auth.setDevLoginEnabled(true);
        assertThrows(IllegalStateException.class, () ->
                new ProdConfigGuard(env("prod"), auth, "https://shop.example.com", "x".repeat(40)).validate());
    }

    @Test
    void prod_withNonHttpsOrigin_fails() {
        assertThrows(IllegalStateException.class, () ->
                new ProdConfigGuard(env("prod"), "http://shop.example.com", "x".repeat(40)).validate());
    }

    @Test
    void prod_withDefaultJwtSecret_fails() {
        assertThrows(IllegalStateException.class, () ->
                new ProdConfigGuard(env("prod"), "https://shop.example.com",
                        ProdConfigGuard.DEFAULT_JWT_SECRET).validate());
    }

    @Test
    void prod_withShortJwtSecret_fails() {
        assertThrows(IllegalStateException.class, () ->
                new ProdConfigGuard(env("prod"), "https://shop.example.com", "short").validate());
    }

    @Test
    void prodMixedWithLocal_fails() {
        assertThrows(IllegalStateException.class, () ->
                new ProdConfigGuard(env("prod", "local"), "*", "x".repeat(40)).validate());
    }

    @Test
    void pureLocal_skipsValidation() {
        assertDoesNotThrow(() ->
                new ProdConfigGuard(env("local"), "*", null).validate());
    }
}
