package io.promoengine.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/rules/import")
@RequiredArgsConstructor
@Tag(name = "Rule Import", description = "Bulk rule import endpoints")
public class RuleImportController {

    @PostMapping(consumes = "multipart/form-data")
    @Operation(operationId = "13_importRules", summary = "Bulk import from XML file upload")
    public ResponseEntity<Map<String, Object>> importFromFile(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        log.info("Import request: file={}, size={}", file.getOriginalFilename(), file.getSize());
        return ResponseEntity.ok(Map.of("imported", 0, "message", "Import not yet implemented"));
    }

    @PostMapping("/s3")
    @Operation(operationId = "14_importRulesFromS3", summary = "Trigger import from S3")
    public ResponseEntity<Map<String, Object>> importFromS3(HttpServletRequest httpRequest) {
        log.info("S3 import triggered");
        return ResponseEntity.ok(Map.of("imported", 0, "message", "S3 import triggered"));
    }
}
