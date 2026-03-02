package io.promoengine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateResponse {
    private String requestId;
    private String storeId;
    private LocalDateTime calculatedAt;
    private OrderSummary summary;
    private List<CalculatedItem> items;
    private List<PromotionSummary> promotions;
    private List<String> skippedItems;
}
