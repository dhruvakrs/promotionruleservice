package io.promoengine.enrichment.item;

import io.promoengine.enrichment.ItemPropertyData;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ItemRepository {
    Optional<ItemPropertyData> findProperties(String sku);
    Map<String, ItemPropertyData> findProperties(List<String> skus);
}
