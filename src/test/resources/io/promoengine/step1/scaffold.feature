Feature: Application scaffold

  Scenario: Application main class exists
    * def PromoEngineApplication = Java.type('io.promoengine.PromoEngineApplication')
    * assert PromoEngineApplication != null

  Scenario: PromoEngineProperties has correct defaults
    * def PromoEngineProperties = Java.type('io.promoengine.PromoEngineProperties')
    * def props = new PromoEngineProperties()
    * match props.engine.debugLevel == 0
    * match props.data.priceIndex == 'promoprice'
    * match props.security.headerName == 'X-API-Key'

  @ignore
  Scenario: Actuator health endpoint responds (requires running server)
    Given url baseUrl
    When path '/actuator/health'
    And method GET
    Then status 200
    And match response.status == 'UP'

  @ignore
  Scenario: Swagger UI is accessible (requires running server)
    Given url baseUrl
    When path '/swagger-ui.html'
    And method GET
    Then status 200
