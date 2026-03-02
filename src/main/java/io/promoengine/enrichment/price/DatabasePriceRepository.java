package io.promoengine.enrichment.price;

import io.promoengine.enrichment.PriceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class DatabasePriceRepository implements PriceRepository {

    private static final String SENTINEL_STORE = "999999";
    private final JdbcTemplate jdbcTemplate;

    public DatabasePriceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<PriceData> findPrice(String sku, String storeCode) {
        try {
            List<PriceData> results = jdbcTemplate.query(
                    "SELECT item_no, store_code, unit_price, category_code, subcategory_code, department_code, food_indicator " +
                    "FROM promoprice WHERE item_no = ? AND store_code IN (?, ?) ORDER BY CASE WHEN store_code = ? THEN 0 ELSE 1 END FETCH FIRST 1 ROWS ONLY",
                    (rs, rowNum) -> PriceData.builder()
                            .sku(rs.getString("item_no"))
                            .storeCode(rs.getString("store_code"))
                            .unitPrice(rs.getBigDecimal("unit_price"))
                            .categoryCode(rs.getString("category_code"))
                            .subcategoryCode(rs.getString("subcategory_code"))
                            .departmentCode(rs.getString("department_code"))
                            .foodItem("Y".equalsIgnoreCase(rs.getString("food_indicator")))
                            .build(),
                    sku, storeCode, SENTINEL_STORE, storeCode
            );
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            log.warn("DB price lookup failed for sku={} store={}: {}", sku, storeCode, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Map<String, PriceData> findPrices(List<String> skus, String storeCode) {
        return skus.stream()
                .map(sku -> Map.entry(sku, findPrice(sku, storeCode)))
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }
}
