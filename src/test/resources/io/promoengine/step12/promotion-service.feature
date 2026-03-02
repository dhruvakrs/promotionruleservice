Feature: PromotionService orchestration

  Background:
    * def service = karate.call('classpath:helpers/promotion-service-mock.js').build()

  Scenario: All items calculated returns 200 with no skippedItems
    * def req = { storeId: '0101', customerId: 'CUST001', items: [{sku: '12345', quantity: 2}] }
    * def result = service.calculate(req, 'default')
    * match result.statusCodeValue == 200
    * assert karate.sizeOf(result.body.skippedItems) == 0

  Scenario: Some items skipped returns 206 Partial Content
    * def req = { storeId: '0101', customerId: 'CUST001', items: [{sku: '12345', quantity: 1}, {sku: 'UNKNOWN', quantity: 1}] }
    * def result = service.calculate(req, 'default')
    * match result.statusCodeValue == 206
    * def skipped = result.body.skippedItems
    * assert karate.sizeOf(skipped) == 1

  Scenario: Enrichment failure throws EnrichmentException
    * def badService = karate.call('classpath:helpers/promotion-service-mock.js').withEnrichmentFailure().build()
    * def exceptionThrown = false
    * eval try { badService.calculate({ storeId: '0101', customerId: 'CUST001', items: [{sku: '12345', quantity: 1}] }, 'default'); } catch(e) { exceptionThrown = true; }
    * assert exceptionThrown == true

  Scenario: Unknown tenant throws TenantNotFoundException
    * def exceptionThrown = false
    * eval try { service.calculate({ storeId: '0101', customerId: 'CUST001', items: [{sku: '12345', quantity: 1}] }, 'unknown-tenant'); } catch(e) { exceptionThrown = true; }
    * assert exceptionThrown == true
