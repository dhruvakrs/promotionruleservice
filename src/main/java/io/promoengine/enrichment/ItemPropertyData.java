package io.promoengine.enrichment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPropertyData {
    private String sku;
    private String categoryCode;
    private String subcategoryCode;
    private String departmentCode;
    private boolean foodItem;
}
