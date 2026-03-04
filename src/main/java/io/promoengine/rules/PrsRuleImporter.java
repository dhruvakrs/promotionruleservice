package io.promoengine.rules;

import io.promoengine.domain.jpa.PromotionRuleEntity;
import io.promoengine.domain.jpa.PromotionRuleHistoryEntity;
import io.promoengine.domain.jpa.PromotionRuleHistoryRepository;
import io.promoengine.domain.jpa.PromotionRuleRepository;
import io.promoengine.dto.request.PrsRuleSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Delta-aware PRS (PromoRuleSet) importer.
 *
 * Import strategy per rule:
 *   action=delete → deactivate if ACTIVE, skip otherwise
 *   action=upsert (default):
 *     – not in DB          → create as DRAFT
 *     – in DB, same/higher version → skip (already current)
 *     – in DB, lower version       → update ruleBody + bump version, reset to DRAFT
 *
 * After processing all rules: ONE engine reload per tenant (via activateBatch / deactivateBatch).
 * This is critical for bulk imports of thousands of rules — not one reload per rule.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrsRuleImporter {

    private final PromotionRuleRepository      ruleRepository;
    private final PromotionRuleHistoryRepository historyRepository;
    private final PrsDrlConverter              drlConverter;
    private final PromotionRuleService         promotionRuleService;

    public Map<String, Object> importPrs(PrsRuleSet ruleSet, String callerTenantId) {
        List<PrsRuleSet.PrsRule> rules = ruleSet.getRules();
        if (rules == null || rules.isEmpty()) {
            return buildSummary(0, 0, 0, 0, rules == null ? 0 : rules.size(), List.of());
        }

        // File-level tenantId overrides caller if set
        String tenantId = (ruleSet.getTenantId() != null && !ruleSet.getTenantId().isBlank())
                ? ruleSet.getTenantId() : callerTenantId;

        log.info("[{}] PRS import started: {} rules in file (format={}, version={})",
                tenantId, rules.size(), ruleSet.getFormat(), ruleSet.getVersion());

        // Batch-load all existing entities to avoid N+1 DB round-trips
        List<String> allIds = rules.stream()
                .map(PrsRuleSet.PrsRule::getId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toList());

        Map<String, PromotionRuleEntity> existing = ruleRepository.findAllById(allIds)
                .stream()
                .collect(Collectors.toMap(PromotionRuleEntity::getPromotionId, e -> e));

        List<String> toActivate   = new ArrayList<>();
        List<String> toDeactivate = new ArrayList<>();
        List<String> errors       = new ArrayList<>();
        int created = 0, updated = 0, deleted = 0, skipped = 0;

        for (PrsRuleSet.PrsRule rule : rules) {
            if (rule.getId() == null || rule.getId().isBlank()) {
                errors.add("(unknown): rule has no id — skipped");
                skipped++;
                continue;
            }

            try {
                String drl = drlConverter.convert(rule);
                PromotionRuleEntity existingEntity = existing.get(rule.getId());

                if ("delete".equalsIgnoreCase(rule.getAction())) {
                    if (existingEntity != null && "ACTIVE".equals(existingEntity.getStatus())) {
                        toDeactivate.add(rule.getId());
                        deleted++;
                    } else {
                        skipped++;
                    }
                    continue;
                }

                // upsert (default)
                if (existingEntity != null) {
                    if (existingEntity.getVersion() >= rule.getVersion()) {
                        log.debug("[{}] Skipping {} — DB version {} >= file version {}",
                                tenantId, rule.getId(), existingEntity.getVersion(), rule.getVersion());
                        skipped++;
                        continue;
                    }
                    // DB version is lower — update
                    existingEntity.setRuleBody(drl);
                    existingEntity.setDescription(rule.getDescription());
                    existingEntity.setPriority(rule.getPriority());
                    existingEntity.setVersion(rule.getVersion());
                    existingEntity.setStatus("DRAFT");
                    existingEntity.setUpdatedAt(LocalDateTime.now());
                    ruleRepository.save(existingEntity);
                    saveHistory(existingEntity,
                            "Updated via PRS import (v" + rule.getVersion() + ")", tenantId);
                    toActivate.add(rule.getId());
                    updated++;
                } else {
                    // new rule
                    PromotionRuleEntity entity = PromotionRuleEntity.builder()
                            .promotionId(rule.getId())
                            .tenantId(tenantId)
                            .ruleBody(drl)
                            .description(rule.getDescription())
                            .priority(rule.getPriority())
                            .version(rule.getVersion())
                            .status("DRAFT")
                            .createdAt(LocalDateTime.now())
                            .build();
                    ruleRepository.save(entity);
                    saveHistory(entity, "Created via PRS import", tenantId);
                    toActivate.add(rule.getId());
                    created++;
                }

            } catch (Exception e) {
                log.warn("[{}] PRS import: rule {} failed — {}", tenantId, rule.getId(), e.getMessage());
                errors.add(rule.getId() + ": " + e.getMessage());
            }
        }

        // Single engine reload for all activations
        if (!toActivate.isEmpty()) {
            Map<String, Object> batchResult = promotionRuleService.activateBatch(toActivate, tenantId);
            @SuppressWarnings("unchecked")
            List<String> batchFailed = (List<String>) batchResult.get("failed");
            errors.addAll(batchFailed);
        }

        // Single engine reload for all deactivations
        if (!toDeactivate.isEmpty()) {
            Map<String, Object> batchResult = promotionRuleService.deactivateBatch(toDeactivate, tenantId);
            @SuppressWarnings("unchecked")
            List<String> batchFailed = (List<String>) batchResult.get("failed");
            errors.addAll(batchFailed);
        }

        log.info("[{}] PRS import complete: created={}, updated={}, deleted={}, skipped={}, errors={}",
                tenantId, created, updated, deleted, skipped, errors.size());

        return buildSummary(created, updated, deleted, skipped, rules.size(), errors);
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

    private Map<String, Object> buildSummary(int created, int updated, int deleted,
                                              int skipped, int total, List<String> errors) {
        return Map.of(
                "total",   total,
                "created", created,
                "updated", updated,
                "deleted", deleted,
                "skipped", skipped,
                "errors",  errors
        );
    }
}
