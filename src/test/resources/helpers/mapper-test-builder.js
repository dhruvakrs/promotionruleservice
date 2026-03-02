function fn() {
    var BigDecimal = Java.type('java.math.BigDecimal');
    var ArrayList = Java.type('java.util.ArrayList');
    var CalculationResult = Java.type('io.promoengine.engine.CalculationResult');
    var PromotionResult = Java.type('io.promoengine.engine.model.PromotionResult');
    var EnrichedTransaction = Java.type('io.promoengine.enrichment.EnrichedTransaction');
    var EnrichedItem = Java.type('io.promoengine.enrichment.EnrichedItem');
    var CalculateRequest = Java.type('io.promoengine.dto.request.CalculateRequest');
    var LocalDate = Java.type('java.time.LocalDate');

    var discountAmount = BigDecimal.valueOf(10.00);
    var subtotalAmount = BigDecimal.valueOf(100.00);
    var promotionType = 0;
    var skippedItems = new ArrayList();

    var builder = {
        withDiscount: function(amount) {
            discountAmount = BigDecimal.valueOf(amount);
            return builder;
        },
        withSubtotal: function(amount) {
            subtotalAmount = BigDecimal.valueOf(amount);
            return builder;
        },
        withStandardPromotion: function() {
            promotionType = 0;
            return builder;
        },
        withCouponPromotion: function() {
            promotionType = 10;
            return builder;
        },
        withSkippedItems: function(items) {
            skippedItems = new ArrayList();
            for (var i = 0; i < items.length; i++) {
                skippedItems.add(items[i]);
            }
            return builder;
        },
        build: function() {
            var promoResults = new ArrayList();
            var promo = PromotionResult.builder().promotionId('TEST-PROMO').promotionType(promotionType).description('Test').discountAmount(discountAmount).timesApplied(1.0).build();
            promoResults.add(promo);

            var enrichedItems = new ArrayList();
            var item = EnrichedItem.builder().sku('12345').storeCode('0101').unitPrice(subtotalAmount).lineAmount(subtotalAmount).quantity(1).build();
            enrichedItems.add(item);

            var calcResult = new CalculationResult(promoResults, skippedItems);

            var tx = EnrichedTransaction.builder().storeId('0101').customerId('CUST001').transactionDate(LocalDate.now()).enrichedItems(enrichedItems).skippedItems(skippedItems).build();

            var req = CalculateRequest.builder().requestId('test-001').storeId('0101').customerId('CUST001').build();

            return { calculationResult: calcResult, enrichedTransaction: tx, request: req };
        }
    };
    return builder;
}
