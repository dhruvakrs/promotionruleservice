package io.promoengine.engine;

import io.promoengine.engine.model.PromotionResult;
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
public class LineCalculationResult {

    private String sku;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private BigDecimal standardDiscount;
    private BigDecimal couponDiscount;
    private BigDecimal finalAmount;
    private List<PromotionResult> appliedPromotions;
}
