function fn() {
    var Optional = Java.type('java.util.Optional');
    var ItemPropertyData = Java.type('io.promoengine.enrichment.ItemPropertyData');
    var HashMap = Java.type('java.util.HashMap');

    var items = new HashMap();

    var repo = {
        withItem: function(sku, categoryCode, subcategoryCode, departmentCode, foodItem) {
            var data = ItemPropertyData.builder().sku(sku).categoryCode(categoryCode).subcategoryCode(subcategoryCode).departmentCode(departmentCode).foodItem(foodItem).build();
            items.put(sku, data);
            return repo;
        },
        findProperties: function(sku) {
            var item = items.get(sku);
            if (item == null) {
                return Optional.empty();
            }
            return Optional.of(item);
        }
    };
    return repo;
}
