package io.promoengine.engine;

import io.promoengine.exception.TenantNotFoundException;
import io.promoengine.rules.RuleDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TenantEngineRegistry {

    private final ConcurrentHashMap<String, PromotionRuleEngine> registry = new ConcurrentHashMap<>();

    public PromotionRuleEngine getEngine(String tenantId) {
        PromotionRuleEngine engine = registry.get(tenantId);
        if (engine == null) {
            throw new TenantNotFoundException(tenantId);
        }
        return engine;
    }

    public void reloadTenant(String tenantId, List<RuleDefinition> rules) {
        PromotionRuleEngine engine = registry.computeIfAbsent(tenantId, PromotionRuleEngine::new);
        engine.reload(rules);
        log.info("Reloaded engine for tenant: {}", tenantId);
    }

    public boolean hasTenant(String tenantId) {
        return registry.containsKey(tenantId);
    }
}
