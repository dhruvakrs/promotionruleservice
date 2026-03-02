Feature: API Key security — Component Verification

  Scenario: ApiKeyAuthFilter class exists
    * def ApiKeyAuthFilter = Java.type('io.promoengine.security.ApiKeyAuthFilter')
    * assert ApiKeyAuthFilter != null

  Scenario: TenantResolver resolves valid API key to tenantId
    * def PromoEngineProperties = Java.type('io.promoengine.PromoEngineProperties')
    * def TenantResolver = Java.type('io.promoengine.security.TenantResolver')
    * def props = new PromoEngineProperties()
    * def resolver = new TenantResolver(props)
    * def result = resolver.resolve('change-me')
    * assert result.isPresent() == true
    * match result.get() == 'default'

  Scenario: TenantResolver returns empty for invalid API key
    * def PromoEngineProperties = Java.type('io.promoengine.PromoEngineProperties')
    * def TenantResolver = Java.type('io.promoengine.security.TenantResolver')
    * def props = new PromoEngineProperties()
    * def resolver = new TenantResolver(props)
    * def result = resolver.resolve('wrong-key-xyz')
    * assert result.isPresent() == false

  Scenario: TenantResolver returns empty for null API key
    * def PromoEngineProperties = Java.type('io.promoengine.PromoEngineProperties')
    * def TenantResolver = Java.type('io.promoengine.security.TenantResolver')
    * def props = new PromoEngineProperties()
    * def resolver = new TenantResolver(props)
    * def result = resolver.resolve(null)
    * assert result.isPresent() == false

  Scenario: SecurityConfig class exists
    * def SecurityConfig = Java.type('io.promoengine.config.SecurityConfig')
    * assert SecurityConfig != null

  @ignore
  Scenario: HTTP - Request with valid API key succeeds (requires running server)
    * url baseUrl
    * header X-API-Key = apiKey
    * path '/api/v1/version'
    * method GET
    Then status 200

  @ignore
  Scenario: HTTP - Request with no API key returns 401 (requires running server)
    * url baseUrl
    * path '/api/v1/promotions/calculate'
    * method GET
    Then status 401
