package io.promoengine.enrichment;

import io.promoengine.enrichment.customer.CustomerRepository;
import io.promoengine.enrichment.item.ItemRepository;
import io.promoengine.enrichment.price.PriceRepository;
import io.promoengine.exception.EnrichmentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrichmentService {

    private final PriceRepository priceRepository;
    private final ItemRepository itemRepository;
    private final CustomerRepository customerRepository;

    public EnrichedTransaction enrich(EnrichRequest request) {
        List<String> skus = request.getItems().stream()
                .map(EnrichRequest.Item::getSku)
                .collect(Collectors.toList());

        try {
            var pricesFuture = CompletableFuture.supplyAsync(() ->
                    priceRepository.findPrices(skus, request.getStoreId()));
            var propertiesFuture = CompletableFuture.supplyAsync(() ->
                    itemRepository.findProperties(skus));
            var customerFuture = CompletableFuture.supplyAsync(() ->
                    customerRepository.findCustomer(request.getCustomerId()));

            CompletableFuture.allOf(pricesFuture, propertiesFuture, customerFuture).join();

            Map<String, PriceData> prices = pricesFuture.join();
            Map<String, ItemPropertyData> properties = propertiesFuture.join();
            Optional<CustomerData> customer = customerFuture.join();

            List<EnrichedItem> enrichedItems = new ArrayList<>();
            List<String> skippedItems = new ArrayList<>();

            for (EnrichRequest.Item item : request.getItems()) {
                if (prices.containsKey(item.getSku())) {
                    PriceData price = prices.get(item.getSku());
                    ItemPropertyData props = properties.get(item.getSku());
                    BigDecimal lineAmount = price.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    EnrichedItem enriched = EnrichedItem.builder()
                            .sku(item.getSku())
                            .storeCode(request.getStoreId())
                            .categoryCode(props != null ? props.getCategoryCode() : null)
                            .subcategoryCode(props != null ? props.getSubcategoryCode() : null)
                            .departmentCode(props != null ? props.getDepartmentCode() : null)
                            .foodItem(props != null && props.isFoodItem())
                            .unitPrice(price.getUnitPrice())
                            .lineAmount(lineAmount)
                            .quantity(item.getQuantity())
                            .build();
                    enrichedItems.add(enriched);
                } else {
                    skippedItems.add(item.getSku());
                }
            }

            return EnrichedTransaction.builder()
                    .storeId(request.getStoreId())
                    .customerId(request.getCustomerId())
                    .transactionDate(request.getTransactionDate())
                    .enrichedItems(enrichedItems)
                    .customerData(customer.orElse(null))
                    .skippedItems(skippedItems)
                    .build();

        } catch (Exception e) {
            throw new EnrichmentException("Enrichment failed for store=" + request.getStoreId(), e);
        }
    }

    public static class EnrichRequest {
        private String storeId;
        private String customerId;
        private LocalDate transactionDate;
        private List<Item> items;

        public EnrichRequest() {}
        public EnrichRequest(String storeId, String customerId, LocalDate transactionDate, List<Item> items) {
            this.storeId = storeId;
            this.customerId = customerId;
            this.transactionDate = transactionDate;
            this.items = items;
        }

        public String getStoreId() { return storeId; }
        public String getCustomerId() { return customerId; }
        public LocalDate getTransactionDate() { return transactionDate; }
        public List<Item> getItems() { return items; }

        public static class Item {
            private String sku;
            private int quantity;
            public Item() {}
            public Item(String sku, int quantity) { this.sku = sku; this.quantity = quantity; }
            public String getSku() { return sku; }
            public int getQuantity() { return quantity; }
        }
    }
}
