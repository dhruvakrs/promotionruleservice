package io.promoengine.controller;

import io.promoengine.domain.jpa.PromotionRuleEntity;
import io.promoengine.domain.jpa.PromotionRuleHistoryEntity;
import io.promoengine.dto.request.CreateRuleRequest;
import io.promoengine.dto.request.UpdateRuleRequest;
import io.promoengine.dto.response.ErrorResponse;
import io.promoengine.rules.PromotionRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
@Tag(name = "Promotion Rule Management", description = "Rule CRUD and lifecycle endpoints")
public class PromotionRuleController {

    private final PromotionRuleService ruleService;

    @PostMapping
    @Operation(operationId = "3_createRule", summary = "Create a promotion rule (DRAFT status)")
    public ResponseEntity<PromotionRuleEntity> createRule(
            @Valid @RequestBody CreateRuleRequest req,
            HttpServletRequest httpRequest) {
        String tenantId = getTenantId(httpRequest);
        Map<String, Object> map = toMap(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleService.create(map, tenantId));
    }

    @GetMapping
    @Operation(operationId = "4_listRules", summary = "List rules (paginated)")
    public ResponseEntity<Page<PromotionRuleEntity>> listRules(
            @RequestParam(required = false) String status,
            Pageable pageable,
            HttpServletRequest httpRequest) {
        String tenantId = getTenantId(httpRequest);
        return ResponseEntity.ok(ruleService.list(tenantId, status, pageable));
    }

    @PostMapping("/{id}/activate")
    @Operation(operationId = "5_activateRule", summary = "Activate a DRAFT rule — pushes to engine")
    public ResponseEntity<PromotionRuleEntity> activateRule(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        String tenantId = getTenantId(httpRequest);
        return ResponseEntity.ok(ruleService.activate(id, tenantId));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(operationId = "6_deactivateRule", summary = "Deactivate an ACTIVE rule")
    public ResponseEntity<PromotionRuleEntity> deactivateRule(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        String tenantId = getTenantId(httpRequest);
        return ResponseEntity.ok(ruleService.deactivate(id, tenantId));
    }

    @PostMapping("/{id}/rollback")
    @Operation(operationId = "7_rollbackRule", summary = "Rollback to a previous version")
    public ResponseEntity<PromotionRuleEntity> rollbackRule(
            @PathVariable String id,
            @RequestParam int version,
            HttpServletRequest httpRequest) {
        String tenantId = getTenantId(httpRequest);
        return ResponseEntity.ok(ruleService.rollback(id, version, tenantId));
    }

    @GetMapping("/{id}/history")
    @Operation(operationId = "8_getRuleHistory", summary = "Get full version history for a rule")
    public ResponseEntity<List<PromotionRuleHistoryEntity>> getRuleHistory(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        String tenantId = getTenantId(httpRequest);
        return ResponseEntity.ok(ruleService.getHistory(id, tenantId));
    }

    private String getTenantId(HttpServletRequest req) {
        String tenantId = (String) req.getAttribute("tenantId");
        return tenantId != null ? tenantId : "default";
    }

    private Map<String, Object> toMap(CreateRuleRequest req) {
        Map<String, Object> map = new HashMap<>();
        map.put("promotionId", req.getPromotionId());
        map.put("ruleBody", req.getRuleBody());
        map.put("description", req.getDescription());
        map.put("priority", req.getPriority());
        map.put("ruleType", req.getRuleType());
        return map;
    }
}
