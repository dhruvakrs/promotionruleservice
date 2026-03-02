package io.promoengine.domain.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "promotion_rule_history")
public class PromotionRuleHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "rule_id")
    private String ruleId;

    @Column(name = "version")
    private int version;

    @Lob
    @Column(name = "rule_body")
    private String ruleBody;

    @Column(name = "status")
    private String status;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "change_reason")
    private String changeReason;
}
