function fn() {
    var Optional = Java.type('java.util.Optional');
    var PriceData = Java.type('io.promoengine.enrichment.PriceData');
    var BigDecimal = Java.type('java.math.BigDecimal');
    var HashMap = Java.type('java.util.HashMap');

    var prices = new HashMap();

    var repo = {
        withPrice: function(sku, storeCode, price) {
            prices.put(sku + ':' + storeCode, BigDecimal.valueOf(price));
            return repo;
        },
        withSentinelPrice: function(sku, price) {
            prices.put(sku + ':999999', BigDecimal.valueOf(price));
            return repo;
        },
        withNoPrice: function(sku) {
            return repo;
        },
        findPrice: function(sku, storeCode) {
            var key = sku + ':' + storeCode;
            var price = prices.get(key);
            if (price == null) {
                var sentinelKey = sku + ':999999';
                price = prices.get(sentinelKey);
            }
            if (price == null) {
                return Optional.empty();
            }
            var data = PriceData.builder().sku(sku).storeCode(storeCode).unitPrice(price).build();
            return Optional.of(data);
        }
    };
    return repo;
}
