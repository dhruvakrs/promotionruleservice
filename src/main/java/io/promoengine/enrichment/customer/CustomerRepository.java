package io.promoengine.enrichment.customer;

import io.promoengine.enrichment.CustomerData;

import java.util.Optional;

public interface CustomerRepository {
    Optional<CustomerData> findCustomer(String customerId);
}
