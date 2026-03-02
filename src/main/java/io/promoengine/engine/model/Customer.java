package io.promoengine.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    private String customerId;
    private String customerGroupCode;
    private String customerTypeCode;
}
