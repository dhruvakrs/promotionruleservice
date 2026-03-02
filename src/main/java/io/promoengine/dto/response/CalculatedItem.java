package io.promoengine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculatedItem {
    private String sku;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private BigDecimal standardDiscount;
    private BigDecimal couponDiscount;
    private BigDecimal finalAmount;
    private List<AppliedPromotion> appliedPromotions;
}
