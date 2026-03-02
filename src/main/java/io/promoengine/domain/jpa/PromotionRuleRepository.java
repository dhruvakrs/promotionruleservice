package io.promoengine.domain.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRuleRepository extends JpaRepository<PromotionRuleEntity, String> {

    List<PromotionRuleEntity> findByTenantIdAndStatus(String tenantId, String status);

    Page<PromotionRuleEntity> findByTenantId(String tenantId, Pageable pageable);

    Page<PromotionRuleEntity> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
}
