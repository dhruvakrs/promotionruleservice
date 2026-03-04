package io.promoengine.controller;

import io.promoengine.dto.request.PrsRuleSet;
import io.promoengine.rules.MmedXmlParser;
import io.promoengine.rules.PrsRuleImporter;
import io.promoengine.rules.PromotionRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/rules/import")
@RequiredArgsConstructor
@Tag(name = "Rule Import", description = "Bulk rule import — MMED XML (legacy) or PRS JSON (new format)")
public class RuleImportController {

    private final MmedXmlParser mmedXmlParser;
    private final PromotionRuleService promotionRuleService;
    private final PrsRuleImporter prsRuleImporter;

    @PostMapping(consumes = "multipart/form-data")
    @Operation(
        operationId = "13_importRules",
        summary = "Bulk import promotion rules from MMED XML file",
        description = "Upload an MMED XML file. Each <Promotion id=\"...\"> element is parsed, " +
                      "converted to DRL, saved as DRAFT, and immediately activated. " +
                      "See src/main/resources/samples/mmed-sample.xml for the expected format."
    )
    public ResponseEntity<Map<String, Object>> importFromFile(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {

        String tenantId = (String) httpRequest.getAttribute("tenantId");
        if (tenantId == null) tenantId = "default";

        log.info("MMED XML import: file={}, size={}, tenant={}", file.getOriginalFilename(), file.getSize(), tenantId);

        try {
            String xml = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<Map<String, String>> parsed = mmedXmlParser.parseXml(xml);

            List<String> imported = new ArrayList<>();
            List<String> failed = new ArrayList<>();

            for (Map<String, String> promo : parsed) {
                String promotionId = promo.get("promotionId");
                String drl = promo.get("drl");
                try {
                    Map<String, Object> req = Map.of(
                        "promotionId", promotionId,
                        "ruleBody", drl,
                        "priority", 10,
                        "description", "Imported from MMED XML"
                    );
                    var created = promotionRuleService.create(req, tenantId);
                    promotionRuleService.activate(created.getPromotionId(), tenantId);
                    imported.add(promotionId);
                    log.info("Imported and activated rule: {}", promotionId);
                } catch (Exception e) {
                    log.warn("Failed to import rule {}: {}", promotionId, e.getMessage());
                    failed.add(promotionId + ": " + e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                "imported", imported.size(),
                "failed", failed.size(),
                "importedIds", imported,
                "errors", failed
            ));

        } catch (Exception e) {
            log.error("MMED XML import failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/prs")
    @Operation(
        operationId = "14_importRulesFromPrs",
        summary = "Bulk import promotion rules from PRS JSON",
        description = "Accepts a PromoRuleSet (PRS) JSON payload. Delta-aware: rules with same " +
                      "or higher version in DB are skipped. New/updated rules are activated in a " +
                      "single engine reload. action=delete deactivates existing ACTIVE rules."
    )
    public ResponseEntity<Map<String, Object>> importFromPrs(
            @RequestBody PrsRuleSet ruleSet,
            HttpServletRequest httpRequest) {

        String tenantId = (String) httpRequest.getAttribute("tenantId");
        if (tenantId == null) tenantId = "default";

        log.info("PRS import: tenant={}, rules={}", tenantId,
                ruleSet.getRules() == null ? 0 : ruleSet.getRules().size());

        try {
            Map<String, Object> result = prsRuleImporter.importPrs(ruleSet, tenantId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("PRS import failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/s3")
    @Operation(operationId = "15_importRulesFromS3", summary = "Trigger import from configured S3 bucket")
    public ResponseEntity<Map<String, Object>> importFromS3(HttpServletRequest httpRequest) {
        log.info("S3 import triggered");
        return ResponseEntity.ok(Map.of("imported", 0, "message", "S3 import not configured in this environment"));
    }
}
