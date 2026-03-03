package io.promoengine.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Promotion calculation request")
public class CalculateRequest {

    @Schema(description = "Optional client-generated request ID for correlation", example = "req-20260303-001")
    private String requestId;

    @NotBlank(message = "storeId is required")
    @Schema(description = "Store code", example = "0101", requiredMode = Schema.RequiredMode.REQUIRED)
    private String storeId;

    @NotNull(message = "customerId is required")
    @Schema(description = "Customer ID", example = "CUST001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerId;

    @NotNull(message = "transactionDate is required")
    @Schema(description = "Transaction date", example = "2026-03-03", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate transactionDate;

    @NotEmpty(message = "items must not be empty")
    @Valid
    @Schema(description = "Items in the transaction", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OrderItem> items;
}
