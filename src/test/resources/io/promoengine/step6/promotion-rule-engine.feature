Feature: PromotionRuleEngine — Drools calculation

  Background:
    * def PromotionRuleEngine = Java.type('io.promoengine.engine.PromotionRuleEngine')
    * def RuleDefinition = Java.type('io.promoengine.rules.RuleDefinition')
    * def EnrichedTransaction = Java.type('io.promoengine.enrichment.EnrichedTransaction')
    * def EnrichedItem = Java.type('io.promoengine.enrichment.EnrichedItem')
    * def BigDecimal = Java.type('java.math.BigDecimal')
    * def validDrl = new java.lang.String(read('test-rule.drl'))
    * def enrichedItem = EnrichedItem.builder().sku('99999').categoryCode('ELEC').unitPrice(BigDecimal.valueOf(100)).lineAmount(BigDecimal.valueOf(100)).quantity(1).storeCode('0101').build()
    * def tx = EnrichedTransaction.builder().storeId('0101').customerId('CUST001').transactionDate(java.time.LocalDate.now()).enrichedItems([enrichedItem]).skippedItems([]).build()

  Scenario: Engine compiles valid DRL and returns PromotionResult
    * def engine = new PromotionRuleEngine('test-tenant')
    * def rule = RuleDefinition.builder().promotionId('ELEC10PCT').ruleBody(validDrl).priority(10).build()
    * engine.reload([rule])
    * def result = engine.calculate(tx)
    * assert karate.sizeOf(result.promotionResults) == 1
    * match result.promotionResults[0].promotionId == 'ELEC10PCT'
    * match result.promotionResults[0].discountAmount.doubleValue() == 10.00
    * assert karate.sizeOf(result.skippedItems) == 0

  Scenario: Engine rejects invalid DRL with RuleCompilationException
    * def engine = new PromotionRuleEngine('test-tenant')
    * def badRule = RuleDefinition.builder().promotionId('BAD').ruleBody('this is not valid DRL at all').priority(10).build()
    * def exceptionThrown = false
    * eval try { engine.reload([badRule]); } catch(e) { exceptionThrown = true; }
    * assert exceptionThrown == true

  Scenario: EngineFactBuilder produces correct facts
    * def EngineFactBuilder = Java.type('io.promoengine.engine.EngineFactBuilder')
    * def facts = EngineFactBuilder.build(tx)
    * assert karate.sizeOf(facts) == 2
    * def txFact = facts[0]
    * assert txFact.class.simpleName == 'Transaction'
    * def storeIdVal = txFact.storeId
    * match storeIdVal == '0101'
