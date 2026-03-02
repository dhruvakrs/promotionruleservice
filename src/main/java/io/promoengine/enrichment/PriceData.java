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
public class PriceData {
    private String sku;
    private String storeCode;
    private BigDecimal unitPrice;
    private String categoryCode;
    private String subcategoryCode;
    private String departmentCode;
    private boolean foodItem;
}
