package io.promoengine.enrichment.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "#{@promoEngineProperties.data.itemIndex}")
public class ItemDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String sku;

    @Field(type = FieldType.Keyword)
    private String categoryCode;

    @Field(type = FieldType.Keyword)
    private String subcategoryCode;

    @Field(type = FieldType.Keyword)
    private String departmentCode;

    @Field(type = FieldType.Boolean)
    private boolean foodItem;
}
