package io.promoengine.enrichment.customer;

import io.promoengine.enrichment.CustomerData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class DatabaseCustomerRepository implements CustomerRepository {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CustomerData> findCustomer(String customerId) {
        try {
            List<CustomerData> results = jdbcTemplate.query(
                    "SELECT customer_id, customer_group_code, customer_type_code " +
                    "FROM promocustomer WHERE customer_id = ? FETCH FIRST 1 ROWS ONLY",
                    (rs, rowNum) -> CustomerData.builder()
                            .customerId(rs.getString("customer_id"))
                            .customerGroupCode(rs.getString("customer_group_code"))
                            .customerTypeCode(rs.getString("customer_type_code"))
                            .build(),
                    customerId
            );
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            log.warn("DB customer lookup failed for customerId={}: {}", customerId, e.getMessage());
            return Optional.empty();
        }
    }
}
