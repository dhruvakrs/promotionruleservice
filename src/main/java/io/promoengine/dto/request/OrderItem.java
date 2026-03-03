package io.promoengine.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @NotBlank(message = "SKU is required")
    private String sku;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    /**
     * Optional unit price. When provided, skips DB/OpenSearch price lookup for this item.
     * When omitted (null), the engine looks up the price from OpenSearch → DB fallback.
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "unitPrice must be greater than 0")
    private BigDecimal unitPrice;

    /**
     * Optional category code. When provided alongside unitPrice, item properties lookup
     * is also skipped and this value is used directly for rule matching.
     */
    private String categoryCode;
}
