package io.promoengine.enrichment.customer;

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
@Document(indexName = "#{@promoEngineProperties.data.customerIndex}")
public class CustomerDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String customerId;

    @Field(type = FieldType.Keyword)
    private String customerGroupCode;

    @Field(type = FieldType.Keyword)
    private String customerTypeCode;
}
