Feature: DTO validation

  Background:
    * def Validator = Java.type('io.promoengine.helper.BeanValidationHelper')

  Scenario: Valid CalculateRequest passes validation
    * def req = { storeId: '0101', customerId: '123456', transactionDate: '2024-01-15', items: [{sku: '12345', quantity: 2}] }
    * def violations = Validator.validate(req)
    * assert karate.sizeOf(violations) == 0

  Scenario: Missing storeId fails validation
    * def req = { customerId: '123456', transactionDate: '2024-01-15', items: [{sku: '12345', quantity: 2}] }
    * def violations = Validator.validate(req)
    * assert karate.sizeOf(violations) >= 1
    * def paths = karate.map(violations, function(v){ return v.propertyPath })
    * assert karate.filter(paths, function(p){ return p.contains('storeId') }).length > 0

  Scenario: Empty items list fails validation
    * def req = { storeId: '0101', customerId: '123456', transactionDate: '2024-01-15', items: [] }
    * def violations = Validator.validate(req)
    * assert karate.sizeOf(violations) >= 1

  Scenario: Item quantity zero fails validation
    * def req = { storeId: '0101', customerId: '123456', transactionDate: '2024-01-15', items: [{sku: '12345', quantity: 0}] }
    * def violations = Validator.validate(req)
    * assert karate.sizeOf(violations) >= 1
    * def paths = karate.map(violations, function(v){ return v.propertyPath })
    * assert karate.filter(paths, function(p){ return p.contains('quantity') }).length > 0

  Scenario: CalculateResponse serializes to correct JSON shape
    * def CalculateResponse = Java.type('io.promoengine.dto.response.CalculateResponse')
    * def resp = CalculateResponse.builder().requestId('uuid-001').storeId('0101').skippedItems(['UNKNOWN01']).build()
    * def json = karate.toJson(resp)
    * match json.requestId == 'uuid-001'
    * match json.skippedItems == ['UNKNOWN01']
