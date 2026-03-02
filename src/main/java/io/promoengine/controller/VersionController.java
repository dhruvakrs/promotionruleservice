package io.promoengine.controller;

import io.promoengine.dto.response.VersionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Tag(name = "System", description = "System information endpoints")
public class VersionController {

    private static final String DROOLS_VERSION = "8.44.0.Final";
    private static final String APP_VERSION = "1.0.0-SNAPSHOT";

    @GetMapping("/version")
    @Operation(operationId = "1_getVersion", summary = "Get engine version info")
    public ResponseEntity<VersionResponse> getVersion() {
        return ResponseEntity.ok(VersionResponse.builder()
                .version(APP_VERSION)
                .droolsVersion(DROOLS_VERSION)
                .builtAt(LocalDateTime.now().toString())
                .artifact("promoengine-service")
                .build());
    }
}
