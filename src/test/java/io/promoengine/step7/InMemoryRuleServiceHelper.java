package io.promoengine.step7;

import io.promoengine.engine.PromotionRuleEngine;
import io.promoengine.exception.RuleCompilationException;
import io.promoengine.rules.RuleDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory mock for PromotionRuleService — used in Karate step7 tests only.
 * Simulates the service lifecycle without requiring a DB or Spring context.
 */
public class InMemoryRuleServiceHelper {

    private final Map<String, Map<String, Object>> rules = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> history = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    public Map<String, Object> create(Map<String, Object> req, String tenantId) {
        String promotionId = (String) req.get("promotionId");
        Map<String, Object> rule = new HashMap<>(req);
        rule.put("id", promotionId);
        rule.put("status", "DRAFT");
        rule.put("version", 1);
        rule.put("tenantId", tenantId);
        rules.put(promotionId, rule);
        appendHistory(promotionId, "DRAFT", 1, (String) req.get("ruleBody"));
        return rule;
    }

    public Map<String, Object> activate(String id, String tenantId) {
        Map<String, Object> rule = getRule(id);
        String ruleBody = (String) rule.get("ruleBody");

        // Validate DRL
        PromotionRuleEngine testEngine = new PromotionRuleEngine("__test__");
        testEngine.reload(List.of(RuleDefinition.builder()
                .promotionId("__test__")
                .ruleBody(ruleBody)
                .priority(0)
                .build()));

        rule.put("status", "ACTIVE");
        appendHistory(id, "ACTIVE", (Integer) rule.get("version"), ruleBody);
        return rule;
    }

    public Map<String, Object> deactivate(String id, String tenantId) {
        Map<String, Object> rule = getRule(id);
        rule.put("status", "INACTIVE");
        appendHistory(id, "INACTIVE", (Integer) rule.get("version"), (String) rule.get("ruleBody"));
        return rule;
    }

    public List<Map<String, Object>> getHistory(String id, String tenantId) {
        return history.getOrDefault(id, new ArrayList<>());
    }

    private Map<String, Object> getRule(String id) {
        Map<String, Object> rule = rules.get(id);
        if (rule == null) throw new RuntimeException("Rule not found: " + id);
        return rule;
    }

    private void appendHistory(String id, String status, int version, String ruleBody) {
        history.computeIfAbsent(id, k -> new ArrayList<>())
                .add(Map.of("ruleId", id, "status", status, "version", version,
                        "ruleBody", ruleBody == null ? "" : ruleBody));
    }
}
