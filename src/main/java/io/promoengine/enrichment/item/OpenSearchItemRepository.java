package io.promoengine.enrichment.item;

import io.promoengine.enrichment.ItemPropertyData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnBean(ElasticsearchOperations.class)
public class OpenSearchItemRepository implements ItemRepository {

    private final ItemEsRepository itemEsRepository;
    private final DatabaseItemRepository databaseItemRepository;

    @Override
    public Optional<ItemPropertyData> findProperties(String sku) {
        return itemEsRepository.findBySku(sku)
                .map(this::toData)
                .or(() -> databaseItemRepository.findProperties(sku));
    }

    @Override
    public Map<String, ItemPropertyData> findProperties(List<String> skus) {
        return skus.stream()
                .map(sku -> Map.entry(sku, findProperties(sku)))
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    private ItemPropertyData toData(ItemDocument doc) {
        return ItemPropertyData.builder()
                .sku(doc.getSku())
                .categoryCode(doc.getCategoryCode())
                .subcategoryCode(doc.getSubcategoryCode())
                .departmentCode(doc.getDepartmentCode())
                .foodItem(doc.isFoodItem())
                .build();
    }
}
