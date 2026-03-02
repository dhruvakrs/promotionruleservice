package io.promoengine.enrichment.item;

import io.promoengine.enrichment.ItemPropertyData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class DatabaseItemRepository implements ItemRepository {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseItemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ItemPropertyData> findProperties(String sku) {
        try {
            List<ItemPropertyData> results = jdbcTemplate.query(
                    "SELECT item_no, category_code, subcategory_code, department_code, food_indicator " +
                    "FROM promoitem WHERE item_no = ? FETCH FIRST 1 ROWS ONLY",
                    (rs, rowNum) -> ItemPropertyData.builder()
                            .sku(rs.getString("item_no"))
                            .categoryCode(rs.getString("category_code"))
                            .subcategoryCode(rs.getString("subcategory_code"))
                            .departmentCode(rs.getString("department_code"))
                            .foodItem("Y".equalsIgnoreCase(rs.getString("food_indicator")))
                            .build(),
                    sku
            );
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            log.warn("DB item lookup failed for sku={}: {}", sku, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Map<String, ItemPropertyData> findProperties(List<String> skus) {
        return skus.stream()
                .map(sku -> Map.entry(sku, findProperties(sku)))
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }
}
