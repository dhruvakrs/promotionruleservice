package io.promoengine.domain.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "promotion_preset")
public class PromotionPresetEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "mmed_group")
    private Integer mmedGroup;

    @Lob
    @Column(name = "rule_body")
    private String ruleBody;

    @Column(name = "status")
    private String status = "ACTIVE";

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
