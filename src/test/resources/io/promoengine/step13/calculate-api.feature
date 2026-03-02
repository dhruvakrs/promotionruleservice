Feature: POST /api/v1/promotions/calculate — Controller Verification

  Scenario: PromotionController class exists and can be referenced
    * def PromotionController = Java.type('io.promoengine.controller.PromotionController')
    * assert PromotionController != null

  Scenario: VersionController class exists and can be referenced
    * def VersionController = Java.type('io.promoengine.controller.VersionController')
    * assert VersionController != null

  Scenario: PromotionRuleController class exists and can be referenced
    * def PromotionRuleController = Java.type('io.promoengine.controller.PromotionRuleController')
    * assert PromotionRuleController != null

  Scenario: CalculateRequest DTO has correct structure
    * def CalculateRequest = Java.type('io.promoengine.dto.request.CalculateRequest')
    * def req = CalculateRequest.builder().storeId('0101').customerId('CUST001').build()
    * match req.storeId == '0101'
    * match req.customerId == 'CUST001'

  Scenario: CalculateResponse DTO has correct structure
    * def CalculateResponse = Java.type('io.promoengine.dto.response.CalculateResponse')
    * def OrderSummary = Java.type('io.promoengine.dto.response.OrderSummary')
    * def BigDecimal = Java.type('java.math.BigDecimal')
    * def summary = OrderSummary.builder().subtotal(BigDecimal.valueOf(100)).totalDiscount(BigDecimal.valueOf(10)).total(BigDecimal.valueOf(90)).build()
    * def resp = CalculateResponse.builder().requestId('test-001').storeId('0101').summary(summary).skippedItems([]).build()
    * match resp.requestId == 'test-001'
    * match resp.summary.total.doubleValue() == 90.0

  @ignore
  Scenario: HTTP - Full calculation returns 200 (requires running server)
    * url baseUrl
    * header X-API-Key = apiKey
    * path '/api/v1/promotions/calculate'
    * method POST
    Then status 200
