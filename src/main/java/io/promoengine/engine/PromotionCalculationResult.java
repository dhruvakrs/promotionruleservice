package io.promoengine.engine;

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
public class PromotionCalculationResult {

    private String promotionId;
    private String type;            // STANDARD or COUPON
    private String description;
    private double timesApplied;
    private BigDecimal discountAmount;
    private List<String> prerequisites;
}
