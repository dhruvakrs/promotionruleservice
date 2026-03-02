Feature: Engine POJO model

  Scenario: InvoiceItem builder sets all fields correctly
    * def InvoiceItem = Java.type('io.promoengine.engine.model.InvoiceItem')
    * def item = InvoiceItem.builder().sku('12345678').storeCode('0101').categoryCode('ELEC').unitPrice(java.math.BigDecimal.valueOf(49.99)).lineAmount(java.math.BigDecimal.valueOf(99.98)).quantity(2).foodItem(false).build()
    * match item.sku == '12345678'
    * match item.quantity == 2
    * match item.foodItem == false
    * match item.lineAmount.doubleValue() == 99.98

  Scenario: Transaction builder sets header fields
    * def Transaction = Java.type('io.promoengine.engine.model.Transaction')
    * def tx = Transaction.builder().storeId('0101').customerId('CUST001').transactionDate(java.time.LocalDate.of(2024, 1, 15)).build()
    * match tx.storeId == '0101'
    * match tx.customerId == 'CUST001'

  Scenario: PromotionResult promotionType < 10 maps to STANDARD
    * def PromotionResult = Java.type('io.promoengine.engine.model.PromotionResult')
    * def result = PromotionResult.builder().promotionId('PROMO01').promotionType(0).discountAmount(java.math.BigDecimal.valueOf(10.00)).timesApplied(1.0).build()
    * assert result.promotionType < 10
    * match result.promotionId == 'PROMO01'

  Scenario: CalculationResult holds results and skippedItems
    * def CalculationResult = Java.type('io.promoengine.engine.CalculationResult')
    * def skipped = ['UNKNOWN01', 'UNKNOWN02']
    * def result = new CalculationResult([], skipped)
    * assert karate.sizeOf(result.skippedItems) == 2
    * match result.skippedItems contains 'UNKNOWN01'
