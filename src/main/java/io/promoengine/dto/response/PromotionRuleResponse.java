package io.promoengine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRuleResponse {
    private String id;
    private String promotionId;
    private String ruleType;
    private String status;
    private int version;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
