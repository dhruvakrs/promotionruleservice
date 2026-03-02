Feature: GlobalExceptionHandler — Error response shapes

  Background:
    * def GlobalExceptionHandler = Java.type('io.promoengine.exception.GlobalExceptionHandler')
    * def RuleCompilationException = Java.type('io.promoengine.exception.RuleCompilationException')
    * def TenantNotFoundException = Java.type('io.promoengine.exception.TenantNotFoundException')
    * def EnrichmentException = Java.type('io.promoengine.exception.EnrichmentException')
    * def RuleSetException = Java.type('io.promoengine.exception.RuleSetException')
    * def handler = new GlobalExceptionHandler()

  Scenario: RuleCompilationException returns 422 with errorCode 14
    * def ex = new RuleCompilationException('tenant-a', 'syntax error on line 3')
    * def result = handler.handleRuleCompilation(ex)
    * match result.statusCode.value() == 422
    * match result.body.errorCode == 14
    * assert result.body.message != null

  Scenario: TenantNotFoundException returns 401 with errorCode 50
    * def ex = new TenantNotFoundException('bad-key')
    * def result = handler.handleTenantNotFound(ex)
    * match result.statusCode.value() == 401
    * match result.body.errorCode == 50

  Scenario: EnrichmentException returns 500 with errorCode 99
    * def cause = new java.lang.RuntimeException('ES timeout')
    * def ex = new EnrichmentException('Price lookup failed', cause)
    * def result = handler.handleEnrichment(ex)
    * match result.statusCode.value() == 500
    * match result.body.errorCode == 99

  Scenario: RuleSetException with errorCode 14 returns 422
    * def ex = new RuleSetException(14, 'DRL compilation failed')
    * def result = handler.handleRuleSet(ex)
    * match result.statusCode.value() == 422
    * match result.body.errorCode == 14

  Scenario: ErrorResponse never exposes stack trace
    * def ex = new RuleCompilationException('tenant-a', 'error')
    * def result = handler.handleRuleCompilation(ex)
    * def body = karate.toJson(result.body)
    * def bodyStr = karate.toString(body)
    * assert bodyStr.indexOf('at io.promoengine') == -1
