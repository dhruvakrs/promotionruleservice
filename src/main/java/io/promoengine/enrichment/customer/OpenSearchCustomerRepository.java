package io.promoengine.enrichment.customer;

import io.promoengine.enrichment.CustomerData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnBean(ElasticsearchOperations.class)
public class OpenSearchCustomerRepository implements CustomerRepository {

    private final CustomerEsRepository customerEsRepository;
    private final DatabaseCustomerRepository databaseCustomerRepository;

    @Override
    public Optional<CustomerData> findCustomer(String customerId) {
        return customerEsRepository.findByCustomerId(customerId)
                .map(this::toData)
                .or(() -> databaseCustomerRepository.findCustomer(customerId));
    }

    private CustomerData toData(CustomerDocument doc) {
        return CustomerData.builder()
                .customerId(doc.getCustomerId())
                .customerGroupCode(doc.getCustomerGroupCode())
                .customerTypeCode(doc.getCustomerTypeCode())
                .build();
    }
}
