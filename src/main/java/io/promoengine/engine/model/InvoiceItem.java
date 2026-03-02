package io.promoengine.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItem {

    private String sku;
    private String storeCode;
    private String categoryCode;
    private String subcategoryCode;
    private String departmentCode;
    private boolean foodItem;
    private BigDecimal unitPrice;
    private BigDecimal lineAmount;  // unitPrice × quantity
    private int quantity;
}
