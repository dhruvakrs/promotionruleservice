package io.promoengine.engine.model;

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
public class PromotionResult {

    private String promotionId;
    private int promotionType;           // <10 = STANDARD, >=10 = COUPON
    private String description;
    private double timesApplied;
    private BigDecimal discountAmount;
    private BigDecimal lineDiscountAmount;
    private List<String> prerequisites;
}
