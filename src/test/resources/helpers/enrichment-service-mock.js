function fn() {
    var Optional = Java.type('java.util.Optional');
    var PriceData = Java.type('io.promoengine.enrichment.PriceData');
    var ItemPropertyData = Java.type('io.promoengine.enrichment.ItemPropertyData');
    var CustomerData = Java.type('io.promoengine.enrichment.CustomerData');
    var BigDecimal = Java.type('java.math.BigDecimal');
    var HashMap = Java.type('java.util.HashMap');
    var ArrayList = Java.type('java.util.ArrayList');
    var EnrichmentService = Java.type('io.promoengine.enrichment.EnrichmentService');
    var EnrichedTransaction = Java.type('io.promoengine.enrichment.EnrichedTransaction');
    var EnrichedItem = Java.type('io.promoengine.enrichment.EnrichedItem');

    var priceMap = new HashMap();
    var itemMap = new HashMap();
    var customerMap = new HashMap();
    var failEnrichment = false;

    var builder = {
        withPrices: function(prices) {
            for (var sku in prices) {
                priceMap.put(sku, BigDecimal.valueOf(prices[sku]));
            }
            return builder;
        },
        withNoPriceFor: function(sku) {
            return builder;
        },
        withSentinelPrice: function(sku, price) {
            priceMap.put(sku + ':sentinel', BigDecimal.valueOf(price));
            return builder;
        },
        withAllItems: function() {
            return builder;
        },
        withEnrichmentFailure: function() {
            failEnrichment = true;
            return builder;
        },
        build: function() {
            return {
                enrich: function(request) {
                    if (failEnrichment) {
                        var EnrichmentException = Java.type('io.promoengine.exception.EnrichmentException');
                        throw new EnrichmentException('Simulated enrichment failure', null);
                    }
                    var items = request.items || [];
                    var storeId = request.storeId;
                    var customerId = request.customerId;
                    var enrichedItems = new ArrayList();
                    var skippedItems = new ArrayList();

                    for (var i = 0; i < items.length; i++) {
                        var sku = items[i].sku;
                        var qty = items[i].quantity;
                        var price = priceMap.get(sku);
                        if (price == null) {
                            var sentinelPrice = priceMap.get(sku + ':sentinel');
                            price = sentinelPrice;
                        }
                        if (price != null) {
                            var lineAmount = price.multiply(BigDecimal.valueOf(qty));
                            var item = EnrichedItem.builder().sku(sku).storeCode(storeId).unitPrice(price).lineAmount(lineAmount).quantity(qty).build();
                            enrichedItems.add(item);
                        } else {
                            skippedItems.add(sku);
                        }
                    }
                    return EnrichedTransaction.builder().storeId(storeId).customerId(customerId).enrichedItems(enrichedItems).skippedItems(skippedItems).build();
                }
            };
        }
    };
    return builder;
}
