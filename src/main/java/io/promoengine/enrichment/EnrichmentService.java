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
            // Only look up skus that don't already have a price provided in the request
            List<String> skusNeedingLookup = request.getItems().stream()
                    .filter(i -> i.getUnitPrice() == null)
                    .map(EnrichRequest.Item::getSku)
                    .collect(Collectors.toList());

            // Run lookups in parallel (skip if nothing to look up)
            var pricesFuture = skusNeedingLookup.isEmpty()
                    ? CompletableFuture.completedFuture(Map.<String, PriceData>of())
                    : CompletableFuture.supplyAsync(() -> priceRepository.findPrices(skusNeedingLookup, request.getStoreId()));
            var propertiesFuture = CompletableFuture.supplyAsync(() ->
                    itemRepository.findProperties(skus));
            var customerFuture = CompletableFuture.supplyAsync(() ->
                    customerRepository.findCustomer(request.getCustomerId()));

            CompletableFuture.allOf(pricesFuture, propertiesFuture, customerFuture).join();

            Map<String, PriceData> lookedUpPrices = pricesFuture.join();
            Map<String, ItemPropertyData> properties = propertiesFuture.join();
            Optional<CustomerData> customer = customerFuture.join();

            List<EnrichedItem> enrichedItems = new ArrayList<>();
            List<String> skippedItems = new ArrayList<>();

            for (EnrichRequest.Item item : request.getItems()) {
                // Resolve unit price: use request-supplied price first, fall back to lookup
                BigDecimal unitPrice = item.getUnitPrice() != null
                        ? item.getUnitPrice()
                        : (lookedUpPrices.containsKey(item.getSku()) ? lookedUpPrices.get(item.getSku()).getUnitPrice() : null);

                if (unitPrice == null) {
                    // No price from request or lookup — skip this item
                    skippedItems.add(item.getSku());
                    continue;
                }

                ItemPropertyData props = properties.get(item.getSku());
                // Use request-supplied categoryCode if provided, otherwise from lookup
                String categoryCode = item.getCategoryCode() != null
                        ? item.getCategoryCode()
                        : (props != null ? props.getCategoryCode() : null);

                BigDecimal lineAmount = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                EnrichedItem enriched = EnrichedItem.builder()
                        .sku(item.getSku())
                        .storeCode(request.getStoreId())
                        .categoryCode(categoryCode)
                        .subcategoryCode(props != null ? props.getSubcategoryCode() : null)
                        .departmentCode(props != null ? props.getDepartmentCode() : null)
                        .foodItem(props != null && props.isFoodItem())
                        .unitPrice(unitPrice)
                        .lineAmount(lineAmount)
                        .quantity(item.getQuantity())
                        .build();
                enrichedItems.add(enriched);
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
            private java.math.BigDecimal unitPrice;   // null = look up; non-null = use as-is
            private String categoryCode;               // null = look up; non-null = use as-is

            public Item() {}
            public Item(String sku, int quantity) { this.sku = sku; this.quantity = quantity; }
            public Item(String sku, int quantity, java.math.BigDecimal unitPrice, String categoryCode) {
                this.sku = sku; this.quantity = quantity;
                this.unitPrice = unitPrice; this.categoryCode = categoryCode;
            }
            public String getSku() { return sku; }
            public int getQuantity() { return quantity; }
            public java.math.BigDecimal getUnitPrice() { return unitPrice; }
            public String getCategoryCode() { return categoryCode; }
        }
    }
}
