Feature: EnrichmentService

  Scenario: All prices resolved — skippedItems is empty
    * def service = karate.call('classpath:helpers/enrichment-service-mock.js').withPrices({ '12345': 49.99, '67890': 29.99 }).withAllItems().build()
    * def req = { storeId: '0101', customerId: 'CUST001', items: [{sku: '12345', quantity: 2}, {sku: '67890', quantity: 1}] }
    * def tx = service.enrich(req)
    * assert karate.sizeOf(tx.skippedItems) == 0
    * assert karate.sizeOf(tx.enrichedItems) == 2

  Scenario: Item with no price goes to skippedItems
    * def service = karate.call('classpath:helpers/enrichment-service-mock.js').withPrices({ '12345': 49.99 }).withNoPriceFor('UNKNOWN01').withAllItems().build()
    * def req = { storeId: '0101', customerId: 'CUST001', items: [{sku: '12345', quantity: 1}, {sku: 'UNKNOWN01', quantity: 1}] }
    * def tx = service.enrich(req)
    * def skipped = tx.skippedItems
    * assert karate.sizeOf(skipped) == 1
    * assert karate.sizeOf(tx.enrichedItems) == 1
    * match tx.enrichedItems[0].sku == '12345'

  Scenario: Sentinel store 999999 used for price fallback
    * def service = karate.call('classpath:helpers/enrichment-service-mock.js').withSentinelPrice('12345', 39.99).withAllItems().build()
    * def req = { storeId: '9999', customerId: 'CUST001', items: [{sku: '12345', quantity: 1}] }
    * def tx = service.enrich(req)
    * assert karate.sizeOf(tx.enrichedItems) == 1
    * match tx.enrichedItems[0].unitPrice.doubleValue() == 39.99
    * assert karate.sizeOf(tx.skippedItems) == 0

  Scenario: EnrichedItem lineAmount equals unitPrice times quantity
    * def service = karate.call('classpath:helpers/enrichment-service-mock.js').withPrices({ '12345': 50.00 }).withAllItems().build()
    * def req = { storeId: '0101', customerId: 'CUST001', items: [{sku: '12345', quantity: 3}] }
    * def tx = service.enrich(req)
    * match tx.enrichedItems[0].lineAmount.doubleValue() == 150.00
