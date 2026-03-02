package io.promoengine.enrichment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerData {
    private String customerId;
    private String customerGroupCode;
    private String customerTypeCode;
}
