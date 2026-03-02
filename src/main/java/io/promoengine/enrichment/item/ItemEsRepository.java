package io.promoengine.enrichment.item;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface ItemEsRepository extends ElasticsearchRepository<ItemDocument, String> {
    Optional<ItemDocument> findBySku(String sku);
}
