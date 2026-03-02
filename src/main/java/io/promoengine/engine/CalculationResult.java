package io.promoengine.engine;

import io.promoengine.engine.model.PromotionResult;
import lombok.Getter;

import java.util.List;

@Getter
public class CalculationResult {

    private final List<PromotionResult> promotionResults;
    private final List<String> skippedItems;

    public CalculationResult(List<PromotionResult> promotionResults, List<String> skippedItems) {
        this.promotionResults = promotionResults;
        this.skippedItems = skippedItems;
    }
}
