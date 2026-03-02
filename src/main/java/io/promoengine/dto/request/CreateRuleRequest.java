package io.promoengine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRuleRequest {

    @NotBlank(message = "promotionId is required")
    private String promotionId;

    private String definitionId;

    @NotBlank(message = "ruleBody is required")
    private String ruleBody;

    private String ruleType;

    private int priority = 10;

    private String description;

    private String descriptionI18n;

    private LocalDate startDate;

    private LocalDate endDate;
}
