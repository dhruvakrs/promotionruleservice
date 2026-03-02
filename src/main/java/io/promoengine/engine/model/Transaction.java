package io.promoengine.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private String storeId;
    private String customerId;
    private LocalDate transactionDate;
}
