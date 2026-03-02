package io.promoengine.rules;

import io.promoengine.domain.jpa.PromotionRuleEntity;
import io.promoengine.domain.jpa.PromotionRuleRepository;
import io.promoengine.engine.TenantEngineRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleExpiryScheduler {

    private final PromotionRuleRepository ruleRepository;
    private final PromotionRuleLoader ruleLoader;
    private final TenantEngineRegistry tenantEngineRegistry;

    @Scheduled(cron = "${promoengine.rules.expiry-cron:0 0 2 * * *}")
    @Transactional
    public void expireRules() {
        log.info("Running rule expiry check");
        LocalDate today = LocalDate.now();

        List<PromotionRuleEntity> toExpire = ruleRepository.findAll().stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .filter(r -> r.getEndDate() != null && r.getEndDate().isBefore(today))
                .collect(Collectors.toList());

        if (toExpire.isEmpty()) {
            log.info("No rules to expire");
            return;
        }

        Map<String, List<PromotionRuleEntity>> byTenant = toExpire.stream()
                .collect(Collectors.groupingBy(PromotionRuleEntity::getTenantId));

        for (PromotionRuleEntity rule : toExpire) {
            rule.setStatus("INACTIVE");
            rule.setUpdatedAt(LocalDateTime.now());
            ruleRepository.save(rule);
            log.info("Expired rule: {} for tenant: {}", rule.getPromotionId(), rule.getTenantId());
        }

        for (String tenantId : byTenant.keySet()) {
            List<RuleDefinition> activeRules = ruleLoader.loadActiveRules(tenantId);
            if (tenantEngineRegistry.hasTenant(tenantId)) {
                tenantEngineRegistry.reloadTenant(tenantId, activeRules);
            }
        }
    }
}
