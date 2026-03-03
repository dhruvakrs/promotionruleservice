package io.promoengine.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Calculated result for a single item")
public class CalculatedItem {
    @Schema(description = "Product SKU", example = "12345678")
    private String sku;
    @Schema(description = "Purchased quantity", example = "2")
    private int quantity;
    @Schema(description = "Product category code resolved from request or enrichment", example = "ELEC")
    private String categoryCode;
    @Schema(description = "Unit price used in calculation (3 decimal places)", example = "49.990")
    private BigDecimal unitPrice;
    @Schema(description = "Line subtotal = unitPrice × quantity", example = "99.980")
    private BigDecimal subtotal;
    @Schema(description = "Total standard promotion discount applied to this line", example = "10.000")
    private BigDecimal standardDiscount;
    @Schema(description = "Total coupon discount applied to this line", example = "0.000")
    private BigDecimal couponDiscount;
    @Schema(description = "Final payable amount = subtotal − standardDiscount − couponDiscount", example = "89.980")
    private BigDecimal finalAmount;
    @Schema(description = "Promotions applied to this line")
    private List<AppliedPromotion> appliedPromotions;
}
