package io.promoengine.enrichment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedItem {
    private String sku;
    private String storeCode;
    private String categoryCode;
    private String subcategoryCode;
    private String departmentCode;
    private boolean foodItem;
    private BigDecimal unitPrice;
    private BigDecimal lineAmount;
    private int quantity;
}
