package io.promoengine.service;

import io.promoengine.dto.request.CalculateRequest;
import io.promoengine.dto.request.OrderItem;
import io.promoengine.dto.response.CalculateResponse;
import io.promoengine.engine.CalculationResult;
import io.promoengine.engine.PromotionRuleEngine;
import io.promoengine.engine.TenantEngineRegistry;
import io.promoengine.enrichment.EnrichmentService;
import io.promoengine.enrichment.EnrichedTransaction;
import io.promoengine.mapper.PromotionResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {

    private final EnrichmentService enrichmentService;
    private final TenantEngineRegistry tenantEngineRegistry;
    private final PromotionResponseMapper responseMapper;

    public ResponseEntity<CalculateResponse> calculate(CalculateRequest request, String tenantId) {
        log.info("Calculating promotions for store={} tenant={}", request.getStoreId(), tenantId);

        EnrichmentService.EnrichRequest enrichReq = buildEnrichRequest(request);
        EnrichedTransaction tx = enrichmentService.enrich(enrichReq);

        PromotionRuleEngine engine = tenantEngineRegistry.getEngine(tenantId);
        CalculationResult result = engine.calculate(tx);

        CalculateResponse response = responseMapper.toResponse(result, request, tx);

        HttpStatus status = tx.getSkippedItems().isEmpty() ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT;
        return ResponseEntity.status(status).body(response);
    }

    private EnrichmentService.EnrichRequest buildEnrichRequest(CalculateRequest req) {
        List<EnrichmentService.EnrichRequest.Item> items = req.getItems().stream()
                .map(i -> new EnrichmentService.EnrichRequest.Item(i.getSku(), i.getQuantity(), i.getUnitPrice(), i.getCategoryCode()))
                .collect(Collectors.toList());
        return new EnrichmentService.EnrichRequest(
                req.getStoreId(), req.getCustomerId(),
                req.getTransactionDate() != null ? req.getTransactionDate() : LocalDate.now(),
                items);
    }
}
