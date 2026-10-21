package com.ray.knowledgeflow.common.utils;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboundUrlValidatorTest {

    @Test
    void publicHttpUrl_ShouldRejectLocalAndPrivateDestinations() {
        assertThrows(IllegalArgumentException.class,
                () -> OutboundUrlValidator.requirePublicHttpUrl("http://127.0.0.1/admin"));
        assertThrows(IllegalArgumentException.class,
                () -> OutboundUrlValidator.requirePublicHttpUrl("http://10.0.0.8/internal"));
        assertThrows(IllegalArgumentException.class,
                () -> OutboundUrlValidator.requirePublicHttpUrl("http://169.254.169.254/latest/meta-data"));
        assertThrows(IllegalArgumentException.class,
                () -> OutboundUrlValidator.requirePublicHttpUrl("file:///etc/passwd"));
        assertDoesNotThrow(() -> OutboundUrlValidator.requirePublicHttpUrl("https://8.8.8.8/feed"));
    }

    @Test
    void credentialedServiceUrl_ShouldRequireExactHttpsHost() {
        assertDoesNotThrow(() -> OutboundUrlValidator.requireHost(
                "https://8.8.8.8/feed", true, Set.of("8.8.8.8")));
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlValidator.requireHost(
                "https://api.github.com.attacker.example/collect", true, Set.of("api.github.com")));
        assertThrows(IllegalArgumentException.class, () -> OutboundUrlValidator.requireHost(
                "http://api.github.com/user/starred", true, Set.of("api.github.com")));
    }
}
