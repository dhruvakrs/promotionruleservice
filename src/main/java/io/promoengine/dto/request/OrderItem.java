package io.promoengine.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "A single item in the transaction")
public class OrderItem {

    @NotBlank(message = "SKU is required")
    @Schema(description = "Product SKU / article number", example = "12345678", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sku;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Purchased quantity (minimum 1)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private int quantity;

    @DecimalMin(value = "0.0", inclusive = false, message = "unitPrice must be greater than 0")
    @Schema(description = "Optional unit price. When provided, skips DB/OpenSearch price lookup for this item.", example = "49.99")
    private BigDecimal unitPrice;

    @Schema(description = "Optional category code. When provided, skips item-properties lookup and uses this value directly for rule matching.", example = "ELEC")
    private String categoryCode;
}
