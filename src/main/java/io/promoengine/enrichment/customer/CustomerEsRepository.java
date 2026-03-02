package io.promoengine.enrichment.customer;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface CustomerEsRepository extends ElasticsearchRepository<CustomerDocument, String> {
    Optional<CustomerDocument> findByCustomerId(String customerId);
}
