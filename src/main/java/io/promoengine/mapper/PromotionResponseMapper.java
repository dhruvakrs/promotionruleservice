package io.promoengine.mapper;

import io.promoengine.dto.request.CalculateRequest;
import io.promoengine.dto.response.AppliedPromotion;
import io.promoengine.dto.response.CalculateResponse;
import io.promoengine.dto.response.CalculatedItem;
import io.promoengine.dto.response.OrderSummary;
import io.promoengine.dto.response.PromotionSummary;
import io.promoengine.engine.CalculationResult;
import io.promoengine.engine.model.PromotionResult;
import io.promoengine.enrichment.EnrichedItem;
import io.promoengine.enrichment.EnrichedTransaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromotionResponseMapper {

    private static final int SCALE = 3;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final int COUPON_TYPE_THRESHOLD = 10;

    public CalculateResponse toResponse(CalculationResult result, CalculateRequest request, EnrichedTransaction tx) {
        List<PromotionResult> promotionResults = result.getPromotionResults();

        BigDecimal subtotal = tx.getEnrichedItems().stream()
                .map(EnrichedItem::getLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, ROUNDING);

        BigDecimal totalDiscount = promotionResults.stream()
                .map(PromotionResult::getDiscountAmount)
                .filter(d -> d != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, ROUNDING);

        BigDecimal total = subtotal.subtract(totalDiscount).setScale(SCALE, ROUNDING);

        List<CalculatedItem> items = tx.getEnrichedItems().stream()
                .map(item -> toCalculatedItem(item, promotionResults))
                .collect(Collectors.toList());

        List<PromotionSummary> promotions = promotionResults.stream()
                .map(this::toPromotionSummary)
                .collect(Collectors.toList());

        OrderSummary summary = OrderSummary.builder()
                .subtotal(subtotal)
                .totalDiscount(totalDiscount)
                .total(total)
                .build();

        return CalculateResponse.builder()
                .requestId(request.getRequestId())
                .storeId(request.getStoreId())
                .calculatedAt(LocalDateTime.now())
                .summary(summary)
                .items(items)
                .promotions(promotions)
                .skippedItems(tx.getSkippedItems())
                .build();
    }

    private CalculatedItem toCalculatedItem(EnrichedItem item, List<PromotionResult> promotions) {
        BigDecimal unitPrice = item.getUnitPrice().setScale(SCALE, ROUNDING);
        BigDecimal subtotal = item.getLineAmount().setScale(SCALE, ROUNDING);

        BigDecimal standardDiscount = BigDecimal.ZERO;
        BigDecimal couponDiscount = BigDecimal.ZERO;
        List<AppliedPromotion> applied = new ArrayList<>();

        for (PromotionResult promo : promotions) {
            if (promo.getLineDiscountAmount() != null) {
                BigDecimal lineDiscount = promo.getLineDiscountAmount().setScale(SCALE, ROUNDING);
                if (promo.getPromotionType() < COUPON_TYPE_THRESHOLD) {
                    standardDiscount = standardDiscount.add(lineDiscount);
                } else {
                    couponDiscount = couponDiscount.add(lineDiscount);
                }
                applied.add(AppliedPromotion.builder()
                        .promotionId(promo.getPromotionId())
                        .type(promo.getPromotionType() < COUPON_TYPE_THRESHOLD ? "STANDARD" : "COUPON")
                        .discountAmount(lineDiscount)
                        .build());
            }
        }

        BigDecimal finalAmount = subtotal.subtract(standardDiscount).subtract(couponDiscount).setScale(SCALE, ROUNDING);

        return CalculatedItem.builder()
                .sku(item.getSku())
                .quantity(item.getQuantity())
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .standardDiscount(standardDiscount.setScale(SCALE, ROUNDING))
                .couponDiscount(couponDiscount.setScale(SCALE, ROUNDING))
                .finalAmount(finalAmount)
                .appliedPromotions(applied)
                .build();
    }

    private PromotionSummary toPromotionSummary(PromotionResult result) {
        return PromotionSummary.builder()
                .promotionId(result.getPromotionId())
                .type(result.getPromotionType() < COUPON_TYPE_THRESHOLD ? "STANDARD" : "COUPON")
                .description(result.getDescription())
                .timesApplied(result.getTimesApplied())
                .discountAmount(result.getDiscountAmount() != null
                        ? result.getDiscountAmount().setScale(SCALE, ROUNDING)
                        : BigDecimal.ZERO.setScale(SCALE, ROUNDING))
                .prerequisites(result.getPrerequisites() != null ? result.getPrerequisites() : Collections.emptyList())
                .build();
    }
}
