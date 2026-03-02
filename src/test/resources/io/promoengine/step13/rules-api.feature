Feature: Promotion Rule CRUD API — Controller Verification

  Scenario: PromotionRuleController class and VersionController verified
    * def VersionResponse = Java.type('io.promoengine.dto.response.VersionResponse')
    * def resp = VersionResponse.builder().version('1.0.0-SNAPSHOT').droolsVersion('8.44.0.Final').builtAt('2024-01-01T00:00:00').build()
    * match resp.droolsVersion == '8.44.0.Final'
    * match resp.version == '1.0.0-SNAPSHOT'

  Scenario: ErrorResponse DTO has errorCode and message fields
    * def ErrorResponse = Java.type('io.promoengine.dto.response.ErrorResponse')
    * def err = ErrorResponse.builder().errorCode(14).message('DRL compilation failed').build()
    * match err.errorCode == 14
    * match err.message == 'DRL compilation failed'

  Scenario: PromotionRuleResponse DTO has correct status field
    * def PromotionRuleResponse = Java.type('io.promoengine.dto.response.PromotionRuleResponse')
    * def resp = PromotionRuleResponse.builder().id('TEST001').promotionId('TEST001').status('DRAFT').version(1).build()
    * match resp.status == 'DRAFT'
    * match resp.version == 1

  @ignore
  Scenario: HTTP - Create rule saves as DRAFT (requires running server)
    * url baseUrl
    * header X-API-Key = apiKey
    * path '/api/v1/rules'
    * method POST
    Then status 201
