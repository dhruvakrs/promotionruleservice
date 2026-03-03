package io.promoengine.enrichment.price;

import io.promoengine.enrichment.PriceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnBean(ElasticsearchOperations.class)
public class OpenSearchPriceRepository implements PriceRepository {

    private static final String SENTINEL_STORE = "999999";

    private final PriceEsRepository priceEsRepository;
    private final DatabasePriceRepository databasePriceRepository;

    @Override
    public Optional<PriceData> findPrice(String sku, String storeCode) {
        // Step 1: store-specific price from ES
        var storePriceFuture = CompletableFuture.supplyAsync(() ->
                priceEsRepository.findBySkuAndStoreCode(sku, storeCode));
        // Step 2: sentinel price from ES (in parallel)
        var sentinelFuture = CompletableFuture.supplyAsync(() ->
                priceEsRepository.findBySkuAndStoreCode(sku, SENTINEL_STORE));

        CompletableFuture.allOf(storePriceFuture, sentinelFuture).join();

        Optional<PriceDocument> storePrice = storePriceFuture.join();
        if (storePrice.isPresent()) {
            return storePrice.map(this::toData);
        }

        Optional<PriceDocument> sentinelPrice = sentinelFuture.join();
        if (sentinelPrice.isPresent()) {
            return sentinelPrice.map(this::toData);
        }

        // Step 3: Oracle DB fallback
        return databasePriceRepository.findPrice(sku, storeCode);
    }

    @Override
    public Map<String, PriceData> findPrices(List<String> skus, String storeCode) {
        return skus.stream()
                .map(sku -> Map.entry(sku, findPrice(sku, storeCode)))
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    private PriceData toData(PriceDocument doc) {
        return PriceData.builder()
                .sku(doc.getSku())
                .storeCode(doc.getStoreCode())
                .unitPrice(doc.getUnitPrice())
                .categoryCode(doc.getCategoryCode())
                .subcategoryCode(doc.getSubcategoryCode())
                .departmentCode(doc.getDepartmentCode())
                .foodItem(doc.isFoodItem())
                .build();
    }
}
