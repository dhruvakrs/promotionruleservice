package io.promoengine.rules;

import io.promoengine.domain.jpa.PromotionRuleEntity;
import io.promoengine.domain.jpa.PromotionRuleHistoryEntity;
import io.promoengine.domain.jpa.PromotionRuleHistoryRepository;
import io.promoengine.domain.jpa.PromotionRuleRepository;
import io.promoengine.engine.TenantEngineRegistry;
import io.promoengine.exception.RuleCompilationException;
import io.promoengine.exception.RuleSetException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionRuleService {

    private final PromotionRuleRepository ruleRepository;
    private final PromotionRuleHistoryRepository historyRepository;
    private final PromotionRuleLoader ruleLoader;
    private final TenantEngineRegistry tenantEngineRegistry;

    @Transactional
    public PromotionRuleEntity create(Map<String, Object> req, String tenantId) {
        String promotionId = (String) req.get("promotionId");
        String ruleBody = (String) req.get("ruleBody");
        String description = (String) req.get("description");
        int priority = req.containsKey("priority") ? ((Number) req.get("priority")).intValue() : 10;

        PromotionRuleEntity entity = PromotionRuleEntity.builder()
                .promotionId(promotionId)
                .tenantId(tenantId)
                .ruleBody(ruleBody)
                .description(description)
                .priority(priority)
                .status("DRAFT")
                .version(1)
                .createdAt(LocalDateTime.now())
                .build();

        PromotionRuleEntity saved = ruleRepository.save(entity);
        saveHistory(saved, "Created as DRAFT", tenantId);
        return saved;
    }

    @Transactional
    public PromotionRuleEntity activate(String id, String tenantId) {
        PromotionRuleEntity entity = ruleRepository.findById(id)
                .orElseThrow(() -> new RuleSetException(16, "Rule not found: " + id));

        // Validate DRL compiles before activating
        validateDrl(entity.getRuleBody(), tenantId);

        entity.setStatus("ACTIVE");
        entity.setUpdatedAt(LocalDateTime.now());
        PromotionRuleEntity saved = ruleRepository.save(entity);
        saveHistory(saved, "Activated", tenantId);

        // Reload engine with new active rules
        List<RuleDefinition> activeRules = ruleLoader.loadActiveRules(tenantId);
        tenantEngineRegistry.reloadTenant(tenantId, activeRules);

        return saved;
    }

    @Transactional
    public PromotionRuleEntity deactivate(String id, String tenantId) {
        PromotionRuleEntity entity = ruleRepository.findById(id)
                .orElseThrow(() -> new RuleSetException(16, "Rule not found: " + id));

        entity.setStatus("INACTIVE");
        entity.setUpdatedAt(LocalDateTime.now());
        PromotionRuleEntity saved = ruleRepository.save(entity);
        saveHistory(saved, "Deactivated", tenantId);

        List<RuleDefinition> activeRules = ruleLoader.loadActiveRules(tenantId);
        tenantEngineRegistry.reloadTenant(tenantId, activeRules);

        return saved;
    }

    @Transactional
    public PromotionRuleEntity rollback(String id, int targetVersion, String tenantId) {
        PromotionRuleEntity current = ruleRepository.findById(id)
                .orElseThrow(() -> new RuleSetException(16, "Rule not found: " + id));

        PromotionRuleHistoryEntity historyEntry = historyRepository
                .findByRuleIdAndVersion(id, targetVersion)
                .orElseThrow(() -> new RuleSetException(16, "No history at version " + targetVersion));

        current.setRuleBody(historyEntry.getRuleBody());
        current.setDescription(historyEntry.getStatus() != null ? current.getDescription() : current.getDescription());
        current.setVersion(targetVersion);
        current.setStatus("DRAFT");
        current.setUpdatedAt(LocalDateTime.now());

        PromotionRuleEntity saved = ruleRepository.save(current);
        saveHistory(saved, "Rolled back to version " + targetVersion, tenantId);
        return saved;
    }

    public List<PromotionRuleHistoryEntity> getHistory(String id, String tenantId) {
        return historyRepository.findByRuleIdOrderByChangedAtAsc(id);
    }

    public Page<PromotionRuleEntity> list(String tenantId, String status, Pageable pageable) {
        if (status != null) {
            return ruleRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        }
        return ruleRepository.findByTenantId(tenantId, pageable);
    }

    private void validateDrl(String ruleBody, String tenantId) {
        try {
            var testEngine = new io.promoengine.engine.PromotionRuleEngine("__validate__" + tenantId);
            testEngine.reload(List.of(RuleDefinition.builder()
                    .promotionId("__validate__")
                    .ruleBody(ruleBody)
                    .priority(0)
                    .build()));
        } catch (RuleCompilationException e) {
            throw e;
        }
    }

    private void saveHistory(PromotionRuleEntity entity, String reason, String changedBy) {
        PromotionRuleHistoryEntity history = PromotionRuleHistoryEntity.builder()
                .ruleId(entity.getPromotionId())
                .version(entity.getVersion())
                .ruleBody(entity.getRuleBody())
                .status(entity.getStatus())
                .changedAt(LocalDateTime.now())
                .changedBy(changedBy)
                .changeReason(reason)
                .build();
        historyRepository.save(history);
    }
}
