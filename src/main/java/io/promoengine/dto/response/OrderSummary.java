package io.promoengine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummary {
    private BigDecimal subtotal;
    private BigDecimal totalDiscount;
    private BigDecimal total;
}
