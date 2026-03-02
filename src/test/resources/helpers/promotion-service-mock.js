function fn() {
    var ArrayList = Java.type('java.util.ArrayList');
    var EnrichedTransaction = Java.type('io.promoengine.enrichment.EnrichedTransaction');
    var EnrichedItem = Java.type('io.promoengine.enrichment.EnrichedItem');
    var CalculationResult = Java.type('io.promoengine.engine.CalculationResult');
    var PromotionResult = Java.type('io.promoengine.engine.model.PromotionResult');
    var CalculateResponse = Java.type('io.promoengine.dto.response.CalculateResponse');
    var OrderSummary = Java.type('io.promoengine.dto.response.OrderSummary');
    var BigDecimal = Java.type('java.math.BigDecimal');
    var LocalDate = Java.type('java.time.LocalDate');
    var EnrichmentException = Java.type('io.promoengine.exception.EnrichmentException');
    var TenantNotFoundException = Java.type('io.promoengine.exception.TenantNotFoundException');

    var failEnrichment = false;
    var knownTenants = ['default'];

    var config = {
        withEnrichmentFailure: function() {
            failEnrichment = true;
            return config;
        },
        build: function() {
            return {
                calculate: function(req, tenantId) {
                    if (!knownTenants.includes(tenantId)) {
                        throw new TenantNotFoundException(tenantId);
                    }
                    if (failEnrichment) {
                        throw new EnrichmentException('Simulated enrichment failure', null);
                    }

                    var items = req.items || [];
                    var enrichedItems = new ArrayList();
                    var skippedItems = new ArrayList();
                    var hasUnknown = false;

                    for (var i = 0; i < items.length; i++) {
                        var sku = items[i].sku;
                        var qty = items[i].quantity || 1;
                        if (sku && sku.indexOf('UNKNOWN') >= 0) {
                            skippedItems.add(sku);
                            hasUnknown = true;
                        } else {
                            var price = BigDecimal.valueOf(100.00);
                            var item = EnrichedItem.builder().sku(sku).storeCode(req.storeId || '0101').unitPrice(price).lineAmount(price.multiply(BigDecimal.valueOf(qty))).quantity(qty).build();
                            enrichedItems.add(item);
                        }
                    }

                    var promoResults = new ArrayList();
                    var tx = EnrichedTransaction.builder().storeId(req.storeId || '0101').customerId(req.customerId || '').enrichedItems(enrichedItems).skippedItems(skippedItems).build();

                    var summary = OrderSummary.builder().subtotal(BigDecimal.valueOf(100.0)).totalDiscount(BigDecimal.valueOf(0.0)).total(BigDecimal.valueOf(100.0)).build();
                    var promoSummaries = new ArrayList();
                    var calcItems = new ArrayList();

                    var response = CalculateResponse.builder().requestId(req.requestId || 'mock-001').storeId(req.storeId || '0101').summary(summary).items(calcItems).promotions(promoSummaries).skippedItems(skippedItems).build();

                    var statusCode = hasUnknown ? 206 : 200;
                    return { statusCodeValue: statusCode, body: response };
                }
            };
        }
    };
    return config;
}
