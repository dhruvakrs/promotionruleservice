package io.promoengine.enrichment.price;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface PriceEsRepository extends ElasticsearchRepository<PriceDocument, String> {
    Optional<PriceDocument> findBySkuAndStoreCode(String sku, String storeCode);
}
