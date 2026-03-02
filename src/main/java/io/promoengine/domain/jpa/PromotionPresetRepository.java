package io.promoengine.domain.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionPresetRepository extends JpaRepository<PromotionPresetEntity, String> {

    List<PromotionPresetEntity> findByTenantIdAndStatus(String tenantId, String status);

    List<PromotionPresetEntity> findByTenantId(String tenantId);
}
