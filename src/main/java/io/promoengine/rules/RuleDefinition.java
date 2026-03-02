package io.promoengine.rules;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleDefinition {
    private String promotionId;
    private String ruleBody;
    private int priority;
    private String description;
}
