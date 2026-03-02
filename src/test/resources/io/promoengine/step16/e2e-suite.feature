Feature: PromoEngine end-to-end component regression suite

  Background:
    * def PromotionRuleEngine = Java.type('io.promoengine.engine.PromotionRuleEngine')
    * def RuleDefinition = Java.type('io.promoengine.rules.RuleDefinition')
    * def EnrichedTransaction = Java.type('io.promoengine.enrichment.EnrichedTransaction')
    * def EnrichedItem = Java.type('io.promoengine.enrichment.EnrichedItem')
    * def BigDecimal = Java.type('java.math.BigDecimal')
    * def validDrl = new java.lang.String(read('classpath:fixtures/valid-rule.drl'))

  Scenario: Full engine lifecycle — load rule, calculate, verify result
    * def engine = new PromotionRuleEngine('e2e-tenant')
    * def rule = RuleDefinition.builder().promotionId('E2E-ELEC-RULE').ruleBody(validDrl).priority(10).build()
    * engine.reload([rule])
    * def item = EnrichedItem.builder().sku('99999').categoryCode('ELEC').unitPrice(BigDecimal.valueOf(100)).lineAmount(BigDecimal.valueOf(100)).quantity(1).storeCode('0101').build()
    * def tx = EnrichedTransaction.builder().storeId('0101').customerId('CUST001').transactionDate(java.time.LocalDate.now()).enrichedItems([item]).skippedItems([]).build()
    * def result = engine.calculate(tx)
    * assert karate.sizeOf(result.promotionResults) == 1
    * match result.promotionResults[0].promotionId == 'VALID-TEST'
    * assert karate.sizeOf(result.skippedItems) == 0

  Scenario: Multi-tenant isolation — separate engines for separate tenants
    * def engineA = new PromotionRuleEngine('tenant-a')
    * def engineB = new PromotionRuleEngine('tenant-b')
    * def ruleA = RuleDefinition.builder().promotionId('RULE-A').ruleBody(validDrl).priority(10).build()
    * def ruleB = RuleDefinition.builder().promotionId('RULE-B').ruleBody(validDrl).priority(5).build()
    * engineA.reload([ruleA])
    * engineB.reload([ruleB])
    * def item = EnrichedItem.builder().sku('12345').categoryCode('ELEC').unitPrice(BigDecimal.valueOf(50)).lineAmount(BigDecimal.valueOf(50)).quantity(1).storeCode('0101').build()
    * def tx = EnrichedTransaction.builder().storeId('0101').customerId('CUST001').transactionDate(java.time.LocalDate.now()).enrichedItems([item]).skippedItems([]).build()
    * def resultA = engineA.calculate(tx)
    * def resultB = engineB.calculate(tx)
    * assert karate.sizeOf(resultA.promotionResults) == 1
    * assert karate.sizeOf(resultB.promotionResults) == 1

  Scenario: Zero-downtime reload — old results still valid after reload
    * def engine = new PromotionRuleEngine('e2e-reload-tenant')
    * def rule1 = RuleDefinition.builder().promotionId('R1').ruleBody(validDrl).priority(10).build()
    * engine.reload([rule1])
    * def item = EnrichedItem.builder().sku('12345').categoryCode('ELEC').unitPrice(BigDecimal.valueOf(50)).lineAmount(BigDecimal.valueOf(50)).quantity(1).storeCode('0101').build()
    * def tx = EnrichedTransaction.builder().storeId('0101').customerId('CUST001').transactionDate(java.time.LocalDate.now()).enrichedItems([item]).skippedItems([]).build()
    * def result1 = engine.calculate(tx)
    * engine.reload([rule1])
    * def result2 = engine.calculate(tx)
    * assert karate.sizeOf(result1.promotionResults) == karate.sizeOf(result2.promotionResults)

  Scenario: Response mapper produces correct total calculation
    * def PromotionResponseMapper = Java.type('io.promoengine.mapper.PromotionResponseMapper')
    * def CalculationResult = Java.type('io.promoengine.engine.CalculationResult')
    * def PromotionResult = Java.type('io.promoengine.engine.model.PromotionResult')
    * def CalculateRequest = Java.type('io.promoengine.dto.request.CalculateRequest')
    * def mapper = new PromotionResponseMapper()
    * def promo = PromotionResult.builder().promotionId('PROMO-E2E').promotionType(0).description('E2E test').discountAmount(BigDecimal.valueOf(20.0)).timesApplied(1.0).build()
    * def item = EnrichedItem.builder().sku('12345').storeCode('0101').unitPrice(BigDecimal.valueOf(100.0)).lineAmount(BigDecimal.valueOf(100.0)).quantity(1).build()
    * def calcResult = new CalculationResult([promo], [])
    * def enrichedTx = EnrichedTransaction.builder().storeId('0101').customerId('CUST001').transactionDate(java.time.LocalDate.now()).enrichedItems([item]).skippedItems([]).build()
    * def req = CalculateRequest.builder().requestId('e2e-001').storeId('0101').customerId('CUST001').build()
    * def response = mapper.toResponse(calcResult, req, enrichedTx)
    * match response.summary.subtotal.doubleValue() == 100.0
    * match response.summary.totalDiscount.doubleValue() == 20.0
    * match response.summary.total.doubleValue() == 80.0
    * match response.promotions[0].type == 'STANDARD'

  Scenario: Version response confirms Drools version
    * def VersionController = Java.type('io.promoengine.controller.VersionController')
    * def controller = new VersionController()
    * def result = controller.getVersion()
    * match result.body.droolsVersion == '8.44.0.Final'
    * match result.body.version == '1.0.0-SNAPSHOT'

  @ignore
  Scenario: HTTP Full E2E - Rule lifecycle (requires running server)
    * url baseUrl
    * header X-API-Key = apiKey
    * path '/api/v1/version'
    * method GET
    Then status 200
    And match response.droolsVersion == '8.44.0.Final'
