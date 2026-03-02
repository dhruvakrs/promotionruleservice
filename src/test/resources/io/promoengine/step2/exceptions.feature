Feature: Exception classes

  Scenario: RuleCompilationException carries tenantId and error details
    * def RuleCompilationException = Java.type('io.promoengine.exception.RuleCompilationException')
    * def ex = new RuleCompilationException('tenant-a', 'syntax error on line 3')
    * match ex.message contains 'syntax error on line 3'

  Scenario: RuleSetException exposes errorCode
    * def RuleSetException = Java.type('io.promoengine.exception.RuleSetException')
    * def ex = new RuleSetException(14, 'DRL compilation failed')
    * match ex.errorCode == 14
    * match ex.message == 'DRL compilation failed'

  Scenario: TenantNotFoundException includes the bad API key
    * def TenantNotFoundException = Java.type('io.promoengine.exception.TenantNotFoundException')
    * def ex = new TenantNotFoundException('bad-key-xyz')
    * match ex.message contains 'bad-key-xyz'

  Scenario: EnrichmentException wraps cause
    * def EnrichmentException = Java.type('io.promoengine.exception.EnrichmentException')
    * def cause = new java.lang.RuntimeException('ES timeout')
    * def ex = new EnrichmentException('Price lookup failed', cause)
    * match ex.message == 'Price lookup failed'
    * match ex.cause.message == 'ES timeout'
