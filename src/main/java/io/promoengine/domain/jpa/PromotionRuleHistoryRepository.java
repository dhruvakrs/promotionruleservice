package io.promoengine.domain.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRuleHistoryRepository extends JpaRepository<PromotionRuleHistoryEntity, Long> {

    List<PromotionRuleHistoryEntity> findByRuleIdOrderByChangedAtDesc(String ruleId);

    List<PromotionRuleHistoryEntity> findByRuleIdOrderByChangedAtAsc(String ruleId);

    java.util.Optional<PromotionRuleHistoryEntity> findByRuleIdAndVersion(String ruleId, int version);

    List<PromotionRuleHistoryEntity> findByRuleIdAndVersionOrderByChangedAtDesc(String ruleId, int version);
}
