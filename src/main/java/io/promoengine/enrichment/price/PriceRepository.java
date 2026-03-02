package io.promoengine.enrichment.price;

import io.promoengine.enrichment.PriceData;

import java.util.Map;
import java.util.Optional;

public interface PriceRepository {
    Optional<PriceData> findPrice(String sku, String storeCode);
    Map<String, PriceData> findPrices(java.util.List<String> skus, String storeCode);
}
