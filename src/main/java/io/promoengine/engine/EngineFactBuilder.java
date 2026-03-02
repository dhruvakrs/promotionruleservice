package io.promoengine.engine;

import io.promoengine.engine.model.InvoiceItem;
import io.promoengine.engine.model.Transaction;
import io.promoengine.enrichment.EnrichedItem;
import io.promoengine.enrichment.EnrichedTransaction;

import java.util.ArrayList;
import java.util.List;

public class EngineFactBuilder {

    private EngineFactBuilder() {}

    public static List<Object> build(EnrichedTransaction tx) {
        List<Object> facts = new ArrayList<>();
        facts.add(Transaction.builder()
                .storeId(tx.getStoreId())
                .customerId(tx.getCustomerId())
                .transactionDate(tx.getTransactionDate())
                .build());
        for (EnrichedItem item : tx.getEnrichedItems()) {
            facts.add(InvoiceItem.builder()
                    .sku(item.getSku())
                    .storeCode(item.getStoreCode())
                    .categoryCode(item.getCategoryCode())
                    .subcategoryCode(item.getSubcategoryCode())
                    .departmentCode(item.getDepartmentCode())
                    .foodItem(item.isFoodItem())
                    .unitPrice(item.getUnitPrice())
                    .lineAmount(item.getLineAmount())
                    .quantity(item.getQuantity())
                    .build());
        }
        return facts;
    }
}
