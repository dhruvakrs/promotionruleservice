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
@Table(name = "promotion_rule")
public class PromotionRuleEntity {

    @Id
    @Column(name = "id")
    private String promotionId;

    @Column(name = "definition_id")
    private String definitionId;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "rule_type")
    private String ruleType;

    @Column(name = "mmed_group")
    private Integer mmedGroup;

    @Column(name = "priority")
    private int priority = 10;

    @Lob
    @Column(name = "rule_body")
    private String ruleBody;

    @Column(name = "description")
    private String description;

    @Lob
    @Column(name = "description_i18n")
    private String descriptionI18n;

    @Column(name = "start_date")
    private java.time.LocalDate startDate;

    @Column(name = "end_date")
    private java.time.LocalDate endDate;

    @Column(name = "status")
    private String status = "DRAFT";

    @Column(name = "version")
    private int version = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;
}
