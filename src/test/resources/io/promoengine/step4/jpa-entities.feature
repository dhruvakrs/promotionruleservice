Feature: JPA entity defaults and structure

  Scenario: PromotionRuleEntity defaults status to DRAFT
    * def entity = new (Java.type('io.promoengine.domain.jpa.PromotionRuleEntity'))()
    * entity.setPromotionId('TEST001')
    * entity.setTenantId('default')
    * entity.setRuleBody('rule "test" when then end')
    * match entity.getStatus() == 'DRAFT'
    * match entity.getVersion() == 1

  Scenario: PromotionRuleEntity ruleBody stores DRL text
    * def entity = new (Java.type('io.promoengine.domain.jpa.PromotionRuleEntity'))()
    * def drl = 'rule "10% Off" when then end'
    * entity.setRuleBody(drl)
    * match entity.getRuleBody() == drl

  Scenario: PromotionRuleHistoryEntity captures version
    * def entity = new (Java.type('io.promoengine.domain.jpa.PromotionRuleHistoryEntity'))()
    * entity.setRuleId('TEST001')
    * entity.setVersion(3)
    * entity.setChangeReason('Manual deactivation')
    * match entity.getVersion() == 3
    * match entity.getChangeReason() == 'Manual deactivation'
