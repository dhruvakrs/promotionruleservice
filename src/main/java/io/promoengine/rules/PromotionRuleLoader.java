package io.promoengine.rules;

import io.promoengine.domain.jpa.PromotionRuleEntity;
import io.promoengine.domain.jpa.PromotionRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionRuleLoader {

    private final PromotionRuleRepository ruleRepository;

    public List<RuleDefinition> loadActiveRules(String tenantId) {
        List<PromotionRuleEntity> activeRules = ruleRepository.findByTenantIdAndStatus(tenantId, "ACTIVE");
        log.info("[{}] Loading {} active rules", tenantId, activeRules.size());
        return activeRules.stream()
                .map(this::toDefinition)
                .collect(Collectors.toList());
    }

    private RuleDefinition toDefinition(PromotionRuleEntity entity) {
        return RuleDefinition.builder()
                .promotionId(entity.getPromotionId())
                .ruleBody(entity.getRuleBody())
                .priority(entity.getPriority())
                .description(entity.getDescription())
                .build();
    }
}
