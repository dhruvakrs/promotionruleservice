package io.promoengine.enrichment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedTransaction {
    private String storeId;
    private String customerId;
    private LocalDate transactionDate;
    private List<EnrichedItem> enrichedItems;
    private List<String> skippedItems;
    private CustomerData customerData;
}
