Feature: OpenSearch document models

  Scenario: PriceDocument builder sets all enrichment fields
    * def PriceDocument = Java.type('io.promoengine.enrichment.price.PriceDocument')
    * def doc = PriceDocument.builder().sku('12345678').storeCode('0101').unitPrice(java.math.BigDecimal.valueOf(49.99)).categoryCode('ELEC').foodItem(false).build()
    * match doc.sku == '12345678'
    * match doc.storeCode == '0101'
    * match doc.unitPrice.doubleValue() == 49.99
    * match doc.foodItem == false

  Scenario: ItemDocument captures product classification fields
    * def ItemDocument = Java.type('io.promoengine.enrichment.item.ItemDocument')
    * def doc = ItemDocument.builder().sku('12345678').categoryCode('ELEC').subcategoryCode('TV').departmentCode('D01').foodItem(false).build()
    * match doc.categoryCode == 'ELEC'
    * match doc.subcategoryCode == 'TV'

  Scenario: CustomerDocument captures segment fields
    * def CustomerDocument = Java.type('io.promoengine.enrichment.customer.CustomerDocument')
    * def doc = CustomerDocument.builder().customerId('CUST001').customerGroupCode('VIP').customerTypeCode('B2C').build()
    * match doc.customerGroupCode == 'VIP'
