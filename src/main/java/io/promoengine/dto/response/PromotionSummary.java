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
public class PromotionSummary {
    private String promotionId;
    private String type;
    private String description;
    private double timesApplied;
    private BigDecimal discountAmount;
    private List<String> prerequisites;
}
