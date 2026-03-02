package io.promoengine.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRuleRequest {
    private String ruleBody;
    private String description;
    private Integer priority;
    private LocalDate startDate;
    private LocalDate endDate;
}
