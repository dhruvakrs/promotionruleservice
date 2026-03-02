package io.promoengine.controller;

import io.promoengine.dto.request.CalculateRequest;
import io.promoengine.dto.response.CalculateResponse;
import io.promoengine.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotion Calculation", description = "Promotion calculation endpoints")
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping("/calculate")
    @Operation(operationId = "2_calculate", summary = "Calculate promotions for a transaction")
    public ResponseEntity<CalculateResponse> calculate(
            @Valid @RequestBody CalculateRequest request,
            HttpServletRequest httpRequest) {
        String tenantId = (String) httpRequest.getAttribute("tenantId");
        if (tenantId == null) tenantId = "default";
        return promotionService.calculate(request, tenantId);
    }
}
