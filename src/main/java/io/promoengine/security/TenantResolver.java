package io.promoengine.security;

import io.promoengine.PromoEngineProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantResolver {

    private final PromoEngineProperties properties;

    public Optional<String> resolve(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        Map<String, String> tenants = properties.getSecurity().getTenants();
        if (tenants != null && tenants.containsKey(apiKey)) {
            return Optional.of(tenants.get(apiKey));
        }
        // Fallback: check single API key config
        String configuredKey = properties.getSecurity().getApiKey();
        if (apiKey.equals(configuredKey)) {
            return Optional.of(properties.getTenancy().getDefaultTenantId());
        }
        return Optional.empty();
    }
}
