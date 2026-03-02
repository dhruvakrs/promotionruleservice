package io.promoengine.dto.request;

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
public class CalculateRequest {

    private String requestId;

    @NotBlank(message = "storeId is required")
    private String storeId;

    @NotNull(message = "customerId is required")
    private String customerId;

    @NotNull(message = "transactionDate is required")
    private LocalDate transactionDate;

    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<OrderItem> items;
}
