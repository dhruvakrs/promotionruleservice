Feature: Enrichment repository fallback chain

  Scenario: Price found for sku + storeCode in primary index
    * def repo = karate.call('classpath:helpers/mock-price-repo.js').withPrice('12345', '0101', 49.99)
    * def result = repo.findPrice('12345', '0101')
    * assert result.isPresent() == true
    * match result.get().unitPrice.doubleValue() == 49.99

  Scenario: Price falls back to sentinel store 999999 when store-specific price missing
    * def repo = karate.call('classpath:helpers/mock-price-repo.js').withSentinelPrice('12345', 39.99)
    * def result = repo.findPrice('12345', '9999')
    * assert result.isPresent() == true
    * match result.get().unitPrice.doubleValue() == 39.99

  Scenario: Price returns empty when not found
    * def repo = karate.call('classpath:helpers/mock-price-repo.js').withNoPrice('UNKNOWN01')
    * def result = repo.findPrice('UNKNOWN01', '0101')
    * assert result.isPresent() == false

  Scenario: Item properties resolved from promoitem index
    * def repo = karate.call('classpath:helpers/mock-item-repo.js').withItem('12345', 'ELEC', 'TV', 'D01', false)
    * def result = repo.findProperties('12345')
    * assert result.isPresent() == true
    * match result.get().categoryCode == 'ELEC'
    * match result.get().subcategoryCode == 'TV'
    * match result.get().foodItem == false

  Scenario: Customer data resolved from promocustomer index
    * def repo = karate.call('classpath:helpers/mock-customer-repo.js').withCustomer('CUST001', 'VIP', 'B2C')
    * def result = repo.findCustomer('CUST001')
    * assert result.isPresent() == true
    * match result.get().customerGroupCode == 'VIP'
