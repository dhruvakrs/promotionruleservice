Feature: PromotionResponseMapper

  Background:
    * def PromotionResponseMapper = Java.type('io.promoengine.mapper.PromotionResponseMapper')
    * def mapper = new PromotionResponseMapper()

  Scenario: Monetary values rounded to 3 decimal places
    * def ctx = karate.call('classpath:helpers/mapper-test-builder.js').withDiscount(10.12345678).build()
    * def response = mapper.toResponse(ctx.calculationResult, ctx.request, ctx.enrichedTransaction)
    * match response.summary.totalDiscount.toString() == '10.123'

  Scenario: promotionType 0 maps to STANDARD in response
    * def ctx = karate.call('classpath:helpers/mapper-test-builder.js').withStandardPromotion().build()
    * def response = mapper.toResponse(ctx.calculationResult, ctx.request, ctx.enrichedTransaction)
    * match response.promotions[0].type == 'STANDARD'

  Scenario: promotionType 10 maps to COUPON in response
    * def ctx = karate.call('classpath:helpers/mapper-test-builder.js').withCouponPromotion().build()
    * def response = mapper.toResponse(ctx.calculationResult, ctx.request, ctx.enrichedTransaction)
    * match response.promotions[0].type == 'COUPON'

  Scenario: summary.total equals subtotal minus totalDiscount
    * def ctx = karate.call('classpath:helpers/mapper-test-builder.js').withSubtotal(200.0).withDiscount(30.0).build()
    * def response = mapper.toResponse(ctx.calculationResult, ctx.request, ctx.enrichedTransaction)
    * match response.summary.subtotal.doubleValue() == 200.0
    * match response.summary.totalDiscount.doubleValue() == 30.0
    * match response.summary.total.doubleValue() == 170.0

  Scenario: skippedItems propagated from EnrichedTransaction
    * def ctx = karate.call('classpath:helpers/mapper-test-builder.js').withSkippedItems(['UNKNOWN01', 'UNKNOWN02']).build()
    * def response = mapper.toResponse(ctx.calculationResult, ctx.request, ctx.enrichedTransaction)
    * assert karate.sizeOf(response.skippedItems) == 2
