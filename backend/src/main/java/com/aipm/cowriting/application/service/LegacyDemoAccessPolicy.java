package com.aipm.cowriting.application.service;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LegacyDemoAccessPolicy {

    private final boolean development;
    private final UUID ownerId;

    public LegacyDemoAccessPolicy(
            @Value("${app.environment:development}") String environment,
            @Value("${app.legacy-demo-owner-id:}") String ownerId
    ) {
        this.development = "development".equalsIgnoreCase(environment)
                || "dev".equalsIgnoreCase(environment)
                || "local".equalsIgnoreCase(environment);
        this.ownerId = parse(ownerId);
    }

    public boolean allows(UUID userId) {
        return development && ownerId != null && ownerId.equals(userId);
    }

    private UUID parse(String value) {
        try {
            return value == null || value.isBlank() ? null : UUID.fromString(value.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
