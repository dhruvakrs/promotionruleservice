package io.promoengine.controller;

import io.promoengine.domain.jpa.PromotionPresetEntity;
import io.promoengine.domain.jpa.PromotionPresetRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/presets")
@RequiredArgsConstructor
@Tag(name = "Promotion Presets", description = "Preset management endpoints")
public class PromotionPresetController {

    private final PromotionPresetRepository presetRepository;

    @PostMapping
    @Operation(operationId = "9_createPreset", summary = "Create a promotion preset")
    public ResponseEntity<PromotionPresetEntity> createPreset(
            @RequestBody Map<String, Object> req,
            HttpServletRequest httpRequest) {
        String tenantId = getTenantId(httpRequest);
        PromotionPresetEntity entity = PromotionPresetEntity.builder()
                .id((String) req.get("id"))
                .tenantId(tenantId)
                .ruleBody((String) req.get("ruleBody"))
                .status("ACTIVE")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(presetRepository.save(entity));
    }

    @GetMapping
    @Operation(operationId = "10_listPresets", summary = "List all presets")
    public ResponseEntity<List<PromotionPresetEntity>> listPresets(HttpServletRequest httpRequest) {
        String tenantId = getTenantId(httpRequest);
        return ResponseEntity.ok(presetRepository.findByTenantId(tenantId));
    }

    @PutMapping("/{id}")
    @Operation(operationId = "11_updatePreset", summary = "Update a preset")
    public ResponseEntity<PromotionPresetEntity> updatePreset(
            @PathVariable String id,
            @RequestBody Map<String, Object> req,
            HttpServletRequest httpRequest) {
        PromotionPresetEntity entity = presetRepository.findById(id)
                .orElseThrow(() -> new io.promoengine.exception.RuleSetException(16, "Preset not found: " + id));
        if (req.containsKey("ruleBody")) entity.setRuleBody((String) req.get("ruleBody"));
        return ResponseEntity.ok(presetRepository.save(entity));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "12_deletePreset", summary = "Deactivate a preset")
    public ResponseEntity<PromotionPresetEntity> deletePreset(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        PromotionPresetEntity entity = presetRepository.findById(id)
                .orElseThrow(() -> new io.promoengine.exception.RuleSetException(16, "Preset not found: " + id));
        entity.setStatus("INACTIVE");
        return ResponseEntity.ok(presetRepository.save(entity));
    }

    private String getTenantId(HttpServletRequest req) {
        String tenantId = (String) req.getAttribute("tenantId");
        return tenantId != null ? tenantId : "default";
    }
}
